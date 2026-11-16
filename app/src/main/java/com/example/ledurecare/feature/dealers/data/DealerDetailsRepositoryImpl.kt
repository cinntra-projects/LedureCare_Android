package com.example.ledurecare.feature.dealers.data

import com.example.ledurecare.core.common.AppError
import com.example.ledurecare.core.common.AppResult
import com.example.ledurecare.core.common.IoDispatcher
import com.example.ledurecare.core.common.safeCall
import com.example.ledurecare.core.network.findArray
import com.example.ledurecare.feature.dealers.domain.Dealer
import com.example.ledurecare.feature.dealers.domain.DealerDetailsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import javax.inject.Inject

@Serializable
data class SalesPersonDto(val SalesEmployeeName: String? = null)

@Serializable
data class BpAddressDto(
    val AddressType: String? = null,
    val Street: String? = null,
    val Block: String? = null,
    val City: String? = null,
    val State: String? = null,
    val ZipCode: String? = null,
)

/** The parts of one `data` entry of `businesspartner/one` the app uses. */
@Serializable
data class BusinessPartnerDto(
    val CardCode: String? = null,
    val CardName: String? = null,
    val SalesPersonCode: List<SalesPersonDto> = emptyList(),
    val BPAddresses: List<BpAddressDto> = emptyList(),
    val U_U_UTL_Zone: String? = null,
)

class DealerDetailsRepositoryImpl @Inject constructor(
    private val api: DealerDetailsApi,
    private val json: Json,
    @IoDispatcher private val io: CoroutineDispatcher,
) : DealerDetailsRepository {

    override suspend fun getDetails(dealer: Dealer): AppResult<Dealer> {
        val result = safeCall(io) { api.getOne(BusinessPartnerRequest(dealer.bpCode)) }
        return when (result) {
            is AppResult.Failure -> result
            is AppResult.Success -> result.data.toDealerDetails(json, dealer)
                ?.let { AppResult.Success(it) }
                ?: AppResult.Failure(AppError.Http(404, "Dealer details not found."))
        }
    }
}

/** Fills [dealer] from the first business partner in the answer, or null if the answer has none. */
internal fun JsonElement.toDealerDetails(json: Json, dealer: Dealer): Dealer? {
    val partner = findArray().firstNotNullOfOrNull {
        runCatching { json.decodeFromJsonElement(BusinessPartnerDto.serializer(), it) }.getOrNull()
    } ?: return null
    return dealer.copy(
        address = partner.BPAddresses.bestAddress()?.format() ?: dealer.address,
        salesPerson = partner.SalesPersonCode.firstNotNullOfOrNull { it.SalesEmployeeName.clean() } ?: dealer.salesPerson,
        zone = partner.U_U_UTL_Zone.clean() ?: dealer.zone,
    )
}

/** The billing address is the dealer's registered one; otherwise whatever address comes first. */
private fun List<BpAddressDto>.bestAddress(): BpAddressDto? =
    firstOrNull { it.AddressType == "bo_BillTo" && it.format().isNotEmpty() }
        ?: firstOrNull { it.format().isNotEmpty() }

/** The server writes missing values as the text "None" or an empty string. */
private fun String?.clean(): String? = this?.trim()?.takeIf { it.isNotEmpty() && !it.equals("none", ignoreCase = true) }

private fun BpAddressDto.format(): String =
    listOf(Block, Street, City, State, ZipCode).mapNotNull { it.clean() }.distinct().joinToString(", ")
