package com.example.ledurecare.feature.auth.data

import com.example.ledurecare.BuildConfig
import com.example.ledurecare.feature.auth.data.remote.AuthApi
import com.example.ledurecare.feature.auth.domain.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthProvidesModule {

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    /** Static sample data or the real API, chosen by `USE_STATIC_AUTH` in app/build.gradle.kts. */
    @Provides
    @Singleton
    fun provideAuthRepository(
        real: Provider<AuthRepositoryImpl>,
        fake: Provider<FakeAuthRepository>,
    ): AuthRepository = if (BuildConfig.USE_STATIC_AUTH) fake.get() else real.get()
}
