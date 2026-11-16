package com.example.ledurecare.feature.auth.data

import com.example.ledurecare.core.storage.SecureKeys
import com.example.ledurecare.core.storage.SecureStorage
import com.example.ledurecare.feature.auth.data.local.UserDao
import com.example.ledurecare.feature.auth.data.remote.AuthApi
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class LogoutTest {

    /** Records everything that happens, in order, so the tests can check the sequence. */
    private val events = mutableListOf<String>()

    private val storage = object : SecureStorage {
        override suspend fun getString(key: String): String? = null
        override suspend fun putString(key: String, value: String) = Unit
        override suspend fun remove(key: String) {
            events += "remove:$key"
        }
        override suspend fun clear() = Unit
    }

    private val dao = mockk<UserDao>().also { coEvery { it.clear() } coAnswers { events += "clear-user" } }
    private val api = mockk<AuthApi>()

    private fun TestScopeRepository(scope: kotlinx.coroutines.test.TestScope) =
        AuthRepositoryImpl(api, dao, storage, UnconfinedTestDispatcher(scope.testScheduler))

    @Test
    fun `sign out calls the server first, then clears the session`() = runTest {
        coEvery { api.logout() } coAnswers { events += "logout-api" }

        TestScopeRepository(this).logout()

        // The token must still be stored while the call is made: the network layer reads it from there.
        assertEquals(listOf("logout-api", "remove:${SecureKeys.AUTH_TOKEN}", "clear-user"), events)
    }

    @Test
    fun `sign out still clears the session when the server cannot be reached`() = runTest {
        coEvery { api.logout() } coAnswers {
            events += "logout-api"
            throw IOException("offline")
        }

        TestScopeRepository(this).logout()

        assertEquals(listOf("logout-api", "remove:${SecureKeys.AUTH_TOKEN}", "clear-user"), events)
    }

    @Test
    fun `sign out does not wait forever for a slow server`() = runTest {
        coEvery { api.logout() } coAnswers {
            events += "logout-api"
            delay(60_000)
        }

        TestScopeRepository(this).logout()

        assertEquals(listOf("logout-api", "remove:${SecureKeys.AUTH_TOKEN}", "clear-user"), events)
        // Only the 3 second limit was waited, not the server's minute.
        assertEquals(true, testScheduler.currentTime < 5_000)
    }

    @Test
    fun `clearing the session leaves the server alone`() = runTest {
        TestScopeRepository(this).clearSession()
        assertEquals(listOf("remove:${SecureKeys.AUTH_TOKEN}", "clear-user"), events)
    }
}
