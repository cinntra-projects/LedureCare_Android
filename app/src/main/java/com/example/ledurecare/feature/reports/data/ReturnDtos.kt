package com.example.ledurecare.feature.reports.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class ReturnListDto(
    val id: Int,
    @SerialName("dli_no") val dliNo: String? = null,
    @SerialName("dealer_name") val dealerName: String? = null,
    @SerialName("bp_code") val bpCode: String? = null,
    @SerialName("audit_date") val auditDate: String? = null,
    val status: String? = null,
    @SerialName("is_draft") val isDraft: Boolean = false,
    @SerialName("items_count") val itemsCount: Int = 0,
    @SerialName("status_changed_at") val statusChangedAt: String? = null,
    @SerialName("created_by_name") val createdByName: String? = null,
    val sla: JsonObject? = null,
)

@Serializable
data class ReturnItemDto(
    @SerialName("item_code") val itemCode: String? = null,
    @SerialName("item_name") val itemName: String? = null,
    val qty: Int = 0,
    val approved: Int? = null,
    val rejected: Int? = null,
)

@Serializable
data class AttachmentDto(
    val id: Int = 0,
    val url: String? = null,
    @SerialName("original_name") val originalName: String? = null,
    @SerialName("content_type") val contentType: String? = null,
    val stage: String? = null,
    @SerialName("uploaded_by_name") val uploadedByName: String? = null,
)

@Serializable
data class ReturnDetailDto(
    val id: Int,
    @SerialName("dli_no") val dliNo: String? = null,
    @SerialName("dealer_name") val dealerName: String? = null,
    @SerialName("bp_code") val bpCode: String? = null,
    @SerialName("customer_code") val customerCode: String? = null,
    @SerialName("audit_date") val auditDate: String? = null,
    val status: String? = null,
    @SerialName("status_changed_at") val statusChangedAt: String? = null,
    @SerialName("total_qty") val totalQty: Int = 0,
    @SerialName("total_box") val totalBox: Int = 0,
    val warehouse: String? = null,
    val items: List<ReturnItemDto> = emptyList(),
    val photos: List<AttachmentDto> = emptyList(),
    /** Each stage with its state ("completed", "current", "waiting") and when it was entered. */
    val tracker: List<TrackerEntryDto> = emptyList(),
    /** The warehouse the goods go to. The plain `warehouse` text is often empty; this carries the name. */
    @SerialName("WarehouseAddress") val warehouseAddress: WarehouseAddressDto? = null,
    val dispatch: JsonObject? = null,
    @SerialName("warehouse_receipt") val warehouseReceipt: JsonObject? = null,
    val sla: JsonObject? = null,
)

/** One row of `tracker` in the detail answer. */
@Serializable
data class TrackerEntryDto(
    val status: String? = null,
    val state: String? = null,
    val at: String? = null,
)

@Serializable
data class WarehouseAddressDto(
    val name: String? = null,
)
