package com.example.vagor

import com.example.vagor.entities.Event
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.Period
import java.time.ZoneId
import java.time.ZonedDateTime

enum class ReminderType { VACATION_START, VACATION_END, EVENT }

// One reminder per vacation start, vacation end and event. The URI is the alarm's identity,
// so each reminder can be replaced or cancelled on its own whatever the item id.
data class ReminderKey(val type: ReminderType, val itemId: Int) {
    fun toUriString(): String = when (type) {
        ReminderType.VACATION_START -> "vagor://reminder/vacation/$itemId/start"
        ReminderType.VACATION_END -> "vagor://reminder/vacation/$itemId/end"
        ReminderType.EVENT -> "vagor://reminder/event/$itemId"
    }

    companion object {
        private val URI_PATTERN = Regex("""vagor://reminder/(?:vacation/(\d+)/(start|end)|event/(\d+))""")

        fun fromUriString(uri: String?): ReminderKey? {
            val match = URI_PATTERN.matchEntire(uri ?: return null) ?: return null
            val (vacationId, edge, eventId) = match.destructured
            return when {
                eventId.isNotEmpty() -> eventId.toIntOrNull()?.let { ReminderKey(ReminderType.EVENT, it) }
                edge == "start" -> vacationId.toIntOrNull()?.let { ReminderKey(ReminderType.VACATION_START, it) }
                else -> vacationId.toIntOrNull()?.let { ReminderKey(ReminderType.VACATION_END, it) }
            }
        }
    }
}

object ReminderTimes {
    val REMINDER_TIME: LocalTime = LocalTime.of(9, 0)

    // An event with a time is reminded this long before it starts.
    val EVENT_LEAD: Duration = Duration.ofHours(1)

    // From Android 12, Android hibernates an app that hasn't been used for about three months,
    // which cancels its alarms. A trip that ends further ahead than this may still have reminders
    // to come by then, if Vagor isn't opened in between (HibernationOfferDialog).
    val HIBERNATION_MARGIN: Period = Period.ofMonths(2)

    // Whether a vacation ending on the stored [endDateText] is further ahead of [today] than
    // HIBERNATION_MARGIN. False when the date can't be read.
    fun mayOutlastHibernation(endDateText: String, today: LocalDate): Boolean {
        val end = DateValidators.parseLocalDate(endDateText) ?: return false
        return end.isAfter(today.plus(HIBERNATION_MARGIN))
    }

    // Epoch millis of 9:00 AM in [zone] on the stored date, or null when the date is invalid
    // or that moment has already passed.
    fun triggerAtMillis(dateText: String, zone: ZoneId, nowMillis: Long): Long? {
        val date = DateValidators.parseLocalDate(dateText) ?: return null
        val triggerAt = date.atTime(REMINDER_TIME).atZone(zone).toInstant().toEpochMilli()
        return triggerAt.takeIf { it > nowMillis }
    }

    // Epoch millis an hour before an event starts, or null once that moment has passed. It's an
    // hour of elapsed time, so a daylight saving change during it doesn't stretch or shrink it.
    fun eventTriggerAtMillis(start: ZonedDateTime, nowMillis: Long): Long? {
        val triggerAt = start.toInstant().minus(EVENT_LEAD).toEpochMilli()
        return triggerAt.takeIf { it > nowMillis }
    }

    // Whether a reminder whose moment has passed may still be on its way, so rebuilding the
    // reminders should leave its alarm alone: an inexact alarm can come late (the end of its
    // window, Doze, Battery Saver). A 9:00 AM reminder is still worth having until its day ends.
    fun dateReminderMayBeDue(dateText: String, zone: ZoneId, nowMillis: Long): Boolean {
        val date = DateValidators.parseLocalDate(dateText) ?: return false
        return nowMillis < date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    }

    // An event's reminder is still worth having until the event starts.
    fun eventReminderMayBeDue(start: ZonedDateTime, nowMillis: Long): Boolean =
        nowMillis < start.toInstant().toEpochMilli()

    // A posted event reminder is out of date this long after the event starts.
    val EVENT_REMINDER_EXPIRY: Duration = Duration.ofHours(1)

    // How long a 9:00 AM reminder posted now stays in the shade: until its day ends in [zone].
    // Null once the day has ended, or when the date can't be read: a reminder that comes late is
    // still posted, with no timeout of its own.
    fun dateReminderTimeoutMillis(dateText: String, zone: ZoneId, nowMillis: Long): Long? {
        val date = DateValidators.parseLocalDate(dateText) ?: return null
        val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return (dayEnd - nowMillis).takeIf { it > 0 }
    }

    // How long an event's reminder posted now stays in the shade: until an hour after the event
    // starts, or, for an event saved before events had a time, until its day ends. Null as for
    // dateReminderTimeoutMillis.
    fun eventReminderTimeoutMillis(event: Event, zone: ZoneId, nowMillis: Long): Long? {
        val start = EventTimes.startOf(event, zone) ?: return dateReminderTimeoutMillis(event.date, zone, nowMillis)
        val expiry = start.toInstant().plus(EVENT_REMINDER_EXPIRY).toEpochMilli()
        return (expiry - nowMillis).takeIf { it > 0 }
    }
}
