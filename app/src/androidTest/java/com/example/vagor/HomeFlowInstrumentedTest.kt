package com.example.vagor

import android.Manifest
import android.content.Context
import android.os.Build
import android.widget.ListView
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.vagor.database.AppDatabase
import com.example.vagor.entities.Event
import com.example.vagor.entities.Vacation
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.util.UUID

// Uses the app's vacation_database but deletes only the rows it creates.
@RunWith(AndroidJUnit4::class)
class HomeFlowInstrumentedTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db: AppDatabase = Room.databaseBuilder(context, AppDatabase::class.java, "vacation_database")
        .allowMainThreadQueries()
        .build()
    private val titles = mutableListOf<String>()

    // Saving a vacation leads to the notification permission prompt, which would cover the list.
    @Before
    fun grantNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            instrumentation.uiAutomation.grantRuntimePermission(
                instrumentation.targetContext.packageName,
                Manifest.permission.POST_NOTIFICATIONS
            )
        }
    }

    @After
    fun deleteCreatedRows() {
        db.vacationDAO().getAllVacations().filter { it.title in titles }.forEach { vacation ->
            db.eventDAO().getEventsForVacation(vacation.id).forEach {
                ReminderScheduler.cancelEvent(context, it.id)
                db.eventDAO().delete(it)
            }
            ReminderScheduler.cancelVacation(context, vacation.id)
            db.vacationDAO().delete(vacation)
        }
        db.close()
    }

    @Test
    fun canCreateVacationFromTheDashboard() {
        val uniqueTitle = uniqueTitle()

        ActivityScenario.launch(HomeActivity::class.java).use {
            onView(withId(R.id.buttonAddVacation)).perform(click())
            onView(withId(R.id.editTitle)).perform(replaceText(uniqueTitle))
            onView(withId(R.id.editHotel)).perform(replaceText("Hotel Vagor"))
            onView(withId(R.id.editStartDate)).perform(replaceText(date(10)))
            onView(withId(R.id.editEndDate)).perform(replaceText(date(12)))
            closeSoftKeyboard()
            onView(withId(R.id.buttonSave)).perform(click())

            onData(tripTitled(uniqueTitle)).inAdapterView(withId(R.id.vacationListView)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun dashboardPutsTheSoonestVacationFirstAndPastOnesBelow() {
        val later = insertVacation(date(40), date(45))
        val sooner = insertVacation(date(5), date(7))
        val ended = insertVacation(date(-9), date(-2))

        ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val adapter = activity.findViewById<ListView>(R.id.vacationListView).adapter
                val rows = (0 until adapter.count).map { adapter.getItem(it) as DashboardRow }
                fun indexOf(vacation: Vacation) =
                    rows.indexOfFirst { it is DashboardRow.Trip && it.vacation.id == vacation.id }
                val pastHeading = rows.indexOf(DashboardRow.PastHeader)

                assertTrue(indexOf(sooner) in 0 until indexOf(later))
                assertTrue(indexOf(later) < pastHeading)
                assertTrue(pastHeading < indexOf(ended))
            }
        }
    }

    @Test
    fun tappingAVacationShowsItsEvents() {
        val vacation = insertVacation(date(20), date(22))
        val event = Event("Event-${UUID.randomUUID().toString().take(8)}", date(21), vacation.id)
        db.eventDAO().insert(event)

        ActivityScenario.launch(HomeActivity::class.java).use {
            onData(tripTitled(vacation.title)).inAdapterView(withId(R.id.vacationListView)).perform(click())

            onView(withId(R.id.vacationPageTitle)).check(matches(withText(vacation.title)))
            onView(withText(event.title)).check(matches(isDisplayed()))
        }
    }

    private fun uniqueTitle() = "Trip-${UUID.randomUUID().toString().take(8)}".also { titles += it }

    private fun insertVacation(start: String, end: String): Vacation {
        val vacation = Vacation(uniqueTitle(), "Hotel Vagor", start, end)
        return vacation.copy(id = db.vacationDAO().insert(vacation).toInt())
    }

    private fun date(daysFromToday: Long) = DateValidators.formatDate(LocalDate.now().plusDays(daysFromToday))

    private fun tripTitled(title: String): Matcher<Any> = object : TypeSafeMatcher<Any>() {
        override fun describeTo(description: Description) {
            description.appendText("dashboard row for $title")
        }

        override fun matchesSafely(item: Any) = item is DashboardRow.Trip && item.vacation.title == title
    }
}
