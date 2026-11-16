package com.example.ledurecare.feature.reports.domain

import com.example.ledurecare.core.common.AppResult

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

interface DliRepository {
    /** Submitted (non-draft) DLIs visible to the signed-in user, [pageSize] at a time, starting at page 1. */
    suspend fun getDliPage(page: Int, pageSize: Int): AppResult<DliPage>

    suspend fun getDli(id: Int): AppResult<DliDetail>
}
