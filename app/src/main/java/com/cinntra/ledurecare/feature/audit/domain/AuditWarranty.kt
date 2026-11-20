package com.cinntra.ledurecare.feature.audit.domain

import java.time.LocalDate

/**
 * Whole months from the manufacturing month to the current month. A quarter counts as its last month
 * (Q1 is March, Q2 June, Q3 September, Q4 December).
 */
fun monthsSinceMfg(type: MfgType, period: Int, year: Int, today: LocalDate): Int {
    val mfgMonth = if (type == MfgType.Quarter) period * 3 else period
    return (today.year * 12 + today.monthValue) - (year * 12 + mfgMonth)
}

/**
 * The warranty of an item whose master data gives a warranty of [warrantyMonths] (`U_UTL_WD`): still in
 * warranty while the item is no more than that many months old, out of warranty after.
 */
fun warrantyFor(warrantyMonths: Int, type: MfgType, period: Int, year: Int, today: LocalDate): WarrantyStatus =
    if (monthsSinceMfg(type, period, year, today) <= warrantyMonths) WarrantyStatus.InWarranty else WarrantyStatus.OutOfWarranty
