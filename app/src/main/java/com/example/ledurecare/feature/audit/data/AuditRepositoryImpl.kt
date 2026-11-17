package com.example.ledurecare.feature.audit.data

import android.content.Context
import android.net.Uri
import com.example.ledurecare.core.common.AppError
import com.example.ledurecare.core.common.AppResult
import com.example.ledurecare.core.common.IoDispatcher
import com.example.ledurecare.core.common.safeCall
import com.example.ledurecare.core.network.unwrapObject
import com.example.ledurecare.feature.audit.domain.AuditCatalog
import com.example.ledurecare.feature.audit.domain.AuditEdit
import com.example.ledurecare.feature.audit.domain.Product
import com.example.ledurecare.feature.dealers.domain.Dealer
import com.example.ledurecare.feature.reports.data.ReturnDetailDto
import com.example.ledurecare.feature.reports.data.ReturnItemDto
import com.example.ledurecare.feature.reports.data.parseDate
import com.example.ledurecare.feature.audit.domain.AuditDraft
import com.example.ledurecare.feature.audit.domain.AuditFile
import com.example.ledurecare.feature.audit.domain.AuditItem
import com.example.ledurecare.feature.audit.domain.AuditRepository
import com.example.ledurecare.feature.audit.domain.GeneratedFile
import com.example.ledurecare.feature.audit.domain.MfgType
import com.example.ledurecare.feature.audit.domain.SubmittedAudit
import com.example.ledurecare.feature.audit.domain.Warehouse
import com.example.ledurecare.feature.audit.domain.WarrantyStatus
import com.example.ledurecare.feature.auth.domain.AuthRepository
import com.example.ledurecare.feature.reports.data.ReturnsApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import java.io.IOException
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/** One entry of the `items` field of `POST api/returns/create/`. Unset fields are left out of the JSON. */
@Serializable
data class CreateItemDto(
    @SerialName("item_code") val itemCode: String,
    @SerialName("item_name") val itemName: String,
    val qty: Int,
    val approved: Int,
    val rejected: Int,
    @SerialName("total_approved") val totalApproved: Int,
    @SerialName("condition_name") val conditionName: String,
    @SerialName("mfg_type") val mfgType: String,
    @SerialName("mfg_month") val mfgMonth: Int? = null,
    @SerialName("mfg_quarter") val mfgQuarter: Int? = null,
    @SerialName("mfg_year") val mfgYear: Int,
    val remarks: String = "",
    @SerialName("warranty_status") val warrantyStatus: String,
    /** The special remark, sent only when the item is out of warranty. */
    @SerialName("warranty_remarks") val warrantyRemarks: String? = null,
)

internal fun AuditItem.toCreateDto() = CreateItemDto(
    itemCode = product.code,
    itemName = product.name,
    qty = qty,
    approved = approved,
    rejected = rejected,
    totalApproved = totalApproved,
    conditionName = condition,
    mfgType = if (mfgType == MfgType.Quarter) "QTR" else "MONTH",
    // Only the part that matches the type is sent: a month for MONTH, a quarter for QTR.
    mfgMonth = mfgPeriod.takeIf { mfgType == MfgType.Month },
    mfgQuarter = mfgPeriod.takeIf { mfgType == MfgType.Quarter },
    mfgYear = mfgYear,
    remarks = remark,
    warrantyStatus = if (warranty == WarrantyStatus.InWarranty) "IN_WARRANTY" else "OUT_OF_WARRANTY",
    warrantyRemarks = remark.takeIf { warranty == WarrantyStatus.OutOfWarranty },
)

