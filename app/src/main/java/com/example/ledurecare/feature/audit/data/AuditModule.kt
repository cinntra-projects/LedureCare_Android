package com.example.ledurecare.feature.audit.data

import com.example.ledurecare.BuildConfig
import com.example.ledurecare.feature.audit.domain.AuditRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuditModule {

    /** Pretend submit or the create-DLI API, chosen by `USE_STATIC_AUDIT` in app/build.gradle.kts. */
    @Provides
    @Singleton
    fun provideAuditRepository(
        real: Provider<AuditRepositoryImpl>,
        fake: Provider<FakeAuditRepository>,
    ): AuditRepository = if (BuildConfig.USE_STATIC_AUDIT) fake.get() else real.get()
}
