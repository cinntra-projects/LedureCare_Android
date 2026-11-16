package com.example.ledurecare.feature.dealers.ui

import com.example.ledurecare.core.common.AppError
import com.example.ledurecare.core.common.AppResult
import com.example.ledurecare.feature.dealers.domain.DEALER_PAGE_SIZE
import com.example.ledurecare.feature.dealers.domain.Dealer
import com.example.ledurecare.feature.dealers.domain.SearchDealersUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What a dealer list shows: the query, the pages fetched so far, and where loading stands. */
data class DealerSearchState(
    val query: String = "",
    val items: List<Dealer> = emptyList(),
    /** Dealers matching [query] on the server, across all pages. */
    val totalCount: Int = 0,
    /** The last page fetched; 0 before the first. */
    val page: Int = 0,
    val hasMore: Boolean = false,
    /** True while the first page of a search is on its way (including the short wait after typing). */
    val isLoading: Boolean = false,
    /** True while a further page is on its way. */
    val isLoadingMore: Boolean = false,
    val error: AppError? = null,
    /** False until a search has answered once, so "no dealers" is not shown before the first answer. */
    val hasLoaded: Boolean = false,
)

/**
 * A dealer list that searches and pages on the server. Typing waits [debounceMs] before asking, so
 * a word is searched once rather than once per letter, and an answer for an old query is dropped
 * if the query has changed since. [reset] starts afresh with no query.
 */
class DealerSearchController(
    private val searchDealers: SearchDealersUseCase,
    private val scope: CoroutineScope,
    private val debounceMs: Long = DEFAULT_DEBOUNCE_MS,
    private val pageSize: Int = DEALER_PAGE_SIZE,
) {
    private val _state = MutableStateFlow(DealerSearchState())
    val state: StateFlow<DealerSearchState> = _state.asStateFlow()

    private var job: Job? = null

    /** Clears the query and loads the first page. Call when a list or picker opens. */
    fun reset() {
        job?.cancel()
        _state.value = DealerSearchState(isLoading = true)
        job = scope.launch { fetch(page = 1, query = "") }
    }

    fun setQuery(query: String) {
        if (query == _state.value.query) return
        job?.cancel()
        _state.update {
            DealerSearchState(query = query, totalCount = 0, isLoading = true)
        }
        job = scope.launch {
            delay(debounceMs)
            fetch(page = 1, query = query)
        }
    }

    /** Fetches the next page, if there is one and nothing else is loading. */
    fun loadMore() {
        val s = _state.value
        if (!s.hasMore || s.isLoading || s.isLoadingMore || s.error != null) return
        job = scope.launch { fetch(page = s.page + 1, query = s.query) }
    }

    /** Tries again whatever failed: the first page, or the page after the last one fetched. */
    fun retry() {
        val s = _state.value
        if (s.isLoading || s.isLoadingMore) return
        job?.cancel()
        job = scope.launch { fetch(page = if (s.items.isEmpty()) 1 else s.page + 1, query = s.query) }
    }

    private suspend fun fetch(page: Int, query: String) {
        val appending = page > 1
        _state.update { it.copy(isLoading = !appending, isLoadingMore = appending, error = null) }
        when (val result = searchDealers(query, page, pageSize)) {
            is AppResult.Success -> _state.update { s ->
                val fresh = result.data.dealers
                s.copy(
                    items = if (appending) (s.items + fresh).distinctBy { it.id } else fresh,
                    totalCount = result.data.totalCount,
                    page = page,
                    hasMore = result.data.hasMore,
                    isLoading = false,
                    isLoadingMore = false,
                    hasLoaded = true,
                )
            }
            is AppResult.Failure -> _state.update {
                it.copy(isLoading = false, isLoadingMore = false, error = result.error, hasLoaded = true)
            }
        }
    }

    companion object {
        const val DEFAULT_DEBOUNCE_MS = 300L
    }
}
