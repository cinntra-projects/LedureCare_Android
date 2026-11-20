package com.cinntra.ledurecare.feature.plan.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinntra.ledurecare.R
import com.cinntra.ledurecare.core.common.AppError
import com.cinntra.ledurecare.core.common.AppResult
import com.cinntra.ledurecare.feature.dealers.domain.Dealer
import com.cinntra.ledurecare.feature.dealers.domain.SearchDealersUseCase
import com.cinntra.ledurecare.feature.dealers.ui.DealerSearchController
import com.cinntra.ledurecare.feature.plan.domain.AddVisitsUseCase
import com.cinntra.ledurecare.feature.plan.domain.DateRange
import com.cinntra.ledurecare.feature.plan.domain.ObserveVisitsUseCase
import com.cinntra.ledurecare.feature.plan.domain.RefreshVisitsUseCase
import com.cinntra.ledurecare.feature.plan.domain.UpdateVisitUseCase
import com.cinntra.ledurecare.feature.plan.domain.VisitChange
import com.cinntra.ledurecare.feature.plan.domain.VisitStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlanViewModel @Inject constructor(
    observeVisits: ObserveVisitsUseCase,
    private val refreshVisits: RefreshVisitsUseCase,
    private val addVisits: AddVisitsUseCase,
    private val updateVisit: UpdateVisitUseCase,
    searchDealers: SearchDealersUseCase,
    clock: Clock,
) : ViewModel() {

    /** Everything the UI state needs that does not come from the plan data itself. */
    private data class Local(
        val view: PlanView = PlanView.Day,
        val selectedDate: LocalDate,
        val justAdded: Set<Long> = emptySet(),
        val sheet: AddSheetState? = null,
        val edit: EditSheetState? = null,
        val refreshing: Boolean = false,
    )

    private val today: LocalDate = LocalDate.now(clock)
    private val local = MutableStateFlow(Local(selectedDate = today))

    // Both sheets list dealers from the server, searched and paged. Only one sheet is open at a time,
    // so one search serves both; opening a sheet starts it afresh.
    private val dealerSearch = DealerSearchController(searchDealers, viewModelScope)

    private val _effects = Channel<PlanEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    // Day and Week need the whole week (the strip shows it); Month needs the whole month.
    private val visits = local
        .map { rangeFor(it.view, it.selectedDate) }
        .distinctUntilChanged()
        .flatMapLatest { observeVisits(it) }

    private val sheetVisits = local
        .map { it.sheet?.date }
        .distinctUntilChanged()
        .flatMapLatest { date ->
            if (date == null) flowOf(emptyList()) else observeVisits(DateRange(date, date))
        }

    val state: StateFlow<PlanUiState> = combine(local, visits, sheetVisits, dealerSearch.state) { l, v, sheetV, dealers ->
        PlanUiState(
            view = l.view,
            today = today,
            selectedDate = l.selectedDate,
            visitsByDate = v.groupBy { it.date },
            justAdded = l.justAdded,
            sheet = l.sheet?.copy(alreadyPlanned = sheetV.map { it.dealerId }.toSet()),
            edit = l.edit,
            dealerSearch = dealers,
            isRefreshing = l.refreshing,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PlanUiState(today = today, selectedDate = today),
    )

    init {
        // Whatever range is on screen is fetched from the server; the screen shows the local copy meanwhile.
        viewModelScope.launch {
            local.map { rangeFor(it.view, it.selectedDate) }.distinctUntilChanged().collectLatest { refresh(it) }
        }
        // The add sheet hides dealers already planned on its date, so that date must be up to date too.
        viewModelScope.launch {
            local.map { it.sheet?.date }.distinctUntilChanged().filterNotNull()
                .collectLatest { refresh(DateRange(it, it)) }
        }
    }

    fun onEvent(event: PlanEvent) {
        when (event) {
            is PlanEvent.ViewChanged -> local.update { it.copy(view = event.view) }
            PlanEvent.Previous -> step(-1)
            PlanEvent.Next -> step(1)
            is PlanEvent.DateSelected -> local.update { it.copy(selectedDate = event.date) }

            is PlanEvent.DealerQueryChanged -> dealerSearch.setQuery(event.query)
            PlanEvent.LoadMoreDealers -> dealerSearch.loadMore()
            PlanEvent.RetryDealers -> dealerSearch.retry()

            PlanEvent.OpenSheet -> openSheet()
            PlanEvent.CloseSheet -> local.update { it.copy(sheet = null) }
            is PlanEvent.SheetDateChanged -> updateSheet { it.copy(date = event.date) }
            is PlanEvent.ToggleDealer -> updateSheet {
                val id = event.dealer.id
                when {
                    id in it.selected -> it.copy(selected = it.selected - id)
                    it.selected.size >= MAX_DEALERS_PER_PLAN -> it
                    else -> it.copy(selected = it.selected + (id to event.dealer))
                }
            }
            PlanEvent.ConfirmAdd -> confirmAdd()

            is PlanEvent.OpenEdit -> openEdit(event.visitId)
            PlanEvent.CloseEdit -> local.update { it.copy(edit = null) }
            is PlanEvent.EditDateChanged -> updateEdit { it.copy(date = event.date) }
            is PlanEvent.EditDealerSelected -> updateEdit { it.copy(dealer = event.dealer) }
            PlanEvent.ConfirmEdit -> confirmEdit()

            is PlanEvent.StartAudit -> startAudit(event.visitId)
            is PlanEvent.ViewDli -> viewDli(event.visitId)
        }
    }

    private suspend fun refresh(range: DateRange) {
        local.update { it.copy(refreshing = true) }
        try {
            // The list loads by itself, so a "no permission" answer (403) is not worth a toast: the screen
            // just stays as it is. Other failures (offline, server errors) are still reported.
            (refreshVisits(range) as? AppResult.Failure)
                ?.takeUnless { (it.error as? AppError.Http)?.code == 403 }
                ?.let { _effects.send(PlanEffect.ShowError(it.error)) }
        } finally {
            local.update { it.copy(refreshing = false) }
        }
    }

    private fun step(direction: Long) = local.update {
        val date = when (it.view) {
            PlanView.Day -> it.selectedDate.plusDays(direction)
            PlanView.Week -> it.selectedDate.plusWeeks(direction)
            PlanView.Month -> it.selectedDate.plusMonths(direction)
        }
        it.copy(selectedDate = date)
    }

    private fun openSheet() {
        local.update { it.copy(sheet = AddSheetState(date = it.selectedDate)) }
        dealerSearch.reset()
    }

    private fun findVisit(visitId: Long) = state.value.visitsByDate.values.flatten().firstOrNull { it.id == visitId }

    /** Only a planned visit can be audited; a completed one has its DLI already. */
    private fun startAudit(visitId: Long) {
        if (findVisit(visitId)?.status == VisitStatus.Done) return viewDli(visitId)
        viewModelScope.launch { _effects.send(PlanEffect.OpenAudit(visitId)) }
    }

    private fun viewDli(visitId: Long) {
        val visit = findVisit(visitId) ?: return
        viewModelScope.launch {
            // A plan completed in this session was marked done locally and has no DLI id yet:
            // ask the server for it again before giving up.
            if (visit.returnId == null) refreshVisits(DateRange(visit.date, visit.date))
            val returnId = findVisit(visitId)?.returnId
            if (returnId == null) message(R.string.plan_audit_unavailable)
            else _effects.send(PlanEffect.OpenDli(returnId))
        }
    }

    private fun openEdit(visitId: Long) {
        val visit = findVisit(visitId)?.takeIf { it.status == VisitStatus.Planned } ?: return
        val edit = EditSheetState(visit = visit, date = visit.date, dealer = Dealer(visit.dealerId, visit.dealerName, visit.bpCode, visit.city))
        local.update { it.copy(edit = edit) }
        dealerSearch.reset()
    }

    private fun confirmAdd() {
        val sheet = local.value.sheet ?: return
        if (sheet.isSaving) return
        val chosen = sheet.selected.values.filter { it.id !in sheet.alreadyPlanned }
        if (chosen.isEmpty()) return
        updateSheet { it.copy(isSaving = true) }
        viewModelScope.launch {
            when (val result = addVisits(sheet.date, chosen)) {
                is AppResult.Success -> {
                    local.update {
                        it.copy(sheet = null, selectedDate = sheet.date, justAdded = result.data.createdIds.toSet())
                    }
                    _effects.send(PlanEffect.PlansAdded(result.data.createdIds.size, result.data.skipped.size))
                }
                is AppResult.Failure -> {
                    updateSheet { it.copy(isSaving = false) }
                    _effects.send(PlanEffect.ShowError(result.error))
                }
            }
        }
    }

    private fun confirmEdit() {
        val edit = local.value.edit ?: return
        if (edit.isSaving) return
        if (!edit.hasChanges) {
            local.update { it.copy(edit = null) }
            return
        }
        val change = VisitChange(
            date = edit.date.takeIf { it != edit.visit.date },
            dealer = edit.dealer.takeIf { it.id != edit.visit.dealerId },
        )

        updateEdit { it.copy(isSaving = true) }
        viewModelScope.launch {
            when (val result = updateVisit(edit.visit.id, change)) {
                is AppResult.Success -> {
                    // Follow a rescheduled plan to its new date so the change is visible.
                    local.update { it.copy(edit = null, selectedDate = if (change.date != null) edit.date else it.selectedDate) }
                    _effects.send(PlanEffect.VisitUpdated)
                }
                is AppResult.Failure -> {
                    updateEdit { it.copy(isSaving = false) }
                    _effects.send(PlanEffect.ShowError(result.error))
                }
            }
        }
    }

    private fun updateSheet(transform: (AddSheetState) -> AddSheetState) =
        local.update { l -> l.copy(sheet = l.sheet?.let(transform)) }

    private fun updateEdit(transform: (EditSheetState) -> EditSheetState) =
        local.update { l -> l.copy(edit = l.edit?.let(transform)) }

    private fun message(res: Int) {
        viewModelScope.launch { _effects.send(PlanEffect.ShowMessage(res)) }
    }

    private fun rangeFor(view: PlanView, date: LocalDate): DateRange = when (view) {
        PlanView.Day, PlanView.Week -> weekRange(date)
        PlanView.Month -> monthRange(YearMonth.from(date))
    }
}
