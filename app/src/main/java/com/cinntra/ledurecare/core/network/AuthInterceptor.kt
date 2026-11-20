package com.cinntra.ledurecare.core.network

import com.cinntra.ledurecare.core.session.SessionManager
import com.cinntra.ledurecare.core.storage.SecureKeys
import com.cinntra.ledurecare.core.storage.SecureStorage
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Adds `Authorization: Bearer <access token>` when a session exists, and tells [SessionManager]
 * when the server rejects that token. Runs on OkHttp's thread.
 */
class AuthInterceptor @Inject constructor(
    private val secureStorage: SecureStorage,
    private val sessionManager: SessionManager,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath

        // Sign-in and password-reset calls are made before there is a valid session. A stale token
        // on them would make the server answer 401 instead of the real error.
        val token = if (PublicPaths.any(path::startsWith)) null else runBlocking { secureStorage.getString(SecureKeys.AUTH_TOKEN) }
        val response = chain.proceed(
            if (token == null) request else request.newBuilder().header("Authorization", "Bearer $token").build()
        )

        if (token != null && response.code == 401 && endsSessionOn401(path)) sessionManager.markExpired()
        return response
    }

    private companion object {
        val PublicPaths = listOf(
            "/api/auth/login/",
            "/api/auth/phone-login/",
            "/api/auth/forgot-password/",
            "/api/auth/verify-otp/",
            "/api/auth/reset-password/",
        )

        /** Other `/api/auth/` calls (logout, change password) can fail with 401 for their own reasons. */
        fun endsSessionOn401(path: String) = !path.startsWith("/api/auth/") || path == "/api/auth/user_info/"
    }
}
