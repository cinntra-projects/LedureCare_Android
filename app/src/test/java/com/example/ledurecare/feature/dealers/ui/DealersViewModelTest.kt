package com.example.ledurecare.feature.dealers.ui

import com.example.ledurecare.core.common.AppError
import com.example.ledurecare.core.common.AppResult
import com.example.ledurecare.feature.dealers.data.StaticDealers
import com.example.ledurecare.feature.dealers.domain.DealerPage
import com.example.ledurecare.feature.dealers.domain.DealerRepository
import com.example.ledurecare.feature.dealers.domain.SearchDealersUseCase
import com.example.ledurecare.feature.dealers.domain.matches
import com.example.ledurecare.feature.plan.data.FakePlanRepository
import com.example.ledurecare.feature.plan.domain.ObserveVisitsUseCase
import com.example.ledurecare.feature.plan.domain.RefreshVisitsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class DealersViewModelTest {

    private val today = LocalDate.of(2026, 10, 28)
    private val clock = Clock.fixed(today.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    /** Pages the 24 sample dealers the way the API does, and can be told to fail. */
    private class SampleServer : DealerRepository {
        val requests = mutableListOf<Triple<String, Int, Int>>()
        var failure: AppError? = null

        override suspend fun searchDealers(query: String, page: Int, pageSize: Int): AppResult<DealerPage> {
            requests += Triple(query, page, pageSize)
            failure?.let { return AppResult.Failure(it) }
            val matching = StaticDealers.all.filter { it.matches(query) }
            val totalPages = ((matching.size + pageSize - 1) / pageSize).coerceAtLeast(1)
            return AppResult.Success(
                DealerPage(matching.drop((page - 1) * pageSize).take(pageSize), page, totalPages, matching.size)
            )
        }
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(
        server: SampleServer = SampleServer(),
        plan: FakePlanRepository = FakePlanRepository(clock),
    ) = DealersViewModel(SearchDealersUseCase(server), ObserveVisitsUseCase(plan), RefreshVisitsUseCase(plan), clock)
        .also { vm -> vm.state.onEach { }.launchIn(CoroutineScope(Dispatchers.Main)) }

    private fun DealersViewModel.row(name: String) = state.value.rows.first { it.dealer.name == name }

    @Test
    fun `opens with the first page and the total from the server`() = runTest {
        val server = SampleServer()
        val vm = viewModel(server)
        advanceUntilIdle()

        with(vm.state.value) {
            assertEquals(20, rows.size)
            assertEquals(24, search.totalCount)
            assertTrue(search.hasMore)
        }
        assertEquals(listOf(Triple("", 1, 20)), server.requests)
    }

    @Test
    fun `scrolling to the end loads the next page`() = runTest {
        val server = SampleServer()
        val vm = viewModel(server)
        advanceUntilIdle()

        vm.onEvent(DealersEvent.LoadMore)
        advanceUntilIdle()

        assertEquals(24, vm.state.value.rows.size)
        assertEquals(false, vm.state.value.search.hasMore)
        assertEquals(listOf(1, 2), server.requests.map { it.second })
    }

    @Test
    fun `typing searches on the server`() = runTest {
        val server = SampleServer()
        val vm = viewModel(server)
        advanceUntilIdle()

        vm.onEvent(DealersEvent.QueryChanged("meerut"))
        advanceUntilIdle()

        assertEquals("meerut", vm.state.value.search.query)
        assertEquals(6, vm.state.value.search.totalCount)
        assertTrue(vm.state.value.rows.all { it.dealer.city == "Meerut" })
        assertEquals("meerut", server.requests.last().first)
        assertEquals(1, server.requests.last().second)
    }

    @Test
    fun `last audit and planned date come from the plan`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        // Amit: audited today. Sumit: planned today. Verma: planned tomorrow.
        assertEquals(today, vm.row("Amit Electric").lastAudit)
        assertNull(vm.row("Amit Electric").planned)
        assertEquals(today, vm.row("Sumit Electric").planned)
        assertEquals(today.plusDays(1), vm.row("Verma Motors").planned)
        assertNull(vm.row("Rathi Enterprises").lastAudit)
        assertNull(vm.row("Rathi Enterprises").planned)
    }

    @Test
    fun `plan information follows the dealers onto later pages`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(DealersEvent.LoadMore)
        advanceUntilIdle()

        // "Yadav Auto Parts" is on page two and has no plan; "Singh Electricals" has an older audit.
        assertNull(vm.row("Yadav Auto Parts").planned)
        assertEquals(today.minusDays(8), vm.row("Singh Electricals").lastAudit)
    }

    @Test
    fun `a failed load is shown and retry recovers`() = runTest {
        val server = SampleServer().apply { failure = AppError.Network }
        val vm = viewModel(server)
        advanceUntilIdle()
        assertEquals(AppError.Network, vm.state.value.search.error)
        assertTrue(vm.state.value.rows.isEmpty())

        server.failure = null
        vm.onEvent(DealersEvent.Retry)
        advanceUntilIdle()
        assertNull(vm.state.value.search.error)
        assertEquals(24, vm.state.value.search.totalCount)
    }
}
