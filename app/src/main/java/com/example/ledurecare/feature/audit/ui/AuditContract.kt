package com.example.ledurecare.feature.audit.ui

import com.example.ledurecare.core.common.AppError
import com.example.ledurecare.feature.audit.domain.AuditField
import com.example.ledurecare.feature.audit.domain.AuditFile
import com.example.ledurecare.feature.audit.domain.AuditItem
import com.example.ledurecare.feature.audit.domain.AuditOptions
import com.example.ledurecare.feature.audit.domain.EntryField
import com.example.ledurecare.feature.audit.domain.MfgType
import com.example.ledurecare.feature.audit.domain.Product
import com.example.ledurecare.feature.audit.domain.Warehouse
import com.example.ledurecare.feature.audit.domain.WarrantyStatus
import com.example.ledurecare.feature.dealers.domain.Dealer
import com.example.ledurecare.feature.dealers.ui.DealerSearchState
import java.time.LocalDate

/** The "add an item" panel, kept as typed text until the engineer taps Add item. */
data class ItemEntry(
    val productQuery: String = "",
    val product: Product? = null,
    val qty: String = "1",
    val mfgType: MfgType? = null,
    val mfgPeriod: Int? = null,
    val mfgYear: Int? = null,
    val condition: String? = null,
    val warranty: WarrantyStatus? = null,
    val remark: String = "",
)

data class AuditUiState(
    val isLoading: Boolean = true,
    val error: AppError? = null,
    val today: LocalDate,
    /** The planned visit's date, for "Started from My Plan · 28 Oct". */
    val startedFrom: LocalDate? = null,
    /** An existing DLI is being edited; [dliNo] is its number. */
    val isEditing: Boolean = false,
    /** The preview page is showing instead of the form. */
    val showPreview: Boolean = false,
    val dliNo: String = "",

    /** The dealer picker's search: its query, pages loaded so far, and loading state. */
    val dealerSearch: DealerSearchState = DealerSearchState(),
    val dealer: Dealer? = null,
    val address: String = "",
    val customerCode: String = "",
    val salesPerson: String = "",
    val inspectionDate: LocalDate,
    val totalBox: String = "",

    /** Result of the last item search (see [suggestions]) and whether another one is running. */
    val productResults: List<Product> = emptyList(),
    val isSearchingProducts: Boolean = false,

    val options: AuditOptions = AuditOptions(emptyList(), emptyList(), emptyList()),
    val entry: ItemEntry = ItemEntry(),
    val editingId: Long? = null,
    val items: List<AuditItem> = emptyList(),

    val warehouse: Warehouse? = null,
    val notes: String = "",
    val files: List<AuditFile> = emptyList(),

    val errors: Set<AuditField> = emptySet(),
    val entryErrors: Set<EntryField> = emptySet(),
    val isSubmitting: Boolean = false,
    val submitError: AppError? = null,
) {
    val zone: String get() = dealer?.zone.orEmpty()

    /** Read-only: the sum of the quantities of the added items. */
    val totalQty: Int get() = items.sumOf { it.qty }

    /** True once there is something worth confirming before the engineer leaves. */
    val isDirty: Boolean
        get() = items.isNotEmpty() || notes.isNotBlank() || files.isNotEmpty() ||
            totalBox.isNotBlank()

    /** What the server's item search found for the typed text, until one is chosen. */
    val suggestions: List<Product>
        get() = if (entry.product != null || entry.productQuery.isBlank()) emptyList() else productResults
}

sealed interface AuditEvent {
    data object Retry : AuditEvent

    // Dealer details
    data object OpenDealerPicker : AuditEvent
    data class DealerQueryChanged(val query: String) : AuditEvent
    data object LoadMoreDealers : AuditEvent
    data object RetryDealers : AuditEvent
    data class DealerSelected(val dealer: Dealer) : AuditEvent
    data class AddressChanged(val value: String) : AuditEvent
    data class CustomerCodeChanged(val value: String) : AuditEvent
    data class SalesPersonChanged(val value: String) : AuditEvent
    data class InspectionDateChanged(val date: LocalDate) : AuditEvent
    data class TotalBoxChanged(val value: String) : AuditEvent

    // Item entry
    data class ProductQueryChanged(val value: String) : AuditEvent
    data class ProductSelected(val product: Product) : AuditEvent
    data class QtyChanged(val value: String) : AuditEvent
    data class MfgTypeChanged(val type: MfgType) : AuditEvent
    data class MfgPeriodChanged(val period: Int) : AuditEvent
    data class MfgYearChanged(val year: Int) : AuditEvent
    /** Apply in the MFG date dialog: month or quarter, and year, set together. */
    data class MfgPicked(val type: MfgType, val period: Int, val year: Int) : AuditEvent
    data object MfgCleared : AuditEvent
    data class ConditionChanged(val condition: String) : AuditEvent
    data class WarrantyChanged(val warranty: WarrantyStatus) : AuditEvent
    data class RemarkChanged(val value: String) : AuditEvent
    data object AddItem : AuditEvent
    data object CancelEdit : AuditEvent

    // Added items
    data class RepeatItem(val id: Long) : AuditEvent
    data class EditItem(val id: Long) : AuditEvent
    data class DeleteItem(val id: Long) : AuditEvent

    // Rest of the form
    data class WarehouseSelected(val warehouseId: Int) : AuditEvent
    data class NotesChanged(val value: String) : AuditEvent
    data class FilesPicked(val files: List<AuditFile>) : AuditEvent
    data class RemoveFile(val uri: String) : AuditEvent
    /** Check the form and show the preview page; nothing is sent yet. */
    data object OpenPreview : AuditEvent
    data object ClosePreview : AuditEvent
    data object Submit : AuditEvent
}

sealed interface AuditEffect {
    data class Submitted(val dliNo: String) : AuditEffect
    data class FilesRejected(val names: List<String>) : AuditEffect
    data object FixErrors : AuditEffect
}
