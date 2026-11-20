package com.cinntra.ledurecare.feature.reports.domain

import com.cinntra.ledurecare.core.common.AppResult
import java.time.LocalDate

/** How many DLIs one page of the list asks the server for. */
const val DLI_PAGE_SIZE = 10

/** One page of the DLI list, plus where it sits among the pages. */
data class DliPage(
    val items: List<Dli>,
    val page: Int,
    val totalPages: Int,
    val totalCount: Int,
) {
    val hasMore: Boolean get() = page < totalPages
}

/**
 * The "new returns / old returns" choice of the list's menu. It is the server's `sort` query: old returns
 * are `oldest_update`; `newest_update` for new returns is assumed. With neither, no `sort` is sent.
 */
enum class ReturnView(val sort: String?) {
    All(null),
    New("newest_update"),
    Old("oldest_update"),
}

/** What the server is asked to narrow the DLI list by: new or old returns, and an audit-date range. */
data class DliFilter(
    val view: ReturnView = ReturnView.All,
    val from: LocalDate? = null,
    val to: LocalDate? = null,
) {
    val hasDates: Boolean get() = from != null
    val isActive: Boolean get() = view != ReturnView.All || hasDates
}

interface DliRepository {
    /** Submitted (non-draft) DLIs visible to the signed-in user, [pageSize] at a time, starting at page 1. */
    suspend fun getDliPage(page: Int, pageSize: Int, filter: DliFilter = DliFilter()): AppResult<DliPage>

    suspend fun getDli(id: Int): AppResult<DliDetail>
}
