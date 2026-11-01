package com.example.vagor

import com.example.vagor.entities.Event
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
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
    fun eventTriggerIsAnHourBeforeItStartsInItsOwnZone() {
        val now = ZonedDateTime.of(2026, 9, 1, 12, 0, 0, 0, zone).toInstant().toEpochMilli()
        val start = ZonedDateTime.of(2026, 9, 16, 19, 30, 0, 0, ZoneId.of("Asia/Tokyo"))
        val expected = ZonedDateTime.of(2026, 9, 16, 18, 30, 0, 0, ZoneId.of("Asia/Tokyo")).toInstant().toEpochMilli()

        assertEquals(expected, ReminderTimes.eventTriggerAtMillis(start, now))
    }

    @Test
    fun eventTriggerCanFallOnTheDayBefore() {
        val start = ZonedDateTime.of(2026, 9, 16, 0, 30, 0, 0, zone)

        assertEquals(
            ZonedDateTime.of(2026, 9, 15, 23, 30, 0, 0, zone).toInstant().toEpochMilli(),
            ReminderTimes.eventTriggerAtMillis(start, 0)
        )
    }

    // An hour of elapsed time, even when the clocks change in that hour.
    @Test
    fun eventTriggerIsAnElapsedHourAcrossDaylightSavingChanges() {
        val newYork = ZoneId.of("America/New_York")
        // Clocks go from 2:00 to 3:00 on March 8, 2026, so 3:30 comes an hour after 1:30.
        val afterSpringForward = ZonedDateTime.of(2026, 3, 8, 3, 30, 0, 0, newYork)
        assertEquals(
            ZonedDateTime.of(2026, 3, 8, 1, 30, 0, 0, newYork).toInstant().toEpochMilli(),
            ReminderTimes.eventTriggerAtMillis(afterSpringForward, 0)
        )
        // Clocks go from 2:00 back to 1:00 on November 1, 2026, so 2:30 comes an hour after the
        // second 1:30.
        val afterFallBack = ZonedDateTime.of(2026, 11, 1, 2, 30, 0, 0, newYork)
        val trigger = ReminderTimes.eventTriggerAtMillis(afterFallBack, 0)!!
        assertEquals(afterFallBack.toInstant().toEpochMilli() - 60 * 60 * 1000L, trigger)
        assertEquals(
            ZonedDateTime.of(2026, 11, 1, 1, 30, 0, 0, newYork).withLaterOffsetAtOverlap().toInstant().toEpochMilli(),
            trigger
        )
    }

    @Test
    fun noEventTriggerOnceTheHourBeforeHasPassed() {
        val start = ZonedDateTime.of(2026, 9, 16, 19, 30, 0, 0, zone)
        val hourBefore = start.minusHours(1).toInstant().toEpochMilli()

        assertEquals(hourBefore, ReminderTimes.eventTriggerAtMillis(start, hourBefore - 1))
        assertNull(ReminderTimes.eventTriggerAtMillis(start, hourBefore))
        // Saved 30 minutes before it starts: too late for a reminder an hour before.
        assertNull(ReminderTimes.eventTriggerAtMillis(start, start.minusMinutes(30).toInstant().toEpochMilli()))
    }

    // A rebuild leaves a passed reminder's alarm alone while it may still be on its way.
    @Test
    fun aPassedDateReminderMayBeDueUntilItsDayEnds() {
        val nineAm = ZonedDateTime.of(2026, 9, 16, 9, 0, 0, 0, zone)
        val midnight = ZonedDateTime.of(2026, 9, 17, 0, 0, 0, 0, zone).toInstant().toEpochMilli()

        assertTrue(
            ReminderTimes.dateReminderMayBeDue("09/16/2026", zone, nineAm.plusMinutes(30).toInstant().toEpochMilli())
        )
        assertTrue(ReminderTimes.dateReminderMayBeDue("09/16/2026", zone, midnight - 1))
        assertFalse(ReminderTimes.dateReminderMayBeDue("09/16/2026", zone, midnight))
        assertFalse(ReminderTimes.dateReminderMayBeDue("9/16/2026", zone, 0))
    }

    @Test
    fun aPassedEventReminderMayBeDueUntilTheEventStarts() {
        val start = ZonedDateTime.of(2026, 9, 16, 19, 30, 0, 0, ZoneId.of("Asia/Tokyo"))
        val startMillis = start.toInstant().toEpochMilli()

        assertTrue(ReminderTimes.eventReminderMayBeDue(start, startMillis - 1))
        assertFalse(ReminderTimes.eventReminderMayBeDue(start, startMillis))
    }

    // Android may hibernate Vagor about three months after it was last opened; a trip ending more
    // than two months ahead is offered the way to stop that.
    @Test
    fun aTripEndingMoreThanTwoMonthsAheadMayOutlastHibernation() {
        val today = LocalDate.of(2026, 11, 1)

        assertFalse(ReminderTimes.mayOutlastHibernation("01/01/2027", today))
        assertTrue(ReminderTimes.mayOutlastHibernation("01/02/2027", today))
        assertTrue(ReminderTimes.mayOutlastHibernation("07/06/2031", today))
        assertFalse(ReminderTimes.mayOutlastHibernation("10/01/2026", today))
        // A date that can't be read isn't offered.
        assertFalse(ReminderTimes.mayOutlastHibernation("1/2/2027", today))
    }

    // A posted reminder leaves the shade once it's out of date.
    @Test
    fun aDateReminderLastsUntilItsDayEnds() {
        val nineAm = ZonedDateTime.of(2026, 9, 16, 9, 0, 0, 0, zone).toInstant().toEpochMilli()
        val midnight = ZonedDateTime.of(2026, 9, 17, 0, 0, 0, 0, zone).toInstant().toEpochMilli()

        assertEquals(
            Duration.ofHours(15).toMillis(),
            ReminderTimes.dateReminderTimeoutMillis("09/16/2026", zone, nineAm)
        )
        assertEquals(1L, ReminderTimes.dateReminderTimeoutMillis("09/16/2026", zone, midnight - 1))
        // One that comes after its day gets no timeout of its own.
        assertNull(ReminderTimes.dateReminderTimeoutMillis("09/16/2026", zone, midnight))
        assertNull(ReminderTimes.dateReminderTimeoutMillis("9/16/2026", zone, nineAm))
    }

    @Test
    fun aDateReminderOnTheDayClocksGoBackLastsTheDaysExtraHour() {
        val newYork = ZoneId.of("America/New_York")
        val justAfterMidnight = ZonedDateTime.of(2026, 11, 1, 0, 30, 0, 0, newYork).toInstant().toEpochMilli()

        assertEquals(
            Duration.ofMinutes(24 * 60 + 30).toMillis(),
            ReminderTimes.dateReminderTimeoutMillis("11/01/2026", newYork, justAfterMidnight)
        )
    }

    @Test
    fun anEventReminderLastsUntilAnHourAfterTheEventStarts() {
        val tokyo = ZoneId.of("Asia/Tokyo")
        val event = Event("Dinner", "09/16/2026", 1, time = "19:30", timeZone = "Asia/Tokyo")
        val start = ZonedDateTime.of(2026, 9, 16, 19, 30, 0, 0, tokyo)

        // Its own zone counts, not the phone's.
        assertEquals(
            Duration.ofHours(2).toMillis(),
            ReminderTimes.eventReminderTimeoutMillis(event, zone, start.minusHours(1).toInstant().toEpochMilli())
        )
        assertNull(ReminderTimes.eventReminderTimeoutMillis(event, zone, start.plusHours(1).toInstant().toEpochMilli()))
    }

    @Test
    fun anEventWithoutATimeHasItsReminderUntilItsDayEnds() {
        val event = Event("Old event", "09/16/2026", 1)
        val nineAm = ZonedDateTime.of(2026, 9, 16, 9, 0, 0, 0, zone).toInstant().toEpochMilli()

        assertEquals(Duration.ofHours(15).toMillis(), ReminderTimes.eventReminderTimeoutMillis(event, zone, nineAm))
    }

    @Test
    fun keysRoundTripThroughTheirUri() {
        val keys = listOf(
            ReminderKey(ReminderType.VACATION_START, 7),
            ReminderKey(ReminderType.VACATION_END, 7),
            ReminderKey(ReminderType.EVENT, 7),
            ReminderKey(ReminderType.VACATION_START, 2001)
        )
        for (key in keys) {
            assertEquals(key, ReminderKey.fromUriString(key.toUriString()))
        }
        assertEquals("vagor://reminder/vacation/7/start", keys[0].toUriString())
        assertEquals("vagor://reminder/event/7", keys[2].toUriString())
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
        assertNull(ReminderKey.fromUriString("vagor://reminder/event/x"))
        assertNull(ReminderKey.fromUriString("https://example.com/reminder/event/7"))
        assertNull(ReminderKey.fromUriString("vagor://reminder/event/99999999999"))
    }
}
