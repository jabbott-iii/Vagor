package com.example.vagor

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.CalendarView
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.vagor.database.AppDatabase
import com.example.vagor.entities.Vacation
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

// Uses the app's vacation_database but deletes only the rows it inserts.
@RunWith(AndroidJUnit4::class)
class CalendarDropdownInstrumentedTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db: AppDatabase = Room.databaseBuilder(context, AppDatabase::class.java, "vacation_database")
        .allowMainThreadQueries()
        .build()
    private val insertedVacations = mutableListOf<Vacation>()

    @After
    fun tearDown() {
        insertedVacations.forEach {
            ReminderScheduler.cancelVacation(context, it.id)
            db.vacationDAO().delete(it)
        }
        db.close()
    }

    @Test
    fun tappingADayFillsTheField() {
        ActivityScenario.launch(VacationDetailActivity::class.java).use {
            onView(withId(R.id.editStartDate)).perform(replaceText("03/10/2030"))
            onView(withId(R.id.editStartDate)).perform(click())
            onView(isAssignableFrom(CalendarView::class.java)).inRoot(isPlatformPopup()).check(matches(isDisplayed()))

            clickDay("20")

            onView(withId(R.id.editStartDate)).check(matches(withText("03/20/2030")))
            onView(isAssignableFrom(CalendarView::class.java)).check(doesNotExist())
        }
    }

    @Test
    fun backClosesTheCalendarWithoutChangingTheDate() {
        ActivityScenario.launch(VacationDetailActivity::class.java).use {
            onView(withId(R.id.editStartDate)).perform(replaceText("03/10/2030"))
            onView(withId(R.id.editStartDate)).perform(click())
            onView(isAssignableFrom(CalendarView::class.java)).inRoot(isPlatformPopup()).check(matches(isDisplayed()))

            pressBack()

            onView(isAssignableFrom(CalendarView::class.java)).check(doesNotExist())
            onView(withId(R.id.editStartDate)).check(matches(withText("03/10/2030")))
        }
    }

    @Test
    fun endDateCalendarStartsAtTheStartDate() {
        ActivityScenario.launch(VacationDetailActivity::class.java).use {
            onView(withId(R.id.editStartDate)).perform(replaceText("03/10/2030"))
            onView(withId(R.id.editEndDate)).perform(click())

            onView(isAssignableFrom(CalendarView::class.java)).inRoot(isPlatformPopup()).check { view, _ ->
                val calendar = view as CalendarView
                assertEquals(LocalDate.of(2030, 3, 10), localDate(calendar.minDate))
                assertEquals(LocalDate.of(2030, 3, 10), localDate(calendar.date))
            }
        }
    }

    @Test
    fun emptyEndDateOpensAtAPastStartDate() {
        ActivityScenario.launch(VacationDetailActivity::class.java).use {
            onView(withId(R.id.editStartDate)).perform(replaceText("01/15/2020"))
            onView(withId(R.id.editEndDate)).perform(click())

            onView(isAssignableFrom(CalendarView::class.java)).inRoot(isPlatformPopup()).check { view, _ ->
                assertEquals(LocalDate.of(2020, 1, 15), localDate((view as CalendarView).date))
            }
            clickDay("20")
            onView(withId(R.id.editEndDate)).check(matches(withText("01/20/2020")))
        }
    }

    @Test
    fun eventCalendarOffersOnlyTheVacationsDays() {
        val vacation = Vacation("Trip-${UUID.randomUUID().toString().take(8)}", "Hotel Vagor", "07/04/2031", "07/09/2031")
        val saved = vacation.copy(id = db.vacationDAO().insert(vacation).toInt())
        insertedVacations += saved
        val intent = Intent(context, EventDetailActivity::class.java).putExtra("vacationId", saved.id)

        ActivityScenario.launch<EventDetailActivity>(intent).use {
            onView(withId(R.id.editEventDate)).perform(click())

            onView(isAssignableFrom(CalendarView::class.java)).inRoot(isPlatformPopup()).check { view, _ ->
                val calendar = view as CalendarView
                assertEquals(LocalDate.of(2031, 7, 4), localDate(calendar.minDate))
                assertEquals(LocalDate.of(2031, 7, 9), localDate(calendar.maxDate))
                assertEquals(LocalDate.of(2031, 7, 4), localDate(calendar.date))
            }
            clickDay("6")
            onView(withId(R.id.editEventDate)).check(matches(withText("07/06/2031")))
        }
    }

    private fun localDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

    // Days are drawn by the calendar, not separate views, so they are clicked the way TalkBack
    // does: through their accessibility nodes.
    private fun clickDay(day: String) {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val deadline = SystemClock.uptimeMillis() + 5000
        var node: AccessibilityNodeInfo? = null
        while (node == null && SystemClock.uptimeMillis() < deadline) {
            node = automation.rootInActiveWindow?.let { findDay(it, day) }
            if (node == null) SystemClock.sleep(100)
        }
        assertTrue("No visible, enabled day $day in the calendar", node != null)
        assertTrue(node!!.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    // A tree walk, because the framework's day nodes aren't found by findAccessibilityNodeInfosByText.
    private fun findDay(node: AccessibilityNodeInfo, day: String): AccessibilityNodeInfo? {
        if (node.text?.toString() == day && node.isVisibleToUser && node.isEnabled && node.isClickable) return node
        for (i in 0 until node.childCount) {
            val found = node.getChild(i)?.let { findDay(it, day) }
            if (found != null) return found
        }
        return null
    }
}
