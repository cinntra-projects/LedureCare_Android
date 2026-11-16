package com.example.ledurecare.core.ui

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** "09 Oct" */
fun LocalDate.shortDate(): String = format(DateTimeFormatter.ofPattern("dd MMM", Locale.getDefault()))

/** "09-10-2026" */
fun LocalDate.numericDate(): String = format(DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.getDefault()))
