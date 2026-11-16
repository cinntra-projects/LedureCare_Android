package com.example.ledurecare.feature.reports.data

import com.example.ledurecare.core.network.NetworkModule
import com.example.ledurecare.core.network.unwrapObject
import com.example.ledurecare.feature.reports.domain.ReturnStatus
import com.example.ledurecare.feature.reports.domain.StageSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardParsingTest {

    private val json = NetworkModule.provideJson()

    private fun summaries(body: String): List<StageSummary> =
        json.decodeFromJsonElement(
            DashboardDto.serializer(),
            requireNotNull(json.parseToJsonElement(body).unwrapObject()),
        ).toStageSummaries()

    /** The pipeline of a real `GET api/returns/dashboard/` response, with the other sections trimmed. */
    private val liveResponse = """
        {
          "status": 200,
          "message": "Success",
          "data": {
            "as_of": "2026-11-02T11:42:42.758218Z",
            "kpis": [{"key": "open_returns", "label": "Open returns", "value": 34, "sub": "of 34 in total",
                      "tone": "default", "total_amount": 1243796, "total_qty": 3573},
                     {"key": "avg_e2e", "label": "Average audit to credit note", "value": null, "sub": "0 completed"}],
            "pipeline": [
              {"status": {"code": "coord", "label": "Pending at service coordinator", "owner": "Service Coordinator"},
               "count": 11, "oldest_waiting_days": 12, "total_value": null, "avg_time_here_days": 0, "is_your_stage": false},
              {"status": {"code": "transit", "label": "In transit", "owner": "Transporter"},
               "count": 3, "oldest_waiting_days": 11, "total_value": null, "avg_time_here_days": 0, "is_your_stage": false},
              {"status": {"code": "wh", "label": "Pending at warehouse", "owner": "Warehouse"},
               "count": 1, "oldest_waiting_days": 5, "total_value": null, "avg_time_here_days": 2, "is_your_stage": false},
              {"status": {"code": "admin_approval", "label": "Pending admin approval", "owner": "Admin"},
               "count": 8, "oldest_waiting_days": 17, "total_value": null, "avg_time_here_days": 1, "is_your_stage": true},
              {"status": {"code": "acc", "label": "Pending at accounts", "owner": "Accounts Team"},
               "count": 6, "oldest_waiting_days": 19, "total_value": null, "avg_time_here_days": 5, "is_your_stage": false},
              {"status": {"code": "sap", "label": "Moved to SAP", "owner": "Waiting for SAP"},
               "count": 5, "oldest_waiting_days": 19, "total_value": null, "avg_time_here_days": null, "is_your_stage": false},
              {"status": {"code": "cn", "label": "Credit note created", "owner": "Synced from SAP"},
               "count": 0, "oldest_waiting_days": null, "total_value": 0, "avg_time_here_days": null, "is_your_stage": false}
            ],
            "needs_action": [{"id": 8, "dli_no": "7136W101426", "status": "acc", "dealer": "Retail Sale (A)", "waiting_days": 19}],
            "recent_activity": []
          },
          "errors": {}
        }
    """.trimIndent()

    @Test
    fun `live pipeline maps to the seven stage cards in order, then the completed card`() {
        val cards = summaries(liveResponse)

        assertEquals(
            listOf(
                ReturnStatus.Coord, ReturnStatus.Transit, ReturnStatus.Wh, ReturnStatus.AdminApproval,
                ReturnStatus.Acc, ReturnStatus.Sap, ReturnStatus.Cn, ReturnStatus.Completed,
            ),
            cards.map { it.status },
        )
        assertEquals(listOf(11, 3, 1, 8, 6, 5, 0, 0), cards.map { it.count })
        assertEquals(listOf(12, 11, 5, 17, 19, 19, 0, 0), cards.map { it.oldestDays })
        assertEquals(listOf(0, 0, 2, 1, 5, 0, 0, 0), cards.map { it.avgDays })
    }

    @Test
    fun `step 8 takes its count and average from the avg_e2e counter`() {
        val completed = summaries(
            """{"data": {"kpis": [{"key": "avg_e2e", "value": 14.5, "sub": "12 completed"}],
                "pipeline": [{"status": {"code": "wh"}, "count": 1}]}}"""
        ).last()
        assertEquals(ReturnStatus.Completed, completed.status)
        assertEquals(12, completed.count)
        assertEquals(14.5, completed.avgTurnaroundDays!!, 0.0)
    }

    @Test
    fun `step 8 shows zero when the counter is missing`() {
        val completed = summaries("""{"data": {"pipeline": [{"status": {"code": "wh"}, "count": 1}]}}""").last()
        assertEquals(ReturnStatus.Completed, completed.status)
        assertEquals(0, completed.count)
    }

    @Test
    fun `a stage with nothing waiting shows zero days, not a crash`() {
        val credit = summaries(liveResponse).first { it.status == ReturnStatus.Cn }
        assertEquals(0, credit.count)
        assertEquals(0, credit.oldestDays)
        assertEquals(0, credit.avgDays)
    }

    @Test
    fun `fractional days are rounded and unknown stages are skipped`() {
        val cards = summaries(
            """{"data": {"pipeline": [
                {"status": {"code": "acc"}, "count": 2, "oldest_waiting_days": 9.6, "avg_time_here_days": 4.4},
                {"status": {"code": "something_new"}, "count": 9, "oldest_waiting_days": 1},
                {"status": null, "count": 4}]}}"""
        )
        assertEquals(2, cards.size) // Accounts, then the Completed card
        assertEquals(10, cards.first().oldestDays)
        assertEquals(4, cards.first().avgDays)
    }

    @Test
    fun `a response without a pipeline gives no cards`() {
        assertEquals(emptyList<StageSummary>(), summaries("""{"status": 200, "data": {"kpis": []}}"""))
    }

    @Test
    fun `bare and enveloped bodies both unwrap`() {
        val pipeline = """"pipeline": [{"status": {"code": "wh"}, "count": 1}]"""
        assertEquals(2, summaries("""{"data": {$pipeline}}""").size)
        assertEquals(2, summaries("""{$pipeline}""").size)
    }
}
