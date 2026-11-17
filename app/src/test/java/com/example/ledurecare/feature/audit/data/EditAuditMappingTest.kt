package com.example.ledurecare.feature.audit.data

import com.example.ledurecare.core.network.NetworkModule
import com.example.ledurecare.core.network.unwrapObject
import com.example.ledurecare.feature.audit.domain.MfgType
import com.example.ledurecare.feature.audit.domain.WarrantyStatus
import com.example.ledurecare.feature.reports.data.ReturnDetailDto
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class EditAuditMappingTest {

    private val json = NetworkModule.provideJson()

    /** The shape of a real `GET api/returns/{id}/` answer, shortened. */
    private val detail = """
        {"status":200,"data":{"id":41,"dli_no":"19006111226","dealer_code":"19006","dealer_name":"Tribeni","customer_code":"19006",
         "address":"4M New Town Square","sales_person":"Umang","zone":"FF Umang","bp_code":"19006",
         "audit_date":"2026-11-12","inspection_date":"2026-11-12","notes":"some notes",
         "WarehouseAddress":{"id":2,"name":"PATNA"},"total_qty":50,"total_box":20,
         "items":[
           {"id":84,"item_code":"30653FG","item_name":"12W Downlight","qty":11,"approved":11,"rejected":0,"total_approved":10,
            "condition_name":"Packed Return","mfg_type":"MONTH","mfg_month":7,"mfg_quarter":null,"mfg_year":2018,
            "remarks":"plain","warranty_status":"IN_WARRANTY","warranty_remarks":""},
           {"id":86,"item_code":"30759FG","item_name":"AC Box","qty":20,"approved":20,"rejected":0,"total_approved":20,
            "condition_name":"Defective","mfg_type":"QTR","mfg_month":null,"mfg_quarter":4,"mfg_year":2023,
            "remarks":"plain","warranty_status":"OUT_OF_WARRANTY","warranty_remarks":"burnt by surge"}]}}
    """.trimIndent()

    private fun dto() = json.decodeFromJsonElement(ReturnDetailDto.serializer(), json.parseToJsonElement(detail).unwrapObject()!!)

    @Test
    fun `the detail answer becomes the form's starting point`() {
        val edit = dto().toAuditEdit(fallbackYear = 2026)

        assertEquals("19006111226", edit.dliNo)
        assertEquals("19006", edit.dealer.id)
        assertEquals("4M New Town Square", edit.address)
        assertEquals("Umang", edit.salesPerson)
        assertEquals("FF Umang", edit.dealer.zone)
        assertEquals(2, edit.warehouseId)
        assertEquals(20, edit.totalBox)
        assertEquals("some notes", edit.notes)

        val first = edit.items[0]
        assertEquals("30653FG", first.product.code)
        assertEquals(10, first.totalApproved)
        assertEquals(MfgType.Month, first.mfgType)
        assertEquals(7, first.mfgPeriod)
        assertEquals(WarrantyStatus.InWarranty, first.warranty)
        assertEquals("plain", first.remark)

        val second = edit.items[1]
        assertEquals(MfgType.Quarter, second.mfgType)
        assertEquals(4, second.mfgPeriod)
        assertEquals(WarrantyStatus.OutOfWarranty, second.warranty)
        assertEquals("burnt by surge", second.remark) // the special remark
    }

    @Test
    fun `an out of warranty item also sends its special remark`() {
        val items = dto().toAuditEdit(2026).items
        val parts = com.example.ledurecare.feature.audit.domain.AuditDraft(
            dealer = null, address = "", customerCode = "", salesPerson = "", inspectionDate = null, totalBox = null,
            totalQty = null, items = items, warehouse = null, notes = "", files = emptyList(),
        ).toCreateParts(json) { "x".toRequestBody() }
        val sent = parts.first { it.headers!!["Content-Disposition"]!!.contains("name=\"items\"") }
            .let { Buffer().also { buf -> it.body.writeTo(buf) }.readUtf8() }
        val array = json.parseToJsonElement(sent).jsonArray

        assertFalse("warranty_remarks" in array[0].jsonObject)
        assertEquals("burnt by surge", array[1].jsonObject["warranty_remarks"]!!.jsonPrimitive.content)
    }
}
