package com.example.vagor

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import java.util.Locale

// Exactly two-digit month, two-digit day and four-digit year, with nothing before or after.
private val APP_DATE_SHAPE = Regex("""\d{2}/\d{2}/\d{4}""")
private val APP_DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MM/dd/uuuu", Locale.US).withResolverStyle(ResolverStyle.STRICT)

object DateValidators {
    fun isValidDate(dateText: String): Boolean = parseLocalDate(dateText) != null

    fun isEndDateAfterOrEqualStartDate(startDate: String, endDate: String): Boolean {
        val start = parseLocalDate(startDate)
        val end = parseLocalDate(endDate)
        return start != null && end != null && !end.isBefore(start)
    }

    fun isDateWithinVacation(eventDate: String, vacationStart: String, vacationEnd: String): Boolean {
        val event = parseLocalDate(eventDate)
        val start = parseLocalDate(vacationStart)
        val end = parseLocalDate(vacationEnd)
        return event != null && start != null && end != null &&
            !end.isBefore(start) &&
            !event.isBefore(start) && !event.isAfter(end)
    }

    fun parseLocalDate(dateText: String): LocalDate? {
        if (!APP_DATE_SHAPE.matches(dateText)) return null
        return try {
            LocalDate.parse(dateText, APP_DATE_FORMAT)
        } catch (_: DateTimeParseException) {
            null
        }
    }
}
