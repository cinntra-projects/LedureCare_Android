package com.example.ledurecare.core.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import retrofit2.HttpException
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

sealed interface AppError {
    data class Validation(val field: Field) : AppError {
        enum class Field { Credentials, Mobile, Otp, Email, PasswordTooShort, PasswordMismatch }
    }

    data object Network : AppError
    /** [message] is the server's own `message` field when the response body had one. */
    data class Http(val code: Int, val message: String? = null) : AppError
    data class Unknown(val cause: Throwable?) : AppError
}

sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Failure -> this
}

/**
 * What to tell the user about a failed call. The API answers `{message, errors}`; for validation
 * failures `message` is only "Validation failed." and the useful text is the first entry in `errors`
 * (for example `{"confirm_password": ["Passwords do not match."]}`), so that wins when present.
 */
internal fun HttpException.serverMessage(): String? = runCatching {
    val body = response()?.errorBody()?.string().orEmpty()
    val root = Json.parseToJsonElement(body).jsonObject
    val fieldError = (root["errors"] as? JsonObject)?.values?.firstNotNullOfOrNull { value ->
        val first = (value as? JsonArray)?.firstOrNull() ?: value
        (first as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
    }
    fieldError ?: (root["message"] as? JsonPrimitive)?.contentOrNull
}.getOrNull()?.takeIf { it.isNotBlank() }

/** Runs [block] on [dispatcher] and converts exceptions into [AppResult.Failure]. */
suspend fun <T> safeCall(dispatcher: CoroutineDispatcher, block: suspend () -> T): AppResult<T> =
    withContext(dispatcher) {
        try {
            AppResult.Success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            AppResult.Failure(AppError.Http(e.code(), e.serverMessage()))
        } catch (e: IOException) {
            AppResult.Failure(AppError.Network)
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e))
        }
    }
