package com.example.ledurecare.feature.reports.data

import com.example.ledurecare.core.common.AppResult
import com.example.ledurecare.core.common.IoDispatcher
import com.example.ledurecare.core.common.safeCall
import com.example.ledurecare.core.network.unwrapObject
import com.example.ledurecare.feature.reports.domain.StageSummary
import com.example.ledurecare.feature.reports.domain.StageSummaryRepository
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
