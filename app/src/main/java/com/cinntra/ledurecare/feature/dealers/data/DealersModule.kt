package com.cinntra.ledurecare.feature.dealers.data

import com.cinntra.ledurecare.BuildConfig
import com.cinntra.ledurecare.feature.dealers.domain.DealerRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DealersProvidesModule {

    @Provides
    @Singleton
    fun provideDealerApi(retrofit: Retrofit): DealerApi = retrofit.create(DealerApi::class.java)

    /** Static sample data or the real API, chosen by `USE_STATIC_DEALERS` in app/build.gradle.kts. */
    @Provides
    @Singleton
    fun provideDealerRepository(
        real: Provider<DealerRepositoryImpl>,
        fake: Provider<FakeDealerRepository>,
    ): DealerRepository = if (BuildConfig.USE_STATIC_DEALERS) fake.get() else real.get()
}
