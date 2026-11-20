package com.cinntra.ledurecare.feature.dealers.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DealerSearchTest {

    private val dealer = Dealer(id = "10008C", name = "Ledure Lightings Limited (Bihar)", bpCode = "10008C", city = "Patna")

    @Test
    fun `a blank query matches everything`() {
        assertTrue(dealer.matches(""))
        assertTrue(dealer.matches("   "))
    }

    @Test
    fun `matches name, code and city in any case`() {
        assertTrue(dealer.matches("lightings"))
        assertTrue(dealer.matches("BIHAR"))
        assertTrue(dealer.matches("10008c"))
        assertTrue(dealer.matches("patna"))
    }

    @Test
    fun `surrounding spaces in the query are ignored`() {
        assertTrue(dealer.matches("  patna "))
    }

    @Test
    fun `other text does not match`() {
        assertFalse(dealer.matches("noida"))
        assertFalse(dealer.matches("99999"))
    }

    @Test
    fun `filtering a list keeps the order`() {
        val dealers = listOf(
            dealer,
            Dealer("10064W", "Prakash Electricals (Kaimur)", "10064W", "Bhabua"),
            Dealer("10009C", "Ledure Lightings Limited (Delhi)", "10009C", "Delhi"),
        )
        assertEquals(listOf("10008C", "10009C"), dealers.filter { it.matches("ledure") }.map { it.id })
    }
}
