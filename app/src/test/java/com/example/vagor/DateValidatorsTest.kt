package com.example.vagor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

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
}
