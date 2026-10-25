package com.example.vagor

import java.time.LocalTime
import java.time.ZoneId

enum class ReminderType { VACATION_START, VACATION_END, EXCURSION }

// One reminder per vacation start, vacation end and excursion. The URI is the alarm's identity,
// so each reminder can be replaced or cancelled on its own whatever the item id.
data class ReminderKey(val type: ReminderType, val itemId: Int) {
    fun toUriString(): String = when (type) {
        ReminderType.VACATION_START -> "vagor://reminder/vacation/$itemId/start"
        ReminderType.VACATION_END -> "vagor://reminder/vacation/$itemId/end"
        ReminderType.EXCURSION -> "vagor://reminder/excursion/$itemId"
    }

    companion object {
        private val URI_PATTERN = Regex("""vagor://reminder/(?:vacation/(\d+)/(start|end)|excursion/(\d+))""")

        fun fromUriString(uri: String?): ReminderKey? {
            val match = URI_PATTERN.matchEntire(uri ?: return null) ?: return null
            val (vacationId, edge, excursionId) = match.destructured
            return when {
                excursionId.isNotEmpty() -> excursionId.toIntOrNull()?.let { ReminderKey(ReminderType.EXCURSION, it) }
                edge == "start" -> vacationId.toIntOrNull()?.let { ReminderKey(ReminderType.VACATION_START, it) }
                else -> vacationId.toIntOrNull()?.let { ReminderKey(ReminderType.VACATION_END, it) }
            }
        }
    }
}

object ReminderTimes {
    val REMINDER_TIME: LocalTime = LocalTime.of(9, 0)

    // Epoch millis of 9:00 AM in [zone] on the stored date, or null when the date is invalid
    // or that moment has already passed.
    fun triggerAtMillis(dateText: String, zone: ZoneId, nowMillis: Long): Long? {
        val date = DateValidators.parseLocalDate(dateText) ?: return null
        val triggerAt = date.atTime(REMINDER_TIME).atZone(zone).toInstant().toEpochMilli()
        return triggerAt.takeIf { it > nowMillis }
    }
}
