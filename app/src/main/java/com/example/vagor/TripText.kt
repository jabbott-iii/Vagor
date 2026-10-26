package com.example.vagor

import android.content.Context
import com.example.vagor.entities.Vacation

// Text shown for a vacation on the dashboard and on its own page.
fun Context.vacationDateRange(vacation: Vacation): String =
    if (vacation.startDate == vacation.endDate) {
        vacation.startDate
    } else {
        getString(R.string.vacation_date_range, vacation.startDate, vacation.endDate)
    }

fun Context.tripStatusText(status: TripStatus): String = when (status) {
    is TripStatus.Upcoming -> if (status.daysUntilStart == 1L) {
        getString(R.string.trip_starts_tomorrow)
    } else {
        val days = status.daysUntilStart.toInt()
        resources.getQuantityString(R.plurals.trip_starts_in_days, days, days)
    }
    is TripStatus.InProgress -> getString(
        when {
            status.startsToday && status.endsToday -> R.string.trip_today_only
            status.startsToday -> R.string.trip_starts_today
            status.endsToday -> R.string.trip_ends_today
            else -> R.string.trip_in_progress
        }
    )
    TripStatus.Past -> getString(R.string.trip_ended)
    TripStatus.InvalidDates -> getString(R.string.trip_check_dates)
}
