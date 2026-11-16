package com.example.ledurecare.feature.plan.data

import com.example.ledurecare.BuildConfig
import com.example.ledurecare.feature.plan.domain.PlanRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlanModule {

    @Provides
    @Singleton
    fun providePlanApi(retrofit: Retrofit): PlanApi = retrofit.create(PlanApi::class.java)

    /** Sample plan or the plan API, chosen by `USE_STATIC_PLAN` in app/build.gradle.kts. */
    @Provides
    @Singleton
    fun providePlanRepository(
        real: Provider<RemotePlanRepository>,
        fake: Provider<FakePlanRepository>,
    ): PlanRepository = if (BuildConfig.USE_STATIC_PLAN) fake.get() else real.get()
}
