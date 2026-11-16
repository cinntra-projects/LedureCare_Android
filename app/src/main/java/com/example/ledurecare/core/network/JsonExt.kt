package com.example.ledurecare.core.network

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

/** Accepts `[...]`, `{data: [...]}`, `{results: [...]}` and `{data: {results: [...]}}`. */
fun JsonElement.findArray(): List<JsonElement> = when (this) {
    is JsonArray -> this
    is JsonObject -> listOf("data", "results")
        .firstNotNullOfOrNull { key -> this[key]?.takeIf { it !is JsonNull }?.findArray() }
        ?: emptyList()
    else -> emptyList()
}

/** Returns the object itself, or the one under `data` when the body is wrapped in the usual envelope. */
fun JsonElement.unwrapObject(): JsonObject? = when (this) {
    is JsonObject -> (this["data"] as? JsonObject)?.takeIf { this["id"] == null } ?: this
    else -> null
}
