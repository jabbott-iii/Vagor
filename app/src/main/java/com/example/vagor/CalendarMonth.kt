package com.example.vagor

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

// Month-grid math for the drop-down calendar (CalendarDropdown.kt). The calendar is always
// Gregorian, the same calendar the app stores dates in, whatever the phone's locale.
object CalendarMonth {
    // Six weeks always cover a month, and a fixed six rows keep the calendar's height steady.
    const val CELLS = 42

    // The earliest and latest dates the calendar offers: the years MM/dd/yyyy can hold.
    val FIRST_DAY: LocalDate = LocalDate.of(1, 1, 1)
    val LAST_DAY: LocalDate = LocalDate.of(9999, 12, 31)

    // The month's days laid out in weeks that start on firstDayOfWeek; cells before the 1st and
    // after the last day are null.
    fun cells(month: YearMonth, firstDayOfWeek: DayOfWeek): List<LocalDate?> {
        val leading = (month.atDay(1).dayOfWeek.value - firstDayOfWeek.value + 7) % 7
        return List(CELLS) { index ->
            val day = index - leading + 1
            if (day in 1..month.lengthOfMonth()) month.atDay(day) else null
        }
    }

    // The seven weekdays in display order.
    fun weekdays(firstDayOfWeek: DayOfWeek): List<DayOfWeek> = List(7) { firstDayOfWeek.plus(it.toLong()) }
}
