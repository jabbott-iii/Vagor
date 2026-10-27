package com.example.vagor

import android.content.Context
import android.icu.text.DateFormat as IcuDateFormat
import android.icu.util.TimeZone as IcuTimeZone
import android.text.format.DateFormat
import androidx.core.os.ConfigurationCompat
import com.example.vagor.entities.Event
import com.example.vagor.entities.Vacation
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale

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

// The language the app's text is shown in.
fun Context.displayLocale(): Locale =
    ConfigurationCompat.getLocales(resources.configuration)[0] ?: Locale.getDefault()

// A time of day in the phone's 12- or 24-hour style, such as "2:30 PM" or "14:30".
fun Context.timeOfDayText(time: LocalTime): String {
    val format = IcuDateFormat.getInstanceForSkeleton(if (DateFormat.is24HourFormat(this)) "Hm" else "hm", displayLocale())
    format.timeZone = IcuTimeZone.GMT_ZONE
    return format.format(Date(time.toSecondOfDay() * 1000L))
}

// An event's time with its time zone's short name on that day, such as "2:30 PM JST", "2:30 PM
// EDT" or "2:30 PM GMT+2" (many zones have no abbreviation). Null when the event has no time.
// The time is the one picked, as the form shows it, even when a daylight saving change skips it.
fun Context.eventTimeText(event: Event): String? {
    val start = EventTimes.startOf(event, ZoneId.systemDefault()) ?: return null
    val time = DateValidators.parseLocalTime(event.time) ?: return null
    val zone = IcuDateFormat.getInstanceForSkeleton("z", displayLocale())
    zone.timeZone = icuTimeZone(start.zone)
    return getString(R.string.event_time_and_zone, timeOfDayText(time), zone.format(Date.from(start.toInstant())))
}

// An event's date, and its time when it has one, as one line: "07/06/2031, 2:30 PM JST".
fun Context.eventWhenText(event: Event): String =
    eventTimeText(event)?.let { getString(R.string.event_date_and_time, event.date, it) } ?: event.date

// ICU names zones by ID too, but writes fixed offsets as "GMT+05:00" rather than "+05:00".
private fun icuTimeZone(zone: ZoneId): IcuTimeZone = when {
    zone == ZoneOffset.UTC -> IcuTimeZone.GMT_ZONE
    zone is ZoneOffset -> IcuTimeZone.getTimeZone("GMT${zone.id}")
    else -> IcuTimeZone.getTimeZone(zone.id)
}
