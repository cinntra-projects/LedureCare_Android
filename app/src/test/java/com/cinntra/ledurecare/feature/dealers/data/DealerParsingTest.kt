package com.cinntra.ledurecare.feature.dealers.data

import com.cinntra.ledurecare.core.network.NetworkModule
import com.cinntra.ledurecare.core.network.findArray
import com.cinntra.ledurecare.feature.dealers.domain.Dealer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class DealerParsingTest {

    private val json: Json = NetworkModule.provideJson()

    private fun parse(body: String): List<Dealer> = json.parseToJsonElement(body).findArray()
        .mapNotNull { json.decodeFromJsonElement(DealerDto.serializer(), it).toDomain() }

    /** The shape of a real `GET api/dealers/bp-lookup/` response. */
    private val liveResponse = """
        {
          "status": 200,
          "message": "Success",
          "data": [
            {"card_code": "10008C", "name": "Ledure Lightings Limited (Bihar)", "city": "Patna"},
            {"card_code": "10064W", "name": "Prakash Electricals (Kaimur)", "city": "Bhabua"}
          ],
          "errors": {}
        }
    """.trimIndent()

    @Test
    fun `live response maps card code, name and city`() {
        val dealers = parse(liveResponse)

        assertEquals(2, dealers.size)
        with(dealers[0]) {
            assertEquals("10008C", id)
            assertEquals("10008C", bpCode)
            assertEquals("Ledure Lightings Limited (Bihar)", name)
            assertEquals("Patna", city)
        }
        assertEquals("Prakash Electricals (Kaimur)", dealers[1].name)
    }

    @Test
    fun `the card code is the dealer's identity`() {
        assertEquals(listOf("10008C", "10064W"), parse(liveResponse).map { it.id })
    }

    @Test
    fun `fields the endpoint does not give are blank`() {
        with(parse(liveResponse).first()) {
            assertEquals("", zone)
            assertEquals("", address)
            assertEquals("", salesPerson)
        }
    }

    @Test
    fun `an entry without a card code is skipped`() {
        val dealers = parse(
            """{"data": [
                {"name": "No code", "city": "Agra"},
                {"card_code": "  ", "name": "Blank code"},
                {"card_code": null, "name": "Null code"},
                {"card_code": "10001C", "name": "Kept", "city": "Noida"}]}"""
        )
        assertEquals(listOf("10001C"), dealers.map { it.id })
    }

    @Test
    fun `a missing name falls back to the code and a missing city is blank`() {
        val dealer = parse("""{"data": [{"card_code": "10002C", "name": null, "city": null}]}""").single()
        assertEquals("10002C", dealer.name)
        assertEquals("", dealer.city)
    }

    @Test
    fun `values are trimmed`() {
        val dealer = parse("""{"data": [{"card_code": " 10003C ", "name": "  Delta Auto ", "city": " Noida "}]}""").single()
        assertEquals("10003C", dealer.id)
        assertEquals("Delta Auto", dealer.name)
        assertEquals("Noida", dealer.city)
    }

    @Test
    fun `bare, enveloped and paginated list shapes all work`() {
        val row = """{"card_code": "10004C", "name": "X", "city": "Y"}"""
        assertEquals(1, parse("[$row]").size)
        assertEquals(1, parse("""{"data": [$row]}""").size)
        assertEquals(1, parse("""{"results": [$row]}""").size)
        assertEquals(1, parse("""{"data": {"results": [$row]}}""").size)
    }

    @Test
    fun `an unexpected shape gives an empty list`() {
        assertEquals(0, parse("""{"status": 200, "data": {}}""").size)
        assertEquals(0, parse("""{"status": 200}""").size)
    }
}
