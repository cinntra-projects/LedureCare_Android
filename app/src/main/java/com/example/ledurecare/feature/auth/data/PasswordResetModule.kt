package com.example.ledurecare.feature.auth.data

import com.example.ledurecare.BuildConfig
import com.example.ledurecare.feature.auth.domain.PasswordResetRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PasswordResetModule {

    /** Sample flow or the real API, chosen by `USE_STATIC_PASSWORD_RESET` in app/build.gradle.kts. */
    @Provides
    @Singleton
    fun providePasswordResetRepository(
        real: Provider<PasswordResetRepositoryImpl>,
        fake: Provider<FakePasswordResetRepository>,
    ): PasswordResetRepository = if (BuildConfig.USE_STATIC_PASSWORD_RESET) fake.get() else real.get()
}
