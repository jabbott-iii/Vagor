package com.example.vagor

import com.example.vagor.entities.Event
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class EventTimesTest {
    private val phoneZone = ZoneId.of("America/New_York")

    private fun event(title: String, date: String, time: String? = null, zone: String? = null) =
        Event(title, date, vacationId = 1, time = time, timeZone = zone)

    @Test
    fun startIsTheDateAndTimeInTheEventsZone() {
        assertEquals(
            ZonedDateTime.of(2031, 7, 6, 14, 30, 0, 0, ZoneId.of("Asia/Tokyo")),
            EventTimes.startOf(event("Museum", "07/06/2031", "14:30", "Asia/Tokyo"), phoneZone)
        )
    }

    @Test
    fun missingOrUnknownZoneUsesTheFallback() {
        val expected = ZonedDateTime.of(2031, 7, 6, 14, 30, 0, 0, phoneZone)
        assertEquals(expected, EventTimes.startOf(event("Museum", "07/06/2031", "14:30"), phoneZone))
        assertEquals(expected, EventTimes.startOf(event("Museum", "07/06/2031", "14:30", "Nowhere/Else"), phoneZone))
    }

    @Test
    fun noStartWithoutAValidDateAndTime() {
        assertNull(EventTimes.startOf(event("Old event", "07/06/2031"), phoneZone))
        assertNull(EventTimes.startOf(event("Loose date", "7/6/2031", "14:30"), phoneZone))
        assertNull(EventTimes.startOf(event("Loose time", "07/06/2031", "2:30"), phoneZone))
    }

    // A time that a daylight saving change skips moves forward, as the clocks do.
    @Test
    fun skippedTimeMovesForward() {
        val start = EventTimes.startOf(event("Early", "03/09/2031", "02:30", "America/New_York"), phoneZone)
        assertEquals(3, start!!.hour)
        assertEquals(30, start.minute)
    }

    @Test
    fun eventsAreOrderedByDateThenTimeThenTitle() {
        val events = listOf(
            event("Dinner", "07/06/2031", "19:00", "Asia/Tokyo"),
            event("bad date", "7/6/2031", "08:00"),
            event("Lunch", "07/06/2031", "12:00", "Europe/Paris"),
            event("Breakfast", "07/06/2031", "08:00"),
            event("Old event", "07/06/2031"),
            event("Arrival", "07/05/2031", "22:00"),
            event("aquarium", "07/06/2031", "12:00"),
        )

        assertEquals(
            listOf("Arrival", "Old event", "Breakfast", "aquarium", "Lunch", "Dinner", "bad date"),
            events.sortedWith(EventTimes.ORDER).map { it.title }
        )
    }
}
