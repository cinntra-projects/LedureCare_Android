package com.cinntra.ledurecare.feature.dealers.domain

import com.cinntra.ledurecare.core.common.AppResult

/** How many dealers one page holds. */
const val DEALER_PAGE_SIZE = 20

/** One page of a dealer search. [totalCount] is the number of dealers matching, across all pages. */
data class DealerPage(
    val dealers: List<Dealer>,
    val page: Int,
    val totalPages: Int,
    val totalCount: Int,
) {
    val hasMore: Boolean get() = page < totalPages
}

interface DealerRepository {
    /**
     * Dealers matching [query] (name, code or city; blank means everyone), one page at a time.
     * Pages start at 1.
     */
    suspend fun searchDealers(query: String, page: Int, pageSize: Int): AppResult<DealerPage>
}
