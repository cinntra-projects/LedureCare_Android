package com.cinntra.ledurecare.feature.plan.ui

import com.cinntra.ledurecare.feature.plan.domain.DateRange
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

/** Weeks run Monday to Sunday. */
fun weekStart(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

fun weekRange(date: LocalDate): DateRange = weekStart(date).let { DateRange(it, it.plusDays(6)) }

fun weekDates(date: LocalDate): List<LocalDate> = weekStart(date).let { start -> List(7) { start.plusDays(it.toLong()) } }

fun monthRange(month: YearMonth): DateRange = DateRange(month.atDay(1), month.atEndOfMonth())

/** Month as Monday-first rows of 7; `null` pads the days outside the month. */
fun monthGrid(month: YearMonth): List<List<LocalDate?>> {
    val leading = month.atDay(1).dayOfWeek.value - 1
    val days: List<LocalDate?> = List(leading) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
    val padded = days + List((7 - days.size % 7) % 7) { null }
    return padded.chunked(7)
}
