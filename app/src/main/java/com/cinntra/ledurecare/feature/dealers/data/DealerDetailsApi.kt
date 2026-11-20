package com.cinntra.ledurecare.feature.dealers.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.POST

@Serializable
data class BusinessPartnerRequest(val CardCode: String)

/** The business-partner server (not the main API): no token is sent. */
interface DealerDetailsApi {
    @POST("businesspartner/one")
    suspend fun getOne(@Body request: BusinessPartnerRequest): JsonElement
}
