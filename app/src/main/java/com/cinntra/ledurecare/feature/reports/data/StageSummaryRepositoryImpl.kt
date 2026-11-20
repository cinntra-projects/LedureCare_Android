package com.cinntra.ledurecare.feature.reports.data

import com.cinntra.ledurecare.core.common.AppResult
import com.cinntra.ledurecare.core.common.IoDispatcher
import com.cinntra.ledurecare.core.common.safeCall
import com.cinntra.ledurecare.core.network.unwrapObject
import com.cinntra.ledurecare.feature.reports.domain.StageSummary
import com.cinntra.ledurecare.feature.reports.domain.StageSummaryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json
import javax.inject.Inject

class StageSummaryRepositoryImpl @Inject constructor(
    private val api: ReturnsApi,
    private val json: Json,
    @IoDispatcher private val io: CoroutineDispatcher,
) : StageSummaryRepository {

    override suspend fun getStageSummaries(): AppResult<List<StageSummary>> = safeCall(io) {
        val body = requireNotNull(api.getDashboard().unwrapObject()) { "Unexpected dashboard response" }
        json.decodeFromJsonElement(DashboardDto.serializer(), body).toStageSummaries()
    }
}
