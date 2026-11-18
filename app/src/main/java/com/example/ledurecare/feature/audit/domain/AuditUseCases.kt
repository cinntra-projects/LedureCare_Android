package com.example.ledurecare.feature.audit.domain

import com.example.ledurecare.core.common.AppResult
import com.example.ledurecare.feature.plan.domain.MarkVisitDoneUseCase
import javax.inject.Inject

/** Everything the form's dropdowns need: the catalog plus the warehouses. Either failing fails the form. */
class GetAuditOptionsUseCase @Inject constructor(
    private val repository: AuditRepository,
    private val warehouses: WarehouseRepository,
) {
    suspend operator fun invoke(): AppResult<AuditOptions> {
        val catalog = when (val result = repository.getCatalog()) {
            is AppResult.Success -> result.data
            is AppResult.Failure -> return result
        }
        val list = when (val result = warehouses.getWarehouses()) {
            is AppResult.Success -> result.data
            is AppResult.Failure -> return result
        }
        return AppResult.Success(AuditOptions(catalog.products, catalog.conditions, list))
    }
}

class SearchProductsUseCase @Inject constructor(private val repository: ProductRepository) {
    suspend operator fun invoke(cardCode: String, text: String): AppResult<List<Product>> =
        repository.search(cardCode, text)
}

class GetSubmittedAuditUseCase @Inject constructor(private val repository: AuditRepository) {
    suspend operator fun invoke(dliNo: String): AppResult<SubmittedAudit> = repository.getSubmitted(dliNo)
}

class GetAuditForEditUseCase @Inject constructor(private val repository: AuditRepository) {
    suspend operator fun invoke(returnId: Int): AppResult<AuditEdit> = repository.getForEdit(returnId)
}

class UpdateAuditUseCase @Inject constructor(private val repository: AuditRepository) {
    suspend operator fun invoke(returnId: Int, draft: AuditDraft): AppResult<SubmittedAudit> =
        repository.update(returnId, draft)
}

/** Submits the audit, then marks the planned visit it came from as done. */
class SubmitAuditUseCase @Inject constructor(
    private val repository: AuditRepository,
    private val markVisitDone: MarkVisitDoneUseCase,
) {
    suspend operator fun invoke(visitId: Long, draft: AuditDraft): AppResult<SubmittedAudit> =
        repository.submit(draft).also { if (it is AppResult.Success) markVisitDone(visitId) }
}
