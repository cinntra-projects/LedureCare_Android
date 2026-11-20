package com.cinntra.ledurecare.feature.plan.ui

import com.cinntra.ledurecare.feature.plan.domain.DateRange
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private fun pattern(p: String) = DateTimeFormatter.ofPattern(p, Locale.getDefault())

/** "Wed, 28 Oct" */
fun LocalDate.dayLabel(): String = format(pattern("EEE, d MMM"))

/** "28 Oct" */
fun LocalDate.shortLabel(): String = format(pattern("d MMM"))

/** "26 Oct – 1 Nov" */
fun DateRange.label(): String = "${start.shortLabel()} – ${end.shortLabel()}"

/** "October 2026" */
fun YearMonth.label(): String = format(pattern("MMMM yyyy"))

/** "October" */
fun YearMonth.nameLabel(): String = month.getDisplayName(TextStyle.FULL, Locale.getDefault())

/** "M", "T", "W"... */
fun LocalDate.narrowWeekday(): String = dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault())
