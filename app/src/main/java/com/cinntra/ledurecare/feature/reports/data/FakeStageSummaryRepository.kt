package com.cinntra.ledurecare.feature.reports.data

import com.cinntra.ledurecare.core.common.AppResult
import com.cinntra.ledurecare.core.common.map
import com.cinntra.ledurecare.feature.reports.domain.StageSummary
import com.cinntra.ledurecare.feature.reports.domain.StageSummaryRepository
import com.cinntra.ledurecare.feature.reports.domain.summarizeStages
import javax.inject.Inject
import javax.inject.Singleton

/** Sample stage cards, counted from the sample DLIs so they always agree with the sample list. */
@Singleton
class FakeStageSummaryRepository @Inject constructor(
    private val dlis: FakeDliRepository,
) : StageSummaryRepository {

    override suspend fun getStageSummaries(): AppResult<List<StageSummary>> =
        dlis.getDlis(refresh = true).map(::summarizeStages)
}
