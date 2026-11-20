package com.cinntra.ledurecare.feature.reports.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ReportLogicTest {

    private fun dli(id: Int, status: ReturnStatus, days: Int) =
        Dli(id, "DLI-$id", "Dealer $id", "BP-$id", status, null, 1, days, false, null)

    @Test
    fun `summary has one entry per stage in pipeline order, empty stages included`() {
        val summaries = summarizeStages(emptyList())
        assertEquals(ReturnStatus.entries, summaries.map { it.status })
        assertEquals(List(8) { 0 }, summaries.map { it.count })
    }

    @Test
    fun `summary counts, oldest and average per stage`() {
        val summaries = summarizeStages(
            listOf(
                dli(1, ReturnStatus.Acc, 9),
                dli(2, ReturnStatus.Acc, 2),
                dli(3, ReturnStatus.Wh, 3),
            )
        ).associateBy { it.status }

        with(summaries.getValue(ReturnStatus.Acc)) {
            assertEquals(2, count)
            assertEquals(9, oldestDays)
            assertEquals(5, avgDays) // (9 + 2) / 2 = 5.5, shown as whole days
        }
        assertEquals(1, summaries.getValue(ReturnStatus.Wh).count)
        assertEquals(0, summaries.getValue(ReturnStatus.Cn).oldestDays)
    }

    @Test
    fun `tracker marks earlier stages done, current stage current, later ones upcoming`() {
        val audit = LocalDate.of(2026, 10, 9)
        val tracker = buildTracker(
            status = ReturnStatus.Acc,
            auditDate = audit,
            changes = listOf(
                StatusChange("coord", LocalDate.of(2026, 10, 10)),
                StatusChange("transit", LocalDate.of(2026, 10, 11)),
                StatusChange("wh", LocalDate.of(2026, 10, 14)),
                StatusChange("admin_approval", LocalDate.of(2026, 10, 16)),
                StatusChange("acc", LocalDate.of(2026, 10, 19)),
            ),
        )

        assertEquals(8, tracker.size)
        assertNull(tracker.first().status)
        assertEquals(audit, tracker.first().date)
        assertEquals(
            listOf(
                StepState.Done, StepState.Done, StepState.Done, StepState.Done, StepState.Done,
                StepState.Current, StepState.Upcoming, StepState.Upcoming,
            ),
            tracker.map { it.state },
        )
        val current = tracker.single { it.state == StepState.Current }
        assertEquals(ReturnStatus.Acc, current.status)
        assertEquals(LocalDate.of(2026, 10, 19), current.date)
        assertNull(tracker.last().date)
    }

    @Test
    fun `re-entered stage uses the latest date`() {
        val tracker = buildTracker(
            ReturnStatus.Wh,
            LocalDate.of(2026, 10, 1),
            listOf(
                StatusChange("wh", LocalDate.of(2026, 10, 5)),
                StatusChange("admin_approval", LocalDate.of(2026, 10, 7)),
                StatusChange("wh", LocalDate.of(2026, 10, 9)), // bounced back by admin
            ),
        )
        assertEquals(LocalDate.of(2026, 10, 9), tracker.single { it.state == StepState.Current }.date)
    }

    @Test
    fun `coordinator step falls back to the audit date when there is no history`() {
        val audit = LocalDate.of(2026, 10, 9)
        val tracker = buildTracker(ReturnStatus.Coord, audit, emptyList())
        assertEquals(audit, tracker[1].date)
    }

    @Test
    fun `search matches dli number, dealer and bp code`() {
        val d = Dli(1, "DLI-1047", "Sharma Auto Agency", "BP-23090", ReturnStatus.Acc, null, 1, 0, false, null)
        assertEquals(true, d.matches("1047"))
        assertEquals(true, d.matches("sharma"))
        assertEquals(true, d.matches("bp-23090"))
        assertEquals(false, d.matches("verma"))
        assertEquals(true, d.matches("  ".trim()))
    }
}
