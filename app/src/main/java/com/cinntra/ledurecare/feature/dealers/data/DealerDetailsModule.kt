package com.cinntra.ledurecare.feature.dealers.data

import com.cinntra.ledurecare.BuildConfig
import com.cinntra.ledurecare.core.network.BusinessPartnerServer
import com.cinntra.ledurecare.feature.dealers.domain.DealerDetailsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DealerDetailsModule {

    @Provides
    @Singleton
    fun provideDealerDetailsApi(@BusinessPartnerServer retrofit: Retrofit): DealerDetailsApi =
        retrofit.create(DealerDetailsApi::class.java)

    @Provides
    @Singleton
    fun provideDealerDetailsRepository(
        real: Provider<DealerDetailsRepositoryImpl>,
        fake: Provider<FakeDealerDetailsRepository>,
    ): DealerDetailsRepository = if (BuildConfig.USE_STATIC_DEALERS) fake.get() else real.get()
}
