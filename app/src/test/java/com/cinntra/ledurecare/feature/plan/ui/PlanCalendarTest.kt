package com.cinntra.ledurecare.feature.plan.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class PlanCalendarTest {

    @Test
    fun `week runs monday to sunday`() {
        // Wed, 28 Oct 2026
        val range = weekRange(LocalDate.of(2026, 10, 28))
        assertEquals(LocalDate.of(2026, 10, 26), range.start)
        assertEquals(LocalDate.of(2026, 11, 1), range.end)
        assertEquals(7, weekDates(LocalDate.of(2026, 10, 28)).size)
    }

    @Test
    fun `monday and sunday stay in the same week`() {
        assertEquals(weekStart(LocalDate.of(2026, 10, 26)), weekStart(LocalDate.of(2026, 11, 1)))
    }

    @Test
    fun `october 2026 grid starts on thursday and pads to full weeks`() {
        val grid = monthGrid(YearMonth.of(2026, 10))
        assertEquals(5, grid.size)
        assertEquals(listOf(null, null, null), grid.first().take(3))
        assertEquals(LocalDate.of(2026, 10, 1), grid.first()[3])
        assertEquals(LocalDate.of(2026, 10, 31), grid.last()[5])
        assertNull(grid.last()[6])
        assertEquals(31, grid.flatten().filterNotNull().size)
    }

    @Test
    fun `month range covers first to last day`() {
        val range = monthRange(YearMonth.of(2026, 2))
        assertEquals(LocalDate.of(2026, 2, 1), range.start)
        assertEquals(LocalDate.of(2026, 2, 28), range.end)
    }
}
