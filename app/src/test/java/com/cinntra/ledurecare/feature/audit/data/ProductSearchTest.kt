package com.cinntra.ledurecare.feature.audit.data

import com.cinntra.ledurecare.core.common.AppResult
import com.cinntra.ledurecare.core.network.NetworkModule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Test

class ProductSearchTest {

    private val json = NetworkModule.provideJson()

    /** The shape of a real `item/bp_order_items` answer. */
    private val answer = """
        {"message":"Success","status":200,"data":[
          {"id":1234,"ItemCode":"30764FG","ItemName":"Nexa Magnetic LED Driver 100W 48V, 1835mA","UnitPrice":"4500.0",
           "Discount":"0.0","HSN":"85044090","UoS":"PCS","U_GST":"18","Status":"tYES","TaxCode":""},
          {"id":987,"ItemCode":"20511FG","ItemName":"LED Panel Light 12W","UnitPrice":"320.0","Discount":"5.0",
           "HSN":"94054090","UoS":"PCS","U_GST":"18","Status":"tYES","TaxCode":""},
          {"id":7,"ItemCode":"WD30","ItemName":"With warranty","U_UTL_WD":30},
          {"id":8,"ItemCode":"WDTXT","ItemName":"Warranty as text","U_UTL_WD":"24"},
          {"id":9,"ItemCode":"WDNONE","ItemName":"Empty warranty","U_UTL_WD":""},
          {"id":5,"ItemCode":"OLD1","ItemName":"Retired item","Status":"tNO"},
          {"id":6,"ItemName":"No code"}],
         "total":4,"has_more":false,"PageNo":1,"MaxSize":50}
    """.trimIndent()

    @Test
    fun `request carries the dealer, the text and the paging`() {
        assertEquals(
            """{"CardCode":"C0001","SearchText":"LED","PageNo":1,"MaxSize":20}""",
            json.encodeToString(BpItemsRequest("C0001", "LED", 1, 20)),
        )
        assertEquals(
            """{"CardCode":"C0001","PageNo":1,"MaxSize":20}""",
            json.encodeToString(BpItemsRequest("C0001", null, 1, 20)),
        )
    }

    @Test
    fun `entries map to products, leaving out inactive and codeless ones`() = runTest {
        val api = mockk<ProductApi>()
        coEvery { api.searchItems(any()) } returns json.parseToJsonElement(answer)

        val products = (ProductRepositoryImpl(api, json, Dispatchers.Unconfined).search("C0001", "LED") as AppResult.Success).data

        assertEquals(listOf("30764FG", "20511FG", "WD30", "WDTXT", "WDNONE"), products.map { it.code })
        assertEquals(listOf(null, null, 30, 24, null), products.map { it.warrantyMonths }) // U_UTL_WD
        with(products[0]) {
            assertEquals("Nexa Magnetic LED Driver 100W 48V, 1835mA", name)
            assertEquals("4500.0", unitPrice)
            assertEquals("85044090", hsn)
            assertEquals("PCS", uom)
            assertEquals("18", gst)
            assertEquals(null, taxCode) // blank
        }
        coVerify { api.searchItems(BpItemsRequest("C0001", "LED", 1, PRODUCT_PAGE_SIZE)) }
    }
}
