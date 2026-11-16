package com.example.ledurecare.feature.reports.data

import com.example.ledurecare.core.common.AppError
import com.example.ledurecare.core.common.AppResult
import com.example.ledurecare.core.common.IoDispatcher
import com.example.ledurecare.core.common.safeCall
import com.example.ledurecare.core.network.findArray
import com.example.ledurecare.core.network.unwrapObject
import com.example.ledurecare.feature.reports.domain.Dli
import com.example.ledurecare.feature.reports.domain.DliDetail
import com.example.ledurecare.feature.reports.domain.DliRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json
import com.example.ledurecare.feature.reports.domain.DliPage
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import java.util.concurrent.ConcurrentHashMap
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

class DliRepositoryImpl @Inject constructor(
    private val api: ReturnsApi,
    private val json: Json,
    private val clock: Clock,
    @IoDispatcher private val io: CoroutineDispatcher,
) : DliRepository {

    /** Who audited each DLI seen in the list, because the detail answer does not say. */
    private val auditedBy = ConcurrentHashMap<Int, String>()

    override suspend fun getDliPage(page: Int, pageSize: Int): AppResult<DliPage> = safeCall(io) {
        val today = LocalDate.now(clock)
        val body = api.getReturns(view = "all", page = page, pageSize = pageSize)
        val items = body.findArray().mapNotNull {
            runCatching { json.decodeFromJsonElement(ReturnListDto.serializer(), it) }.getOrNull()?.toDomain(today)
        }
        items.forEach { dli -> dli.auditedBy?.let { auditedBy[dli.id] = it } }
        val paging = body.unwrapObject()
        fun number(key: String) = (paging?.get(key) as? JsonPrimitive)?.intOrNull
        DliPage(
            items = items,
            page = number("current_page") ?: page,
            totalPages = (number("total_pages") ?: 1).coerceAtLeast(1),
            totalCount = number("count") ?: items.size,
        )
    }

    override suspend fun getDli(id: Int): AppResult<DliDetail> = safeCall(io) {
        val detail = json.decodeFromJsonElement(
            ReturnDetailDto.serializer(),
            requireNotNull(api.getReturn(id).unwrapObject()) { "Unexpected DLI response" },
        )
        // The answer carries its own tracker: every stage reached, with the date it was entered.
        val changes = detail.tracker.mapNotNull { it.toChange() }
        detail.toDomain(json, changes, auditedBy[id], LocalDate.now(clock))
            ?: throw IllegalStateException("Unknown DLI status")
    }
}
