package com.cinntra.ledurecare.feature

import com.cinntra.ledurecare.core.common.AppResult
import com.cinntra.ledurecare.feature.auth.data.FakeAuthRepository
import com.cinntra.ledurecare.feature.plan.data.FakePlanRepository
import com.cinntra.ledurecare.feature.plan.domain.DateRange
import com.cinntra.ledurecare.feature.plan.domain.VisitStatus
import com.cinntra.ledurecare.feature.dealers.data.StaticDealers
import com.cinntra.ledurecare.feature.reports.data.FakeDliRepository
import com.cinntra.ledurecare.feature.reports.domain.ReturnStatus
import com.cinntra.ledurecare.feature.reports.domain.StepState
import com.cinntra.ledurecare.feature.reports.domain.summarizeStages
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

class StaticDataTest {

    private val today = LocalDate.of(2026, 10, 28) // a Wednesday
    private val clock = Clock.fixed(today.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    @Test
    fun `stage cards match the design`() = runTest {
        val dlis = (FakeDliRepository().getDlis(refresh = true) as AppResult.Success).data
        val byStage = summarizeStages(dlis).associateBy { it.status }

        assertEquals(listOf(0, 0, 3, 2, 2, 1, 0, 74), ReturnStatus.entries.map { byStage.getValue(it).count })
        with(byStage.getValue(ReturnStatus.Wh)) {
            assertEquals(3, oldestDays)
            assertEquals(2, avgDays)
        }
        with(byStage.getValue(ReturnStatus.Acc)) {
            assertEquals(9, oldestDays)
            assertEquals(5, avgDays)
        }
        assertEquals(9.5, byStage.getValue(ReturnStatus.Completed).avgTurnaroundDays!!, 0.001)
    }

    @Test
    fun `dli 1047 detail matches the design`() = runTest {
        val detail = (FakeDliRepository().getDli(1047) as AppResult.Success).data

        assertEquals("Sharma Auto Agency, Agra", detail.dealerName)
        assertEquals(ReturnStatus.Acc, detail.status)
        assertTrue(detail.isOverSla)
        assertEquals(9, detail.daysInStage)
        assertEquals(LocalDate.of(2026, 10, 19), detail.stageSince)
        assertEquals("Ravi Kumar", detail.auditedBy)
        assertEquals(3 to 14, detail.totalBox to detail.totalQty)
        assertEquals(2, detail.items.size)
        assertEquals(3, detail.attachments.size)
        assertEquals(
            listOf(
                StepState.Done, StepState.Done, StepState.Done, StepState.Done, StepState.Done,
                StepState.Current, StepState.Upcoming, StepState.Upcoming,
            ),
            detail.tracker.map { it.state },
        )
    }

    @Test
    fun `every sample dli opens, and completed ones have a fully done tracker`() = runTest {
        val repo = FakeDliRepository()
        val dlis = (repo.getDlis(refresh = false) as AppResult.Success).data
        dlis.forEach { dli ->
            val detail = (repo.getDli(dli.id) as AppResult.Success).data
            assertEquals(8, detail.tracker.size)
            if (dli.status == ReturnStatus.Completed) {
                assertTrue(detail.tracker.all { it.state == StepState.Done })
            }
        }
        assertTrue(repo.getDli(-1) is AppResult.Failure)
    }

    @Test
    fun `plan is seeded around today with done and planned visits`() = runTest {
        val repo = FakePlanRepository(clock)
        val day = repo.observeVisits(DateRange(today, today)).first()

        assertEquals(3, day.size)
        assertEquals(listOf(VisitStatus.Done, VisitStatus.Planned, VisitStatus.Planned), day.map { it.status })

        val week = repo.observeVisits(DateRange(LocalDate.of(2026, 10, 26), LocalDate.of(2026, 11, 1))).first()
        assertEquals(7, week.size)
        assertEquals(3, week.count { it.status == VisitStatus.Done })
    }

    @Test
    fun `plan adds new visits, skips duplicates, and resets on clear`() = runTest {
        val repo = FakePlanRepository(clock)
        val verma = StaticDealers.byId(4)
        val kapoor = StaticDealers.byId(9)

        val added = (repo.addVisits(today, listOf(verma, kapoor)) as AppResult.Success).data // Verma is not planned for today yet
        assertEquals(2, added.createdIds.size)
        assertEquals(0, added.skipped.size)
        val again = (repo.addVisits(today, listOf(verma)) as AppResult.Success).data
        assertEquals(0, again.createdIds.size)
        assertEquals(listOf(verma.id), again.skipped.map { it.dealerCode })

        repo.clear()
        assertEquals(3, repo.observeVisits(DateRange(today, today)).first().size)
    }

    @Test
    fun `sample sign in accepts any credentials and a bad otp is rejected`() = runTest {
        val auth = FakeAuthRepository()
        assertEquals(false, auth.isLoggedIn())

        assertTrue(auth.login("anyone", "anything", rememberMe = true) is AppResult.Success)
        assertEquals(true, auth.isLoggedIn())
        assertEquals("anyone", auth.rememberedLogin()?.userId)
        assertNotNull(auth.observeUser().first())

        auth.logout()
        assertEquals(false, auth.isLoggedIn())
        assertEquals("anyone", auth.rememberedLogin()?.userId) // "remember me" survives sign out

        assertTrue(auth.verifyOtp("9876543210", "482910") is AppResult.Success)
        assertTrue(auth.verifyOtp("9876543210", "000000") is AppResult.Failure)
    }
}
