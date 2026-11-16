package com.example.ledurecare.feature.audit.data

import kotlinx.serialization.json.JsonElement
import retrofit2.http.GET
import retrofit2.http.Query

interface WarehouseApi {
    /**
     * Paged: `data.results` holds the items, with `data.total_pages` telling how many pages there are.
     * Passing `null` leaves the page out, which gives the first page.
     */
    @GET("api/warehouses/")
    suspend fun getWarehouses(@Query("page") page: Int?): JsonElement
}
