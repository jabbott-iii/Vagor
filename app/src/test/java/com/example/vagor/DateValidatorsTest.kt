package com.example.vagor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class DateValidatorsTest {
    @Test
    fun validDateUsesExpectedPattern() {
        assertTrue(DateValidators.isValidDate("09/16/2026"))
        assertFalse(DateValidators.isValidDate("2026-09-16"))
        assertFalse(DateValidators.isValidDate("02/30/2026"))
    }

    @Test
    fun looseDatesAreRejected() {
        assertFalse(DateValidators.isValidDate("09/16/2026abc"))
        assertFalse(DateValidators.isValidDate("9/16/2026"))
        assertFalse(DateValidators.isValidDate("09/6/2026"))
        assertFalse(DateValidators.isValidDate("09/16/26"))
        assertFalse(DateValidators.isValidDate("09/16/20261"))
        assertFalse(DateValidators.isValidDate(" 09/16/2026"))
        assertFalse(DateValidators.isValidDate("09/16/2026 "))
        assertFalse(DateValidators.isValidDate("09-16-2026"))
        assertFalse(DateValidators.isValidDate("+9/16/2026"))
        assertFalse(DateValidators.isValidDate("13/01/2026"))
        assertFalse(DateValidators.isValidDate("00/10/2026"))
        assertFalse(DateValidators.isValidDate("10/00/2026"))
        assertFalse(DateValidators.isValidDate(""))
    }

    @Test
    fun leapDaysFollowTheCalendar() {
        assertTrue(DateValidators.isValidDate("02/29/2028"))
        assertFalse(DateValidators.isValidDate("02/29/2027"))
        assertFalse(DateValidators.isValidDate("02/29/2100"))
        assertTrue(DateValidators.isValidDate("02/29/2000"))
    }

    @Test
    fun comparisonsRejectLooseDates() {
        assertFalse(DateValidators.isEndDateAfterOrEqualStartDate("9/16/2026", "09/18/2026"))
        assertFalse(DateValidators.isDateWithinVacation("09/17/26", "09/16/2026", "09/18/2026"))
    }

    @Test
    fun endDateMustNotPrecedeStartDate() {
        val startDate = "09/16/2026"
        val sameDayEndDate = "09/16/2026"
        val earlierEndDate = "09/15/2026"

        assertTrue(DateValidators.isEndDateAfterOrEqualStartDate(startDate, sameDayEndDate))
        assertFalse(DateValidators.isEndDateAfterOrEqualStartDate(startDate, earlierEndDate))
    }

    @Test
    fun eventDateMustStayInsideVacationWindow() {
        assertTrue(DateValidators.isDateWithinVacation("09/17/2026", "09/16/2026", "09/18/2026"))
        assertFalse(DateValidators.isDateWithinVacation("09/19/2026", "09/16/2026", "09/18/2026"))
        assertFalse(DateValidators.isDateWithinVacation("09/17/2026", "09/18/2026", "09/16/2026"))
    }

    @Test
    fun parseLocalDateReturnsTheCalendarDate() {
        assertEquals(LocalDate.of(2026, 9, 16), DateValidators.parseLocalDate("09/16/2026"))
        assertEquals(LocalDate.of(2028, 2, 29), DateValidators.parseLocalDate("02/29/2028"))
        assertNull(DateValidators.parseLocalDate("02/30/2026"))
        assertNull(DateValidators.parseLocalDate("2026-09-16"))
    }

    @Test
    fun formatDateWritesTheStoredFormat() {
        assertEquals("09/06/2026", DateValidators.formatDate(LocalDate.of(2026, 9, 6)))
        assertEquals("12/31/1999", DateValidators.formatDate(LocalDate.of(1999, 12, 31)))
        val date = LocalDate.of(2028, 2, 29)
        assertEquals(date, DateValidators.parseLocalDate(DateValidators.formatDate(date)))
    }

    @Test
    fun timesAreStoredAsTwoDigitHourAndMinute() {
        assertEquals(LocalTime.of(9, 5), DateValidators.parseLocalTime("09:05"))
        assertEquals(LocalTime.of(23, 59), DateValidators.parseLocalTime("23:59"))
        assertEquals(LocalTime.MIDNIGHT, DateValidators.parseLocalTime("00:00"))
        assertEquals("09:05", DateValidators.formatTime(LocalTime.of(9, 5)))
        assertEquals("14:30", DateValidators.formatTime(LocalTime.of(14, 30, 45)))
    }

    @Test
    fun looseTimesAreRejected() {
        assertNull(DateValidators.parseLocalTime(null))
        assertNull(DateValidators.parseLocalTime(""))
        assertNull(DateValidators.parseLocalTime("9:05"))
        assertNull(DateValidators.parseLocalTime("09:5"))
        assertNull(DateValidators.parseLocalTime("24:00"))
        assertNull(DateValidators.parseLocalTime("12:60"))
        assertNull(DateValidators.parseLocalTime("09:05:00"))
        assertNull(DateValidators.parseLocalTime(" 09:05"))
        assertNull(DateValidators.parseLocalTime("9:05 AM"))
    }

    @Test
    fun timeZonesAreZoneIds() {
        assertEquals(ZoneId.of("Asia/Tokyo"), DateValidators.parseZone("Asia/Tokyo"))
        assertNull(DateValidators.parseZone(null))
        assertNull(DateValidators.parseZone(""))
        assertNull(DateValidators.parseZone("Mars/Olympus_Mons"))
        assertNull(DateValidators.parseZone("not a zone"))
    }
}
