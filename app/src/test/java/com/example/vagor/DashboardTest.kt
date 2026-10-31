package com.example.vagor

import com.example.vagor.entities.Vacation
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DashboardTest {
    private val today = LocalDate.of(2026, 10, 25)

    private fun vacation(id: Int, start: String, end: String, title: String = "Trip $id") =
        Vacation(title, "Hotel", start, end, id)

    private fun order(rows: List<DashboardRow>): List<String> = rows.map {
        when (it) {
            is DashboardRow.Trip -> it.vacation.title
            DashboardRow.PastHeader -> "PAST"
        }
    }

    @Test
    fun soonestVacationIsFirstAndLatestIsLast() {
        val rows = Dashboard.rows(
            listOf(
                vacation(1, "12/20/2026", "12/27/2026", "Winter"),
                vacation(2, "10/30/2026", "11/02/2026", "Soon"),
                vacation(3, "03/01/2027", "03/05/2027", "Spring")
            ),
            today
        )

        assertEquals(listOf("Soon", "Winter", "Spring"), order(rows))
    }

    @Test
    fun currentVacationComesBeforeUpcomingOnes() {
        val rows = Dashboard.rows(
            listOf(
                vacation(1, "10/26/2026", "10/28/2026", "Tomorrow"),
                vacation(2, "10/20/2026", "10/30/2026", "Now")
            ),
            today
        )

        assertEquals(listOf("Now", "Tomorrow"), order(rows))
    }

    @Test
    fun pastVacationsGoBelowAHeadingMostRecentFirst() {
        val rows = Dashboard.rows(
            listOf(
                vacation(1, "01/01/2025", "01/05/2025", "Long ago"),
                vacation(2, "11/01/2026", "11/03/2026", "Upcoming"),
                vacation(3, "10/01/2026", "10/24/2026", "Just ended")
            ),
            today
        )

        assertEquals(listOf("Upcoming", "PAST", "Just ended", "Long ago"), order(rows))
    }

    @Test
    fun onlyPastVacationsStillGetTheHeading() {
        val rows = Dashboard.rows(listOf(vacation(1, "01/01/2025", "01/05/2025", "Old")), today)

        assertEquals(listOf("PAST", "Old"), order(rows))
    }

    @Test
    fun noVacationsMeansNoRows() {
        assertEquals(emptyList<DashboardRow>(), Dashboard.rows(emptyList(), today))
    }

    @Test
    fun vacationsWithBadDatesGoAfterCurrentOnesAndBeforePastOnes() {
        val rows = Dashboard.rows(
            listOf(
                vacation(1, "9/16/2026", "09/18/2026", "Loose date"),
                vacation(2, "01/01/2025", "01/05/2025", "Old"),
                vacation(3, "11/05/2026", "11/01/2026", "Backwards"),
                vacation(4, "11/01/2026", "11/03/2026", "Upcoming")
            ),
            today
        )

        assertEquals(listOf("Upcoming", "Backwards", "Loose date", "PAST", "Old"), order(rows))
    }

    @Test
    fun sameStartDateSortsByEndThenTitle() {
        val rows = Dashboard.rows(
            listOf(
                vacation(1, "11/01/2026", "11/09/2026", "Long"),
                vacation(2, "11/01/2026", "11/03/2026", "b short"),
                vacation(3, "11/01/2026", "11/03/2026", "A short")
            ),
            today
        )

        assertEquals(listOf("A short", "b short", "Long"), order(rows))
    }

    @Test
    fun statusDescribesWhereTheVacationStands() {
        assertEquals(TripStatus.Upcoming(1), Dashboard.status(vacation(1, "10/26/2026", "10/27/2026"), today))
        assertEquals(TripStatus.Upcoming(68), Dashboard.status(vacation(1, "01/01/2027", "01/02/2027"), today))
        assertEquals(
            TripStatus.InProgress(startsToday = true, endsToday = false),
            Dashboard.status(vacation(1, "10/25/2026", "10/27/2026"), today)
        )
        assertEquals(
            TripStatus.InProgress(startsToday = false, endsToday = true),
            Dashboard.status(vacation(1, "10/20/2026", "10/25/2026"), today)
        )
        assertEquals(
            TripStatus.InProgress(startsToday = true, endsToday = true),
            Dashboard.status(vacation(1, "10/25/2026", "10/25/2026"), today)
        )
        assertEquals(
            TripStatus.InProgress(startsToday = false, endsToday = false),
            Dashboard.status(vacation(1, "10/20/2026", "10/30/2026"), today)
        )
        assertEquals(TripStatus.Past, Dashboard.status(vacation(1, "10/20/2026", "10/24/2026"), today))
        assertEquals(TripStatus.InvalidDates, Dashboard.status(vacation(1, "10/20/26", "10/24/2026"), today))
        assertEquals(TripStatus.InvalidDates, Dashboard.status(vacation(1, "10/24/2026", "10/20/2026"), today))
    }
}
