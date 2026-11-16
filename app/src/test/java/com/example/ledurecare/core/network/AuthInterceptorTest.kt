package com.example.ledurecare.core.network

import com.example.ledurecare.core.session.SessionManager
import com.example.ledurecare.core.storage.SecureKeys
import com.example.ledurecare.core.storage.SecureStorage
import io.mockk.every
import io.mockk.mockk
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthInterceptorTest {

    private class FakeStorage(var token: String?) : SecureStorage {
        override suspend fun getString(key: String): String? = if (key == SecureKeys.AUTH_TOKEN) token else null
        override suspend fun putString(key: String, value: String) = Unit
        override suspend fun remove(key: String) = Unit
        override suspend fun clear() = Unit
    }

    private class Result(val sentRequest: Request, val session: SessionManager)

    /** Runs one request through the interceptor; the "server" answers with [code]. */
    private fun call(path: String, token: String?, code: Int): Result {
        val session = SessionManager()
        val request = Request.Builder().url("http://server.test$path").build()
        var sent: Request? = null
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns request
        every { chain.proceed(any()) } answers {
            val forwarded = firstArg<Request>().also { sent = it }
            Response.Builder().request(forwarded).protocol(Protocol.HTTP_1_1).code(code).message("").build()
        }
        AuthInterceptor(FakeStorage(token), session).intercept(chain)
        return Result(checkNotNull(sent), session)
    }

    @Test
    fun `a signed in request carries the bearer token`() {
        val result = call("/api/returns/dashboard/", token = "abc", code = 200)
        assertEquals("Bearer abc", result.sentRequest.header("Authorization"))
        assertFalse(result.session.expired.value)
    }

    @Test
    fun `no token means no header`() {
        assertNull(call("/api/returns/dashboard/", token = null, code = 200).sentRequest.header("Authorization"))
    }

    @Test
    fun `sign in and password reset calls never carry a stale token`() {
        listOf(
            "/api/auth/login/",
            "/api/auth/phone-login/send-otp/",
            "/api/auth/phone-login/verify-otp/",
            "/api/auth/forgot-password/",
            "/api/auth/verify-otp/",
            "/api/auth/reset-password/",
        ).forEach { path ->
            assertNull(path, call(path, token = "stale", code = 200).sentRequest.header("Authorization"))
        }
    }

    @Test
    fun `the reset token on the reset call is left alone, not replaced by the login token`() {
        val session = SessionManager()
        val request = Request.Builder()
            .url("http://server.test/api/auth/reset-password/")
            .header("Authorization", "Bearer reset-token")
            .build()
        var sent: Request? = null
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns request
        every { chain.proceed(any()) } answers {
            val forwarded = firstArg<Request>().also { sent = it }
            Response.Builder().request(forwarded).protocol(Protocol.HTTP_1_1).code(200).message("").build()
        }

        AuthInterceptor(FakeStorage("login-token"), session).intercept(chain)

        assertEquals("Bearer reset-token", sent?.header("Authorization"))
    }

    @Test
    fun `logout still carries the token`() {
        assertEquals("Bearer abc", call("/api/auth/logout/", token = "abc", code = 200).sentRequest.header("Authorization"))
    }

    @Test
    fun `a 401 on a signed in request marks the session expired`() {
        assertTrue(call("/api/returns/dashboard/", token = "abc", code = 401).session.expired.value)
        assertTrue(call("/api/auth/user_info/", token = "abc", code = 401).session.expired.value)
    }

    @Test
    fun `a 401 without a session is just a failed sign in, not an expiry`() {
        assertFalse(call("/api/auth/login/", token = null, code = 401).session.expired.value)
        assertFalse(call("/api/returns/dashboard/", token = null, code = 401).session.expired.value)
    }

    @Test
    fun `a 401 from sign in or logout never expires the session`() {
        assertFalse(call("/api/auth/login/", token = "stale", code = 401).session.expired.value)
        assertFalse(call("/api/auth/logout/", token = "abc", code = 401).session.expired.value)
        assertFalse(call("/api/auth/change-password/", token = "abc", code = 401).session.expired.value)
    }

    @Test
    fun `other error codes do not expire the session`() {
        listOf(400, 403, 404, 500).forEach { code ->
            assertFalse("$code", call("/api/returns/dashboard/", token = "abc", code = code).session.expired.value)
        }
    }
}
