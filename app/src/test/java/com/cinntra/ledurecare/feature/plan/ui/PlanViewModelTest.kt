package com.cinntra.ledurecare.feature.plan.ui

import app.cash.turbine.test
import com.cinntra.ledurecare.core.common.AppError
import com.cinntra.ledurecare.core.common.AppResult
import com.cinntra.ledurecare.feature.dealers.domain.Dealer
import com.cinntra.ledurecare.feature.dealers.domain.DealerRepository
import com.cinntra.ledurecare.feature.dealers.domain.DealerPage
import com.cinntra.ledurecare.feature.dealers.domain.SearchDealersUseCase
import com.cinntra.ledurecare.feature.dealers.domain.matches
import com.cinntra.ledurecare.feature.plan.domain.AddVisitsResult
import com.cinntra.ledurecare.feature.plan.domain.AddVisitsUseCase
import com.cinntra.ledurecare.feature.plan.domain.DateRange
import com.cinntra.ledurecare.feature.plan.domain.ObserveVisitsUseCase
import com.cinntra.ledurecare.feature.plan.domain.PlanRepository
import com.cinntra.ledurecare.feature.plan.domain.PlannedVisit
import com.cinntra.ledurecare.feature.plan.domain.RefreshVisitsUseCase
import com.cinntra.ledurecare.feature.plan.domain.SkippedDealer
import com.cinntra.ledurecare.feature.plan.domain.UpdateVisitUseCase
import com.cinntra.ledurecare.feature.plan.domain.VisitChange
import com.cinntra.ledurecare.feature.plan.domain.VisitStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class PlanViewModelTest {

    private val today = LocalDate.of(2026, 10, 28) // a Wednesday
    private val clock = Clock.fixed(today.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    private val dealers = listOf(
        Dealer("1", "Verma Motors", "BP-22310", "Ghaziabad"),
        Dealer("2", "Gupta Spares", "BP-23140", "Firozabad"),
        Dealer("3", "Delta Auto", "BP-22871", "Noida"),
    )

    /** A plan store that keeps everything in memory and records what the view model asks of it. */
    private class FakePlanRepository : PlanRepository {
        val all = MutableStateFlow<List<PlannedVisit>>(emptyList())
        private var nextId = 100L

        val refreshed = mutableListOf<DateRange>()
        val updates = mutableListOf<Pair<Long, VisitChange>>()
        var refreshResult: AppResult<Unit> = AppResult.Success(Unit)
        var addFailure: AppError? = null
        var updateFailure: AppError? = null

        /** Dealers the "server" refuses as already planned, although the app has not been told so. */
        var alsoSkip: Set<String> = emptySet()

        override fun observeVisits(range: DateRange): Flow<List<PlannedVisit>> =
            all.map { list -> list.filter { it.date >= range.start && it.date <= range.end } }

        override suspend fun refresh(range: DateRange): AppResult<Unit> {
            refreshed += range
            return refreshResult
        }

        override suspend fun addVisits(date: LocalDate, dealers: List<Dealer>): AppResult<AddVisitsResult> {
            addFailure?.let { return AppResult.Failure(it) }
            val (fresh, duplicates) = dealers.partition { d ->
                d.id !in alsoSkip && all.value.none { it.dealerId == d.id && it.date == date }
            }
            val created = fresh.map { PlannedVisit(nextId++, it.id, it.name, it.bpCode, it.city, date, VisitStatus.Planned) }
            all.value = all.value + created
            return AppResult.Success(
                AddVisitsResult(created.map { it.id }, duplicates.map { SkippedDealer(it.id, "Already planned for this date.") })
            )
        }

        override suspend fun updateVisit(id: Long, change: VisitChange): AppResult<Unit> {
            updateFailure?.let { return AppResult.Failure(it) }
            updates += id to change
            all.value = all.value.map { v ->
                if (v.id != id) v else v.copy(
                    date = change.date ?: v.date,
                    dealerId = change.dealer?.id ?: v.dealerId,
                    dealerName = change.dealer?.name ?: v.dealerName,
                )
            }
            return AppResult.Success(Unit)
        }

        override suspend fun getVisit(id: Long): PlannedVisit? = all.value.firstOrNull { it.id == id }

        override suspend fun markDone(id: Long) {
            all.value = all.value.map { if (it.id == id) it.copy(status = VisitStatus.Done) else it }
        }

        override suspend fun clear() {
            all.value = emptyList()
        }
    }

    /** Pages [result] the way the API does (20 a page, search by name, code or city), or fails. */
    private class FakeDealerRepository(var result: AppResult<List<Dealer>>) : DealerRepository {
        override suspend fun searchDealers(query: String, page: Int, pageSize: Int): AppResult<DealerPage> =
            when (val r = result) {
                is AppResult.Failure -> r
                is AppResult.Success -> {
                    val matching = r.data.filter { it.matches(query) }
                    val totalPages = ((matching.size + pageSize - 1) / pageSize).coerceAtLeast(1)
                    AppResult.Success(DealerPage(matching.drop((page - 1) * pageSize).take(pageSize), page, totalPages, matching.size))
                }
            }
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(
        plan: FakePlanRepository = FakePlanRepository(),
        dealerResult: AppResult<List<Dealer>> = AppResult.Success(dealers),
    ) = viewModel(plan, FakeDealerRepository(dealerResult))

    private fun viewModel(plan: FakePlanRepository, dealerRepo: FakeDealerRepository) = PlanViewModel(
        ObserveVisitsUseCase(plan),
        RefreshVisitsUseCase(plan),
        AddVisitsUseCase(plan),
        UpdateVisitUseCase(plan),
        SearchDealersUseCase(dealerRepo),
        clock,
    ).also { vm -> vm.state.onEach { }.launchIn(kotlinx.coroutines.CoroutineScope(Dispatchers.Main)) }

    private fun visit(id: Long, dealer: Dealer, date: LocalDate) =
        PlannedVisit(id, dealer.id, dealer.name, dealer.bpCode, dealer.city, date, VisitStatus.Planned)

    // ---------------------------------------------------------------- navigation

    @Test
    fun `starts on today in day view`() = runTest {
        val vm = viewModel()
        assertEquals(PlanView.Day, vm.state.value.view)
        assertEquals(today, vm.state.value.selectedDate)
    }

    @Test
    fun `previous and next step by the active view`() = runTest {
        val vm = viewModel()
        vm.onEvent(PlanEvent.Next)
        assertEquals(today.plusDays(1), vm.state.value.selectedDate)

        vm.onEvent(PlanEvent.ViewChanged(PlanView.Week))
        vm.onEvent(PlanEvent.Previous)
        assertEquals(today.plusDays(1).minusWeeks(1), vm.state.value.selectedDate)

        vm.onEvent(PlanEvent.ViewChanged(PlanView.Month))
        vm.onEvent(PlanEvent.Next)
        assertEquals(today.plusDays(1).minusWeeks(1).plusMonths(1), vm.state.value.selectedDate)
    }

    // ---------------------------------------------------------------- fetching from the server

    @Test
    fun `the week on screen is fetched when the screen opens`() = runTest {
        val plan = FakePlanRepository()
        viewModel(plan)
        assertEquals(listOf(DateRange(LocalDate.of(2026, 10, 26), LocalDate.of(2026, 11, 1))), plan.refreshed)
    }

    @Test
    fun `moving to another week or to month view fetches that range`() = runTest {
        val plan = FakePlanRepository()
        val vm = viewModel(plan)

        vm.onEvent(PlanEvent.DateSelected(today.plusDays(1))) // same week: no new fetch
        assertEquals(1, plan.refreshed.size)

        vm.onEvent(PlanEvent.ViewChanged(PlanView.Week))
        vm.onEvent(PlanEvent.Next)
        assertEquals(DateRange(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 8)), plan.refreshed.last())

        vm.onEvent(PlanEvent.ViewChanged(PlanView.Month))
        assertEquals(DateRange(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)), plan.refreshed.last())
    }

    @Test
    fun `opening the add sheet fetches its date so already planned dealers are known`() = runTest {
        val plan = FakePlanRepository()
        val vm = viewModel(plan)
        vm.onEvent(PlanEvent.OpenSheet)
        assertTrue(DateRange(today, today) in plan.refreshed)

        vm.onEvent(PlanEvent.SheetDateChanged(today.plusDays(9)))
        assertEquals(DateRange(today.plusDays(9), today.plusDays(9)), plan.refreshed.last())
    }

    @Test
    fun `a failed fetch is reported and the screen keeps working`() = runTest {
        val plan = FakePlanRepository()
        val vm = viewModel(plan)
        plan.refreshResult = AppResult.Failure(AppError.Network)

        vm.effects.test {
            vm.onEvent(PlanEvent.ViewChanged(PlanView.Month)) // a new range to fetch
            assertEquals(PlanEffect.ShowError(AppError.Network), awaitItem())
        }
        assertFalse(vm.state.value.isRefreshing)
    }

    @Test
    fun `a no-permission answer while loading the list shows no toast`() = runTest {
        val plan = FakePlanRepository()
        val vm = viewModel(plan)
        plan.refreshResult = AppResult.Failure(AppError.Http(403, "You do not have permission."))

        vm.effects.test {
            vm.onEvent(PlanEvent.ViewChanged(PlanView.Month))
            expectNoEvents()
        }
        assertFalse(vm.state.value.isRefreshing)
    }

    // ---------------------------------------------------------------- adding

    @Test
    fun `opening the sheet loads the first page of dealers for the selected date`() = runTest {
        val vm = viewModel()
        vm.onEvent(PlanEvent.OpenSheet)
        advanceUntilIdle()

        assertEquals(today, vm.state.value.sheet!!.date)
        assertEquals(dealers, vm.state.value.dealerSearch.items)
        assertEquals(3, vm.state.value.dealerSearch.totalCount)
    }

    @Test
    fun `confirming adds selected dealers, closes the sheet and marks them just added`() = runTest {
        val plan = FakePlanRepository()
        val vm = viewModel(plan)
        vm.onEvent(PlanEvent.OpenSheet)
        vm.onEvent(PlanEvent.ToggleDealer(dealers[0]))
        vm.onEvent(PlanEvent.ToggleDealer(dealers[1]))
        assertEquals(2, vm.state.value.sheet!!.selectedCount)

        vm.effects.test {
            vm.onEvent(PlanEvent.ConfirmAdd)
            assertEquals(PlanEffect.PlansAdded(created = 2, skipped = 0), awaitItem())
        }

        val state = vm.state.value
        assertNull(state.sheet)
        assertEquals(listOf("Verma Motors", "Gupta Spares"), state.visitsOn(today).map { it.dealerName })
        assertEquals(state.visitsOn(today).map { it.id }.toSet(), state.justAdded)
    }

    @Test
    fun `dealers the server skipped are counted, and only the created ones are marked just added`() = runTest {
        // The app offers dealer 1, but the server already has it planned for that date (another device).
        val plan = FakePlanRepository().apply { alsoSkip = setOf("1") }
        val vm = viewModel(plan)
        vm.onEvent(PlanEvent.OpenSheet)
        vm.onEvent(PlanEvent.ToggleDealer(dealers[0]))
        vm.onEvent(PlanEvent.ToggleDealer(dealers[1]))

        vm.effects.test {
            vm.onEvent(PlanEvent.ConfirmAdd)
            assertEquals(PlanEffect.PlansAdded(created = 1, skipped = 1), awaitItem())
        }
        assertEquals(listOf("Gupta Spares"), vm.state.value.visitsOn(today).map { it.dealerName })
        assertEquals(1, vm.state.value.justAdded.size)
    }

    @Test
    fun `when the server adds nothing the error is shown and the sheet stays open`() = runTest {
        val plan = FakePlanRepository().apply { addFailure = AppError.Http(400, "All dealers are already planned.") }
        val vm = viewModel(plan)
        vm.onEvent(PlanEvent.OpenSheet)
        vm.onEvent(PlanEvent.ToggleDealer(dealers[0]))

        vm.effects.test {
            vm.onEvent(PlanEvent.ConfirmAdd)
            assertEquals(PlanEffect.ShowError(AppError.Http(400, "All dealers are already planned.")), awaitItem())
        }
        assertTrue(vm.state.value.sheet != null)
        assertFalse(vm.state.value.sheet!!.isSaving)
        assertEquals(setOf("1"), vm.state.value.sheet!!.selected.keys) // the choice is kept for another try
    }

    @Test
    fun `dealers already planned on the sheet date are reported so the sheet can hide them`() = runTest {
        val plan = FakePlanRepository()
        val vm = viewModel(plan)
        vm.onEvent(PlanEvent.OpenSheet)
        vm.onEvent(PlanEvent.ToggleDealer(dealers[0]))
        vm.onEvent(PlanEvent.ConfirmAdd)

        vm.onEvent(PlanEvent.OpenSheet)
        assertEquals(setOf("1"), vm.state.value.sheet!!.alreadyPlanned)
        assertEquals(0, vm.state.value.sheet!!.selectedCount)
    }

    @Test
    fun `a dealer ticked on one page is still sent after a new search replaces the list`() = runTest {
        val plan = FakePlanRepository()
        val vm = viewModel(plan)
        vm.onEvent(PlanEvent.OpenSheet)
        advanceUntilIdle()
        vm.onEvent(PlanEvent.ToggleDealer(dealers[0])) // Verma, tick
        vm.onEvent(PlanEvent.DealerQueryChanged("noida")) // the list now shows only Delta Auto
        advanceUntilIdle()
        assertEquals(listOf("3"), vm.state.value.dealerSearch.items.map { it.id })
        vm.onEvent(PlanEvent.ToggleDealer(dealers[2]))

        vm.effects.test {
            vm.onEvent(PlanEvent.ConfirmAdd)
            assertEquals(PlanEffect.PlansAdded(created = 2, skipped = 0), awaitItem())
        }
        assertEquals(listOf("Verma Motors", "Delta Auto"), vm.state.value.visitsOn(today).map { it.dealerName })
    }

    @Test
    fun `confirm with nothing selected keeps the sheet open`() = runTest {
        val vm = viewModel()
        vm.onEvent(PlanEvent.OpenSheet)
        vm.onEvent(PlanEvent.ConfirmAdd)
        assertTrue(vm.state.value.sheet != null)
    }

    @Test
    fun `at most 100 dealers can be selected at once`() = runTest {
        val many = List(120) { Dealer("D$it", "Dealer $it", "D$it", "City") }
        val vm = viewModel(dealerResult = AppResult.Success(many))
        vm.onEvent(PlanEvent.OpenSheet)
        many.forEach { vm.onEvent(PlanEvent.ToggleDealer(it)) }

        assertEquals(MAX_DEALERS_PER_PLAN, vm.state.value.sheet!!.selected.size)
        vm.onEvent(PlanEvent.ToggleDealer(many[0])) // deselecting still works at the limit
        assertEquals(MAX_DEALERS_PER_PLAN - 1, vm.state.value.sheet!!.selected.size)
    }

    @Test
    fun `search is done on the server by name, bp code or city`() = runTest {
        val vm = viewModel()
        vm.onEvent(PlanEvent.OpenSheet)
        advanceUntilIdle()

        vm.onEvent(PlanEvent.DealerQueryChanged("noida"))
        advanceUntilIdle()
        assertEquals(listOf("3"), vm.state.value.dealerSearch.items.map { it.id })

        vm.onEvent(PlanEvent.DealerQueryChanged("bp-2314"))
        advanceUntilIdle()
        assertEquals(listOf("2"), vm.state.value.dealerSearch.items.map { it.id })
    }

    @Test
    fun `opening the sheet again starts with a clean search`() = runTest {
        val vm = viewModel()
        vm.onEvent(PlanEvent.OpenSheet)
        vm.onEvent(PlanEvent.DealerQueryChanged("noida"))
        advanceUntilIdle()
        vm.onEvent(PlanEvent.CloseSheet)

        vm.onEvent(PlanEvent.OpenSheet)
        advanceUntilIdle()
        assertEquals("", vm.state.value.dealerSearch.query)
        assertEquals(3, vm.state.value.dealerSearch.items.size)
    }

    @Test
    fun `scrolling to the end of the dealer list loads the next page`() = runTest {
        val many = List(45) { Dealer("D$it", "Dealer $it", "D$it", "City") }
        val vm = viewModel(dealerResult = AppResult.Success(many))
        vm.onEvent(PlanEvent.OpenSheet)
        advanceUntilIdle()
        assertEquals(20, vm.state.value.dealerSearch.items.size)

        vm.onEvent(PlanEvent.LoadMoreDealers)
        advanceUntilIdle()
        assertEquals(40, vm.state.value.dealerSearch.items.size)
    }

    @Test
    fun `dealer load failure is shown and can be retried`() = runTest {
        val plan = FakePlanRepository()
        val repo = FakeDealerRepository(AppResult.Failure(AppError.Network))
        val vm = viewModel(plan, repo)

        vm.onEvent(PlanEvent.OpenSheet)
        advanceUntilIdle()
        assertEquals(AppError.Network, vm.state.value.dealerSearch.error)

        repo.result = AppResult.Success(dealers)
        vm.onEvent(PlanEvent.RetryDealers)
        advanceUntilIdle()
        assertEquals(dealers, vm.state.value.dealerSearch.items)
    }

    // ---------------------------------------------------------------- editing

    private fun planWithTodayVisit() = FakePlanRepository().apply { all.value = listOf(visit(7, dealers[0], today)) }

    @Test
    fun `opening edit starts on the plan's own date and dealer, with nothing changed`() = runTest {
        val vm = viewModel(planWithTodayVisit())
        vm.onEvent(PlanEvent.OpenEdit(7))

        val edit = vm.state.value.edit!!
        assertEquals(today, edit.date)
        assertEquals("1", edit.dealer.id)
        assertEquals(edit.currentDealer, edit.dealer)
        assertFalse(edit.hasChanges)
    }

    @Test
    fun `the edit sheet lists the same searched, paged dealers`() = runTest {
        val vm = viewModel(planWithTodayVisit())
        vm.onEvent(PlanEvent.OpenEdit(7))
        advanceUntilIdle()
        assertEquals(dealers, vm.state.value.dealerSearch.items)

        vm.onEvent(PlanEvent.DealerQueryChanged("noida"))
        advanceUntilIdle()
        assertEquals(listOf("3"), vm.state.value.dealerSearch.items.map { it.id })
        assertEquals("1", vm.state.value.edit!!.dealer.id) // searching does not lose the current choice
    }

    @Test
    fun `opening edit for a plan that is not on screen does nothing`() = runTest {
        val vm = viewModel(planWithTodayVisit())
        vm.onEvent(PlanEvent.OpenEdit(999))
        assertNull(vm.state.value.edit)
    }

    @Test
    fun `a plan whose dealer is not in the dealer list can still be edited`() = runTest {
        val gone = Dealer("GONE", "Old Dealer", "GONE", "Agra")
        val plan = FakePlanRepository().apply { all.value = listOf(visit(7, gone, today)) }
        val vm = viewModel(plan)
        vm.onEvent(PlanEvent.OpenEdit(7))
        advanceUntilIdle()

        assertEquals(gone, vm.state.value.edit!!.dealer) // built from the plan itself
        assertTrue(vm.state.value.dealerSearch.items.none { it.id == "GONE" })

        vm.onEvent(PlanEvent.EditDateChanged(today.plusDays(1)))
        vm.onEvent(PlanEvent.ConfirmEdit)
        assertEquals(listOf(7L to VisitChange(date = today.plusDays(1), dealer = null)), plan.updates) // dealer untouched
    }

    @Test
    fun `changing only the date sends only the date and follows the plan there`() = runTest {
        val plan = planWithTodayVisit()
        val vm = viewModel(plan)
        vm.onEvent(PlanEvent.OpenEdit(7))
        vm.onEvent(PlanEvent.EditDateChanged(today.plusDays(10)))
        assertTrue(vm.state.value.edit!!.hasChanges)

        vm.effects.test {
            vm.onEvent(PlanEvent.ConfirmEdit)
            assertEquals(PlanEffect.VisitUpdated, awaitItem())
        }

        assertEquals(listOf(7L to VisitChange(date = today.plusDays(10), dealer = null)), plan.updates)
        assertNull(vm.state.value.edit)
        assertEquals(today.plusDays(10), vm.state.value.selectedDate)
        assertEquals(listOf("Verma Motors"), vm.state.value.visitsOn(today.plusDays(10)).map { it.dealerName })
    }

    @Test
    fun `changing only the dealer sends only the dealer and stays on the same date`() = runTest {
        val plan = planWithTodayVisit()
        val vm = viewModel(plan)
        vm.onEvent(PlanEvent.OpenEdit(7))
        vm.onEvent(PlanEvent.EditDealerSelected(dealers[2]))

        vm.effects.test {
            vm.onEvent(PlanEvent.ConfirmEdit)
            assertEquals(PlanEffect.VisitUpdated, awaitItem())
        }

        assertEquals(listOf(7L to VisitChange(date = null, dealer = dealers[2])), plan.updates)
        assertEquals(today, vm.state.value.selectedDate)
        assertEquals(listOf("Delta Auto"), vm.state.value.visitsOn(today).map { it.dealerName })
    }

    @Test
    fun `changing both date and dealer sends both`() = runTest {
        val plan = planWithTodayVisit()
        val vm = viewModel(plan)
        vm.onEvent(PlanEvent.OpenEdit(7))
        vm.onEvent(PlanEvent.EditDateChanged(today.plusDays(1)))
        vm.onEvent(PlanEvent.EditDealerSelected(dealers[1]))
        vm.onEvent(PlanEvent.ConfirmEdit)

        assertEquals(listOf(7L to VisitChange(today.plusDays(1), dealers[1])), plan.updates)
    }

    @Test
    fun `confirming with no changes closes the sheet without calling the server`() = runTest {
        val plan = planWithTodayVisit()
        val vm = viewModel(plan)
        vm.onEvent(PlanEvent.OpenEdit(7))
        vm.onEvent(PlanEvent.EditDateChanged(today.plusDays(1)))
        vm.onEvent(PlanEvent.EditDateChanged(today)) // back to where it started
        vm.onEvent(PlanEvent.ConfirmEdit)

        assertNull(vm.state.value.edit)
        assertTrue(plan.updates.isEmpty())
    }

    @Test
    fun `a server error while editing is shown and the sheet stays open to try again`() = runTest {
        val plan = planWithTodayVisit().apply { updateFailure = AppError.Http(403, "You do not have permission.") }
        val vm = viewModel(plan)
        vm.onEvent(PlanEvent.OpenEdit(7))
        vm.onEvent(PlanEvent.EditDateChanged(today.plusDays(2)))

        vm.effects.test {
            vm.onEvent(PlanEvent.ConfirmEdit)
            assertEquals(PlanEffect.ShowError(AppError.Http(403, "You do not have permission.")), awaitItem())
        }
        assertTrue(vm.state.value.edit != null)
        assertFalse(vm.state.value.edit!!.isSaving)
        assertEquals(today.plusDays(2), vm.state.value.edit!!.date) // the engineer's change is kept
    }

    @Test
    fun `search narrows the dealers in the edit sheet`() = runTest {
        val vm = viewModel(planWithTodayVisit())
        vm.onEvent(PlanEvent.OpenEdit(7))
        advanceUntilIdle()
        vm.onEvent(PlanEvent.DealerQueryChanged("noida"))
        advanceUntilIdle()
        assertEquals(listOf("3"), vm.state.value.dealerSearch.items.map { it.id })
    }

    @Test
    fun `closing the edit sheet discards the changes`() = runTest {
        val plan = planWithTodayVisit()
        val vm = viewModel(plan)
        vm.onEvent(PlanEvent.OpenEdit(7))
        vm.onEvent(PlanEvent.EditDateChanged(today.plusDays(3)))
        vm.onEvent(PlanEvent.CloseEdit)
        assertNull(vm.state.value.edit)
        assertTrue(plan.updates.isEmpty())
    }

    // ---------------------------------------------------------------- starting an audit

    @Test
    fun `start audit asks the screen to open the form for that visit`() = runTest {
        val vm = viewModel(planWithTodayVisit())
        vm.effects.test {
            vm.onEvent(PlanEvent.StartAudit(7))
            assertEquals(PlanEffect.OpenAudit(7), awaitItem())
        }
    }
}
