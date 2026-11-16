package com.example.ledurecare.feature.audit.data

import com.example.ledurecare.core.common.AppResult
import com.example.ledurecare.feature.audit.domain.Warehouse
import com.example.ledurecare.feature.audit.domain.WarehouseRepository
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/** Sample warehouses, used while `BuildConfig.USE_STATIC_WAREHOUSES` is on. */
@Singleton
class FakeWarehouseRepository @Inject constructor() : WarehouseRepository {

    override suspend fun getWarehouses(): AppResult<List<Warehouse>> {
        delay(300)
        return AppResult.Success(
            listOf(
                Warehouse(1, "Ghaziabad WH – Site IV, Sahibabad"),
                Warehouse(2, "Noida WH – Sector 63"),
                Warehouse(3, "Meerut WH – Partapur"),
                Warehouse(4, "Agra WH – Sikandra"),
            )
        )
    }
}
