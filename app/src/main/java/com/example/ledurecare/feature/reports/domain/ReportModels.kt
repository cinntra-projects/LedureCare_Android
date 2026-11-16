package com.example.ledurecare.feature.reports.domain

import java.time.LocalDate

/** Stages of a DLI (return) in pipeline order. [code] is the backend's `status` value. */
enum class ReturnStatus(val code: String) {
    Coord("coord"),
    Transit("transit"),
    Wh("wh"),
    AdminApproval("admin_approval"),
    Acc("acc"),
    Sap("sap"),
    Cn("cn"),

    /** Closed and settled. Not a backend status yet; only the static data uses it. */
    Completed("completed");

    /** 1-based step number shown on the stage cards. */
    val step: Int get() = ordinal + 1

    companion object {
        fun fromCode(code: String?): ReturnStatus? = entries.firstOrNull { it.code == code }
    }
}

data class Dli(
    val id: Int,
    val dliNo: String,
    val dealerName: String,
    val bpCode: String,
    val status: ReturnStatus,
    val auditDate: LocalDate?,
    val itemsCount: Int,
    val daysInStage: Int,
    val isOverSla: Boolean,
    val auditedBy: String?,
    /** Days from audit to closure; only set for completed DLIs. */
    val turnaroundDays: Int? = null,
) {
    fun matches(query: String): Boolean = query.isBlank() ||
        dliNo.contains(query, ignoreCase = true) ||
        dealerName.contains(query, ignoreCase = true) ||
        bpCode.contains(query, ignoreCase = true)
}

data class StageSummary(
    val status: ReturnStatus,
    val count: Int,
    val oldestDays: Int,
    val avgDays: Int,
    /** Average days from audit to closure; only for the Completed stage. */
    val avgTurnaroundDays: Double? = null,
)

enum class AttachmentKind { Pdf, Image, Other }

data class Attachment(
    val id: Int,
    val name: String,
    val url: String,
    val kind: AttachmentKind,
    val isAuditForm: Boolean,
)

data class DliItem(
    val code: String,
    val name: String,
    val qty: Int,
    val approved: Int?,
    val rejected: Int?,
)

enum class StepState { Done, Current, Upcoming }

/** One row of the tracker. [status] is null for the first "Submitted by Service Engineer" row. */
data class TrackerStep(
    val status: ReturnStatus?,
    val state: StepState,
    val date: LocalDate?,
)

data class DliDetail(
    val id: Int,
    val dliNo: String,
    val dealerName: String,
    val status: ReturnStatus,
    val daysInStage: Int,
    val stageSince: LocalDate?,
    val isOverSla: Boolean,
    val customerCode: String,
    val auditedBy: String?,
    val auditDate: LocalDate?,
    val totalBox: Int,
    val totalQty: Int,
    val warehouse: String,
    val items: List<DliItem>,
    val attachments: List<Attachment>,
    val tracker: List<TrackerStep>,
)

data class StatusChange(val toStatus: String, val date: LocalDate)
