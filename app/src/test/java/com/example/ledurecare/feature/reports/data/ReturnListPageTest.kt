package com.example.ledurecare.feature.reports.data

import com.example.ledurecare.core.common.AppResult
import com.example.ledurecare.core.network.NetworkModule
import com.example.ledurecare.feature.reports.domain.ReturnStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

class ReturnListPageTest {

    private val json = NetworkModule.provideJson()
    private val clock = Clock.fixed(LocalDate.of(2026, 11, 16).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    /** One entry in the shape of a real `GET api/returns/?view=all` answer. */
    private val live = """
        {"status": 200, "message": "Success", "data": {
          "count": 37, "total_pages": 4, "current_page": 1, "page_size": null, "next": null, "previous": null,
          "results": [
            {"id": 41, "dli_no": "19006111226", "dealer_code": "19006", "dealer_name": "Tribeni Constructions Ltd. (Kolkata) (R)",
             "bp_code": "19006", "inspection_date": "2026-11-12", "status": "sap", "status_label": "Moved to SAP",
             "audit_date": "2026-11-12", "is_draft": false, "items_count": 3, "total_qty": 50, "total_box": 20, "days_open": 4,
             "created_by_name": "Service Engineer",
             "tracker": [{"status": "coord", "state": "completed", "at": "2026-11-12T10:45:17.880373Z", "days_in_stage": 0}],
             "sla": {"sla_breached": true, "breach_days": 2},
             "status_changed_at": "2026-11-12T17:41:45.271467+05:30"},
            {"id": 42, "dli_no": "D-DRAFT", "status": "coord", "is_draft": true},
            {"id": 43, "dli_no": "D-NEW", "status": "something_new"}
          ]}, "errors": {}}
    """.trimIndent()

    private fun repo(api: ReturnsApi) = DliRepositoryImpl(api, json, clock, Dispatchers.Unconfined)

    @Test
    fun `a live page maps its entries and paging, skipping drafts and unknown statuses`() = runTest {
        val api = mockk<ReturnsApi>()
        coEvery { api.getReturns(any(), any(), any()) } returns json.parseToJsonElement(live)

        val page = (repo(api).getDliPage(page = 1, pageSize = 10) as AppResult.Success).data

        assertEquals(1, page.page)
        assertEquals(4, page.totalPages)
        assertEquals(37, page.totalCount)
        assertTrue(page.hasMore)
        val dli = page.items.single()
        assertEquals(41, dli.id)
        assertEquals("19006111226", dli.dliNo)
        assertEquals("Tribeni Constructions Ltd. (Kolkata) (R)", dli.dealerName)
        assertEquals(ReturnStatus.Sap, dli.status)
        assertEquals(3, dli.itemsCount)
        assertEquals(4, dli.daysInStage)
        assertTrue(dli.isOverSla) // sla.sla_breached
        assertEquals("Service Engineer", dli.auditedBy)
    }

    @Test
    fun `the page, page size and view are sent to the server`() = runTest {
        val api = mockk<ReturnsApi>()
        coEvery { api.getReturns(any(), any(), any()) } returns json.parseToJsonElement(live)

        repo(api).getDliPage(page = 3, pageSize = 10)

        coVerify { api.getReturns(view = "all", page = 3, pageSize = 10) }
    }

    @Test
    fun `a bare list is one page`() = runTest {
        val api = mockk<ReturnsApi>()
        coEvery { api.getReturns(any(), any(), any()) } returns
            json.parseToJsonElement("""[{"id": 1, "dli_no": "A", "status": "wh"}]""")

        val page = (repo(api).getDliPage(1, 10) as AppResult.Success).data
        assertEquals(1, page.items.size)
        assertEquals(1, page.totalPages)
        assertFalse(page.hasMore)
    }
}
