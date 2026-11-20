package com.cinntra.ledurecare.core.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber

/**
 * Sends OkHttp's request and response lines to Timber under the `API` tag, with JSON bodies
 * pretty-printed so they are readable in logcat. Filter logcat by `API` to follow a call.
 */
class TimberHttpLogger : HttpLoggingInterceptor.Logger {

    override fun log(message: String) {
        Timber.tag(TAG).d(format(message))
    }

    internal companion object {
        const val TAG = "API"

        private val pretty = Json { prettyPrint = true }

        /** A JSON object or array body is pretty-printed; any other line (URL, header, status) is left as is. */
        fun format(message: String): String {
            if (!message.startsWith("{") && !message.startsWith("[")) return message
            return runCatching {
                pretty.encodeToString(JsonElement.serializer(), pretty.parseToJsonElement(message))
            }.getOrDefault(message)
        }
    }
}
