package com.example.ledurecare.feature.dealers.data

import com.example.ledurecare.core.network.NetworkModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DealerPageParsingTest {

    private val json = NetworkModule.provideJson()
    private fun page(body: String, requested: Int = 1) = json.parseToJsonElement(body).toDealerPage(json, requested)

    @Test
    fun `paged response carries page, total pages and count`() {
        val p = page(
            """{"status":200,"data":{"count":25,"total_pages":3,"current_page":2,
               "results":[{"card_code":"A1","name":"Alpha","city":"Patna"}]}}""",
            requested = 2,
        )
        assertEquals(2, p.page)
        assertEquals(3, p.totalPages)
        assertEquals(25, p.totalCount)
        assertEquals(listOf("A1"), p.dealers.map { it.id })
        assertTrue(p.hasMore)
    }

    @Test
    fun `last page has no more`() {
        val p = page("""{"data":{"count":25,"total_pages":3,"current_page":3,"results":[]}}""", requested = 3)
        assertFalse(p.hasMore)
    }

    @Test
    fun `a bare list is a single page`() {
        val p = page("""{"status":200,"data":[{"card_code":"A1","name":"Alpha","city":"Patna"},{"card_code":"","name":"No code"}]}""")
        assertEquals(1, p.totalPages)
        assertEquals(listOf("A1"), p.dealers.map { it.id }) // blank card_code skipped
        assertFalse(p.hasMore)
    }
}
