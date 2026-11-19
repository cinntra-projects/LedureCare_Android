package com.example.ledurecare.feature.reports.ui

import androidx.lifecycle.SavedStateHandle
import com.example.ledurecare.core.common.AppError
import com.example.ledurecare.core.common.AppResult
import com.example.ledurecare.feature.auth.domain.AuthRepository
import com.example.ledurecare.feature.auth.domain.User
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import com.example.ledurecare.feature.reports.domain.Dli
import com.example.ledurecare.feature.reports.domain.DliDetail
import com.example.ledurecare.feature.reports.domain.DliPage
import com.example.ledurecare.feature.reports.domain.DliFilter
import com.example.ledurecare.feature.reports.domain.ReturnView
import java.time.LocalDate
import com.example.ledurecare.feature.reports.domain.DliRepository
import com.example.ledurecare.feature.reports.domain.GetDlisUseCase
import com.example.ledurecare.feature.reports.domain.ReturnStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DliListViewModelTest {

    private fun dli(id: Int, name: String, status: ReturnStatus, days: Int) =
        Dli(id, "DLI-$id", name, "BP-$id", status, null, 1, days, false, null)

    private val all = listOf(
        dli(1047, "Sharma Auto Agency", ReturnStatus.Acc, 9),
        dli(1052, "Sumit Electric", ReturnStatus.Acc, 2),
        dli(1060, "Verma Motors", ReturnStatus.Wh, 4),
    )

    /** Pages [dlis] like the server does, [pageSize] at a time, and can fail on demand. */
    private class FakeRepository(var dlis: List<Dli>, var failure: AppError? = null, val pageSize: Int = 100) : DliRepository {
        val pages = mutableListOf<Int>()
        val filters = mutableListOf<DliFilter>()
        override suspend fun getDliPage(page: Int, pageSize: Int, filter: DliFilter): AppResult<DliPage> {
            pages += page
            filters += filter
            failure?.let { return AppResult.Failure(it) }
            val size = this.pageSize
            val totalPages = ((dlis.size + size - 1) / size).coerceAtLeast(1)
            return AppResult.Success(DliPage(dlis.drop((page - 1) * size).take(size), page, totalPages, dlis.size))
        }
        override suspend fun getDli(id: Int): AppResult<DliDetail> = error("not used")
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(
        repo: FakeRepository = FakeRepository(all),
        args: Map<String, Any?> = emptyMap(),
        user: User? = null,
    ): DliListViewModel {
        val auth = mockk<AuthRepository> { every { observeUser() } returns flowOf(user) }
        return DliListViewModel(SavedStateHandle(args), GetDlisUseCase(repo), auth)
    }

    private fun engineer(code: String) = User("2", "Service Engineer", "se@ledure.com", roleCode = code)

    @Test
    fun `a view or date range restarts the list from page one with that filter`() = runTest {
        val repo = FakeRepository(all)
        val vm = viewModel(repo)
        assertEquals(DliFilter(), repo.filters.last())

        vm.onEvent(DliListEvent.ViewSelected(ReturnView.New))
        assertEquals(ReturnView.New, repo.filters.last().view)
        assertEquals(1, repo.pages.last())

        val from = LocalDate.of(2026, 11, 1)
        val to = LocalDate.of(2026, 11, 15)
        vm.onEvent(DliListEvent.DateRangeSelected(from, to))
        assertEquals(DliFilter(ReturnView.New, from, to), repo.filters.last())

        vm.onEvent(DliListEvent.ViewSelected(ReturnView.New)) // the active choice again clears it
        assertEquals(DliFilter(ReturnView.All, from, to), repo.filters.last())

        vm.onEvent(DliListEvent.DateRangeCleared)
        assertEquals(DliFilter(), repo.filters.last())
    }

    @Test
    fun `only a service engineer sees edit, and only on an editable DLI`() = runTest {
        val editable = dli(1, "A", ReturnStatus.Coord, 1).copy(isEditable = true)
        val locked = dli(2, "B", ReturnStatus.Coord, 1)
        val repo = FakeRepository(listOf(editable, locked))

        val asEngineer = viewModel(repo, user = engineer("service_engineer")).state.value
        assertTrue(asEngineer.canEdit(editable))
        assertFalse(asEngineer.canEdit(locked))

        val asAdmin = viewModel(repo, user = engineer("admin")).state.value
        assertFalse(asAdmin.canEdit(editable))
        assertFalse(viewModel(repo).state.value.canEdit(editable))
    }

    @Test
    fun `no filter shows everything in the order the server sent it`() = runTest {
        val vm = viewModel()
        assertEquals(listOf(1047, 1052, 1060), vm.state.value.visible.map { it.id })
    }

    @Test
    fun `stage argument pre-filters the list`() = runTest {
        val vm = viewModel(args = mapOf("stage" to "acc"))
        assertEquals(ReturnStatus.Acc, vm.state.value.stage)
        assertEquals(listOf(1047, 1052), vm.state.value.visible.map { it.id })
    }

    @Test
    fun `clearing the stage searches all stages`() = runTest {
        val vm = viewModel(args = mapOf("stage" to "acc"))
        vm.onEvent(DliListEvent.StageSelected(null))
        assertEquals(3, vm.state.value.visible.size)
    }

    @Test
    fun `search combines with the stage filter`() = runTest {
        val vm = viewModel(args = mapOf("stage" to "acc"))
        vm.onEvent(DliListEvent.QueryChanged("sumit"))
        assertEquals(listOf(1052), vm.state.value.visible.map { it.id })
        vm.onEvent(DliListEvent.StageSelected(null))
        vm.onEvent(DliListEvent.QueryChanged("verma"))
        assertEquals(listOf(1060), vm.state.value.visible.map { it.id })
    }

    @Test
    fun `focus search argument is exposed`() = runTest {
        assertTrue(viewModel(args = mapOf("focusSearch" to true)).state.value.focusSearch)
        assertEquals(false, viewModel().state.value.focusSearch)
    }

    @Test
    fun `load failure is shown and retry reloads`() = runTest {
        val repo = FakeRepository(all, failure = AppError.Network)
        val vm = viewModel(repo)
        assertEquals(AppError.Network, vm.state.value.error)

        repo.failure = null
        vm.onEvent(DliListEvent.Retry)
        assertNull(vm.state.value.error)
        assertEquals(3, vm.state.value.all.size)
        assertEquals(listOf(1, 1), repo.pages)
    }

    // ---------------------------------------------------------------- paging

    private val many = List(25) { dli(it + 1, "Dealer ${it + 1}", ReturnStatus.Acc, 1) }

    @Test
    fun `the first page loads and more pages follow on request`() = runTest {
        val repo = FakeRepository(many, pageSize = 10)
        val vm = viewModel(repo)
        assertEquals(10, vm.state.value.all.size)
        assertEquals(25, vm.state.value.totalCount)
        assertTrue(vm.state.value.hasMore)

        vm.onEvent(DliListEvent.LoadMore)
        vm.onEvent(DliListEvent.LoadMore)
        assertEquals(25, vm.state.value.all.size)
        assertFalse(vm.state.value.hasMore)
        assertEquals(many.map { it.id }, vm.state.value.visible.map { it.id })

        vm.onEvent(DliListEvent.LoadMore) // nothing left: no request
        assertEquals(listOf(1, 2, 3), repo.pages)
    }

    @Test
    fun `a failed later page keeps the rows and retry asks for that page again`() = runTest {
        val repo = FakeRepository(many, pageSize = 10)
        val vm = viewModel(repo)

        repo.failure = AppError.Network
        vm.onEvent(DliListEvent.LoadMore)
        assertEquals(AppError.Network, vm.state.value.loadMoreError)
        assertEquals(10, vm.state.value.all.size)

        vm.onEvent(DliListEvent.LoadMore) // blocked until retried
        assertEquals(listOf(1, 2), repo.pages)

        repo.failure = null
        vm.onEvent(DliListEvent.Retry)
        assertNull(vm.state.value.loadMoreError)
        assertEquals(20, vm.state.value.all.size)
    }

    @Test
    fun `a filter pulls in the remaining pages so no match is missed`() = runTest {
        val repo = FakeRepository(many + dli(99, "Last Page Dealer", ReturnStatus.Wh, 1), pageSize = 10)
        val vm = viewModel(repo)
        assertEquals(10, vm.state.value.all.size)

        vm.onEvent(DliListEvent.QueryChanged("last page"))
        assertEquals(listOf(99), vm.state.value.visible.map { it.id })
        assertFalse(vm.state.value.hasMore)
    }
}
