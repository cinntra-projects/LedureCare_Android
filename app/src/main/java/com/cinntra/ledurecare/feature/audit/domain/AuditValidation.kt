package com.cinntra.ledurecare.feature.audit.domain

enum class AuditField { Dealer, Address, CustomerCode, SalesPerson, InspectionDate, TotalBox, Items, Warehouse }

enum class EntryField { Product, Qty, Approved, MfgType, MfgPeriod, MfgYear, Condition, Warranty, Remark }

/** What the engineer has typed for one item, parsed but not yet checked. */
data class ItemInput(
    val product: Product?,
    val qty: Int?,
    val approved: Int?,
    val rejected: Int?,
    val totalApproved: Int?,
    val mfgType: MfgType?,
    val mfgPeriod: Int?,
    val mfgYear: Int?,
    val condition: String?,
    val warranty: WarrantyStatus?,
    val remark: String,
)

private val YearRange = 1990..2100

fun validateItem(input: ItemInput): Set<EntryField> = buildSet {
    val qty = input.qty
    if (input.product == null) add(EntryField.Product)
    if (qty == null || qty < 1) add(EntryField.Qty)

    val approved = input.approved ?: 0
    val rejected = input.rejected ?: 0
    val finalQty = input.totalApproved ?: approved
    if (qty != null && (approved + rejected > qty || finalQty > qty)) add(EntryField.Approved)

    if (input.mfgType == null) add(EntryField.MfgType)
    val periodRange = if (input.mfgType == MfgType.Quarter) 1..4 else 1..12
    if (input.mfgPeriod == null || input.mfgPeriod !in periodRange) add(EntryField.MfgPeriod)
    if (input.mfgYear == null || input.mfgYear !in YearRange) add(EntryField.MfgYear)
    if (input.condition.isNullOrBlank()) add(EntryField.Condition)
    if (input.warranty == null) add(EntryField.Warranty)
    // Out of warranty needs a special remark saying why.
    if (input.warranty == WarrantyStatus.OutOfWarranty && input.remark.isBlank()) add(EntryField.Remark)
}

/** Builds the item once [validateItem] passes; null otherwise. */
fun ItemInput.toItem(id: Long): AuditItem? {
    if (validateItem(this).isNotEmpty()) return null
    val approvedCount = approved ?: 0
    return AuditItem(
        id = id,
        product = product!!,
        qty = qty!!,
        approved = approvedCount,
        rejected = rejected ?: 0,
        totalApproved = totalApproved ?: approvedCount,
        mfgType = mfgType!!,
        mfgPeriod = mfgPeriod!!,
        mfgYear = mfgYear!!,
        condition = condition!!,
        warranty = warranty!!,
        remark = remark.trim(),
    )
}

fun validateAudit(draft: AuditDraft): Set<AuditField> = buildSet {
    if (draft.dealer == null) add(AuditField.Dealer)
    if (draft.address.isBlank()) add(AuditField.Address)
    if (draft.customerCode.isBlank()) add(AuditField.CustomerCode)
    if (draft.salesPerson.isBlank()) add(AuditField.SalesPerson)
    if (draft.inspectionDate == null) add(AuditField.InspectionDate)
    if (draft.totalBox == null) add(AuditField.TotalBox)
    if (draft.items.isEmpty()) add(AuditField.Items)
    if (draft.items.isNotEmpty() && draft.warehouse == null) add(AuditField.Warehouse)
}
