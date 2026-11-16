package com.example.ledurecare.feature.plan.data

import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Raw elements: bodies are wrapped in the usual `{status, message, data, errors}` envelope. */
interface PlanApi {
    /**
     * Plans between [dateFrom] and [dateTo], both inclusive and `yyyy-MM-dd`. A `null` leaves that
     * side open. Without page parameters the server returns every row.
     */
    @GET("api/plans/")
    suspend fun getPlans(
        @Query("date_from") dateFrom: String?,
        @Query("date_to") dateTo: String?,
    ): JsonElement

    @POST("api/plans/create/")
    suspend fun createPlans(@Body request: CreatePlansRequestDto): JsonElement

    @POST("api/plans/{id}/update/")
    suspend fun updatePlan(@Path("id") id: Long, @Body request: UpdatePlanRequestDto): JsonElement
}
