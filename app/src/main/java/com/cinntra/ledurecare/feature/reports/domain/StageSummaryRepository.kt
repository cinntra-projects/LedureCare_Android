package com.cinntra.ledurecare.feature.reports.domain

import com.cinntra.ledurecare.core.common.AppResult

/** The counters on the Reports stage cards. */
interface StageSummaryRepository {
    suspend fun getStageSummaries(): AppResult<List<StageSummary>>
}
