package com.cinntra.ledurecare.feature.dealers.data

import com.cinntra.ledurecare.core.network.NetworkModule
import com.cinntra.ledurecare.feature.dealers.domain.Dealer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DealerDetailsParsingTest {

    private val json = NetworkModule.provideJson()
    private val dealer = Dealer("9195", "Golden", "9195", "Noida")
    private fun parse(body: String) = json.parseToJsonElement(body).toDealerDetails(json, dealer)

    @Test
    fun `fills billing address, sales person and zone from the live shape`() {
        val d = parse(
            """{"message":"Success","status":200,"data":[{"id":5914,"CardCode":"9195",
              "SalesPersonCode":[{"id":230,"SalesEmployeeCode":"35","SalesEmployeeName":"Mayank "}],
              "BPAddresses":[
                {"AddressType":"bo_ShipTo","Block":"Ship block","City":"Other"},
                {"AddressType":"bo_BillTo","Street":"None","Block":"C-17 Sector - 9 Noida","City":"Gautam Buddha Nagar","State":"UP","ZipCode":"201301"}],
              "U_U_UTL_Zone":"N5 (UP West 1)","U_TYPE":[]}]}"""
        )!!
        assertEquals("C-17 Sector - 9 Noida, Gautam Buddha Nagar, UP, 201301", d.address)
        assertEquals("Mayank", d.salesPerson)
        assertEquals("N5 (UP West 1)", d.zone)
        assertEquals("Golden", d.name)
    }

    @Test
    fun `missing values keep what the dealer already had`() {
        val d = parse("""{"data":[{"CardCode":"9195","SalesPersonCode":[],"BPAddresses":[],"U_U_UTL_Zone":"None"}]}""")!!
        assertEquals("", d.address)
        assertEquals("", d.zone)
    }

    @Test
    fun `an empty answer is no dealer`() {
        assertNull(parse("""{"status":200,"data":[]}"""))
    }
}