/** An item of the detail answer, back into a form item. */
internal fun ReturnItemDto.toAuditItem(id: Long, fallbackYear: Int): AuditItem {
    val quarter = mfgType.equals("QTR", ignoreCase = true) || mfgType.equals("QUARTER", ignoreCase = true)
    val out = warrantyStatus.equals("OUT_OF_WARRANTY", ignoreCase = true)
    val approvedCount = approved ?: 0
    return AuditItem(
        id = id,
        product = Product(itemCode.orEmpty(), itemName.orEmpty()),
        qty = qty,
        approved = approvedCount,
        rejected = rejected ?: 0,
        totalApproved = totalApproved ?: approvedCount,
        mfgType = if (quarter) MfgType.Quarter else MfgType.Month,
        mfgPeriod = (if (quarter) mfgQuarter else mfgMonth) ?: 1,
        mfgYear = mfgYear ?: fallbackYear,
        condition = conditionName.orEmpty(),
        warranty = if (out) WarrantyStatus.OutOfWarranty else WarrantyStatus.InWarranty,
        // An out-of-warranty item keeps its special remark in `warranty_remarks`.
        remark = (if (out) warrantyRemarks?.takeIf { it.isNotBlank() } ?: remarks else remarks).orEmpty(),
    )
}

/** The whole detail answer as the form's starting point. */
internal fun ReturnDetailDto.toAuditEdit(fallbackYear: Int): AuditEdit {
    val code = dealerCode?.takeIf { it.isNotBlank() } ?: bpCode.orEmpty()
    return AuditEdit(
        dliNo = dliNo.orEmpty(),
        dealer = Dealer(
            id = code,
            name = dealerName.orEmpty(),
            bpCode = bpCode?.takeIf { it.isNotBlank() } ?: code,
            city = "",
            zone = zone.orEmpty(),
            address = address.orEmpty(),
            salesPerson = salesPerson.orEmpty(),
        ),
        address = address.orEmpty(),
        customerCode = customerCode?.takeIf { it.isNotBlank() } ?: code,
        salesPerson = salesPerson.orEmpty(),
        inspectionDate = parseDate(inspectionDate ?: auditDate),
        totalBox = totalBox,
        items = items.mapIndexed { index, item -> item.toAuditItem(index + 1L, fallbackYear) },
        warehouseId = warehouseAddress?.id,
        notes = notes.orEmpty(),
    )
}

/**
 * The warehouse object to send as `WarehouseAddress`: the server's own object when the list gave it,
 * otherwise a small one built from what the app knows.
 */
internal fun Warehouse.toAddressJson(json: Json): String =
    details.takeIf { it.isNotBlank() } ?: json.encodeToString(
        JsonObject.serializer(),
        buildJsonObject {
            put("id", id)
            put("name", name)
            put("dispatch_address", address)
        },
    )

/**
 * The multipart body of `POST api/returns/create/`. Text fields go as plain parts, `items` and
 * `WarehouseAddress` as JSON text, and each file as an `attachments` part built by [fileBody].
 */
internal fun AuditDraft.toCreateParts(json: Json, fileBody: (AuditFile) -> RequestBody): List<MultipartBody.Part> {
    val parts = mutableListOf<MultipartBody.Part>()
    fun text(name: String, value: String) = parts.add(MultipartBody.Part.createFormData(name, value))

    text("dealer_code", dealer?.bpCode.orEmpty())
    text("dealer_name", dealer?.name.orEmpty())
    text("customer_code", customerCode)
    text("address", address)
    text("sales_person", salesPerson)
    text("zone", dealer?.zone.orEmpty())
    text("audit_date", (inspectionDate ?: LocalDate.now()).toString())
    text("notes", notes)
    text("is_draft", "false")
    totalQty?.let { text("total_qty", it.toString()) }
    totalBox?.let { text("total_box", it.toString()) }
    text("items", json.encodeToString(ListSerializer(CreateItemDto.serializer()), items.map { it.toCreateDto() }))
    warehouse?.let { text("WarehouseAddress", it.toAddressJson(json)) }
    files.forEach { file -> parts.add(MultipartBody.Part.createFormData("attachments", file.name, fileBody(file))) }
    return parts
}

/** Streams a picked file straight from its `content://` address, so a big file is never held in memory. */
private class UriRequestBody(
    private val context: Context,
    private val file: AuditFile,
) : RequestBody() {
    override fun contentType() = file.mimeType.takeIf { it.isNotBlank() }?.toMediaTypeOrNull()
    override fun contentLength() = file.sizeBytes.takeIf { it > 0 } ?: -1L
    override fun writeTo(sink: BufferedSink) {
        val input = context.contentResolver.openInputStream(Uri.parse(file.uri))
            ?: throw IOException("Cannot read ${file.name}")
        input.use { sink.writeAll(it.source()) }
    }
}

