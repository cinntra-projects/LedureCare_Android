package com.example.ledurecare.feature.plan.domain

import java.time.LocalDate

enum class VisitStatus { Planned, Done }

data class PlannedVisit(
    val id: Long,
    val dealerId: String,
    val dealerName: String,
    val bpCode: String,
    val city: String,
    val date: LocalDate,
    val status: VisitStatus,
    /** The submitted DLI, known once the plan is completed. */
    val returnId: Int? = null,
)

data class DateRange(val start: LocalDate, val end: LocalDate)

/** A dealer the server did not plan (for example, it is already planned for that date). */
data class SkippedDealer(val dealerCode: String, val reason: String)

/** What adding dealers to a date did: which visits were created, and which dealers were left out and why. */
data class AddVisitsResult(
    val createdIds: List<Long>,
    val skipped: List<SkippedDealer>,
)

/** Changes to one plan. Only what is set is sent: a new date, a new dealer, or both. */
data class VisitChange(
    val date: LocalDate? = null,
    val dealer: com.example.ledurecare.feature.dealers.domain.Dealer? = null,
)
