package com.example.ledurecare.feature.audit.data

import com.example.ledurecare.core.common.AppResult
import com.example.ledurecare.core.common.IoDispatcher
import com.example.ledurecare.core.common.safeCall
import com.example.ledurecare.core.network.findArray
import com.example.ledurecare.core.network.unwrapObject
import com.example.ledurecare.feature.audit.domain.Warehouse
import com.example.ledurecare.feature.audit.domain.WarehouseRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import javax.inject.Inject

/** One entry of `data.results` in `GET api/warehouses/`; only what the form needs. */
@Serializable
data class WarehouseDto(
    val id: Int,
    val name: String? = null,
    @SerialName("dispatch_address") val dispatchAddress: String? = null,
    @SerialName("dispatch_city") val dispatchCity: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
)

class WarehouseRepositoryImpl @Inject constructor(
    private val api: WarehouseApi,
    private val json: Json,
    @IoDispatcher private val io: CoroutineDispatcher,
) : WarehouseRepository {

    override suspend fun getWarehouses(): AppResult<List<Warehouse>> = safeCall(io) {
        val first = api.getWarehouses(null)
        val pages = first.totalPages().coerceIn(1, MAX_PAGES)
        val all = first.warehouses(json) + (2..pages).flatMap { page -> api.getWarehouses(page).warehouses(json) }
        all.distinctBy { it.id }
    }

    private companion object {
        /** A guard against a server that reports an absurd page count. */
        const val MAX_PAGES = 20
    }
}

private fun JsonElement.totalPages(): Int =
    (unwrapObject()?.get("total_pages") as? JsonPrimitive)?.intOrNull ?: 1

/** Active warehouses on this page. Entries the app cannot read are skipped, not fatal. */
internal fun JsonElement.warehouses(json: Json): List<Warehouse> = findArray().mapNotNull { element ->
    runCatching { json.decodeFromJsonElement(WarehouseDto.serializer(), element) }.getOrNull()
        ?.takeIf { it.isActive }
        ?.toDomain(details = element.toString())
}

internal fun WarehouseDto.toDomain(details: String = "") = Warehouse(
    details = details,
    id = id,
    name = name?.trim().orEmpty().ifEmpty { "Warehouse $id" },
    address = listOfNotNull(dispatchAddress, dispatchCity)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString(", "),
)
