package com.example.vagor

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.provider.CalendarContract
import com.example.vagor.entities.Event
import com.example.vagor.entities.Vacation
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

// Add to Calendar: Intent.ACTION_INSERT opens a calendar app's own new-event screen, Google
// Calendar's when it's installed, filled in with one vacation or event. Vagor needs no calendar
// permission and never reads or writes the calendar itself; the calendar app decides whether and
// where to save it (some save when the user leaves that screen).

// Plain-Kotlin times for those intents, kept apart from Android so they can be unit tested.
object CalendarTimes {
    // Events have a start but no length, so they go in as an hour, which the user can change there.
    val EVENT_LENGTH: Duration = Duration.ofHours(1)

    // An all-day end is at least an hour into its last day, both in elapsed time and on the clock.
    private val ALL_DAY_END_INTO_DAY: Duration = Duration.ofHours(1)
    private val ALL_DAY_END_TIME: LocalTime = LocalTime.of(1, 0)

    // The moment that stands for [date] in an all-day intent. Calendar apps take its date, some in
    // the phone's time zone and some in UTC, and the later of the two midnights is on [date] in
    // both.
    fun allDayMillis(date: LocalDate, phoneZone: ZoneId): Long =
        maxOf(date.atStartOfDay(ZoneOffset.UTC).toInstant(), date.atStartOfDay(phoneZone).toInstant()).toEpochMilli()

    // The moment that ends an all-day item on [date]: an hour after allDayMillis, or 1:00 on the
    // phone's clock, whichever is later. An end at a midnight is read as the day before by some
    // apps (Android's original calendar and Etar) and as that day by others (Fossify); one inside
    // the last day is read as that day by all of them. Google Calendar doesn't document which it
    // does. Both are needed: where clocks go back from 1:00 to midnight, an hour after midnight is
    // midnight again, and where they skip from midnight to 1:00, 1:00 is the day's first moment.
    fun allDayEndMillis(date: LocalDate, phoneZone: ZoneId): Long = maxOf(
        allDayMillis(date, phoneZone) + ALL_DAY_END_INTO_DAY.toMillis(),
        date.atTime(ALL_DAY_END_TIME).atZone(phoneZone).toInstant().toEpochMilli()
    )

    // A vacation's first day and the end of its last, or null when a stored date is invalid.
    fun vacationSpanMillis(vacation: Vacation, phoneZone: ZoneId): Pair<Long, Long>? {
        val start = DateValidators.parseLocalDate(vacation.startDate) ?: return null
        val end = DateValidators.parseLocalDate(vacation.endDate) ?: return null
        return allDayMillis(start, phoneZone) to allDayEndMillis(end, phoneZone)
    }
}

private const val GOOGLE_CALENDAR = "com.google.android.calendar"
private const val CALENDAR_EVENTS_TYPE = "vnd.android.cursor.dir/event"

// A vacation as an all-day item over its dates, with its trip details and events as the
// description. Null when a stored date is invalid. No time zone goes with it: an all-day item is
// read in the calendar app's own.
fun Context.vacationCalendarIntent(vacation: Vacation, events: List<Event>): Intent? {
    val (begin, end) = CalendarTimes.vacationSpanMillis(vacation, ZoneId.systemDefault()) ?: return null
    val eventLines = events.joinToString("\n") { getString(R.string.event_share_line, it.title, eventWhenText(it)) }
    val description = listOfNotNull(
        vacation.details.takeIf { it.isNotBlank() },
        eventLines.takeIf { it.isNotEmpty() }?.let { getString(R.string.calendar_vacation_events, it) }
    ).joinToString("\n\n")
    return insertIntent(vacation.title, description, begin, end)
        .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
}

// An event at its time in its own time zone, an hour long. Null without a valid date and time.
// Some calendar apps show it in the phone's time zone instead, so the description names its own.
fun Context.eventCalendarIntent(event: Event, vacation: Vacation): Intent? {
    val start = EventTimes.startOf(event, ZoneId.systemDefault()) ?: return null
    val begin = start.toInstant().toEpochMilli()
    val timeAndVacation = listOfNotNull(
        eventTimeText(event)?.let {
            getString(R.string.event_share_time, it, TimeZoneChoices.label(this, start.zone.id))
        },
        getString(R.string.reminder_part_of_vacation, vacation.title)
    ).joinToString("\n")
    val description = if (event.details.isBlank()) {
        timeAndVacation
    } else {
        getString(R.string.calendar_event_description, event.details, timeAndVacation)
    }
    return insertIntent(event.title, description, begin, begin + CalendarTimes.EVENT_LENGTH.toMillis())
        .putExtra(CalendarContract.Events.EVENT_TIMEZONE, start.zone.id)
}

// The type is given so the intent doesn't depend on asking the calendar provider for it.
private fun insertIntent(title: String, description: String, begin: Long, end: Long): Intent =
    Intent(Intent.ACTION_INSERT)
        .setDataAndType(CalendarContract.Events.CONTENT_URI, CALENDAR_EVENTS_TYPE)
        .putExtra(CalendarContract.Events.TITLE, title)
        .putExtra(CalendarContract.Events.DESCRIPTION, description)
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, begin)
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end)

// When a calendar app was last opened. A second tap while it's opening would open a second
// new-event screen, and some calendar apps save each one when it's left.
internal var calendarOpenedAt = Long.MIN_VALUE / 2
private const val SECOND_TAP_MILLIS = 2000L

// Opens Google Calendar's new-event screen, or another calendar app's when Google Calendar isn't
// installed, or says that no app can add it. A dialog, because a toast is cut at two lines.
fun Activity.openCalendarInsert(insert: Intent) {
    val now = SystemClock.elapsedRealtime()
    if (now - calendarOpenedAt < SECOND_TAP_MILLIS) return
    for (intent in listOf(Intent(insert).setPackage(GOOGLE_CALENDAR), insert)) {
        try {
            startActivity(intent)
            calendarOpenedAt = now
            return
        } catch (_: ActivityNotFoundException) {
            // Try the next one.
        }
    }
    MaterialAlertDialogBuilder(this)
        .setMessage(R.string.no_calendar_app)
        .setPositiveButton(android.R.string.ok, null)
        .show()
}
