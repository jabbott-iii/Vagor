package com.example.vagor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DateValidatorsTest {
    @Test
    fun validDateUsesExpectedPattern() {
        assertTrue(DateValidators.isValidDate("09/16/2026"))
        assertFalse(DateValidators.isValidDate("2026-09-16"))
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
    }
}
