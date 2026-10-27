package com.example.vagor

import com.example.vagor.entities.Event
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

// Plain-Kotlin event time math, kept apart from Android so it can be unit tested.
object EventTimes {
    // A new event's time picker opens at 9:00 AM.
    val DEFAULT_TIME: LocalTime = LocalTime.of(9, 0)

    // Date order, then time of day, then title. Events saved before events had a time come first
    // on their date. Each event's own time is used, whatever its time zone.
    val ORDER: Comparator<Event> =
        compareBy<Event, Long?>(nullsLast()) { DateValidators.parseLocalDate(it.date)?.toEpochDay() }
            .thenBy(nullsFirst()) { DateValidators.parseLocalTime(it.time) }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }

    // When the event starts, in its own time zone (or [fallbackZone] when it has none or an
    // unknown one). Null when it has no valid date and time.
    fun startOf(event: Event, fallbackZone: ZoneId): ZonedDateTime? {
        val date = DateValidators.parseLocalDate(event.date) ?: return null
        val time = DateValidators.parseLocalTime(event.time) ?: return null
        return date.atTime(time).atZone(DateValidators.parseZone(event.timeZone) ?: fallbackZone)
    }
}
