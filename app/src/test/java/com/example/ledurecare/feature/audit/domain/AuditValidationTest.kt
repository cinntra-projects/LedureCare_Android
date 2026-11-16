package com.example.ledurecare.feature.audit.domain

import com.example.ledurecare.feature.dealers.data.StaticDealers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class AuditValidationTest {

    private val product = Product("LD-2210", "Filter kit")

    @Test
    fun `out of warranty needs a special remark`() {
        val out = input(warranty = WarrantyStatus.OutOfWarranty)
        assertEquals(setOf(EntryField.Remark), validateItem(out))
        assertEquals(emptySet<EntryField>(), validateItem(out.copy(remark = "Burnt by power surge")))
        assertEquals(emptySet<EntryField>(), validateItem(input(warranty = WarrantyStatus.InWarranty)))
    }

    private fun input(
        product: Product? = this.product,
        qty: Int? = 12,
        approved: Int? = 10,
        rejected: Int? = 2,
        totalApproved: Int? = 10,
        mfgType: MfgType? = MfgType.Quarter,
        mfgPeriod: Int? = 2,
        mfgYear: Int? = 2025,
        condition: String? = "Damaged",
        warranty: WarrantyStatus? = WarrantyStatus.InWarranty,
    ) = ItemInput(product, qty, approved, rejected, totalApproved, mfgType, mfgPeriod, mfgYear, condition, warranty, "")

    @Test
    fun `a complete item is valid and builds`() {
        assertEquals(emptySet<EntryField>(), validateItem(input()))
        val item = input().toItem(7)!!
        assertEquals(7, item.id)
        assertEquals(12, item.qty)
        assertEquals(false, item.copied)
    }

    @Test
    fun `missing required fields are reported`() {
        val empty = ItemInput(null, null, null, null, null, null, null, null, null, null, "")
        assertEquals(
            setOf(
                EntryField.Product, EntryField.Qty, EntryField.MfgType, EntryField.MfgPeriod,
                EntryField.MfgYear, EntryField.Condition, EntryField.Warranty,
            ),
            validateItem(empty),
        )
        assertNull(empty.toItem(1))
    }

    @Test
    fun `qty must be at least one`() {
        assertEquals(setOf(EntryField.Qty), validateItem(input(qty = 0, approved = 0, rejected = 0, totalApproved = 0)))
    }

    @Test
    fun `approved plus rejected cannot exceed qty, nor can the final qty`() {
        assertEquals(setOf(EntryField.Approved), validateItem(input(approved = 11, rejected = 2, totalApproved = 11)))
        assertEquals(setOf(EntryField.Approved), validateItem(input(approved = 5, rejected = 0, totalApproved = 13)))
        assertEquals(emptySet<EntryField>(), validateItem(input(approved = 0, rejected = 2, totalApproved = 0, qty = 2)))
    }

    @Test
    fun `blank approved and rejected count as zero and final qty follows approved`() {
        val item = input(approved = 4, rejected = null, totalApproved = null).toItem(1)!!
        assertEquals(0, item.rejected)
        assertEquals(4, item.totalApproved)
    }

    @Test
    fun `period must fit the date type`() {
        assertEquals(setOf(EntryField.MfgPeriod), validateItem(input(mfgType = MfgType.Quarter, mfgPeriod = 5)))
        assertEquals(emptySet<EntryField>(), validateItem(input(mfgType = MfgType.Month, mfgPeriod = 12)))
        assertEquals(setOf(EntryField.MfgPeriod), validateItem(input(mfgType = MfgType.Month, mfgPeriod = 13)))
    }

    private fun draft(
        items: List<AuditItem> = listOf(input().toItem(1)!!),
        warehouse: Warehouse? = Warehouse(1, "Ghaziabad WH"),
        totalBox: Int? = 3,
        totalQty: Int? = 14,
    ) = AuditDraft(
        dealer = StaticDealers.byId(2),
        address = "Delhi Road, Meerut, UP",
        customerCode = "BP-23128",
        salesPerson = "Rohit Sharma",
        inspectionDate = LocalDate.of(2026, 10, 28),
        totalBox = totalBox,
        totalQty = totalQty,
        items = items,
        warehouse = warehouse,
        notes = "",
        files = emptyList(),
    )

    @Test
    fun `a complete audit is valid`() {
        assertEquals(emptySet<AuditField>(), validateAudit(draft()))
        assertNotNull(draft().dealer)
    }

    @Test
    fun `totals and items are required`() {
        assertEquals(
            setOf(AuditField.TotalBox, AuditField.Items),
            validateAudit(draft(items = emptyList(), warehouse = null, totalBox = null, totalQty = null)),
        )
    }

    @Test
    fun `warehouse is required once there are items`() {
        assertEquals(setOf(AuditField.Warehouse), validateAudit(draft(warehouse = null)))
    }

    @Test
    fun `blank dealer details are reported`() {
        val blank = draft().copy(dealer = null, address = " ", customerCode = "", salesPerson = "", inspectionDate = null)
        assertEquals(
            setOf(
                AuditField.Dealer, AuditField.Address, AuditField.CustomerCode,
                AuditField.SalesPerson, AuditField.InspectionDate,
            ),
            validateAudit(blank),
        )
    }
}
