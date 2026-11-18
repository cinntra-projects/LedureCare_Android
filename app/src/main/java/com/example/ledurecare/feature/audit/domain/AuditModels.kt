package com.example.ledurecare.feature.audit.domain

import com.example.ledurecare.feature.dealers.domain.Dealer
import java.time.LocalDate

const val MAX_FILE_BYTES: Long = 10L * 1024 * 1024

/** An item a dealer can return. The price and tax fields come from the item search and are sent back with the DLI. */
data class Product(
    val code: String,
    val name: String,
    val unitPrice: String? = null,
    val discount: String? = null,
    val hsn: String? = null,
    val uom: String? = null,
    val gst: String? = null,
    val taxCode: String? = null,
)

data class Warehouse(
    val id: Int,
    val name: String,
    /** Where goods are dispatched from, if the server gives it. */
    val address: String = "",
    /**
     * The server's own JSON object for this warehouse, kept as text so a DLI can be submitted with the
     * complete `WarehouseAddress`. Blank for sample data.
     */
    val details: String = "",
) {
    /** What the dropdown shows: the location only, not the full address. */
    val label: String get() = name
}

enum class MfgType { Month, Quarter }

enum class WarrantyStatus { InWarranty, OutOfWarranty }

data class AuditItem(
    val id: Long,
    val product: Product,
    val qty: Int,
    val approved: Int,
    val rejected: Int,
    /** Final quantity after the audit. */
    val totalApproved: Int,
    val mfgType: MfgType,
    /** Month 1-12, or quarter 1-4, depending on [mfgType]. */
    val mfgPeriod: Int,
    val mfgYear: Int,
    val condition: String,
    val warranty: WarrantyStatus,
    val remark: String,
    /** Created by "Repeat" and not edited since; the card is highlighted until then. */
    val copied: Boolean = false,
)

data class AuditFile(
    val uri: String,
    val name: String,
    val sizeBytes: Long,
    val mimeType: String,
)

/** The product and condition lists the form offers. Warehouses come from their own repository. */
data class AuditCatalog(
    val products: List<Product>,
    val conditions: List<String>,
)

data class AuditOptions(
    val products: List<Product>,
    val conditions: List<String>,
    val warehouses: List<Warehouse>,
)

data class AuditDraft(
    val dealer: Dealer?,
    val address: String,
    val customerCode: String,
    val salesPerson: String,
    val inspectionDate: LocalDate?,
    val totalBox: Int?,
    val totalQty: Int?,
    val items: List<AuditItem>,
    val warehouse: Warehouse?,
    val notes: String,
    val files: List<AuditFile>,
)

/** A submitted DLI loaded back into the form so its service engineer can change it. */
data class AuditEdit(
    val dliNo: String,
    val dealer: Dealer,
    val address: String,
    val customerCode: String,
    val salesPerson: String,
    val inspectionDate: LocalDate?,
    val totalBox: Int?,
    val items: List<AuditItem>,
    /** The warehouse chosen when the DLI was created; matched against the warehouse list. */
    val warehouseId: Int?,
    val notes: String,
)

/** The audit-form PDF the backend generates when a DLI is submitted. */
data class GeneratedFile(val name: String, val sizeBytes: Long, val pages: Int)

/** A DLI as it was submitted: what the confirmation screen and the document viewer show. */
data class SubmittedAudit(
    val dliNo: String,
    val dealerName: String,
    val bpCode: String,
    val address: String,
    val inspectionDate: LocalDate,
    val auditedBy: String,
    val auditorRole: String,
    val items: List<AuditItem>,
    val notes: String,
    val form: GeneratedFile,
    val attachments: List<AuditFile>,
)
