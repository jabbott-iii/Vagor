package com.example.vagor

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.view.View
import android.widget.ListView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBackUnconditionally
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.util.UUID

// Uses the app's vacation_database but deletes only the rows it creates.
@RunWith(AndroidJUnit4::class)
class HomeFlowInstrumentedTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db: AppDatabase = AppDatabase.open(context)
    private val titles = mutableListOf<String>()
    private var hibernationOffered = false

    // Saving a vacation leads to the notification permission prompt, which would cover the list.
    // Saving offers to keep Android from pausing Vagor only in the tests of that offer.
    @Before
    fun grantNotificationsAndSkipHibernationOffer() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            instrumentation.uiAutomation.grantRuntimePermission(
                instrumentation.targetContext.packageName,
                Manifest.permission.POST_NOTIFICATIONS
            )
        }
        hibernationOffered = HibernationOfferDialog.offered
        HibernationOfferDialog.offered = true
    }

    @After
    fun deleteCreatedRows() {
        HibernationOfferDialog.offered = hibernationOffered
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

    // Trip details are optional, so they're left empty here.
    @Test
    fun canCreateVacationFromTheDashboard() {
        val uniqueTitle = uniqueTitle()

        ActivityScenario.launch(HomeActivity::class.java).use {
            onView(withId(R.id.buttonAddVacation)).perform(click())
            onView(withId(R.id.editTitle)).perform(replaceText(uniqueTitle))
            onView(withId(R.id.editStartDate)).perform(setDate(date(10)))
            onView(withId(R.id.editEndDate)).perform(setDate(date(12)))
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

    @Test
    fun deletingFromEditReturnsToTheDashboard() {
        val vacation = insertVacation(date(30), date(32))

        ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
            onData(tripTitled(vacation.title)).inAdapterView(withId(R.id.vacationListView)).perform(click())
            onView(withId(R.id.buttonEditVacation)).perform(click())
            onView(withId(R.id.buttonDelete)).perform(scrollTo(), click())

            // The vacation page closed itself, so the dashboard is showing, without the vacation.
            onView(withId(R.id.buttonAddVacation)).check(matches(isDisplayed()))
            onView(withId(R.id.vacationPageTitle)).check(doesNotExist())
            assertNull(db.vacationDAO().getVacationById(vacation.id))
            scenario.onActivity { activity ->
                val adapter = activity.findViewById<ListView>(R.id.vacationListView).adapter
                val rows = (0 until adapter.count).map { adapter.getItem(it) }
                assertTrue(rows.none { it is DashboardRow.Trip && it.vacation.id == vacation.id })
            }
        }
    }

    @Test
    fun editingAVacationUpdatesItsPage() {
        val vacation = insertVacation(date(30), date(32))

        ActivityScenario.launch(HomeActivity::class.java).use {
            onData(tripTitled(vacation.title)).inAdapterView(withId(R.id.vacationListView)).perform(click())
            onView(withId(R.id.buttonEditVacation)).perform(click())
            onView(withId(R.id.editTripDetails)).perform(replaceText("Hotel Two\nFlight AB123"))
            closeSoftKeyboard()
            onView(withId(R.id.buttonSave)).perform(scrollTo(), click())

            onView(withId(R.id.vacationPageTripDetails)).check(matches(withText("Hotel Two\nFlight AB123")))

            onView(withId(R.id.buttonEditVacation)).perform(click())
            onView(withId(R.id.editTripDetails)).perform(replaceText(""))
            closeSoftKeyboard()
            onView(withId(R.id.buttonSave)).perform(scrollTo(), click())

            onView(withId(R.id.vacationPageTripDetails)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        }
    }

    // Moving or shortening a trip mustn't leave its events outside it.
    @Test
    fun aVacationCantBeMovedAwayFromItsEvents() {
        val vacation = insertVacation(date(30), date(32))
        db.eventDAO().insert(Event("Event-${UUID.randomUUID().toString().take(8)}", date(31), vacation.id))
        val intent = Intent(context, VacationDetailActivity::class.java)
            .putExtra("vacationId", vacation.id)
            .putExtra("title", vacation.title)
            .putExtra("details", vacation.details)
            .putExtra("startDate", vacation.startDate)
            .putExtra("endDate", vacation.endDate)

        ActivityScenario.launch<VacationDetailActivity>(intent).use { scenario ->
            onView(withId(R.id.editStartDate)).perform(setDate(date(40)))
            onView(withId(R.id.editEndDate)).perform(setDate(date(42)))
            onView(withId(R.id.buttonSave)).perform(scrollTo(), click())

            assertEquals(vacation, db.vacationDAO().getVacationById(vacation.id))
            assertEquals(Lifecycle.State.RESUMED, scenario.state)

            // Shortened to the event's day, it still holds the event.
            onView(withId(R.id.editStartDate)).perform(setDate(date(31)))
            onView(withId(R.id.editEndDate)).perform(setDate(date(31)))
            onView(withId(R.id.buttonSave)).perform(scrollTo(), click())

            assertEquals(date(31), db.vacationDAO().getVacationById(vacation.id)?.endDate)
        }
    }

    // From Android 12, while Android may pause Vagor when it's unused, as it may after a test
    // install. A trip ending within two months isn't offered; one ending later is, once per launch.
    @Test
    fun savingAFarOffVacationOffersToKeepItsRemindersOnce() {
        offerHibernationOnNextSave()

        ActivityScenario.launch(VacationDetailActivity::class.java).use { scenario ->
            fillAndSave(date(50), date(55))
            awaitState(scenario, Lifecycle.State.DESTROYED)
        }

        val title = uniqueTitle()
        ActivityScenario.launch(VacationDetailActivity::class.java).use { scenario ->
            fillAndSave(date(70), date(75), title)
            onView(withText(R.string.hibernation_offer_title)).inRoot(isDialog()).check(matches(isDisplayed()))
            // A second tap behind the opening offer can't save the vacation twice.
            scenario.onActivity { assertFalse(it.findViewById<View>(R.id.buttonSave).isEnabled) }

            // The offer outlasts a rotation, and whichever way it's closed, the form closes.
            scenario.recreate()
            onView(withText(R.string.not_now)).inRoot(isDialog()).perform(click())
            awaitState(scenario, Lifecycle.State.DESTROYED)
        }
        assertEquals(1, db.vacationDAO().getAllVacations().count { it.title == title })

        // Asked once per launch: the next save just closes the form.
        ActivityScenario.launch(VacationDetailActivity::class.java).use { scenario ->
            fillAndSave(date(80), date(85))
            awaitState(scenario, Lifecycle.State.DESTROYED)
        }
    }

    @Test
    fun hibernationOfferOpensAppInfo() {
        offerHibernationOnNextSave()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val appInfo = watchForOwnAppInfo()
        try {
            ActivityScenario.launch(VacationDetailActivity::class.java).use { scenario ->
                fillAndSave(date(70), date(75))
                onView(withText(R.string.open_settings)).inRoot(isDialog()).perform(click())
                awaitState(scenario, Lifecycle.State.DESTROYED)
            }
            assertTrue("Vagor's App info didn't open", instrumentation.checkMonitorHit(appInfo, 1))
        } finally {
            instrumentation.removeMonitor(appInfo)
        }
        // Vagor's App info is on top; Back leaves it.
        leaveOtherAppsScreen()
    }

    @Test
    fun backOnTheHibernationOfferClosesTheForm() {
        offerHibernationOnNextSave()
        val title = uniqueTitle()

        ActivityScenario.launch(VacationDetailActivity::class.java).use { scenario ->
            fillAndSave(date(70), date(75), title)
            onView(withText(R.string.hibernation_offer_title)).inRoot(isDialog()).check(matches(isDisplayed()))
            pressBackUnconditionally()
            awaitState(scenario, Lifecycle.State.DESTROYED)
        }
        assertEquals(1, db.vacationDAO().getAllVacations().count { it.title == title })
    }

    @Test
    fun newVacationsAndEventsHaveNoDeleteButton() {
        ActivityScenario.launch(VacationDetailActivity::class.java).use {
            onView(withId(R.id.buttonDelete)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        }
        val vacation = insertVacation(date(30), date(32))
        val intent = Intent(context, EventDetailActivity::class.java).putExtra("vacationId", vacation.id)
        ActivityScenario.launch<EventDetailActivity>(intent).use {
            onView(withId(R.id.buttonDeleteEvent)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        }
    }

    // TalkBack announces each screen by its title; a form's heading says the same and is a heading.
    @Test
    fun eachScreenHasItsOwnTitle() {
        ActivityScenario.launch(VacationDetailActivity::class.java).use {
            assertTitle(it, R.string.add_vacation, R.id.vacationFormHeading)
        }
        val vacation = insertVacation(date(30), date(32))
        val event = Event("Event-${UUID.randomUUID().toString().take(8)}", date(31), vacation.id)
        val eventId = db.eventDAO().insert(event).toInt()
        val editVacation = Intent(context, VacationDetailActivity::class.java)
            .putExtra("vacationId", vacation.id)
            .putExtra("title", vacation.title)
            .putExtra("details", vacation.details)
            .putExtra("startDate", vacation.startDate)
            .putExtra("endDate", vacation.endDate)
        ActivityScenario.launch<VacationDetailActivity>(editVacation).use {
            assertTitle(it, R.string.edit_vacation, R.id.vacationFormHeading)
        }
        val page = Intent(context, EventListActivity::class.java).putExtra("vacationId", vacation.id)
        ActivityScenario.launch<EventListActivity>(page).use { scenario ->
            scenario.onActivity { assertEquals(vacation.title, it.title.toString()) }
        }
        val newEvent = Intent(context, EventDetailActivity::class.java).putExtra("vacationId", vacation.id)
        ActivityScenario.launch<EventDetailActivity>(newEvent).use {
            assertTitle(it, R.string.add_event, R.id.eventFormHeading)
        }
        ActivityScenario.launch<EventDetailActivity>(Intent(newEvent).putExtra("eventId", eventId)).use {
            assertTitle(it, R.string.edit_event, R.id.eventFormHeading)
        }
    }

    private fun <A : Activity> assertTitle(scenario: ActivityScenario<A>, title: Int, headingId: Int) {
        scenario.onActivity { activity ->
            val heading = activity.findViewById<TextView>(headingId)
            assertEquals(context.getString(title), activity.title.toString())
            assertEquals(context.getString(title), heading.text.toString())
            assertTrue(ViewCompat.isAccessibilityHeading(heading))
        }
    }

    // Android 11 and earlier don't pause unused apps, and the user may have turned it off for Vagor.
    private fun offerHibernationOnNextSave() {
        assumeTrue("Android doesn't pause unused apps", context.mayHibernate())
        HibernationOfferDialog.offered = false
    }

    private fun fillAndSave(start: String, end: String, title: String = uniqueTitle()) {
        onView(withId(R.id.editTitle)).perform(replaceText(title))
        onView(withId(R.id.editStartDate)).perform(setDate(start))
        onView(withId(R.id.editEndDate)).perform(setDate(end))
        closeSoftKeyboard()
        onView(withId(R.id.buttonSave)).perform(scrollTo(), click())
    }

    // A finishing activity is destroyed after Espresso's click returns.
    private fun awaitState(scenario: ActivityScenario<*>, state: Lifecycle.State) {
        val deadline = SystemClock.uptimeMillis() + 5000
        while (scenario.state != state && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(50)
        assertEquals(state, scenario.state)
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
