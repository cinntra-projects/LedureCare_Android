package com.example.ledurecare.feature.plan.domain

import com.example.ledurecare.core.common.AppResult
import com.example.ledurecare.feature.dealers.domain.Dealer
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * The visits planned by the signed-in user. Reads come from a local copy that [refresh] fills from the
 * server by date range; changes are written to the server and the copy is updated from its answer.
 */
interface PlanRepository {
    /** The plans in [range], as last fetched or changed. Emits again whenever they change. */
    fun observeVisits(range: DateRange): Flow<List<PlannedVisit>>

    /** Fetches the plans in [range] from the server into the local copy. */
    suspend fun refresh(range: DateRange): AppResult<Unit>

    /** Plans [dealers] on [date]. The server skips any already planned there and says which. */
    suspend fun addVisits(date: LocalDate, dealers: List<Dealer>): AppResult<AddVisitsResult>

    /** Reschedules a plan, changes its dealer, or both. */
    suspend fun updateVisit(id: Long, change: VisitChange): AppResult<Unit>

    suspend fun getVisit(id: Long): PlannedVisit?

    /** Marks a visit's audit as done once its DLI has been submitted. */
    suspend fun markDone(id: Long)

    suspend fun clear()
}
