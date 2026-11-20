package com.cinntra.ledurecare.feature.reports.domain

import java.time.LocalDate

/** One summary per stage, in pipeline order, including stages with no DLIs. */
fun summarizeStages(dlis: List<Dli>): List<StageSummary> = ReturnStatus.entries.map { status ->
    val inStage = dlis.filter { it.status == status }
    val days = inStage.map { it.daysInStage }
    StageSummary(
        status = status,
        count = days.size,
        oldestDays = days.maxOrNull() ?: 0,
        avgDays = if (days.isEmpty()) 0 else days.average().toInt(),
        avgTurnaroundDays = inStage.mapNotNull { it.turnaroundDays }.takeIf { it.isNotEmpty() }?.average(),
    )
}

/**
 * Builds the tracker from the DLI's current [status] and its status history.
 * A stage's date is when the DLI last entered it; stages not reached yet have none.
 */
fun buildTracker(status: ReturnStatus, auditDate: LocalDate?, changes: List<StatusChange>): List<TrackerStep> {
    val submitted = TrackerStep(status = null, state = StepState.Done, date = auditDate)
    // "Completed" is the end of the line, not a tracker row: a completed DLI has every row done.
    val stages = ReturnStatus.entries.filter { it != ReturnStatus.Completed }.map { stage ->
        val state = when {
            stage.ordinal < status.ordinal -> StepState.Done
            stage == status -> StepState.Current
            else -> StepState.Upcoming
        }
        val date = if (state == StepState.Upcoming) {
            null
        } else {
            changes.lastOrNull { it.toStatus == stage.code }?.date ?: auditDate.takeIf { stage == ReturnStatus.Coord }
        }
        TrackerStep(stage, state, date)
    }
    return listOf(submitted) + stages
}
