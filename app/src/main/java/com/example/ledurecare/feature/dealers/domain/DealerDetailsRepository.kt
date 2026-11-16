package com.example.ledurecare.feature.dealers.domain

import com.example.ledurecare.core.common.AppResult
import javax.inject.Inject

/** Looks one dealer up by its code, for the details the search list does not carry (address, sales person, zone). */
interface DealerDetailsRepository {
    /** The dealer with [Dealer.address], [Dealer.salesPerson] and [Dealer.zone] filled in as far as known. */
    suspend fun getDetails(dealer: Dealer): AppResult<Dealer>
}

class GetDealerDetailsUseCase @Inject constructor(
    private val repository: DealerDetailsRepository,
) {
    suspend operator fun invoke(dealer: Dealer): AppResult<Dealer> = repository.getDetails(dealer)
}
