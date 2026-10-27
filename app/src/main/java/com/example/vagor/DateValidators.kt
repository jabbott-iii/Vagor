package com.example.vagor

import java.time.DateTimeException
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import java.util.Locale

// Exactly two-digit month, two-digit day and four-digit year, with nothing before or after.
private val APP_DATE_SHAPE = Regex("""\d{2}/\d{2}/\d{4}""")
private val APP_DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MM/dd/uuuu", Locale.US).withResolverStyle(ResolverStyle.STRICT)

// Event times are stored as exactly two-digit hour (00 to 23) and minute, such as "14:30".
private val APP_TIME_SHAPE = Regex("""\d{2}:\d{2}""")
private val APP_TIME_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm", Locale.US).withResolverStyle(ResolverStyle.STRICT)

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

    fun formatDate(date: LocalDate): String = date.format(APP_DATE_FORMAT)

    fun parseLocalDate(dateText: String): LocalDate? {
        if (!APP_DATE_SHAPE.matches(dateText)) return null
        return try {
            LocalDate.parse(dateText, APP_DATE_FORMAT)
        } catch (_: DateTimeParseException) {
            null
        }
    }

    fun formatTime(time: LocalTime): String = time.format(APP_TIME_FORMAT)

    fun parseLocalTime(timeText: String?): LocalTime? {
        if (timeText == null || !APP_TIME_SHAPE.matches(timeText)) return null
        return try {
            LocalTime.parse(timeText, APP_TIME_FORMAT)
        } catch (_: DateTimeException) {
            null
        }
    }

    // A stored time zone ID, such as "Asia/Tokyo", or null when it's missing or unknown.
    fun parseZone(zoneText: String?): ZoneId? {
        if (zoneText.isNullOrEmpty()) return null
        return try {
            ZoneId.of(zoneText)
        } catch (_: DateTimeException) {
            null
        }
    }
}
