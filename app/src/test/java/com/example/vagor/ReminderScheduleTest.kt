package com.example.vagor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderScheduleTest {
    private val zone = ZoneId.of("America/Phoenix")

    @Test
    fun triggerIsNineAmLocalOnTheStoredDate() {
        val now = ZonedDateTime.of(2026, 9, 1, 12, 0, 0, 0, zone).toInstant().toEpochMilli()
        val expected = ZonedDateTime.of(2026, 9, 16, 9, 0, 0, 0, zone).toInstant().toEpochMilli()

        assertEquals(expected, ReminderTimes.triggerAtMillis("09/16/2026", zone, now))
    }

    @Test
    fun triggerFollowsTheZoneItIsComputedIn() {
        val now = ZonedDateTime.of(2026, 9, 1, 12, 0, 0, 0, zone).toInstant().toEpochMilli()
        val tokyo = ZoneId.of("Asia/Tokyo")
        val expected = ZonedDateTime.of(2026, 9, 16, 9, 0, 0, 0, tokyo).toInstant().toEpochMilli()

        assertEquals(expected, ReminderTimes.triggerAtMillis("09/16/2026", tokyo, now))
    }

    @Test
    fun triggerIsNineAmAcrossDaylightSavingChange() {
        val newYork = ZoneId.of("America/New_York")
        val now = ZonedDateTime.of(2026, 3, 1, 12, 0, 0, 0, newYork).toInstant().toEpochMilli()
        val expected = ZonedDateTime.of(2026, 3, 8, 9, 0, 0, 0, newYork).toInstant().toEpochMilli()

        assertEquals(expected, ReminderTimes.triggerAtMillis("03/08/2026", newYork, now))
    }

    @Test
    fun noTriggerOnceNineAmHasPassed() {
        val sameDayAfterNine = ZonedDateTime.of(2026, 9, 16, 9, 0, 0, 0, zone).toInstant().toEpochMilli()
        val sameDayBeforeNine = sameDayAfterNine - 1

        assertNull(ReminderTimes.triggerAtMillis("09/16/2026", zone, sameDayAfterNine))
        assertEquals(sameDayAfterNine, ReminderTimes.triggerAtMillis("09/16/2026", zone, sameDayBeforeNine))
        assertNull(ReminderTimes.triggerAtMillis("09/15/2026", zone, sameDayBeforeNine))
    }

    @Test
    fun noTriggerForInvalidDate() {
        assertNull(ReminderTimes.triggerAtMillis("invalid-date", zone, 0))
        assertNull(ReminderTimes.triggerAtMillis("02/30/2026", zone, 0))
    }

    @Test
    fun keysRoundTripThroughTheirUri() {
        val keys = listOf(
            ReminderKey(ReminderType.VACATION_START, 7),
            ReminderKey(ReminderType.VACATION_END, 7),
            ReminderKey(ReminderType.EXCURSION, 7),
            ReminderKey(ReminderType.VACATION_START, 2001),
        )
        for (key in keys) {
            assertEquals(key, ReminderKey.fromUriString(key.toUriString()))
        }
        assertEquals("vagor://reminder/vacation/7/start", keys[0].toUriString())
        assertEquals("vagor://reminder/excursion/7", keys[2].toUriString())
    }

    @Test
    fun keysStayDistinctForLargeIds() {
        // The old id + 1000/2000/3000 request codes collided here.
        val vacationEnd = ReminderKey(ReminderType.VACATION_END, 1)
        val laterVacationStart = ReminderKey(ReminderType.VACATION_START, 1001)

        assertNotEquals(vacationEnd.toUriString(), laterVacationStart.toUriString())
    }

    @Test
    fun unknownUrisAreRejected() {
        assertNull(ReminderKey.fromUriString(null))
        assertNull(ReminderKey.fromUriString(""))
        assertNull(ReminderKey.fromUriString("vagor://reminder/vacation/7"))
        assertNull(ReminderKey.fromUriString("vagor://reminder/vacation/7/middle"))
        assertNull(ReminderKey.fromUriString("vagor://reminder/excursion/x"))
        assertNull(ReminderKey.fromUriString("https://example.com/reminder/excursion/7"))
        assertNull(ReminderKey.fromUriString("vagor://reminder/excursion/99999999999"))
    }
}
