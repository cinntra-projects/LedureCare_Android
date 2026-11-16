package com.example.ledurecare.feature.plan.ui

import androidx.annotation.StringRes
import com.example.ledurecare.core.common.AppError
import com.example.ledurecare.feature.dealers.domain.Dealer
import com.example.ledurecare.feature.dealers.ui.DealerSearchState
import com.example.ledurecare.feature.plan.domain.PlannedVisit
import com.example.ledurecare.feature.plan.domain.VisitStatus
import java.time.LocalDate
import java.time.YearMonth

/** The server accepts one to 100 dealers per request. */
const val MAX_DEALERS_PER_PLAN = 100

enum class PlanView { Day, Week, Month }

data class AddSheetState(
    val date: LocalDate,
    /**
     * The dealers ticked so far, by id. Whole dealers are kept, not just ids, because a new search
     * replaces the visible list and a ticked dealer must still be sent when it is no longer on screen.
     */
    val selected: Map<String, Dealer> = emptyMap(),
    /** Dealers already planned on [date]; they are not offered again. */
    val alreadyPlanned: Set<String> = emptySet(),
    val isSaving: Boolean = false,
) {
    val selectedCount: Int get() = selected.keys.count { it !in alreadyPlanned }
}

/** Editing one plan: a new date, a different dealer, or both. */
data class EditSheetState(
    val visit: PlannedVisit,
    val date: LocalDate,
    /** The dealer chosen for the plan; the plan's own dealer until another is picked. */
    val dealer: Dealer,
    val isSaving: Boolean = false,
) {
    val hasChanges: Boolean get() = date != visit.date || dealer.id != visit.dealerId

    /** The plan's own dealer, as the plan describes it. */
    val currentDealer: Dealer get() = Dealer(visit.dealerId, visit.dealerName, visit.bpCode, visit.city)
}

data class PlanUiState(
    val view: PlanView = PlanView.Day,
    val today: LocalDate,
    val selectedDate: LocalDate,
    val visitsByDate: Map<LocalDate, List<PlannedVisit>> = emptyMap(),
    /** Ids of visits added in this session; highlighted with a green border. */
    val justAdded: Set<Long> = emptySet(),
    val sheet: AddSheetState? = null,
    val edit: EditSheetState? = null,
    /** The dealer search both sheets list from: query, pages loaded, loading state. */
    val dealerSearch: DealerSearchState = DealerSearchState(),
    /** True while plans are being fetched from the server. */
    val isRefreshing: Boolean = false,
) {
    val month: YearMonth get() = YearMonth.from(selectedDate)

    fun visitsOn(date: LocalDate): List<PlannedVisit> = visitsByDate[date].orEmpty()

    fun visitsIn(dates: Collection<LocalDate>): List<PlannedVisit> = dates.flatMap(::visitsOn)

    /** The first still-planned visit today; it gets the highlighted "Start" treatment. */
    val upNextId: Long?
        get() = visitsOn(today).firstOrNull { it.status == VisitStatus.Planned }?.id
}

sealed interface PlanEvent {
    data class ViewChanged(val view: PlanView) : PlanEvent
    data object Previous : PlanEvent
    data object Next : PlanEvent
    data class DateSelected(val date: LocalDate) : PlanEvent

    // The dealer list shown in either sheet
    data class DealerQueryChanged(val query: String) : PlanEvent
    data object LoadMoreDealers : PlanEvent
    data object RetryDealers : PlanEvent

    // Add dealers
    data object OpenSheet : PlanEvent
    data object CloseSheet : PlanEvent
    data class SheetDateChanged(val date: LocalDate) : PlanEvent
    data class ToggleDealer(val dealer: Dealer) : PlanEvent
    data object ConfirmAdd : PlanEvent

    // Edit a plan
    data class OpenEdit(val visitId: Long) : PlanEvent
    data object CloseEdit : PlanEvent
    data class EditDateChanged(val date: LocalDate) : PlanEvent
    data class EditDealerSelected(val dealer: Dealer) : PlanEvent
    data object ConfirmEdit : PlanEvent

    data class StartAudit(val visitId: Long) : PlanEvent
    data class ViewDli(val visitId: Long) : PlanEvent
}

sealed interface PlanEffect {
    data class OpenAudit(val visitId: Long) : PlanEffect

    /** Show the submitted (read-only) DLI of a completed plan. */
    data class OpenDli(val returnId: Int) : PlanEffect
    data class ShowMessage(@StringRes val messageRes: Int) : PlanEffect
    data class ShowError(val error: AppError) : PlanEffect

    /** [created] dealers were planned; [skipped] were left out, for example because they were already planned. */
    data class PlansAdded(val created: Int, val skipped: Int) : PlanEffect
    data object VisitUpdated : PlanEffect
}
