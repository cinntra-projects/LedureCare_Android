package com.example.ledurecare.feature.audit.data

import com.example.ledurecare.BuildConfig
import com.example.ledurecare.feature.audit.domain.WarehouseRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WarehouseModule {

    @Provides
    @Singleton
    fun provideWarehouseApi(retrofit: Retrofit): WarehouseApi = retrofit.create(WarehouseApi::class.java)

    /** Sample warehouses or the warehouses API, chosen by `USE_STATIC_WAREHOUSES` in app/build.gradle.kts. */
    @Provides
    @Singleton
    fun provideWarehouseRepository(
        real: Provider<WarehouseRepositoryImpl>,
        fake: Provider<FakeWarehouseRepository>,
    ): WarehouseRepository = if (BuildConfig.USE_STATIC_WAREHOUSES) fake.get() else real.get()
}
