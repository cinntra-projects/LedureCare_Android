package com.example.ledurecare.feature.audit.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.POST

/** `item/bp_order_items`: the items one dealer can order, optionally narrowed by [SearchText]. */
@Serializable
data class BpItemsRequest(
    val CardCode: String,
    val SearchText: String? = null,
    val PageNo: Int,
    val MaxSize: Int,
)

/** The business-partner server (not the main API): no token is sent. */
interface ProductApi {
    @POST("item/bp_order_items")
    suspend fun searchItems(@Body request: BpItemsRequest): JsonElement
}
