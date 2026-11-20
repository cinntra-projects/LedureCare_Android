package com.cinntra.ledurecare.feature.audit.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class AuditWarrantyTest {

    private val today = LocalDate.of(2026, 10, 28)

    private fun month(m: Int, y: Int, wd: Int = 30) = warrantyFor(wd, MfgType.Month, m, y, today)
    private fun quarter(q: Int, y: Int, wd: Int = 30) = warrantyFor(wd, MfgType.Quarter, q, y, today)

    @Test
    fun `a month up to the warranty period old is in warranty, older is out`() {
        assertEquals(29, monthsSinceMfg(MfgType.Month, 5, 2024, today))
        assertEquals(WarrantyStatus.InWarranty, month(5, 2024)) // 29 months
        assertEquals(WarrantyStatus.InWarranty, month(4, 2024)) // exactly 30
        assertEquals(WarrantyStatus.OutOfWarranty, month(3, 2024)) // 31
    }

    @Test
    fun `a quarter counts as its last month`() {
        assertEquals(WarrantyStatus.InWarranty, quarter(2, 2024)) // June 2024 = 28 months
        assertEquals(WarrantyStatus.OutOfWarranty, quarter(1, 2024)) // March 2024 = 31 months
        assertEquals(WarrantyStatus.InWarranty, quarter(4, 2025)) // December 2025
        assertEquals(WarrantyStatus.InWarranty, quarter(3, 2024, wd = 25)) // September 2024 = 25 months
        assertEquals(WarrantyStatus.OutOfWarranty, quarter(3, 2024, wd = 24))
    }

    @Test
    fun `a date in the future is in warranty`() {
        assertEquals(WarrantyStatus.InWarranty, month(12, 2026))
    }
}
