package com.example.ledurecare.feature.reports.data

import com.example.ledurecare.core.common.AppError
import com.example.ledurecare.core.common.AppResult
import com.example.ledurecare.feature.reports.domain.Attachment
import com.example.ledurecare.feature.reports.domain.AttachmentKind
import com.example.ledurecare.feature.reports.domain.Dli
import com.example.ledurecare.feature.reports.domain.DliDetail
import com.example.ledurecare.feature.reports.domain.DliPage
import com.example.ledurecare.feature.reports.domain.DliItem
import com.example.ledurecare.feature.reports.domain.DliRepository
import com.example.ledurecare.feature.reports.domain.ReturnStatus
import com.example.ledurecare.feature.reports.domain.StatusChange
import com.example.ledurecare.feature.reports.domain.buildTracker
import kotlinx.coroutines.delay
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Sample DLIs used while `BuildConfig.USE_STATIC_DATA` is on. */
@Singleton
class FakeDliRepository @Inject constructor() : DliRepository {

    /** Everything at once; the sample stage cards count from this. */
    suspend fun getDlis(refresh: Boolean): AppResult<List<Dli>> {
        delay(300)
        return AppResult.Success(StaticDlis.all.map { it.toDli() })
    }

    override suspend fun getDliPage(page: Int, pageSize: Int): AppResult<DliPage> {
        delay(300)
        val all = StaticDlis.all.map { it.toDli() }
        val totalPages = ((all.size + pageSize - 1) / pageSize).coerceAtLeast(1)
        return AppResult.Success(DliPage(all.drop((page - 1) * pageSize).take(pageSize), page, totalPages, all.size))
    }

    override suspend fun getDli(id: Int): AppResult<DliDetail> {
        delay(300)
        val sample = StaticDlis.all.firstOrNull { it.id == id }
            ?: return AppResult.Failure(AppError.Http(404, "DLI not found."))
        return AppResult.Success(sample.toDetail())
    }
}

internal data class SampleDli(
    val id: Int,
    val dliNo: String,
    val dealer: String,
    val bpCode: String,
    val status: ReturnStatus,
    val audit: LocalDate,
    val since: LocalDate,
    val itemsCount: Int,
    val daysInStage: Int,
    val overSla: Boolean,
    val auditedBy: String,
    val box: Int,
    val qty: Int,
    val warehouse: String,
    val turnaround: Int? = null,
    val changes: List<StatusChange>? = null,
)

private fun SampleDli.toDli() = Dli(
    id = id,
    dliNo = dliNo,
    dealerName = dealer,
    bpCode = bpCode,
    status = status,
    auditDate = audit,
    itemsCount = itemsCount,
    daysInStage = daysInStage,
    isOverSla = overSla,
    auditedBy = auditedBy,
    turnaroundDays = turnaround,
)

private val SampleParts = listOf(
    DliItem("LD-2210", "Filter kit", qty = 12, approved = 10, rejected = 2),
    DliItem("LD-3105", "Control board", qty = 2, approved = 0, rejected = 2),
    DliItem("LD-4180", "Power adapter", qty = 6, approved = 6, rejected = 0),
)

private fun SampleDli.toDetail(): DliDetail {
    val history = changes ?: ReturnStatus.entries
        .filter { it != ReturnStatus.Completed && it.ordinal <= status.ordinal }
        .map { stage ->
            val date = if (stage == status) since else audit.plusDays(stage.ordinal + 1L)
            StatusChange(stage.code, date)
        }
    return DliDetail(
        id = id,
        dliNo = dliNo,
        dealerName = dealer,
        status = status,
        daysInStage = daysInStage,
        stageSince = since,
        isOverSla = overSla,
        customerCode = bpCode,
        auditedBy = auditedBy,
        auditDate = audit,
        totalBox = box,
        totalQty = qty,
        warehouse = warehouse,
        items = SampleParts.take(if (id == 1047) 2 else itemsCount.coerceIn(1, SampleParts.size)),
        attachments = listOf(
            Attachment(1, "${dliNo}_Audit_Form.pdf", "https://example.com/static/${dliNo}_Audit_Form.pdf", AttachmentKind.Pdf, true),
            Attachment(2, "defect_photo_1.jpg", "https://example.com/static/defect_photo_1.jpg", AttachmentKind.Image, false),
            Attachment(3, "defect_photo_2.jpg", "https://example.com/static/defect_photo_2.jpg", AttachmentKind.Image, false),
        ),
        tracker = buildTracker(status, audit, history),
    )
}

internal object StaticDlis {
    private fun oct(day: Int) = LocalDate.of(2026, 10, day)

