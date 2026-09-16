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
        assertTrue(DateValidators.isEndDateAfterOrEqualStartDate("09/16/2026", "09/16/2026"))
        assertFalse(DateValidators.isEndDateAfterOrEqualStartDate("09/16/2026", "09/15/2026"))
    }

    @Test
    fun excursionDateMustStayInsideVacationWindow() {
        assertTrue(DateValidators.isDateWithinVacation("09/17/2026", "09/16/2026", "09/18/2026"))
        assertFalse(DateValidators.isDateWithinVacation("09/19/2026", "09/16/2026", "09/18/2026"))
    }
}
