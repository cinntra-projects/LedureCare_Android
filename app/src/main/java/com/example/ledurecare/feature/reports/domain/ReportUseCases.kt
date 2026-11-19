package com.example.ledurecare.feature.reports.domain

import com.example.ledurecare.core.common.AppResult
import javax.inject.Inject

class GetStageSummariesUseCase @Inject constructor(private val repository: StageSummaryRepository) {
    suspend operator fun invoke(): AppResult<List<StageSummary>> = repository.getStageSummaries()
}

class GetDlisUseCase @Inject constructor(private val repository: DliRepository) {
    suspend operator fun invoke(page: Int = 1, pageSize: Int = DLI_PAGE_SIZE, filter: DliFilter = DliFilter()): AppResult<DliPage> =
        repository.getDliPage(page, pageSize, filter)
}

class GetDliDetailUseCase @Inject constructor(private val repository: DliRepository) {
    suspend operator fun invoke(id: Int): AppResult<DliDetail> = repository.getDli(id)
}
