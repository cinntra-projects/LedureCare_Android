package com.cinntra.ledurecare.feature.dealers.domain

import com.cinntra.ledurecare.core.common.AppResult
import javax.inject.Inject

class SearchDealersUseCase @Inject constructor(
    private val repository: DealerRepository,
) {
    suspend operator fun invoke(
        query: String,
        page: Int = 1,
        pageSize: Int = DEALER_PAGE_SIZE,
    ): AppResult<DealerPage> = repository.searchDealers(query.trim(), page, pageSize)
}
