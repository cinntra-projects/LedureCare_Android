package com.example.ledurecare.feature.audit.data

import com.example.ledurecare.core.network.NetworkModule
import com.example.ledurecare.feature.audit.domain.AuditDraft
import com.example.ledurecare.feature.audit.domain.AuditFile
import com.example.ledurecare.feature.audit.domain.AuditItem
import com.example.ledurecare.feature.audit.domain.MfgType
import com.example.ledurecare.feature.audit.domain.Product
import com.example.ledurecare.feature.audit.domain.Warehouse
import com.example.ledurecare.feature.audit.domain.WarrantyStatus
import com.example.ledurecare.feature.dealers.domain.Dealer
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CreateReturnPartsTest {

    private val json = NetworkModule.provideJson()

    private val monthItem = AuditItem(
        id = 1, product = Product("30653FG", "12W Air Downlight Black (N.W.)"), qty = 11, approved = 10, rejected = 1,
        totalApproved = 10, mfgType = MfgType.Month, mfgPeriod = 7, mfgYear = 2018, condition = "Packed Return",
        warranty = WarrantyStatus.InWarranty, remark = "cmldfms",
    )
    private val quarterItem = monthItem.copy(
        id = 2, mfgType = MfgType.Quarter, mfgPeriod = 4, mfgYear = 2023, warranty = WarrantyStatus.OutOfWarranty, remark = "",
    )

    private val draft = AuditDraft(
        dealer = Dealer("19006", "Tribeni Constructions Ltd. (Kolkata) (R)", "19006", "Kolkata", zone = "FF Umang"),
        address = "4M New Town Square, Kolkata",
        customerCode = "19006",
        salesPerson = "Umang",
        inspectionDate = LocalDate.of(2026, 11, 16),
        totalBox = 20,
        totalQty = 50,
        items = listOf(monthItem, quarterItem),
        warehouse = Warehouse(2, "PATNA", "Plot 4155, Patna", details = """{"id":2,"name":"PATNA","gstin":"10AACCL8907C1Z0"}"""),
        notes = "some notes",
        files = listOf(AuditFile("content://doc/1", "photo.jpg", 5, "image/jpeg")),
    )

    private fun MultipartBody.Part.name() = Regex("name=\"([^\"]*)\"").find(headers!!["Content-Disposition"]!!)!!.groupValues[1]
    private fun MultipartBody.Part.text() = Buffer().also { body.writeTo(it) }.readUtf8()

    private fun parts() = draft.toCreateParts(json) { "bytes".toRequestBody() }
    private fun field(name: String) = parts().first { it.name() == name }.text()

    @Test
    fun `plain fields are sent as text parts`() {
        assertEquals("19006", field("dealer_code"))
        assertEquals("Tribeni Constructions Ltd. (Kolkata) (R)", field("dealer_name"))
        assertEquals("19006", field("customer_code"))
        assertEquals("4M New Town Square, Kolkata", field("address"))
        assertEquals("Umang", field("sales_person"))
        assertEquals("FF Umang", field("zone"))
        assertEquals("2026-11-16", field("audit_date"))
        assertEquals("some notes", field("notes"))
        assertEquals("false", field("is_draft"))
        assertEquals("50", field("total_qty"))
        assertEquals("20", field("total_box"))
    }

    @Test
    fun `items go as a JSON list with only the matching month or quarter`() {
        val items = json.parseToJsonElement(field("items")).jsonArray

        assertEquals(2, items.size)
        val first = items[0].jsonObject
        assertEquals("30653FG", first["item_code"]!!.jsonPrimitive.content)
        assertEquals("MONTH", first["mfg_type"]!!.jsonPrimitive.content)
        assertEquals(7, first["mfg_month"]!!.jsonPrimitive.content.toInt())
        assertFalse("mfg_quarter" in first)
        assertEquals("IN_WARRANTY", first["warranty_status"]!!.jsonPrimitive.content)
        assertEquals("Packed Return", first["condition_name"]!!.jsonPrimitive.content)

        val second = items[1].jsonObject
        assertEquals("QTR", second["mfg_type"]!!.jsonPrimitive.content)
        assertEquals(4, second["mfg_quarter"]!!.jsonPrimitive.content.toInt())
        assertFalse("mfg_month" in second)
        assertEquals("OUT_OF_WARRANTY", second["warranty_status"]!!.jsonPrimitive.content)
    }

    @Test
    fun `the warehouse is sent as the server's own object`() {
        assertEquals("""{"id":2,"name":"PATNA","gstin":"10AACCL8907C1Z0"}""", field("WarehouseAddress"))
    }

    @Test
    fun `a sample warehouse is sent as a small object`() {
        val sample = draft.copy(warehouse = Warehouse(1, "Noida WH", "Sector 63"))
        val sent = sample.toCreateParts(json) { "x".toRequestBody() }.first { it.name() == "WarehouseAddress" }.text()
        val obj = json.parseToJsonElement(sent).jsonObject
        assertEquals("Noida WH", obj["name"]!!.jsonPrimitive.content)
        assertEquals("1", obj["id"]!!.jsonPrimitive.content)
    }

    @Test
    fun `each file is its own attachments part with its name`() {
        val files = parts().filter { it.name() == "attachments" }
        assertEquals(1, files.size)
        assertTrue(files.single().headers!!["Content-Disposition"]!!.contains("filename=\"photo.jpg\""))
        assertEquals("bytes", files.single().text())
    }
}
