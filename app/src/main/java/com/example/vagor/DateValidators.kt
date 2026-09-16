package com.example.vagor

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val APP_DATE_PATTERN = "MM/dd/yyyy"

object DateValidators {
    private fun parser() = SimpleDateFormat(APP_DATE_PATTERN, Locale.US).apply {
        isLenient = false
    }

    fun isValidDate(dateText: String): Boolean = parseDate(dateText) != null

    fun isEndDateAfterOrEqualStartDate(startDate: String, endDate: String): Boolean {
        val start = parseDate(startDate)
        val end = parseDate(endDate)
        return start != null && end != null && !end.before(start)
    }

    fun isDateWithinVacation(excursionDate: String, vacationStart: String, vacationEnd: String): Boolean {
        val excursion = parseDate(excursionDate)
        val start = parseDate(vacationStart)
        val end = parseDate(vacationEnd)
        return excursion != null && start != null && end != null &&
            !end.before(start) &&
            !excursion.before(start) && !excursion.after(end)
    }

    fun parseDate(dateText: String): Date? = try {
        parser().parse(dateText)
    } catch (_: ParseException) {
        null
    }
}
