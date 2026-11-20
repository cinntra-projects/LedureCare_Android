package com.cinntra.ledurecare.feature.plan.data

import com.cinntra.ledurecare.core.common.AppResult
import com.cinntra.ledurecare.core.common.IoDispatcher
import com.cinntra.ledurecare.core.common.safeCall
import com.cinntra.ledurecare.core.network.findArray
import com.cinntra.ledurecare.core.network.unwrapObject
import com.cinntra.ledurecare.feature.dealers.domain.Dealer
import com.cinntra.ledurecare.feature.plan.domain.AddVisitsResult
import com.cinntra.ledurecare.feature.plan.domain.DateRange
import com.cinntra.ledurecare.feature.plan.domain.PlanRepository
import com.cinntra.ledurecare.feature.plan.domain.PlannedVisit
import com.cinntra.ledurecare.feature.plan.domain.SkippedDealer
import com.cinntra.ledurecare.feature.plan.domain.VisitChange
import com.cinntra.ledurecare.feature.plan.domain.VisitStatus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The plan API (`api/plans/`). The server keeps no "audit done" state for a plan, so that is
 * remembered here, for the life of the app only.
 */
@Singleton
class RemotePlanRepository @Inject constructor(
    private val api: PlanApi,
    private val json: Json,
    @IoDispatcher private val io: CoroutineDispatcher,
) : PlanRepository {

    private val cache = MutableStateFlow<Map<Long, PlannedVisit>>(emptyMap())
    private val doneIds: MutableSet<Long> = ConcurrentHashMap.newKeySet()

    override fun observeVisits(range: DateRange): Flow<List<PlannedVisit>> =
        cache.map { all ->
            all.values
                .filter { it.date >= range.start && it.date <= range.end }
                .sortedWith(compareBy({ it.date }, { it.id }))
        }.distinctUntilChanged()

    override suspend fun refresh(range: DateRange): AppResult<Unit> = safeCall(io) {
        val fetched = fetch(range)
        // Replace everything the app knew inside the range, so plans removed or moved elsewhere vanish.
        cache.update { current ->
            current.filterValues { it.date < range.start || it.date > range.end } + fetched.associateBy { it.id }
        }
    }

    override suspend fun addVisits(date: LocalDate, dealers: List<Dealer>): AppResult<AddVisitsResult> = safeCall(io) {
        val request = CreatePlansRequestDto(
            planDate = date.toString(),
            dealers = dealers.map { PlanDealerDto(it.id, it.name, it.city.takeIf(String::isNotBlank)) },
        )
        val data = requireNotNull(api.createPlans(request).unwrapObject()) { "Unexpected plan response" }
        val created = (data["created"] as? JsonArray).orEmpty().mapNotNull { it.toVisit() }
        val skipped = (data["skipped"] as? JsonArray).orEmpty().mapNotNull { element ->
            runCatching { json.decodeFromJsonElement(SkippedPlanDto.serializer(), element) }.getOrNull()
        }.map { SkippedDealer(it.dealerCode.orEmpty(), it.reason.orEmpty()) }

        cache.update { it + created.associateBy { visit -> visit.id } }
        AddVisitsResult(createdIds = created.map { it.id }, skipped = skipped)
    }

    override suspend fun updateVisit(id: Long, change: VisitChange): AppResult<Unit> = safeCall(io) {
        val request = UpdatePlanRequestDto(
            planDate = change.date?.toString(),
            dealerCode = change.dealer?.id,
            dealerName = change.dealer?.name,
            // A new dealer brings its own city, even if blank, so the old dealer's city does not linger.
            city = change.dealer?.city,
        )
        val updated = api.updatePlan(id, request).unwrapObject()?.toVisit()
        cache.update { current ->
            // Prefer the server's answer; if it was unreadable, apply the requested change as sent.
            val next = updated ?: current[id]?.let { old ->
                old.copy(
                    date = change.date ?: old.date,
                    dealerId = change.dealer?.id ?: old.dealerId,
                    dealerName = change.dealer?.name ?: old.dealerName,
                    bpCode = change.dealer?.id ?: old.bpCode,
                    city = change.dealer?.city ?: old.city,
                )
            }
            if (next == null) current else current + (id to next)
        }
    }

    override suspend fun getVisit(id: Long): PlannedVisit? {
        cache.value[id]?.let { return it }
        // Not seen yet (for example the cache was cleared): load everything once and look again.
        runCatching { safeCall(io) { cache.update { it + fetch(null).associateBy { visit -> visit.id } } } }
        return cache.value[id]
    }

    override suspend fun markDone(id: Long) {
        doneIds += id
        cache.update { current ->
            current[id]?.let { current + (id to it.copy(status = VisitStatus.Done)) } ?: current
        }
    }

    override suspend fun clear() {
        doneIds.clear()
        cache.value = emptyMap()
    }

    private suspend fun fetch(range: DateRange?): List<PlannedVisit> =
        api.getPlans(range?.start?.toString(), range?.end?.toString()).findArray().mapNotNull { it.toVisit() }

    private fun JsonElement.toVisit(): PlannedVisit? {
        val dto = runCatching { json.decodeFromJsonElement(PlanDto.serializer(), this) }.getOrNull() ?: return null
        return dto.toDomain(isDone = dto.id in doneIds)
    }
}

private fun String?.isCompletedStatus() = this?.trim()?.lowercase() in setOf("completed", "complete", "done")

/** Null when the entry has no usable date or dealer code. */
internal fun PlanDto.toDomain(isDone: Boolean = false): PlannedVisit? {
    val date = planDate?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
    val code = dealerCode?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return PlannedVisit(
        id = id,
        dealerId = code,
        dealerName = dealerName?.trim().orEmpty().ifEmpty { code },
        bpCode = code,
        city = city?.trim().orEmpty(),
        date = date,
        // A plan with a DLI is completed, even if the status text is missing or worded differently.
        status = if (isDone || status.isCompletedStatus() || returnIdValue != null) VisitStatus.Done else VisitStatus.Planned,
        returnId = returnIdValue,
    )
}
