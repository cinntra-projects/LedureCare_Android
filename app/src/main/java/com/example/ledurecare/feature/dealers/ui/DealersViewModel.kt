package com.example.ledurecare.feature.dealers.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ledurecare.feature.dealers.domain.Dealer
import com.example.ledurecare.feature.dealers.domain.SearchDealersUseCase
import com.example.ledurecare.feature.plan.domain.DateRange
import com.example.ledurecare.feature.plan.domain.ObserveVisitsUseCase
import com.example.ledurecare.feature.plan.domain.PlannedVisit
import com.example.ledurecare.feature.plan.domain.RefreshVisitsUseCase
import com.example.ledurecare.feature.plan.domain.VisitStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

data class DealerRowUi(
    val dealer: Dealer,
    /** Latest finished audit, or null if there has not been one. */
    val lastAudit: LocalDate?,
    /** Next upcoming planned visit, or null if none. */
    val planned: LocalDate?,
)

data class DealersUiState(
    val today: LocalDate,
    /** The search: its query, how many dealers match in all, and where loading stands. */
    val search: DealerSearchState = DealerSearchState(isLoading = true),
    /** The dealers loaded so far, each with its plan information. */
    val rows: List<DealerRowUi> = emptyList(),
)

sealed interface DealersEvent {
    data class QueryChanged(val query: String) : DealersEvent
    data object LoadMore : DealersEvent
    data object Retry : DealersEvent
}

@HiltViewModel
class DealersViewModel @Inject constructor(
    searchDealers: SearchDealersUseCase,
    observeVisits: ObserveVisitsUseCase,
    private val refreshVisits: RefreshVisitsUseCase,
    clock: Clock,
) : ViewModel() {

    private val today = LocalDate.now(clock)
    private val dealerSearch = DealerSearchController(searchDealers, viewModelScope)

    // Wide enough to cover every audit and plan that matters for "last audit" / "planned".
    private val visitRange = DateRange(today.minusYears(1), today.plusYears(1))
    private val visits = observeVisits(visitRange)

    val state: StateFlow<DealersUiState> = combine(dealerSearch.state, visits) { search, v ->
        DealersUiState(today = today, search = search, rows = rows(search.items, v))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DealersUiState(today = today))

    init {
        dealerSearch.reset()
        // The "Planned" chips come from the plan. If it cannot be fetched they are simply absent,
        // and the dealer list itself is still shown, so a failure here is not reported.
        viewModelScope.launch { refreshVisits(visitRange) }
    }

    fun onEvent(event: DealersEvent) {
        when (event) {
            is DealersEvent.QueryChanged -> dealerSearch.setQuery(event.query)
            DealersEvent.LoadMore -> dealerSearch.loadMore()
            DealersEvent.Retry -> dealerSearch.retry()
        }
    }

    private fun rows(dealers: List<Dealer>, visits: List<PlannedVisit>): List<DealerRowUi> {
        val byDealer = visits.groupBy { it.dealerId }
        return dealers.map { dealer ->
            val own = byDealer[dealer.id].orEmpty()
            DealerRowUi(
                dealer = dealer,
                lastAudit = own.filter { it.status == VisitStatus.Done }.maxOfOrNull { it.date },
                planned = own.filter { it.status == VisitStatus.Planned && !it.date.isBefore(today) }
                    .minOfOrNull { it.date },
            )
        }
    }
}
