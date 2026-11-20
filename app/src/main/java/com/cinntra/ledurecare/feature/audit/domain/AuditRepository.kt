package com.cinntra.ledurecare.feature.audit.domain

import com.cinntra.ledurecare.core.common.AppResult

interface AuditRepository {
    /** Products and conditions the form offers. */
    suspend fun getCatalog(): AppResult<AuditCatalog>

    /** Creates the DLI from a validated [draft]. */
    suspend fun submit(draft: AuditDraft): AppResult<SubmittedAudit>

    /** The existing DLI [returnId], shaped for the form. */
    suspend fun getForEdit(returnId: Int): AppResult<AuditEdit>

    /** Saves the changes made to the existing DLI [returnId]. */
    suspend fun update(returnId: Int, draft: AuditDraft): AppResult<SubmittedAudit>

    /** A previously submitted DLI, for the confirmation screen and the document viewer. */
    suspend fun getSubmitted(dliNo: String): AppResult<SubmittedAudit>
}

interface ProductRepository {
    /** Items the dealer [cardCode] can order whose code or name matches [text]. */
    suspend fun search(cardCode: String, text: String): AppResult<List<Product>>
}

interface WarehouseRepository {
    /** Active warehouses a DLI can be sent to. */
    suspend fun getWarehouses(): AppResult<List<Warehouse>>
}
