package com.example.vagor

import com.example.vagor.entities.Vacation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

class CalendarTimesTest {
    private fun dateOf(millis: Long, zone: ZoneId): LocalDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    private val zones = listOf("America/Phoenix", "Asia/Tokyo", "Pacific/Kiritimati", "Pacific/Pago_Pago", "UTC")
        .map { ZoneId.of(it) }

    // Calendar apps read an all-day time's date in the phone's zone or in UTC.
    @Test
    fun anAllDayMomentFallsOnItsDateInThePhonesZoneAndInUtc() {
        val date = LocalDate.of(2026, 10, 27)
        for (zone in zones) {
            val millis = CalendarTimes.allDayMillis(date, zone)
            assertEquals("$zone", date, dateOf(millis, zone))
            assertEquals("$zone in UTC", date, dateOf(millis, ZoneOffset.UTC))
        }
    }

    // In Santiago the clocks skip from midnight to 1:00 when daylight saving starts.
    @Test
    fun anAllDayMomentExistsWhenMidnightIsSkipped() {
        val zone = ZoneId.of("America/Santiago")
        val date = LocalDate.of(2026, 9, 6)
        assertEquals(date, dateOf(CalendarTimes.allDayMillis(date, zone), zone))
        assertEquals(date, dateOf(CalendarTimes.allDayMillis(date, zone), ZoneOffset.UTC))
    }

    // An end at a midnight is read as the day before by some apps and as that day by others; an
    // end inside the last day is read as that day by all of them.
    @Test
    fun aVacationEndsInsideItsLastDay() {
        for (zone in zones) {
            val (begin, end) = CalendarTimes.vacationSpanMillis(
                Vacation("Trip", "", "10/27/2026", "10/29/2026"),
                zone
            )!!

            for (readIn in listOf(zone, ZoneOffset.UTC)) {
                assertEquals("$zone read in $readIn", LocalDate.of(2026, 10, 27), dateOf(begin, readIn))
                assertEndsInside(LocalDate.of(2026, 10, 29), end, readIn)
            }
        }
    }

    // In the Azores and Cuba the clocks go back from 1:00 to midnight, so an hour after midnight
    // is midnight again.
    @Test
    fun aVacationEndingWhenClocksGoBackOverMidnightEndsInsideItsLastDay() {
        val cases =
            mapOf("Atlantic/Azores" to LocalDate.of(2026, 10, 25), "America/Havana" to LocalDate.of(2026, 11, 1))
        for ((id, last) in cases) {
            val zone = ZoneId.of(id)
            val end = CalendarTimes.allDayEndMillis(last, zone)
            assertEndsInside(last, end, zone)
            assertEndsInside(last, end, ZoneOffset.UTC)
        }
    }

    @Test
    fun aOneDayVacationEndsOnItsDayAfterItBegins() {
        for (zone in zones + ZoneId.of("America/Santiago")) {
            val day = if (zone.id == "America/Santiago") "09/06/2026" else "10/27/2026"
            val (begin, end) = CalendarTimes.vacationSpanMillis(Vacation("Day trip", "", day, day), zone)!!

            assertTrue("$zone", end > begin)
            assertEquals("$zone", dateOf(begin, zone), dateOf(end, zone))
        }
    }

    private fun assertEndsInside(date: LocalDate, end: Long, readIn: ZoneId) {
        val local = Instant.ofEpochMilli(end).atZone(readIn)
        assertEquals("read in $readIn", date, local.toLocalDate())
        assertTrue("ends at midnight read in $readIn", local.toLocalTime() != LocalTime.MIDNIGHT)
    }

    @Test
    fun noSpanForAnInvalidDate() {
        val zone = ZoneId.of("America/Phoenix")
        assertNull(CalendarTimes.vacationSpanMillis(Vacation("Trip", "", "10/27/26", "10/29/2026"), zone))
        assertNull(CalendarTimes.vacationSpanMillis(Vacation("Trip", "", "10/27/2026", "02/30/2027"), zone))
    }
}
