package com.example.vagor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class CalendarMonthTest {
    @Test
    fun monthStartsUnderItsWeekday() {
        // October 1, 2026 is a Thursday.
        val sundayFirst = CalendarMonth.cells(YearMonth.of(2026, 10), DayOfWeek.SUNDAY)
        assertEquals(CalendarMonth.CELLS, sundayFirst.size)
        assertEquals(listOf(null, null, null, null), sundayFirst.take(4))
        assertEquals(LocalDate.of(2026, 10, 1), sundayFirst[4])
        assertEquals(LocalDate.of(2026, 10, 31), sundayFirst[34])
        assertNull(sundayFirst[35])

        val mondayFirst = CalendarMonth.cells(YearMonth.of(2026, 10), DayOfWeek.MONDAY)
        assertEquals(LocalDate.of(2026, 10, 1), mondayFirst[3])
    }

    @Test
    fun aMonthStartingOnTheFirstWeekdayHasNoLeadingBlanks() {
        // November 1, 2026 is a Sunday.
        val cells = CalendarMonth.cells(YearMonth.of(2026, 11), DayOfWeek.SUNDAY)
        assertEquals(LocalDate.of(2026, 11, 1), cells[0])
        assertEquals(30, cells.count { it != null })
    }

    @Test
    fun aLongMonthCanNeedSixWeeks() {
        // August 1, 2026 is a Saturday, so with Sunday first the 31st falls in the sixth week.
        val cells = CalendarMonth.cells(YearMonth.of(2026, 8), DayOfWeek.SUNDAY)
        assertEquals(LocalDate.of(2026, 8, 1), cells[6])
        assertEquals(LocalDate.of(2026, 8, 31), cells[36])
    }

    @Test
    fun februaryFollowsTheGregorianLeapYearRule() {
        assertEquals(29, CalendarMonth.cells(YearMonth.of(2028, 2), DayOfWeek.SUNDAY).count { it != null })
        assertEquals(28, CalendarMonth.cells(YearMonth.of(2100, 2), DayOfWeek.SUNDAY).count { it != null })
        assertEquals(28, CalendarMonth.cells(YearMonth.of(1900, 2), DayOfWeek.SUNDAY).count { it != null })
        assertEquals(29, CalendarMonth.cells(YearMonth.of(2000, 2), DayOfWeek.SUNDAY).count { it != null })
    }

    @Test
    fun weekdaysStartOnTheGivenDay() {
        assertEquals(
            listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY, DayOfWeek.MONDAY),
            CalendarMonth.weekdays(DayOfWeek.SATURDAY).take(3)
        )
        assertEquals(7, CalendarMonth.weekdays(DayOfWeek.MONDAY).toSet().size)
    }

    @Test
    fun theOfferedRangeFitsTheStoredFormat() {
        assertEquals("01/01/0001", DateValidators.formatDate(CalendarMonth.FIRST_DAY))
        assertEquals("12/31/9999", DateValidators.formatDate(CalendarMonth.LAST_DAY))
    }
}
