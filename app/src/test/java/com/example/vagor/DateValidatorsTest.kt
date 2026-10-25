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
    fun endDateMustNotPrecedeStartDate() {
        val startDate = "09/16/2026"
        val sameDayEndDate = "09/16/2026"
        val earlierEndDate = "09/15/2026"

        assertTrue(DateValidators.isEndDateAfterOrEqualStartDate(startDate, sameDayEndDate))
        assertFalse(DateValidators.isEndDateAfterOrEqualStartDate(startDate, earlierEndDate))
    }

    @Test
    fun excursionDateMustStayInsideVacationWindow() {
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
