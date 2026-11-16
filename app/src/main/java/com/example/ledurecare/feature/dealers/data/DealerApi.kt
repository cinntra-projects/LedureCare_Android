package com.example.ledurecare.feature.dealers.data

import kotlinx.serialization.json.JsonElement
import retrofit2.http.GET
import retrofit2.http.Query

interface DealerApi {
    /**
     * Dealers whose name, code or city contains [search], one page at a time. A `null` leaves that
     * parameter out. Returned as a raw element: the list may arrive bare or wrapped in the usual
     * `data` envelope with `total_pages` and `count`.
     */
    @GET("api/dealers/bp-lookup/")
    suspend fun searchDealers(
        @Query("search") search: String?,
        @Query("page") page: Int?,
        @Query("page_size") pageSize: Int?,
    ): JsonElement
}
