package com.example.ledurecare.feature.reports.data

import com.example.ledurecare.feature.reports.domain.ReturnStatus
import com.example.ledurecare.feature.reports.domain.StageSummary
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

/** Only the part of `GET api/returns/dashboard/` the stage cards need: `data.pipeline`. */
@Serializable
data class DashboardDto(
    val pipeline: List<PipelineStageDto> = emptyList(),
    val kpis: List<KpiDto> = emptyList(),
)

/** A headline counter. `avg_e2e` is the average audit-to-credit-note time; its `sub` reads "N completed". */
@Serializable
data class KpiDto(
    val key: String? = null,
    val value: Double? = null,
    val sub: String? = null,
)

@Serializable
data class PipelineStageDto(
    val status: PipelineStatusDto? = null,
    val count: Int = 0,
    @SerialName("oldest_waiting_days") val oldestWaitingDays: Double? = null,
    @SerialName("avg_time_here_days") val avgTimeHereDays: Double? = null,
)

@Serializable
data class PipelineStatusDto(
    val code: String? = null,
    val label: String? = null,
    val owner: String? = null,
)

/**
 * One summary per stage the app knows, in the order the server sent them.
 * Titles and owners come from the app's own strings, so the server's labels are not read.
 * A stage with nothing waiting has no oldest or average, which show as 0.
 */
internal fun DashboardDto.toStageSummaries(): List<StageSummary> {
    val stages = pipeline.mapNotNull { stage ->
        val status = ReturnStatus.fromCode(stage.status?.code) ?: return@mapNotNull null
        StageSummary(
            status = status,
            count = stage.count,
            oldestDays = stage.oldestWaitingDays?.roundToInt() ?: 0,
            avgDays = stage.avgTimeHereDays?.roundToInt() ?: 0,
        )
    }
    // Step 8 is not a pipeline stage: the API only reports it through the `avg_e2e` counter.
    return if (stages.isEmpty()) stages else stages + completedSummary()
}

private val COMPLETED_COUNT = Regex("(\\d+)\\s+completed", RegexOption.IGNORE_CASE)

/** The "Completed" card: how many returns are closed and how long audit to closure took on average. */
private fun DashboardDto.completedSummary(): StageSummary {
    val avgE2e = kpis.firstOrNull { it.key == "avg_e2e" }
    return StageSummary(
        status = ReturnStatus.Completed,
        count = avgE2e?.sub?.let { COMPLETED_COUNT.find(it)?.groupValues?.get(1)?.toIntOrNull() } ?: 0,
        oldestDays = 0,
        avgDays = 0,
        avgTurnaroundDays = avgE2e?.value,
    )
}
