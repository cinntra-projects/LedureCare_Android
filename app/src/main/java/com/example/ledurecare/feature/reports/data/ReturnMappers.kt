package com.example.ledurecare.feature.reports.data

import com.example.ledurecare.feature.reports.domain.Attachment
import com.example.ledurecare.feature.reports.domain.AttachmentKind
import com.example.ledurecare.feature.reports.domain.Dli
import com.example.ledurecare.feature.reports.domain.DliDetail
import com.example.ledurecare.feature.reports.domain.DliItem
import com.example.ledurecare.feature.reports.domain.ReturnStatus
import com.example.ledurecare.feature.reports.domain.StatusChange
import com.example.ledurecare.feature.reports.domain.buildTracker
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Accepts both `2026-10-09` and `2026-10-09T11:24:58+05:30`. */
internal fun parseDate(value: String?): LocalDate? =
    value?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

private val SlaBreachKeys = listOf("sla_breached", "breached", "is_breached", "breach", "over_sla", "is_over_sla", "overdue", "is_overdue")

/** The `sla` object's shape is not documented; any known "breached" flag set to true counts. */
internal fun JsonObject?.isOverSla(): Boolean =
    this != null && SlaBreachKeys.any { (this[it] as? JsonPrimitive)?.booleanOrNull == true }

private fun daysSince(since: LocalDate?, today: LocalDate): Int =
    since?.let { ChronoUnit.DAYS.between(it, today).toInt().coerceAtLeast(0) } ?: 0

/** Null for drafts and for statuses this app does not know. */
internal fun ReturnListDto.toDomain(today: LocalDate): Dli? {
    val parsedStatus = ReturnStatus.fromCode(status)
    if (isDraft || parsedStatus == null) return null
    val audit = parseDate(auditDate)
    return Dli(
        id = id,
        dliNo = dliNo.orEmpty(),
        dealerName = dealerName.orEmpty(),
        bpCode = bpCode.orEmpty(),
        status = parsedStatus,
        auditDate = audit,
        itemsCount = itemsCount,
        daysInStage = daysSince(parseDate(statusChangedAt) ?: audit, today),
        isOverSla = sla.isOverSla(),
        auditedBy = createdByName?.takeIf { it.isNotBlank() },
        isEditable = isEditable,
    )
}

internal fun AttachmentDto.toDomain(isAuditForm: Boolean): Attachment? {
    val link = url?.takeIf { it.isNotBlank() } ?: return null
    val name = originalName?.takeIf { it.isNotBlank() } ?: link.substringAfterLast('/')
    val type = contentType.orEmpty()
    val kind = when {
        type.contains("pdf", ignoreCase = true) || name.endsWith(".pdf", ignoreCase = true) -> AttachmentKind.Pdf
        type.startsWith("image", ignoreCase = true) -> AttachmentKind.Image
        else -> AttachmentKind.Other
    }
    return Attachment(id = id, name = name, url = link, kind = kind, isAuditForm = isAuditForm)
}

/** Reads `attachments: [...]` out of the dispatch / warehouse-receipt sections, ignoring other shapes. */
private fun JsonObject?.attachments(json: Json): List<AttachmentDto> =
    (this?.get("attachments") as? JsonArray).orEmpty().mapNotNull {
        runCatching { json.decodeFromJsonElement(AttachmentDto.serializer(), it) }.getOrNull()
    }

/** A stage that was reached has a date; one still "waiting" has none. */
internal fun TrackerEntryDto.toChange(): StatusChange? {
    val to = status ?: return null
    if (state.equals("waiting", ignoreCase = true)) return null
    val date = parseDate(at) ?: return null
    return StatusChange(to, date)
}

internal fun ReturnDetailDto.toDomain(
    json: Json,
    changes: List<StatusChange>,
    auditedBy: String?,
    today: LocalDate,
): DliDetail? {
    val parsedStatus = ReturnStatus.fromCode(status) ?: return null
    val audit = parseDate(auditDate)
    val since = parseDate(statusChangedAt) ?: changes.lastOrNull { it.toStatus == parsedStatus.code }?.date ?: audit
    val attachments = photos.mapNotNull { it.toDomain(isAuditForm = true) } +
        (dispatch.attachments(json) + warehouseReceipt.attachments(json)).mapNotNull { it.toDomain(isAuditForm = false) }
    return DliDetail(
        id = id,
        dliNo = dliNo.orEmpty(),
        dealerName = dealerName.orEmpty(),
        status = parsedStatus,
        daysInStage = daysSince(since, today),
        stageSince = since,
        isOverSla = sla.isOverSla(),
        customerCode = customerCode?.takeIf { it.isNotBlank() } ?: bpCode.orEmpty(),
        // The detail answer does not name the auditor; the list does, and the audit photos say who uploaded them.
        auditedBy = auditedBy ?: photos.firstNotNullOfOrNull { it.uploadedByName?.takeIf(String::isNotBlank) },
        auditDate = audit,
        totalBox = totalBox,
        totalQty = totalQty,
        warehouse = warehouse?.takeIf { it.isNotBlank() } ?: warehouseAddress?.name.orEmpty(),
        items = items.map { DliItem(it.itemCode.orEmpty(), it.itemName.orEmpty(), it.qty, it.approved, it.rejected) },
        attachments = attachments,
        tracker = buildTracker(parsedStatus, audit, changes),
    )
}
