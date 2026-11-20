package com.cinntra.ledurecare.feature.auth.ui

import com.cinntra.ledurecare.core.session.SessionManager
import com.cinntra.ledurecare.feature.auth.data.FakeAuthRepository
import com.cinntra.ledurecare.feature.auth.domain.ClearSessionUseCase
import com.cinntra.ledurecare.feature.plan.data.FakePlanRepository
import com.cinntra.ledurecare.feature.plan.domain.ClearPlanUseCase
import com.cinntra.ledurecare.feature.plan.domain.DateRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTest {

    private val today = LocalDate.of(2026, 10, 28)
    private val clock = Clock.fixed(today.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `the dialog shows once the session is marked expired`() = runTest {
        val session = SessionManager()
        val vm = SessionViewModel(session, ClearSessionUseCase(FakeAuthRepository()), ClearPlanUseCase(FakePlanRepository(clock)))

        assertFalse(vm.expired.value)
        session.markExpired()
        assertTrue(vm.expired.value)
    }

    @Test
    fun `logging in again wipes the local session and plan, closes the dialog, then moves on`() = runTest {
        val session = SessionManager()
        val auth = FakeAuthRepository().also { it.login("ops", "secret", rememberMe = false) }
        val plan = FakePlanRepository(clock).also { it.addVisits(today, emptyList()) }
        val vm = SessionViewModel(session, ClearSessionUseCase(auth), ClearPlanUseCase(plan))
        session.markExpired()
        assertTrue(auth.isLoggedIn())

        var moved = false
        vm.loginAgain { moved = true }

        assertFalse(auth.isLoggedIn())
        assertFalse(vm.expired.value)
        assertTrue(moved)
        // The plan is reset to its starting state rather than left from the old session.
        assertEquals(3, plan.observeVisits(DateRange(today, today)).first().size)
    }

    @Test
    fun `many failing calls still give one expiry`() = runTest {
        val session = SessionManager()
        repeat(5) { session.markExpired() }
        assertTrue(session.expired.value)
        session.reset()
        assertFalse(session.expired.value)
    }
}
