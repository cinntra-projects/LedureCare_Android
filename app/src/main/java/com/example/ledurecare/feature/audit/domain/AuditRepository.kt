package com.example.ledurecare.feature.audit.domain

import com.example.ledurecare.core.common.AppResult

interface AuditRepository {
    /** Products and conditions the form offers. */
    suspend fun getCatalog(): AppResult<AuditCatalog>

    /** Creates the DLI from a validated [draft]. */
    suspend fun submit(draft: AuditDraft): AppResult<SubmittedAudit>

    /** A previously submitted DLI, for the confirmation screen and the document viewer. */
    suspend fun getSubmitted(dliNo: String): AppResult<SubmittedAudit>
}

interface WarehouseRepository {
    /** Active warehouses a DLI can be sent to. */
    suspend fun getWarehouses(): AppResult<List<Warehouse>>
}
