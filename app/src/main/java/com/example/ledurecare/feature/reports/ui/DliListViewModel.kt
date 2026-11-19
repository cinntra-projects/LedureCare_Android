package com.example.ledurecare.feature.reports.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ledurecare.core.common.AppError
import com.example.ledurecare.core.common.AppResult
import com.example.ledurecare.feature.auth.domain.AuthRepository
import com.example.ledurecare.feature.reports.domain.Dli
import com.example.ledurecare.feature.reports.domain.DliFilter
import com.example.ledurecare.feature.reports.domain.ReturnView
import java.time.LocalDate
import com.example.ledurecare.feature.reports.domain.GetDlisUseCase
import com.example.ledurecare.feature.reports.domain.ReturnStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** `role.code` of a service engineer in the login answer. */
internal const val SERVICE_ENGINEER_ROLE = "service_engineer"

data class DliListUiState(
    val isLoading: Boolean = true,
    val error: AppError? = null,
    /** Every DLI loaded so far, in the order the server sent them. */
    val all: List<Dli> = emptyList(),
    val query: String = "",
    val stage: ReturnStatus? = null,
    val focusSearch: Boolean = false,
    val page: Int = 0,
    val hasMore: Boolean = false,
    val totalCount: Int = 0,
    val isLoadingMore: Boolean = false,
    /** A later page failed; the rows already shown stay and Retry asks for that page again. */
    val loadMoreError: AppError? = null,
    /** The signed-in user is a service engineer, the only role that may edit a DLI. */
    val isServiceEngineer: Boolean = false,
    /** What the server is asked for: new or old returns, and an audit-date range. */
    val filter: DliFilter = DliFilter(),
) {
    /** Edit shows only for a service engineer, and only on a DLI the server marks editable. */
    fun canEdit(dli: Dli): Boolean = isServiceEngineer && dli.isEditable

    val isFiltering: Boolean get() = stage != null || query.isNotBlank()

    /** The loaded DLIs that match the stage and search text. */
    val visible: List<Dli>
        get() = all
            .filter { stage == null || it.status == stage }
            .filter { it.matches(query) }
}

sealed interface DliListEvent {
    data class QueryChanged(val query: String) : DliListEvent
    data class StageSelected(val stage: ReturnStatus?) : DliListEvent
    /** Menu choice; choosing the active one again clears it. */
    data class ViewSelected(val view: ReturnView) : DliListEvent
    data class DateRangeSelected(val from: LocalDate, val to: LocalDate) : DliListEvent
    data object DateRangeCleared : DliListEvent
    data object LoadMore : DliListEvent
    data object Retry : DliListEvent
}

@HiltViewModel
class DliListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getDlis: GetDlisUseCase,
    authRepository: AuthRepository,
) : ViewModel() {

    // Navigation arguments, stored under their property names (see DliListRoute).
    private val _state = MutableStateFlow(
        DliListUiState(
            stage = ReturnStatus.fromCode(savedStateHandle.get<String>("stage")),
            focusSearch = savedStateHandle.get<Boolean>("focusSearch") ?: false,
        )
    )
    val state: StateFlow<DliListUiState> = _state.asStateFlow()

    private var job: Job? = null

    init {
        // The role comes from the login answer (`role.code`), stored with the user.
        viewModelScope.launch {
            authRepository.observeUser().collect { user ->
                _state.update { it.copy(isServiceEngineer = user?.roleCode == SERVICE_ENGINEER_ROLE) }
            }
        }
        load()
    }

    fun onEvent(event: DliListEvent) {
        when (event) {
            is DliListEvent.QueryChanged -> {
                _state.update { it.copy(query = event.query) }
                fillForFilter()
            }
            is DliListEvent.StageSelected -> {
                _state.update { it.copy(stage = event.stage) }
                fillForFilter()
            }
            is DliListEvent.ViewSelected -> changeFilter {
                it.copy(view = if (it.view == event.view) ReturnView.All else event.view)
            }
            is DliListEvent.DateRangeSelected -> changeFilter { it.copy(from = event.from, to = event.to) }
            DliListEvent.DateRangeCleared -> changeFilter { it.copy(from = null, to = null) }
            DliListEvent.LoadMore -> loadMore()
            DliListEvent.Retry -> if (_state.value.all.isEmpty()) load() else loadMore(retry = true)
        }
    }

    /** A new server filter starts the list over from page 1. */
    private fun changeFilter(change: (DliFilter) -> DliFilter) {
        _state.update { it.copy(filter = change(it.filter), all = emptyList(), page = 0, hasMore = false, isLoadingMore = false) }
        load()
    }

    private fun load() {
        job?.cancel()
        _state.update { it.copy(isLoading = true, error = null, loadMoreError = null) }
        job = viewModelScope.launch {
            when (val result = getDlis(page = 1, filter = _state.value.filter)) {
                is AppResult.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            all = result.data.items,
                            page = result.data.page,
                            hasMore = result.data.hasMore,
                            totalCount = result.data.totalCount,
                        )
                    }
                    fillForFilter()
                }
                is AppResult.Failure -> _state.update { it.copy(isLoading = false, error = result.error) }
            }
        }
    }

    private fun loadMore(retry: Boolean = false) {
        val s = _state.value
        if (!s.hasMore || s.isLoading || s.isLoadingMore) return
        if (s.loadMoreError != null && !retry) return // wait for Retry, so a failing page is not hammered
        _state.update { it.copy(isLoadingMore = true, loadMoreError = null) }
        job = viewModelScope.launch {
            when (val result = getDlis(page = s.page + 1, filter = s.filter)) {
                is AppResult.Success -> {
                    _state.update {
                        it.copy(
                            isLoadingMore = false,
                            all = (it.all + result.data.items).distinctBy(Dli::id),
                            page = result.data.page,
                            hasMore = result.data.hasMore,
                            totalCount = result.data.totalCount,
                        )
                    }
                    fillForFilter()
                }
                is AppResult.Failure -> _state.update { it.copy(isLoadingMore = false, loadMoreError = result.error) }
            }
        }
    }

    /**
     * The stage and search filters work on the pages loaded so far, so with a filter on, the remaining
     * pages are fetched too; otherwise a match on a page not yet loaded would be missed.
     */
    private fun fillForFilter() {
        if (_state.value.isFiltering) loadMore()
    }
}
