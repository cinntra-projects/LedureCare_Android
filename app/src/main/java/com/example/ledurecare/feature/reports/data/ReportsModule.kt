package com.example.ledurecare.feature.reports.data

import com.example.ledurecare.BuildConfig
import com.example.ledurecare.feature.reports.domain.DliRepository
import com.example.ledurecare.feature.reports.domain.StageSummaryRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ReportsProvidesModule {

    @Provides
    @Singleton
    fun provideReturnsApi(retrofit: Retrofit): ReturnsApi = retrofit.create(ReturnsApi::class.java)

    /** The stage cards: sample data or the dashboard API, chosen by `USE_STATIC_STAGES`. */
    @Provides
    @Singleton
    fun provideStageSummaryRepository(
        real: Provider<StageSummaryRepositoryImpl>,
        fake: Provider<FakeStageSummaryRepository>,
    ): StageSummaryRepository = if (BuildConfig.USE_STATIC_STAGES) fake.get() else real.get()

    /** The DLI list and detail: sample data or the returns API, chosen by `USE_STATIC_DLIS`. */
    @Provides
    @Singleton
    fun provideDliRepository(
        real: Provider<DliRepositoryImpl>,
        fake: Provider<FakeDliRepository>,
    ): DliRepository = if (BuildConfig.USE_STATIC_DLIS) fake.get() else real.get()
}
