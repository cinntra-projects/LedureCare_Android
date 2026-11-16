package com.example.ledurecare.feature.plan.data

import com.example.ledurecare.core.common.AppResult
import com.example.ledurecare.feature.dealers.data.StaticDealers
import com.example.ledurecare.feature.dealers.domain.Dealer
import com.example.ledurecare.feature.plan.domain.AddVisitsResult
import com.example.ledurecare.feature.plan.domain.DateRange
import com.example.ledurecare.feature.plan.domain.PlanRepository
import com.example.ledurecare.feature.plan.domain.PlannedVisit
import com.example.ledurecare.feature.plan.domain.SkippedDealer
import com.example.ledurecare.feature.plan.domain.VisitChange
import com.example.ledurecare.feature.plan.domain.VisitStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory plan seeded around today, used while `BuildConfig.USE_STATIC_PLAN` is on.
 * Changes last until the app process ends.
 */
@Singleton
class FakePlanRepository @Inject constructor(
    private val clock: Clock,
) : PlanRepository {

    private val visits = MutableStateFlow(seed())

    override fun observeVisits(range: DateRange): Flow<List<PlannedVisit>> =
        visits.map { all -> all.filter { it.date >= range.start && it.date <= range.end } }

    override suspend fun refresh(range: DateRange): AppResult<Unit> = AppResult.Success(Unit)

    override suspend fun addVisits(date: LocalDate, dealers: List<Dealer>): AppResult<AddVisitsResult> {
        var created = emptyList<Long>()
        var skipped = emptyList<SkippedDealer>()
        visits.update { current ->
            var nextId = (current.maxOfOrNull { it.id } ?: 0L) + 1
            val (fresh, duplicates) = dealers.partition { dealer ->
                current.none { it.dealerId == dealer.id && it.date == date }
            }
            val added = fresh.map { PlannedVisit(nextId++, it.id, it.name, it.bpCode, it.city, date, VisitStatus.Planned) }
            created = added.map { it.id }
            skipped = duplicates.map { SkippedDealer(it.id, "Already planned for this date.") }
            current + added
        }
        return AppResult.Success(AddVisitsResult(created, skipped))
    }

    override suspend fun updateVisit(id: Long, change: VisitChange): AppResult<Unit> {
        visits.update { all ->
            all.map { visit ->
                if (visit.id != id) {
                    visit
                } else {
                    visit.copy(
                        date = change.date ?: visit.date,
                        dealerId = change.dealer?.id ?: visit.dealerId,
                        dealerName = change.dealer?.name ?: visit.dealerName,
                        bpCode = change.dealer?.bpCode ?: visit.bpCode,
                        city = change.dealer?.city ?: visit.city,
                    )
                }
            }
        }
        return AppResult.Success(Unit)
    }

    override suspend fun getVisit(id: Long): PlannedVisit? = visits.value.firstOrNull { it.id == id }

    override suspend fun markDone(id: Long) {
        visits.update { all -> all.map { if (it.id == id) it.copy(status = VisitStatus.Done) else it } }
    }

    override suspend fun clear() {
        visits.value = seed()
    }

    private fun seed(): List<PlannedVisit> {
        val today = LocalDate.now(clock)
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        var id = 1L
        fun visit(date: LocalDate, dealerNumber: Int, status: VisitStatus): PlannedVisit {
            val dealer = StaticDealers.byId(dealerNumber)
            return PlannedVisit(id++, dealer.id, dealer.name, dealer.bpCode, dealer.city, date, status)
        }

        val thisWeek = listOf(
            visit(monday, 6, VisitStatus.Done),
            visit(monday, 7, VisitStatus.Done),
            visit(today, 1, VisitStatus.Done),
            visit(today, 2, VisitStatus.Planned),
            visit(today, 3, VisitStatus.Planned),
            visit(today.plusDays(1), 4, VisitStatus.Planned),
            visit(today.plusDays(2), 5, VisitStatus.Planned),
        )
        val earlier = listOf(4L to 8, 6L to 9, 8L to 10, 9L to 11, 12L to 12, 13L to 1, 15L to 2)
            .map { (daysAgo, dealerNumber) -> visit(today.minusDays(daysAgo), dealerNumber, VisitStatus.Done) }
        return thisWeek + earlier
    }
}
