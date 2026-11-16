package com.example.ledurecare.feature.reports.domain

import com.example.ledurecare.core.common.AppResult

/** The counters on the Reports stage cards. */
interface StageSummaryRepository {
    suspend fun getStageSummaries(): AppResult<List<StageSummary>>
}
