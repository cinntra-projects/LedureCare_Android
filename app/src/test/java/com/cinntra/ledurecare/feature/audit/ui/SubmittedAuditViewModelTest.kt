package com.cinntra.ledurecare.feature.audit.ui

import androidx.lifecycle.SavedStateHandle
import com.cinntra.ledurecare.core.common.AppResult
import com.cinntra.ledurecare.feature.audit.data.FakeAuditRepository
import com.cinntra.ledurecare.feature.audit.domain.AuditDraft
import com.cinntra.ledurecare.feature.audit.domain.AuditFile
import com.cinntra.ledurecare.feature.audit.domain.GetSubmittedAuditUseCase
import com.cinntra.ledurecare.feature.audit.domain.ItemInput
import com.cinntra.ledurecare.feature.audit.domain.MfgType
import com.cinntra.ledurecare.feature.audit.domain.Product
import com.cinntra.ledurecare.feature.audit.domain.SubmittedAudit
import com.cinntra.ledurecare.feature.audit.domain.Warehouse
import com.cinntra.ledurecare.feature.audit.domain.WarrantyStatus
import com.cinntra.ledurecare.feature.audit.domain.toItem
import com.cinntra.ledurecare.feature.dealers.data.StaticDealers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class SubmittedAuditViewModelTest {

    private val photo = AuditFile("content://photo", "defect_photo_1.jpg", 1_200_000, "image/jpeg")
    private val invoice = AuditFile("content://invoice", "invoice_BP-23128.pdf", 340_000, "application/pdf")

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun draft(): AuditDraft {
        val item = ItemInput(
            product = Product("LD-2210", "Filter kit"), qty = 12, approved = 10, rejected = 2, totalApproved = 10,
            mfgType = MfgType.Quarter, mfgPeriod = 2, mfgYear = 2025, condition = "Damaged",
            warranty = WarrantyStatus.InWarranty, remark = "",
        ).toItem(1)!!
        return AuditDraft(
            dealer = StaticDealers.byId(2),
            address = "Delhi Road, Meerut, UP",
            customerCode = "BP-23128",
            salesPerson = "Rohit Sharma",
            inspectionDate = LocalDate.of(2026, 10, 28),
            totalBox = 3,
            totalQty = 14,
            items = listOf(item),
            warehouse = Warehouse(1, "Ghaziabad WH"),
            notes = "Water damage on 2 filter kits.",
            files = listOf(photo, invoice),
        )
    }

    private suspend fun FakeAuditRepository.submitSample(): SubmittedAudit =
        (submit(draft()) as AppResult.Success).data

    private fun viewModel(repo: FakeAuditRepository, vararg args: Pair<String, Any?>) =
        SubmittedAuditViewModel(SavedStateHandle(mapOf(*args)), GetSubmittedAuditUseCase(repo))

    @Test
    fun `a submitted audit can be looked up with everything that was sent`() = runTest {
        val repo = FakeAuditRepository()
        val submitted = repo.submitSample()

        assertEquals("DLI-1065", submitted.dliNo)
        assertEquals("Sumit Electric", submitted.dealerName)
        assertEquals("BP-23128", submitted.bpCode)
        assertEquals("DLI-1065_Audit_Form.pdf", submitted.form.name)
        assertEquals(listOf(photo, invoice), submitted.attachments)
        assertEquals("Water damage on 2 filter kits.", submitted.notes)
        assertEquals(submitted, (repo.getSubmitted("DLI-1065") as AppResult.Success).data)
    }

    @Test
    fun `each submit gets the next DLI number`() = runTest {
        val repo = FakeAuditRepository()
        assertEquals("DLI-1065", repo.submitSample().dliNo)
        assertEquals("DLI-1066", repo.submitSample().dliNo)
    }

    @Test
    fun `an unknown DLI is reported as not found`() = runTest {
        assertTrue(FakeAuditRepository().getSubmitted("DLI-0000") is AppResult.Failure)
    }

    @Test
    fun `the confirmation screen loads the audit and has no viewed file`() = runTest {
        val repo = FakeAuditRepository().also { it.submitSample() }
        val state = viewModel(repo, "dliNo" to "DLI-1065").state.value

        assertEquals("DLI-1065", state.audit?.dliNo)
        assertNull(state.viewed)
        assertNull(state.error)
    }

    @Test
    fun `the viewer opens the audit form, or an attachment by position`() = runTest {
        val repo = FakeAuditRepository().also { it.submitSample() }

        assertEquals(
            ViewedFile.AuditForm,
            viewModel(repo, "dliNo" to "DLI-1065", "fileIndex" to AUDIT_FORM_INDEX).state.value.viewed,
        )
        assertEquals(
            ViewedFile.Attachment(photo),
            viewModel(repo, "dliNo" to "DLI-1065", "fileIndex" to 0).state.value.viewed,
        )
        assertEquals(
            ViewedFile.Attachment(invoice),
            viewModel(repo, "dliNo" to "DLI-1065", "fileIndex" to 1).state.value.viewed,
        )
    }

    @Test
    fun `an attachment position that does not exist is an error`() = runTest {
        val repo = FakeAuditRepository().also { it.submitSample() }
        val state = viewModel(repo, "dliNo" to "DLI-1065", "fileIndex" to 5).state.value
        assertNotNull(state.error)
        assertNull(state.viewed)
    }

    @Test
    fun `a missing DLI shows an error on the confirmation screen`() = runTest {
        val state = viewModel(FakeAuditRepository(), "dliNo" to "DLI-9999").state.value
        assertNotNull(state.error)
        assertNull(state.audit)
    }
}
