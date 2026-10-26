package com.example.vagor

import com.example.vagor.entities.Vacation
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// Where a vacation stands on a given day.
sealed interface TripStatus {
    data class Upcoming(val daysUntilStart: Long) : TripStatus
    data class InProgress(val startsToday: Boolean, val endsToday: Boolean) : TripStatus
    data object Past : TripStatus

    // A date that isn't a strict MM/dd/yyyy date, or an end before the start.
    data object InvalidDates : TripStatus
}

sealed interface DashboardRow {
    data class Trip(val vacation: Vacation, val status: TripStatus) : DashboardRow
    data object PastHeader : DashboardRow
}

object Dashboard {
    fun status(vacation: Vacation, today: LocalDate): TripStatus {
        val start = DateValidators.parseLocalDate(vacation.startDate)
        val end = DateValidators.parseLocalDate(vacation.endDate)
        return when {
            start == null || end == null || end.isBefore(start) -> TripStatus.InvalidDates
            end.isBefore(today) -> TripStatus.Past
            start.isAfter(today) -> TripStatus.Upcoming(ChronoUnit.DAYS.between(today, start))
            else -> TripStatus.InProgress(startsToday = start == today, endsToday = end == today)
        }
    }

    // Current and upcoming vacations first, soonest at the top. Vacations with dates that need
    // fixing follow them, then a Past heading and the vacations that have ended, most recent first.
    fun rows(vacations: List<Vacation>, today: LocalDate): List<DashboardRow> {
        val trips = vacations.map { DashboardRow.Trip(it, status(it, today)) }
        val (past, notPast) = trips.partition { it.status == TripStatus.Past }
        val (invalid, current) = notPast.partition { it.status == TripStatus.InvalidDates }

        val rows = mutableListOf<DashboardRow>()
        rows += current.sortedWith(
            compareBy<DashboardRow.Trip> { startOf(it) }
                .thenBy { endOf(it) }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.vacation.title }
                .thenBy { it.vacation.id }
        )
        rows += invalid.sortedWith(
            compareBy<DashboardRow.Trip, String>(String.CASE_INSENSITIVE_ORDER) { it.vacation.title }
                .thenBy { it.vacation.id }
        )
        if (past.isNotEmpty()) {
            rows += DashboardRow.PastHeader
            rows += past.sortedWith(
                compareByDescending<DashboardRow.Trip> { endOf(it) }
                    .thenByDescending { startOf(it) }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.vacation.title }
                    .thenBy { it.vacation.id }
            )
        }
        return rows
    }

    private fun startOf(trip: DashboardRow.Trip): LocalDate? = DateValidators.parseLocalDate(trip.vacation.startDate)

    private fun endOf(trip: DashboardRow.Trip): LocalDate? = DateValidators.parseLocalDate(trip.vacation.endDate)
}
