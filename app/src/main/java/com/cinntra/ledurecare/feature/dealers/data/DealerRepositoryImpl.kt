package com.cinntra.ledurecare.feature.dealers.data

import com.cinntra.ledurecare.core.common.AppResult
import com.cinntra.ledurecare.core.common.IoDispatcher
import com.cinntra.ledurecare.core.common.safeCall
import com.cinntra.ledurecare.core.network.findArray
import com.cinntra.ledurecare.core.network.unwrapObject
import com.cinntra.ledurecare.feature.dealers.domain.Dealer
import com.cinntra.ledurecare.feature.dealers.domain.DealerPage
import com.cinntra.ledurecare.feature.dealers.domain.DealerRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import javax.inject.Inject

/** One entry of `data.results` in `GET api/dealers/bp-lookup/`. */
@Serializable
data class DealerDto(
    @SerialName("card_code") val cardCode: String? = null,
    val name: String? = null,
    val city: String? = null,
)

class DealerRepositoryImpl @Inject constructor(
    private val api: DealerApi,
    private val json: Json,
    @IoDispatcher private val io: CoroutineDispatcher,
) : DealerRepository {

    override suspend fun searchDealers(query: String, page: Int, pageSize: Int): AppResult<DealerPage> = safeCall(io) {
        val body = api.searchDealers(query.trim().takeIf { it.isNotEmpty() }, page, pageSize)
        body.toDealerPage(json, requestedPage = page)
    }
}

/**
 * Reads a bp-lookup answer. A paged answer carries `total_pages` and `count`; a bare list (a server
 * that ignores the page parameters) counts as one page holding everything.
 */
internal fun JsonElement.toDealerPage(json: Json, requestedPage: Int): DealerPage {
    val dealers = findArray().mapNotNull {
        runCatching { json.decodeFromJsonElement(DealerDto.serializer(), it) }.getOrNull()?.toDomain()
    }
    val paging = unwrapObject()
    fun number(key: String) = (paging?.get(key) as? JsonPrimitive)?.intOrNull
    return DealerPage(
        dealers = dealers,
        page = number("current_page") ?: requestedPage,
        totalPages = (number("total_pages") ?: 1).coerceAtLeast(1),
        totalCount = number("count") ?: dealers.size,
    )
}

/** An entry without a `card_code` cannot be told apart from others, so it is skipped. */
internal fun DealerDto.toDomain(): Dealer? {
    val code = cardCode?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return Dealer(
        id = code,
        name = name?.trim().orEmpty().ifEmpty { code },
        bpCode = code,
        city = city?.trim().orEmpty(),
    )
}
