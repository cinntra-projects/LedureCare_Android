package com.example.ledurecare.feature.reports.data

import kotlinx.serialization.json.JsonElement
import okhttp3.MultipartBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/** Raw elements: bodies may be bare or wrapped in the usual `data` envelope. */
interface ReturnsApi {
    /** Role-aware overview; the stage counters are in `data.pipeline`. */
    @GET("api/returns/dashboard/")
    suspend fun getDashboard(): JsonElement

    /** The DLI list, one page at a time. `view=all` is what the Reports search sends. */
    @GET("api/returns/")
    suspend fun getReturns(
        @Query("view") view: String = "all",
        @Query("page") page: Int? = null,
        @Query("page_size") pageSize: Int? = null,
    ): JsonElement

    /**
     * Creates a DLI. Sent as multipart: plain text fields, `items` and `WarehouseAddress` as JSON text,
     * and one `attachments` part per file.
     */
    @Multipart
    @POST("api/returns/create/")
    suspend fun createReturn(@Part parts: List<MultipartBody.Part>): JsonElement

    /** Saves changes to a DLI; the same multipart fields as create. */
    @Multipart
    @POST("api/returns/{id}/edit/")
    suspend fun editReturn(@Path("id") id: Int, @Part parts: List<MultipartBody.Part>): JsonElement

    @GET("api/returns/{id}/")
    suspend fun getReturn(@Path("id") id: Int): JsonElement
}
