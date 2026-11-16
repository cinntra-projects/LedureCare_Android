package com.example.ledurecare.feature.audit.ui

import androidx.lifecycle.SavedStateHandle
import com.example.ledurecare.feature.audit.data.FakeAuditRepository
import com.example.ledurecare.feature.audit.data.FakeWarehouseRepository
import com.example.ledurecare.feature.audit.domain.Warehouse
import com.example.ledurecare.feature.audit.domain.WarehouseRepository
import com.example.ledurecare.core.common.AppResult
import com.example.ledurecare.feature.audit.domain.AuditField
import com.example.ledurecare.feature.audit.domain.AuditFile
import com.example.ledurecare.feature.audit.domain.EntryField
import com.example.ledurecare.feature.audit.domain.GetAuditOptionsUseCase
import com.example.ledurecare.feature.audit.domain.MAX_FILE_BYTES
import com.example.ledurecare.feature.audit.domain.MfgType
import com.example.ledurecare.feature.audit.domain.Product
import com.example.ledurecare.feature.audit.domain.SubmitAuditUseCase
import com.example.ledurecare.feature.audit.domain.WarrantyStatus
import com.example.ledurecare.feature.dealers.data.FakeDealerRepository
import com.example.ledurecare.feature.dealers.data.StaticDealers
import com.example.ledurecare.feature.dealers.data.FakeDealerDetailsRepository
import com.example.ledurecare.feature.dealers.domain.Dealer
import com.example.ledurecare.feature.dealers.domain.DealerDetailsRepository
import com.example.ledurecare.feature.dealers.domain.GetDealerDetailsUseCase
import com.example.ledurecare.feature.dealers.domain.SearchDealersUseCase
import com.example.ledurecare.feature.plan.data.FakePlanRepository
import com.example.ledurecare.feature.plan.domain.DateRange
import com.example.ledurecare.feature.plan.domain.GetVisitUseCase
import com.example.ledurecare.feature.plan.domain.MarkVisitDoneUseCase
import com.example.ledurecare.feature.plan.domain.VisitStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class AuditViewModelTest {

    private val today = LocalDate.of(2026, 10, 28)
    private val clock = Clock.fixed(today.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
    private val filter = Product("LD-2210", "Filter kit")

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private class Harness(val vm: AuditViewModel, val plan: FakePlanRepository, val visitId: Long)

    /** Opens the form for today's planned visit to Sumit Electric, as in the design. */
    private class OneWarehouse : WarehouseRepository {
        override suspend fun getWarehouses() = AppResult.Success(listOf(Warehouse(5, "Jaipur", "A-85A Road No.9, Jaipur")))
    }

    private suspend fun TestScope.open(
        warehouses: WarehouseRepository = FakeWarehouseRepository(),
        details: DealerDetailsRepository = FakeDealerDetailsRepository(),
    ): Harness {
        val plan = FakePlanRepository(clock)
        val visit = plan.observeVisits(DateRange(today, today)).first().first { it.dealerName == "Sumit Electric" }
        val vm = AuditViewModel(
            SavedStateHandle(mapOf("visitId" to visit.id)),
            GetVisitUseCase(plan),
            SearchDealersUseCase(FakeDealerRepository()),
            GetDealerDetailsUseCase(details),
            GetAuditOptionsUseCase(FakeAuditRepository(), warehouses),
            SubmitAuditUseCase(FakeAuditRepository(), MarkVisitDoneUseCase(plan)),
            clock,
        )
        advanceUntilIdle()
        return Harness(vm, plan, visit.id)
    }

    private fun AuditViewModel.fillItem(qty: String = "12", approved: String = "10", rejected: String = "2") {
        onEvent(AuditEvent.ProductSelected(filter))
        onEvent(AuditEvent.QtyChanged(qty))
        onEvent(AuditEvent.ApprovedChanged(approved))
        onEvent(AuditEvent.RejectedChanged(rejected))
        onEvent(AuditEvent.MfgTypeChanged(MfgType.Quarter))
        onEvent(AuditEvent.MfgPeriodChanged(2))
        onEvent(AuditEvent.MfgYearChanged(2025))
        onEvent(AuditEvent.ConditionChanged("Damaged"))
        onEvent(AuditEvent.WarrantyChanged(WarrantyStatus.InWarranty))
    }

    @Test
    fun `form is prefilled from the planned visit`() = runTest {
        val state = open().vm.state.value
        assertFalse(state.isLoading)
        assertEquals(today, state.startedFrom)
        assertEquals("Sumit Electric", state.dealer?.name)
        assertEquals("Delhi Road, Meerut, UP", state.address)
        assertEquals("BP-23128", state.customerCode)
        assertEquals("Rohit Sharma", state.salesPerson)
        assertEquals(today, state.inspectionDate)
        assertEquals("North UP", state.zone)
        assertNull(state.warehouse) // several warehouses and no zone to go by: the engineer chooses
        assertTrue(state.items.isEmpty())
    }

    @Test
    fun `choosing another dealer refills the dealer details`() = runTest {
        val vm = open().vm
        vm.onEvent(AuditEvent.DealerSelected(StaticDealers.byId(4)))
        assertEquals("Verma Motors", vm.state.value.dealer?.name)
        assertEquals("BP-22310", vm.state.value.customerCode)
        assertEquals("Main Market, Ghaziabad, UP", vm.state.value.address)
    }

    @Test
    fun `adding an incomplete item reports what is missing and adds nothing`() = runTest {
        val vm = open().vm
        vm.onEvent(AuditEvent.AddItem)
        assertTrue(vm.state.value.items.isEmpty())
        assertEquals(
            setOf(
                EntryField.Product, EntryField.MfgType, EntryField.MfgPeriod,
                EntryField.MfgYear, EntryField.Condition, EntryField.Warranty,
            ),
            vm.state.value.entryErrors,
        )
    }

    @Test
    fun `a valid item is added and the panel resets`() = runTest {
        val vm = open().vm
        vm.fillItem()
        vm.onEvent(AuditEvent.AddItem)

        val state = vm.state.value
        assertEquals(1, state.items.size)
        assertEquals(12, state.items.single().qty)
        assertEquals(ItemEntry(), state.entry)
        assertTrue(state.entryErrors.isEmpty())
        assertNull(state.warehouse) // still the engineer's choice
    }

    @Test
    fun `final qty follows approved until it is edited`() = runTest {
        val vm = open().vm
        vm.onEvent(AuditEvent.ApprovedChanged("7"))
        assertEquals("7", vm.state.value.entry.totalApproved)
        vm.onEvent(AuditEvent.TotalApprovedChanged("5"))
        vm.onEvent(AuditEvent.ApprovedChanged("8"))
        assertEquals("5", vm.state.value.entry.totalApproved)
    }

    @Test
    fun `numeric fields keep digits only`() = runTest {
        val vm = open().vm
        vm.onEvent(AuditEvent.QtyChanged("1a2-"))
        vm.onEvent(AuditEvent.TotalBoxChanged("3x"))
        assertEquals("12", vm.state.value.entry.qty)
        assertEquals("3", vm.state.value.totalBox)
    }

    @Test
    fun `approved plus rejected over qty is rejected`() = runTest {
        val vm = open().vm
        vm.fillItem(qty = "5", approved = "4", rejected = "3")
        vm.onEvent(AuditEvent.AddItem)
        assertTrue(vm.state.value.items.isEmpty())
        assertEquals(setOf(EntryField.Approved), vm.state.value.entryErrors)
        vm.onEvent(AuditEvent.RejectedChanged("1"))
        assertTrue(EntryField.Approved !in vm.state.value.entryErrors)
    }

    @Test
    fun `changing the date type clears the chosen month or quarter`() = runTest {
        val vm = open().vm
        vm.onEvent(AuditEvent.MfgTypeChanged(MfgType.Quarter))
        vm.onEvent(AuditEvent.MfgPeriodChanged(4))
        vm.onEvent(AuditEvent.MfgTypeChanged(MfgType.Quarter))
        assertEquals(4, vm.state.value.entry.mfgPeriod)
        vm.onEvent(AuditEvent.MfgTypeChanged(MfgType.Month))
        assertNull(vm.state.value.entry.mfgPeriod)
    }

    @Test
    fun `product search suggests matches until one is chosen`() = runTest {
        val vm = open().vm
        vm.onEvent(AuditEvent.ProductQueryChanged("ld-3"))
        assertEquals(listOf("LD-3105"), vm.state.value.suggestions.map { it.code })
        vm.onEvent(AuditEvent.ProductSelected(filter))
        assertTrue(vm.state.value.suggestions.isEmpty())
    }

    @Test
    fun `repeat copies the item right below with qty one and highlights it`() = runTest {
        val vm = open().vm
        vm.fillItem()
        vm.onEvent(AuditEvent.AddItem)
        val original = vm.state.value.items.single()

        vm.onEvent(AuditEvent.RepeatItem(original.id))

        val items = vm.state.value.items
        assertEquals(2, items.size)
        assertEquals(original.id, items[0].id)
        with(items[1]) {
            assertEquals(1, qty)
            assertEquals(0, approved)
            assertEquals(0, rejected)
            assertTrue(copied)
            assertEquals(original.product, product)
            assertEquals(original.mfgYear, mfgYear)
        }
        assertTrue(items[1].id != original.id)
    }

    @Test
    fun `editing loads the item, updating replaces it in place and clears the copied flag`() = runTest {
        val vm = open().vm
        vm.fillItem()
        vm.onEvent(AuditEvent.AddItem)
        vm.onEvent(AuditEvent.RepeatItem(vm.state.value.items.single().id))
        val copy = vm.state.value.items[1]

        vm.onEvent(AuditEvent.EditItem(copy.id))
        assertEquals(copy.id, vm.state.value.editingId)
        assertEquals("1", vm.state.value.entry.qty)

        vm.onEvent(AuditEvent.QtyChanged("3"))
        vm.onEvent(AuditEvent.AddItem)

        val items = vm.state.value.items
        assertEquals(2, items.size)
        assertEquals(copy.id, items[1].id)
        assertEquals(3, items[1].qty)
        assertFalse(items[1].copied)
        assertNull(vm.state.value.editingId)
    }

    @Test
    fun `deleting an item removes it and leaves edit mode if it was being edited`() = runTest {
        val vm = open().vm
        vm.fillItem()
        vm.onEvent(AuditEvent.AddItem)
        val id = vm.state.value.items.single().id

        vm.onEvent(AuditEvent.EditItem(id))
        vm.onEvent(AuditEvent.DeleteItem(id))

        assertTrue(vm.state.value.items.isEmpty())
        assertNull(vm.state.value.editingId)
        assertEquals(ItemEntry(), vm.state.value.entry)
    }

    @Test
    fun `files over 10 MB are refused and duplicates ignored`() = runTest {
        val vm = open().vm
        val photo = AuditFile("content://1", "defect_photo_1.jpg", 1_200_000, "image/jpeg")
        val huge = AuditFile("content://2", "scan.pdf", MAX_FILE_BYTES + 1, "application/pdf")

        vm.onEvent(AuditEvent.FilesPicked(listOf(photo, huge)))
        vm.onEvent(AuditEvent.FilesPicked(listOf(photo)))

        assertEquals(listOf(photo), vm.state.value.files)
        assertEquals(AuditEffect.FilesRejected(listOf("scan.pdf")), vm.effects.first())

        vm.onEvent(AuditEvent.RemoveFile(photo.uri))
        assertTrue(vm.state.value.files.isEmpty())
    }

    @Test
    fun `submitting an incomplete form highlights the problems and sends nothing`() = runTest {
        val h = open()
        h.vm.onEvent(AuditEvent.Submit)

        assertEquals(setOf(AuditField.TotalBox, AuditField.Items), h.vm.state.value.errors)
        assertEquals(AuditEffect.FixErrors, h.vm.effects.first())
        assertFalse(h.vm.state.value.isSubmitting)
        assertEquals(VisitStatus.Planned, h.plan.getVisit(h.visitId)!!.status)
    }

    @Test
    fun `total quantity is the sum of total approved of the added items`() = runTest {
        val vm = open().vm
        assertEquals(0, vm.state.value.totalQty)
        vm.fillItem(qty = "12", approved = "10", rejected = "2")
        vm.onEvent(AuditEvent.AddItem)
        vm.fillItem(qty = "8", approved = "5", rejected = "3")
        vm.onEvent(AuditEvent.AddItem)
        assertEquals(15, vm.state.value.totalQty)
        vm.onEvent(AuditEvent.DeleteItem(vm.state.value.items.first().id))
        assertEquals(5, vm.state.value.totalQty)
    }

    @Test
    fun `editing a field clears its error`() = runTest {
        val vm = open().vm
        vm.onEvent(AuditEvent.Submit)
        assertTrue(AuditField.TotalBox in vm.state.value.errors)
        vm.onEvent(AuditEvent.TotalBoxChanged("3"))
        assertTrue(AuditField.TotalBox !in vm.state.value.errors)
        assertTrue(AuditField.Items in vm.state.value.errors)
    }

    @Test
    fun `with several warehouses the engineer must choose one before submitting`() = runTest {
        val h = open()
        h.vm.onEvent(AuditEvent.TotalBoxChanged("3"))
        h.vm.fillItem()
        h.vm.onEvent(AuditEvent.AddItem)

        h.vm.onEvent(AuditEvent.Submit)
        assertEquals(setOf(AuditField.Warehouse), h.vm.state.value.errors)

        h.vm.onEvent(AuditEvent.WarehouseSelected(2))
        assertEquals("Noida WH – Sector 63", h.vm.state.value.warehouse?.name)
        assertTrue(AuditField.Warehouse !in h.vm.state.value.errors)
    }

    @Test
    fun `a single warehouse is selected for the engineer`() = runTest {
        val vm = open(OneWarehouse()).vm
        assertEquals(5, vm.state.value.warehouse?.id)
    }

    @Test
    fun `a failing warehouse list fails the form with a retry`() = runTest {
        val failing = object : WarehouseRepository {
            override suspend fun getWarehouses() = com.example.ledurecare.core.common.AppResult.Failure(
                com.example.ledurecare.core.common.AppError.Network
            )
        }
        val vm = open(failing).vm
        assertEquals(com.example.ledurecare.core.common.AppError.Network, vm.state.value.error)
    }

    @Test
    fun `a complete audit is submitted and the planned visit becomes done`() = runTest {
        val h = open()
        h.vm.onEvent(AuditEvent.TotalBoxChanged("3"))
        h.vm.fillItem()
        h.vm.onEvent(AuditEvent.AddItem)
        h.vm.onEvent(AuditEvent.WarehouseSelected(1))

        h.vm.onEvent(AuditEvent.Submit)
        advanceUntilIdle()

        val effect = h.vm.effects.first()
        assertTrue(effect is AuditEffect.Submitted)
        assertTrue((effect as AuditEffect.Submitted).dliNo.startsWith("DLI-"))
        assertEquals(VisitStatus.Done, h.plan.getVisit(h.visitId)!!.status)
        assertFalse(h.vm.state.value.isSubmitting)
    }

    @Test
    fun `form counts as dirty once something is entered`() = runTest {
        val vm = open().vm
        assertFalse(vm.state.value.isDirty)
        vm.onEvent(AuditEvent.NotesChanged("Boxes were wet"))
        assertTrue(vm.state.value.isDirty)
    }

    private class StubDetails(var result: AppResult<Dealer>) : DealerDetailsRepository {
        val asked = mutableListOf<String>()
        override suspend fun getDetails(dealer: Dealer): AppResult<Dealer> {
            asked += dealer.bpCode
            return (result as? AppResult.Success)?.let { AppResult.Success(dealer.copy(address = it.data.address, salesPerson = it.data.salesPerson, zone = it.data.zone)) } ?: result
        }
    }

    @Test
    fun `the address is filled from the dealer lookup`() = runTest {
        val stub = StubDetails(AppResult.Success(Dealer("x", "x", "x", "x", zone = "N5", address = "C-17 Sector - 9 Noida, UP", salesPerson = "Mayank")))
        val vm = open(details = stub).vm
        assertEquals("C-17 Sector - 9 Noida, UP", vm.state.value.address)
        assertEquals("Mayank", vm.state.value.salesPerson)
        assertEquals("N5", vm.state.value.zone)
        assertEquals(listOf("BP-23128"), stub.asked)

        vm.onEvent(AuditEvent.DealerSelected(StaticDealers.byId(4)))
        assertEquals(listOf("BP-23128", "BP-22310"), stub.asked) // the lookup runs again for the new dealer
    }

    @Test
    fun `a failed lookup leaves the address to be typed`() = runTest {
        val vm = open(details = StubDetails(AppResult.Failure(com.example.ledurecare.core.common.AppError.Network))).vm
        assertFalse(vm.state.value.isLoading)
        assertEquals("", vm.state.value.address)
        vm.onEvent(AuditEvent.AddressChanged("Typed by hand"))
        assertEquals("Typed by hand", vm.state.value.address)
    }

    @Test
    fun `the date dialog sets type, period and year together and clear empties them`() = runTest {
        val vm = open().vm
        vm.onEvent(AuditEvent.AddItem) // shows the missing-date errors
        assertTrue(EntryField.MfgPeriod in vm.state.value.entryErrors)

        vm.onEvent(AuditEvent.MfgPicked(MfgType.Quarter, 1, 2026))
        with(vm.state.value) {
            assertEquals(MfgType.Quarter, entry.mfgType)
            assertEquals(1, entry.mfgPeriod)
            assertEquals(2026, entry.mfgYear)
            assertTrue(entryErrors.none { it == EntryField.MfgType || it == EntryField.MfgPeriod || it == EntryField.MfgYear })
        }

        vm.onEvent(AuditEvent.MfgCleared)
        assertNull(vm.state.value.entry.mfgType)
        assertNull(vm.state.value.entry.mfgPeriod)
        assertNull(vm.state.value.entry.mfgYear)
    }
}