    private val inProgress = listOf(
        SampleDli(
            id = 1060, dliNo = "DLI-1060", dealer = "Verma Motors, Ghaziabad", bpCode = "BP-22310",
            status = ReturnStatus.Wh, audit = oct(20), since = oct(25), itemsCount = 3, daysInStage = 3,
            overSla = false, auditedBy = "Anita Rao", box = 2, qty = 20, warehouse = "Ghaziabad WH – Loni",
        ),
        SampleDli(
            id = 1061, dliNo = "DLI-1061", dealer = "Gupta Spares, Firozabad", bpCode = "BP-23140",
            status = ReturnStatus.Wh, audit = oct(21), since = oct(26), itemsCount = 2, daysInStage = 2,
            overSla = false, auditedBy = "Ravi Kumar", box = 1, qty = 8, warehouse = "Agra WH – Sikandra",
        ),
        SampleDli(
            id = 1062, dliNo = "DLI-1062", dealer = "Delta Auto, Noida", bpCode = "BP-22871",
            status = ReturnStatus.Wh, audit = oct(22), since = oct(27), itemsCount = 1, daysInStage = 1,
            overSla = false, auditedBy = "Imran Khan", box = 1, qty = 6, warehouse = "Noida WH – Sector 63",
        ),
        SampleDli(
            id = 1063, dliNo = "DLI-1063", dealer = "Star Electric, Noida", bpCode = "BP-22655",
            status = ReturnStatus.AdminApproval, audit = oct(20), since = oct(27), itemsCount = 2, daysInStage = 1,
            overSla = false, auditedBy = "Imran Khan", box = 2, qty = 11, warehouse = "Noida WH – Sector 63",
        ),
        SampleDli(
            id = 1064, dliNo = "DLI-1064", dealer = "Amit Electric, Noida", bpCode = "BP-23125",
            status = ReturnStatus.AdminApproval, audit = oct(19), since = oct(28), itemsCount = 3, daysInStage = 0,
            overSla = false, auditedBy = "Anita Rao", box = 2, qty = 14, warehouse = "Noida WH – Sector 63",
        ),
        SampleDli(
            id = 1047, dliNo = "DLI-1047", dealer = "Sharma Auto Agency, Agra", bpCode = "BP-23090",
            status = ReturnStatus.Acc, audit = oct(9), since = oct(19), itemsCount = 14, daysInStage = 9,
            overSla = true, auditedBy = "Ravi Kumar", box = 3, qty = 14, warehouse = "Agra WH – Sikandra",
            changes = listOf(
                StatusChange("coord", oct(10)),
                StatusChange("transit", oct(11)),
                StatusChange("wh", oct(14)),
                StatusChange("admin_approval", oct(16)),
                StatusChange("acc", oct(19)),
            ),
        ),
        SampleDli(
            id = 1052, dliNo = "DLI-1052", dealer = "Sumit Electric, Meerut", bpCode = "BP-23128",
            status = ReturnStatus.Acc, audit = oct(13), since = oct(26), itemsCount = 6, daysInStage = 2,
            overSla = false, auditedBy = "Anita Rao", box = 2, qty = 9, warehouse = "Meerut WH – Partapur",
        ),
        SampleDli(
            id = 1058, dliNo = "DLI-1058", dealer = "ABM Electrical, Bijnor", bpCode = "BP-21222",
            status = ReturnStatus.Sap, audit = oct(14), since = oct(26), itemsCount = 4, daysInStage = 2,
            overSla = false, auditedBy = "Imran Khan", box = 2, qty = 12, warehouse = "Meerut WH – Partapur",
        ),
    )

    private val engineers = listOf("Ravi Kumar", "Anita Rao", "Imran Khan")
    private val places = listOf("Agra", "Meerut", "Noida", "Ghaziabad", "Bareilly", "Mathura")

    /** 74 closed DLIs, alternating 9 and 10 days audit-to-closure (average 9.5). */
    private val completed = List(74) { i ->
        val audit = LocalDate.of(2026, 9, 1).plusDays((i % 28).toLong())
        val turnaround = if (i % 2 == 0) 9 else 10
        SampleDli(
            id = 3000 + i, dliNo = "DLI-%04d".format(900 + i),
            dealer = "Dealer ${i + 1}, ${places[i % places.size]}", bpCode = "BP-%05d".format(20000 + i),
            status = ReturnStatus.Completed, audit = audit, since = audit.plusDays(turnaround.toLong()),
            itemsCount = 2 + i % 6, daysInStage = 2 + i % 15, overSla = false,
            auditedBy = engineers[i % engineers.size], box = 1 + i % 4, qty = 6 + i % 10,
            warehouse = "Agra WH – Sikandra", turnaround = turnaround,
        )
    }

    val all: List<SampleDli> = inProgress + completed
}
