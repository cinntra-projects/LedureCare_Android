package com.cinntra.ledurecare.feature.plan.domain

import com.cinntra.ledurecare.core.common.AppResult
import com.cinntra.ledurecare.feature.dealers.domain.Dealer
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

class ObserveVisitsUseCase @Inject constructor(private val repository: PlanRepository) {
    operator fun invoke(range: DateRange): Flow<List<PlannedVisit>> = repository.observeVisits(range)
}

class RefreshVisitsUseCase @Inject constructor(private val repository: PlanRepository) {
    suspend operator fun invoke(range: DateRange): AppResult<Unit> = repository.refresh(range)
}

class AddVisitsUseCase @Inject constructor(private val repository: PlanRepository) {
    suspend operator fun invoke(date: LocalDate, dealers: List<Dealer>): AppResult<AddVisitsResult> =
        if (dealers.isEmpty()) {
            AppResult.Success(AddVisitsResult(emptyList(), emptyList()))
        } else {
            repository.addVisits(date, dealers)
        }
}

class UpdateVisitUseCase @Inject constructor(private val repository: PlanRepository) {
    suspend operator fun invoke(id: Long, change: VisitChange): AppResult<Unit> = repository.updateVisit(id, change)
}

class GetVisitUseCase @Inject constructor(private val repository: PlanRepository) {
    suspend operator fun invoke(id: Long): PlannedVisit? = repository.getVisit(id)
}

class MarkVisitDoneUseCase @Inject constructor(private val repository: PlanRepository) {
    suspend operator fun invoke(id: Long) = repository.markDone(id)
}

class ClearPlanUseCase @Inject constructor(private val repository: PlanRepository) {
    suspend operator fun invoke() = repository.clear()
}