/**
 * Submits a DLI audit to the server. The product and condition lists are still the built-in samples,
 * because no API for them has been shared.
 */
@Singleton
class AuditRepositoryImpl @Inject constructor(
    private val api: ReturnsApi,
    private val auth: AuthRepository,
    private val json: Json,
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) : AuditRepository {

    /** The confirmation screen reads back what was just submitted. */
    private val submitted = ConcurrentHashMap<String, SubmittedAudit>()

    override suspend fun getCatalog(): AppResult<AuditCatalog> = AppResult.Success(SampleAuditCatalog)

    override suspend fun submit(draft: AuditDraft): AppResult<SubmittedAudit> {
        val body = when (val result = safeCall(io) { api.createReturn(draft.toCreateParts(json) { UriRequestBody(context, it) }) }) {
            is AppResult.Success -> result.data
            is AppResult.Failure -> return result
        }
        val created = body.unwrapObject()
        val dliNo = (created?.get("dli_no") as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
            ?: return AppResult.Failure(AppError.Http(502, "The server did not return a DLI number."))
        return AppResult.Success(remember(dliNo, draft))
    }

    /** DLI numbers of the DLIs opened for editing, in case the edit answer does not repeat them. */
    private val knownDliNo = ConcurrentHashMap<Int, String>()

    override suspend fun getForEdit(returnId: Int): AppResult<AuditEdit> {
        val body = when (val result = safeCall(io) { api.getReturn(returnId) }) {
            is AppResult.Success -> result.data
            is AppResult.Failure -> return result
        }
        val detail = body.unwrapObject()
            ?.let { runCatching { json.decodeFromJsonElement(ReturnDetailDto.serializer(), it) }.getOrNull() }
            ?: return AppResult.Failure(AppError.Http(502, "The DLI could not be read."))
        knownDliNo[returnId] = detail.dliNo.orEmpty()
        return AppResult.Success(detail.toAuditEdit(fallbackYear = LocalDate.now().year))
    }

    override suspend fun update(returnId: Int, draft: AuditDraft): AppResult<SubmittedAudit> {
        val body = when (val result = safeCall(io) { api.editReturn(returnId, draft.toCreateParts(json) { UriRequestBody(context, it) }) }) {
            is AppResult.Success -> result.data
            is AppResult.Failure -> return result
        }
        val dliNo = (body.unwrapObject()?.get("dli_no") as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
            ?: knownDliNo[returnId]?.takeIf { it.isNotBlank() }
            ?: return AppResult.Failure(AppError.Http(502, "The server did not return a DLI number."))
        return AppResult.Success(remember(dliNo, draft))
    }

    /** Builds what the confirmation screen shows and keeps it for [getSubmitted]. */
    private suspend fun remember(dliNo: String, draft: AuditDraft): SubmittedAudit {
        val user = auth.observeUser().first()
        val record = SubmittedAudit(
            dliNo = dliNo,
            dealerName = draft.dealer?.name.orEmpty(),
            bpCode = draft.dealer?.bpCode.orEmpty(),
            address = draft.address,
            inspectionDate = draft.inspectionDate ?: LocalDate.now(),
            auditedBy = user?.name.orEmpty(),
            auditorRole = user?.roleName.orEmpty(),
            items = draft.items,
            notes = draft.notes,
            // The audit-form PDF is written on the phone when it is opened, so there is no size to show.
            form = GeneratedFile(name = "${dliNo}_Audit_Form.pdf", sizeBytes = 0, pages = 1),
            attachments = draft.files,
        )
        submitted[dliNo] = record
        return record
    }

    override suspend fun getSubmitted(dliNo: String): AppResult<SubmittedAudit> =
        submitted[dliNo]?.let { AppResult.Success(it) }
            ?: AppResult.Failure(AppError.Http(404, "Submitted DLI not found."))
}
