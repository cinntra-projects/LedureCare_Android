package com.example.ledurecare.feature.dealers.data

import com.example.ledurecare.core.common.AppResult
import com.example.ledurecare.feature.dealers.domain.Dealer
import com.example.ledurecare.feature.dealers.domain.DealerDetailsRepository
import com.example.ledurecare.feature.dealers.domain.DealerPage
import com.example.ledurecare.feature.dealers.domain.DealerRepository
import com.example.ledurecare.feature.dealers.domain.matches
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/** Searches and pages the sample dealers the way the API does, while `BuildConfig.USE_STATIC_DEALERS` is on. */
@Singleton
class FakeDealerRepository @Inject constructor() : DealerRepository {

    override suspend fun searchDealers(query: String, page: Int, pageSize: Int): AppResult<DealerPage> {
        delay(200)
        val matching = StaticDealers.all.filter { it.matches(query) }
        val totalPages = ((matching.size + pageSize - 1) / pageSize).coerceAtLeast(1)
        val slice = matching.drop((page - 1) * pageSize).take(pageSize)
        return AppResult.Success(DealerPage(slice, page, totalPages, matching.size))
    }
}

/** Static mode: the sample dealers already carry their details. */
class FakeDealerDetailsRepository @Inject constructor() : DealerDetailsRepository {
    override suspend fun getDetails(dealer: Dealer): AppResult<Dealer> {
        delay(150)
        return AppResult.Success(StaticDealers.all.firstOrNull { it.id == dealer.id } ?: dealer)
    }
}
