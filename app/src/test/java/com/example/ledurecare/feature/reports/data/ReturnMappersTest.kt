package com.example.ledurecare.feature.reports.data

import com.example.ledurecare.core.network.NetworkModule
import com.example.ledurecare.core.network.findArray
import com.example.ledurecare.core.network.unwrapObject
import com.example.ledurecare.feature.reports.domain.AttachmentKind
import com.example.ledurecare.feature.reports.domain.ReturnStatus
import com.example.ledurecare.feature.reports.domain.StepState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReturnMappersTest {

    private val json = NetworkModule.provideJson()
    private val today = LocalDate.of(2026, 10, 28)

    private fun listItems(body: String) = json.parseToJsonElement(body).findArray()
        .map { json.decodeFromJsonElement(ReturnListDto.serializer(), it) }

    @Test
    fun `list item maps status, days in stage and audited by`() {
        val dli = listItems(
            """[{"id": 5, "dli_no": "DLI-1047", "dealer_name": "Sharma Auto Agency", "bp_code": "BP-23090",
                "audit_date": "2026-10-09", "status": "acc", "is_draft": false, "items_count": 14,
                "status_changed_at": "2026-10-19T10:00:00+05:30", "created_by_name": "Ravi Kumar",
                "tracker": [], "sla": {}}]"""
        ).single().toDomain(today)!!

        assertEquals(ReturnStatus.Acc, dli.status)
        assertEquals(9, dli.daysInStage)
        assertEquals("Ravi Kumar", dli.auditedBy)
        assertEquals(LocalDate.of(2026, 10, 9), dli.auditDate)
        assertEquals(false, dli.isOverSla)
    }

    @Test
    fun `drafts and unknown statuses are dropped`() {
        val items = listItems(
            """{"data": [
                {"id": 1, "status": "coord", "is_draft": true},
                {"id": 2, "status": "mystery"},
                {"id": 3, "status": null},
                {"id": 4, "status": "cn"}]}"""
        ).mapNotNull { it.toDomain(today) }
        assertEquals(listOf(4), items.map { it.id })
    }

    @Test
    fun `days in stage falls back to audit date and never goes negative`() {
        val fromAudit = listItems("""[{"id": 1, "status": "wh", "audit_date": "2026-10-20"}]""").single().toDomain(today)!!
        assertEquals(8, fromAudit.daysInStage)
        val future = listItems("""[{"id": 2, "status": "wh", "status_changed_at": "2026-11-05"}]""").single().toDomain(today)!!
        assertEquals(0, future.daysInStage)
    }

    @Test
    fun `any known sla breach flag marks the dli over sla`() {
        val breached = listItems("""[{"id": 1, "status": "acc", "sla": {"is_breached": true, "days": 3}}]""").single().toDomain(today)!!
        assertTrue(breached.isOverSla)
        val fine = listItems("""[{"id": 2, "status": "acc", "sla": {"is_breached": false}}]""").single().toDomain(today)!!
        assertEquals(false, fine.isOverSla)
    }

    @Test
    fun `detail maps items, attachments and builds the tracker from history`() {
        val body = """
            {"status": 200, "data": {
              "id": 5, "dli_no": "DLI-1047", "dealer_name": "Sharma Auto Agency", "customer_code": "BP-23090",
              "audit_date": "2026-10-09", "status": "acc", "status_changed_at": "2026-10-19T10:00:00+05:30",
              "total_qty": 14, "total_box": 3, "warehouse": "Agra WH - Sikandra",
              "items": [
                {"item_code": "LD-2210", "item_name": "Filter kit", "qty": 12, "approved": 10, "rejected": 2},
                {"item_code": "LD-3105", "item_name": "Control board", "qty": 2, "approved": null, "rejected": null}],
              "photos": [{"id": 1, "url": "/media/return-photos/1/", "original_name": "DLI-1047_Audit_Form.pdf", "content_type": "application/pdf", "stage": "AUDIT"}],
              "dispatch": {"attachments": [{"id": 2, "url": "http://x/y.jpg", "original_name": "defect_photo_1.jpg", "content_type": "image/jpeg"}]},
              "warehouse_receipt": null,
              "sla": {"breached": true}}}
        """.trimIndent()
        val dto = json.decodeFromJsonElement(
            ReturnDetailDto.serializer(),
            json.parseToJsonElement(body).unwrapObject()!!,
        )
        val changes = listOf(
            TrackerEntryDto("coord", "completed", "2026-10-10T09:00:00+05:30").toChange()!!,
            TrackerEntryDto("acc", "current", "2026-10-19T10:00:00+05:30").toChange()!!,
        )

        val detail = dto.toDomain(json, changes, "Ravi Kumar", today)!!

        assertEquals("BP-23090", detail.customerCode)
        assertEquals("Ravi Kumar", detail.auditedBy)
        assertEquals(9, detail.daysInStage)
        assertEquals(LocalDate.of(2026, 10, 19), detail.stageSince)
        assertTrue(detail.isOverSla)
        assertEquals(2, detail.items.size)
        assertNull(detail.items[1].approved)
        assertEquals(listOf(AttachmentKind.Pdf, AttachmentKind.Image), detail.attachments.map { it.kind })
        assertEquals(listOf(true, false), detail.attachments.map { it.isAuditForm })
        assertEquals(StepState.Current, detail.tracker.single { it.state == StepState.Current }.state)
    }

    @Test
    fun `live detail takes the warehouse name, auditor and tracker dates from the answer`() {
        val body = """
            {"status": 200, "data": {
              "id": 41, "dli_no": "19006111226", "dealer_name": "Tribeni Constructions Ltd. (Kolkata) (R)",
              "customer_code": "19006", "audit_date": "2026-11-12", "status": "sap",
              "status_changed_at": "2026-11-12T17:41:45.271467+05:30", "warehouse": "",
              "WarehouseAddress": {"id": 2, "name": "PATNA", "dispatch_city": "Patna"},
              "total_qty": 50, "total_box": 20,
              "items": [{"id": 84, "item_code": "30653FG", "item_name": "12W Air Downlight Black (N.W.)",
                         "unit_price": "480.00", "qty": 11, "approved": 11, "rejected": 0, "total_box": null}],
              "photos": [{"id": 60, "url": "http://h/api/media/return-photos/60/?token=t", "original_name": "eoa.htm",
                          "content_type": "text/html", "stage": "AUDIT", "uploaded_by_name": "Service Engineer"}],
              "tracker": [
                {"status": "coord", "state": "completed", "at": "2026-11-12T10:45:17.880373Z", "days_in_stage": 0},
                {"status": "transit", "state": "completed", "at": "2026-11-12T11:59:40.609695Z"},
                {"status": "wh", "state": "completed", "at": "2026-11-13T12:08:14.081035Z"},
                {"status": "admin_approval", "state": "completed", "at": "2026-11-14T12:09:46.960736Z"},
                {"status": "acc", "state": "completed", "at": "2026-11-15T12:10:13.397359Z"},
                {"status": "sap", "state": "current", "at": "2026-11-16T12:11:45.273158Z", "days_in_stage": 4},
                {"status": "cn", "state": "waiting", "at": null}],
              "sla": {"sla_breached": true, "breach_days": 2}}}
        """.trimIndent()
        val dto = json.decodeFromJsonElement(ReturnDetailDto.serializer(), json.parseToJsonElement(body).unwrapObject()!!)

        val detail = dto.toDomain(json, dto.tracker.mapNotNull { it.toChange() }, auditedBy = null, today = today)!!

        assertEquals("PATNA", detail.warehouse)
        assertEquals("Service Engineer", detail.auditedBy)
        assertTrue(detail.isOverSla)
        assertEquals(1, detail.attachments.size)
        assertEquals(StepState.Done, detail.tracker.first { it.status == ReturnStatus.Acc }.state)
        assertEquals(LocalDate.of(2026, 11, 13), detail.tracker.first { it.status == ReturnStatus.Wh }.date)
        assertEquals(StepState.Current, detail.tracker.first { it.status == ReturnStatus.Sap }.state)
        assertNull(detail.tracker.first { it.status == ReturnStatus.Cn }.date) // not reached yet
    }

    @Test
    fun `wrapped and bare detail bodies both unwrap`() {
        assertEquals(7, json.parseToJsonElement("""{"id": 7, "status": "wh"}""").unwrapObject()!!["id"].toString().toInt())
        assertEquals(7, json.parseToJsonElement("""{"data": {"id": 7}}""").unwrapObject()!!["id"].toString().toInt())
    }

    @Test
    fun `detail with an unknown status is rejected`() {
        val dto = json.decodeFromJsonElement(ReturnDetailDto.serializer(), json.parseToJsonElement("""{"id": 1, "status": "zzz"}"""))
        assertNull(dto.toDomain(json, emptyList(), null, today))
    }
}
