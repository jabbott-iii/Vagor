package com.example.vagor

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.view.View
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import androidx.core.view.isVisible
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
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.vagor.database.AppDatabase
import com.example.vagor.entities.Event
import com.example.vagor.entities.Vacation
import org.hamcrest.Matchers.anything
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

// The event form's time, time zone and details, how the vacation page shows them, and the offer
// of exact alarms after saving. Uses the app's vacation_database but deletes only the rows it
// inserts, and cancels the alarms that saving an event sets.
@RunWith(AndroidJUnit4::class)
class EventFormInstrumentedTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db: AppDatabase = AppDatabase.open(context)
    private val insertedVacations = mutableListOf<Vacation>()
    private var exactAlarmsOffered = false

    // Saving offers exact alarms only in the tests of that offer.
    @Before
    fun skipExactAlarmOffer() {
        exactAlarmsOffered = ExactAlarmOfferDialog.offered
        ExactAlarmOfferDialog.offered = true
    }

    @After
    fun tearDown() {
        ExactAlarmOfferDialog.offered = exactAlarmsOffered
        insertedVacations.forEach { vacation ->
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
    fun savingNeedsATimeAndKeepsTheDetails() {
        val vacation = insertVacation()

        ActivityScenario.launch<EventDetailActivity>(newEventIntent(vacation)).use {
            onView(withId(R.id.editEventTitle)).perform(replaceText("Museum"))
            onView(withId(R.id.editEventDate)).perform(setDate("07/06/2031"))
            onView(withId(R.id.buttonSaveEvent)).perform(scrollTo(), click())
            assertTrue(db.eventDAO().getEventsForVacation(vacation.id).isEmpty())

            // The clock opens at 9:00 AM for a new event.
            onView(withId(R.id.editEventTime)).perform(scrollTo(), click())
            onView(
                withId(com.google.android.material.R.id.material_timepicker_ok_button)
            ).inRoot(isDialog()).perform(click())
            onView(withId(R.id.editEventTime)).check(matches(withText(context.timeOfDayText(LocalTime.of(9, 0)))))
            onView(withId(R.id.editEventDetails)).perform(replaceText("Tickets at the door\nBring ID"))
            closeSoftKeyboard()
            onView(withId(R.id.buttonSaveEvent)).perform(scrollTo(), click())
        }

        val saved = db.eventDAO().getEventsForVacation(vacation.id).single()
        assertEquals("Museum", saved.title)
        assertEquals("07/06/2031", saved.date)
        assertEquals("09:00", saved.time)
        assertEquals(ZoneId.systemDefault().id, saved.timeZone)
        assertEquals("Tickets at the door\nBring ID", saved.details)
    }

    @Test
    fun timeZoneIsPickedFromASearchableList() {
        val vacation = insertVacation()

        ActivityScenario.launch<EventDetailActivity>(newEventIntent(vacation)).use {
            onView(
                withId(R.id.editEventTimeZone)
            ).check(matches(withText(TimeZoneChoices.fieldText(context, ZoneId.systemDefault().id))))
            onView(withId(R.id.editEventTimeZone)).perform(scrollTo(), click())

            searchTimeZones("tokyo")
            onView(withId(R.id.timeZoneList)).inRoot(isDialog()).check { view, _ ->
                assertEquals(1, (view as ListView).adapter.count)
            }
            onData(
                anything()
            ).inAdapterView(withId(R.id.timeZoneList)).inRoot(isDialog()).atPosition(0).perform(click())
            onView(withId(R.id.timeZoneList)).check(doesNotExist())
            onView(
                withId(R.id.editEventTimeZone)
            ).check(matches(withText(TimeZoneChoices.fieldText(context, "Asia/Tokyo"))))

            // The list opens scrolled to Tokyo, near its end, but a search starts at its first match.
            onView(withId(R.id.editEventTimeZone)).perform(scrollTo(), click())
            onView(withId(R.id.timeZoneList)).inRoot(isDialog()).check { view, _ ->
                val list = view as ListView
                val tokyo = (0 until list.adapter.count).first {
                    (list.adapter.getItem(it) as TimeZoneChoice).id == "Asia/Tokyo"
                }
                assertTrue(list.firstVisiblePosition > 0)
                assertTrue(tokyo in list.firstVisiblePosition..list.lastVisiblePosition)
            }
            searchTimeZones("a")
            onView(withId(R.id.timeZoneList)).inRoot(isDialog()).check { view, _ ->
                assertEquals(0, (view as ListView).firstVisiblePosition)
            }

            // Accents and case don't matter, and a search with no match says so.
            searchTimeZones("SAO PAULO")
            onView(withId(R.id.timeZoneList)).inRoot(isDialog()).check { view, _ ->
                assertEquals(1, (view as ListView).adapter.count)
            }
            searchTimeZones("nowhere at all")
            onView(withId(R.id.timeZoneNoMatches)).inRoot(isDialog()).check(matches(isDisplayed()))
            onView(withText(android.R.string.cancel)).inRoot(isDialog()).perform(click())
            onView(
                withId(R.id.editEventTimeZone)
            ).check(matches(withText(TimeZoneChoices.fieldText(context, "Asia/Tokyo"))))

            onView(withId(R.id.editEventTitle)).perform(replaceText("Sushi"))
            onView(withId(R.id.editEventDate)).perform(setDate("07/05/2031"))
            pickDefaultTime()
            onView(withId(R.id.buttonSaveEvent)).perform(scrollTo(), click())
        }

        assertEquals("Asia/Tokyo", db.eventDAO().getEventsForVacation(vacation.id).single().timeZone)
    }

    @Test
    fun anEventOpensWithItsTimeZoneAndDetails() {
        val vacation = insertVacation()
        val event =
            insertEvent(
                Event(
                    "Louvre",
                    "07/07/2031",
                    vacation.id,
                    details = "Pyramid entrance",
                    time = "14:30",
                    timeZone = "Europe/Paris"
                )
            )

        ActivityScenario.launch<EventDetailActivity>(editEventIntent(event)).use {
            onView(withId(R.id.editEventTime)).check(matches(withText(context.timeOfDayText(LocalTime.of(14, 30)))))
            onView(
                withId(R.id.editEventTimeZone)
            ).check(matches(withText(TimeZoneChoices.fieldText(context, "Europe/Paris"))))
            onView(withId(R.id.editEventDetails)).check(matches(withText("Pyramid entrance")))

            // The clock opens at the event's time.
            onView(withId(R.id.editEventTime)).perform(scrollTo(), click())
            onView(
                withId(com.google.android.material.R.id.material_timepicker_ok_button)
            ).inRoot(isDialog()).perform(click())
            onView(withId(R.id.buttonSaveEvent)).perform(scrollTo(), click())
        }

        assertEquals(event, db.eventDAO().getEventById(event.id))
    }

    @Test
    fun anEventDeletedSinceItWasOpenedClosesTheForm() {
        val vacation = insertVacation()
        val event = insertEvent(Event("Gone", "07/05/2031", vacation.id, time = "10:00", timeZone = "Asia/Tokyo"))
        db.eventDAO().delete(event)

        ActivityScenario.launch<EventDetailActivity>(editEventIntent(event)).use { scenario ->
            assertEquals(Lifecycle.State.DESTROYED, scenario.state)
        }
        assertNull(db.eventDAO().getEventById(event.id))
    }

    @Test
    fun anEventSavedWithoutATimeNeedsOneBeforeSaving() {
        val vacation = insertVacation()
        val event = insertEvent(Event("Old event", "07/05/2031", vacation.id))

        ActivityScenario.launch<EventDetailActivity>(editEventIntent(event)).use {
            onView(withId(R.id.editEventTime)).check(matches(withText("")))
            onView(
                withId(R.id.editEventTimeZone)
            ).check(matches(withText(TimeZoneChoices.fieldText(context, ZoneId.systemDefault().id))))
            onView(withId(R.id.buttonSaveEvent)).perform(scrollTo(), click())
            assertEquals(event, db.eventDAO().getEventById(event.id))

            pickDefaultTime()
            onView(withId(R.id.buttonSaveEvent)).perform(scrollTo(), click())
        }

        assertEquals("09:00", db.eventDAO().getEventById(event.id)!!.time)
    }

    // The picked values live outside their fields, so they must survive a rotation, and a clock
    // left open must still set the time.
    @Test
    fun rotationKeepsThePickedTimeAndZoneAndAnOpenClock() {
        val vacation = insertVacation()

        ActivityScenario.launch<EventDetailActivity>(newEventIntent(vacation)).use { scenario ->
            onView(withId(R.id.editEventTimeZone)).perform(scrollTo(), click())
            searchTimeZones("tokyo")
            onData(
                anything()
            ).inAdapterView(withId(R.id.timeZoneList)).inRoot(isDialog()).atPosition(0).perform(click())
            onView(
                withId(R.id.editEventTimeZone)
            ).check(matches(withText(TimeZoneChoices.fieldText(context, "Asia/Tokyo"))))
            onView(withId(R.id.editEventTime)).perform(scrollTo(), click())
            onView(
                withId(com.google.android.material.R.id.material_timepicker_ok_button)
            ).inRoot(isDialog()).check(matches(isDisplayed()))

            scenario.recreate()

            onView(
                withId(com.google.android.material.R.id.material_timepicker_ok_button)
            ).inRoot(isDialog()).perform(click())
            onView(withId(R.id.editEventTime)).check(matches(withText(context.timeOfDayText(LocalTime.of(9, 0)))))

            scenario.recreate()

            onView(withId(R.id.editEventTime)).check(matches(withText(context.timeOfDayText(LocalTime.of(9, 0)))))
            onView(
                withId(R.id.editEventTimeZone)
            ).check(matches(withText(TimeZoneChoices.fieldText(context, "Asia/Tokyo"))))
            onView(withId(R.id.editEventTitle)).perform(replaceText("Ramen"))
            onView(withId(R.id.editEventDate)).perform(setDate("07/05/2031"))
            onView(withId(R.id.buttonSaveEvent)).perform(scrollTo(), click())
        }

        val saved = db.eventDAO().getEventsForVacation(vacation.id).single()
        assertEquals("09:00", saved.time)
        assertEquals("Asia/Tokyo", saved.timeZone)
    }

    @Test
    fun timeAndZoneFieldsAreReadAsDropDownListsThatCantBeTypedInto() {
        val vacation = insertVacation()

        ActivityScenario.launch<EventDetailActivity>(newEventIntent(vacation)).use {
            pickDefaultTime()
            onView(withId(R.id.editEventTime)).check { view, _ -> assertPickOnly(view, R.string.event_time) }
            onView(withId(R.id.editEventTimeZone)).check { view, _ -> assertPickOnly(view, R.string.event_time_zone) }
        }
    }

    @Test
    fun vacationPageShowsEachEventsTimeAndDetailsInTimeOrder() {
        val vacation = insertVacation()
        val dinner = insertEvent(Event("Dinner", "07/05/2031", vacation.id, time = "19:00", timeZone = "Asia/Tokyo"))
        val breakfast =
            insertEvent(
                Event(
                    "Breakfast",
                    "07/05/2031",
                    vacation.id,
                    details = "Hotel lobby",
                    time = "08:00",
                    timeZone = "Asia/Tokyo"
                )
            )
        val old = insertEvent(Event("Old event", "07/05/2031", vacation.id))
        val intent = Intent(context, EventListActivity::class.java).putExtra("vacationId", vacation.id)

        ActivityScenario.launch<EventListActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val rows = activity.findViewById<LinearLayout>(R.id.eventRows)
                fun row(index: Int): View = rows.getChildAt(index)
                fun text(index: Int, id: Int) = row(index).findViewById<TextView>(id).text.toString()

                assertEquals(
                    listOf("Old event", "Breakfast", "Dinner"),
                    (0 until rows.childCount).map {
                        text(it, R.id.eventTitle)
                    }
                )
                assertEquals(context.dateText(old.date), text(0, R.id.eventDate))
                assertEquals(
                    context.getString(
                        R.string.event_date_and_time,
                        context.dateText(breakfast.date),
                        context.eventTimeText(breakfast)
                    ),
                    text(1, R.id.eventDate)
                )
                assertTrue(text(1, R.id.eventDate).contains(context.timeOfDayText(LocalTime.of(8, 0))))
                assertEquals("Hotel lobby", text(1, R.id.eventDetails))
                assertTrue(row(1).findViewById<View>(R.id.eventDetails).isVisible)
                assertFalse(row(2).findViewById<View>(R.id.eventDetails).isVisible)
                assertNull(context.eventTimeText(old))
                assertTrue(context.eventTimeText(dinner)!!.startsWith(context.timeOfDayText(LocalTime.of(19, 0))))
                // A time that New York's spring-forward change skips shows as picked, like the form.
                val skipped = Event("Early", "03/09/2031", vacation.id, time = "02:30", timeZone = "America/New_York")
                assertTrue(context.eventTimeText(skipped)!!.startsWith(context.timeOfDayText(LocalTime.of(2, 30))))
            }
        }
    }

    // Android 14 and later install apps without exact alarms, as a test run does, and taking
    // them away would stop the app and the test with it. So these run only while they're off.
    @Test
    fun savingOffersExactAlarmsOnceWhenTheReminderCantBeExact() {
        val vacation = offerExactAlarmsOnNextSave()

        ActivityScenario.launch<EventDetailActivity>(newEventIntent(vacation)).use { scenario ->
            fillAndSave("Museum")
            onView(withText(R.string.exact_reminders_title)).inRoot(isDialog()).check(matches(isDisplayed()))
            // A second tap behind the opening offer can't save the event twice.
            scenario.onActivity { assertFalse(it.findViewById<View>(R.id.buttonSaveEvent).isEnabled) }

            // The offer outlasts a rotation, and whichever way it's closed, the form closes.
            scenario.recreate()
            onView(withText(R.string.not_now)).inRoot(isDialog()).perform(click())
            awaitState(scenario, Lifecycle.State.DESTROYED)
        }
        assertEquals(listOf("Museum"), db.eventDAO().getEventsForVacation(vacation.id).map { it.title })

        // Asked once per launch: the next save just closes the form.
        ActivityScenario.launch<EventDetailActivity>(newEventIntent(vacation)).use { scenario ->
            fillAndSave("Gallery")
            awaitState(scenario, Lifecycle.State.DESTROYED)
        }
        assertEquals(2, db.eventDAO().getEventsForVacation(vacation.id).size)
    }

    @Test
    fun exactAlarmOfferOpensAlarmsAndReminders() {
        val vacation = offerExactAlarmsOnNextSave()

        ActivityScenario.launch<EventDetailActivity>(newEventIntent(vacation)).use { scenario ->
            fillAndSave("Museum")
            onView(withText(R.string.open_settings)).inRoot(isDialog()).perform(click())
            awaitState(scenario, Lifecycle.State.DESTROYED)
        }
        // Android's Alarms & reminders screen is on top; Back leaves it.
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val deadline = SystemClock.uptimeMillis() + 5000
        var shown: CharSequence? = null
        while (SystemClock.uptimeMillis() < deadline) {
            shown = automation.rootInActiveWindow?.packageName
            if (shown != null && shown != context.packageName) break
            SystemClock.sleep(100)
        }
        assertTrue("Expected another app's screen on top, not $shown", shown != null && shown != context.packageName)
        automation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
        // Android handles the Back later. On a slow emulator the next test's screen could open first,
        // and the Back would close it instead.
        val leaveDeadline = SystemClock.uptimeMillis() + 5000
        var front: CharSequence? = shown
        while (SystemClock.uptimeMillis() < leaveDeadline) {
            front = automation.rootInActiveWindow?.packageName
            if (front != null && front != shown) break
            SystemClock.sleep(100)
        }
        assertTrue("Back didn't leave $shown, $front is in front", front != null && front != shown)
    }

    @Test
    fun backOnTheExactAlarmOfferClosesTheForm() {
        val vacation = offerExactAlarmsOnNextSave()

        ActivityScenario.launch<EventDetailActivity>(newEventIntent(vacation)).use { scenario ->
            fillAndSave("Museum")
            onView(withText(R.string.exact_reminders_title)).inRoot(isDialog()).check(matches(isDisplayed()))
            // The form is the only screen here, so Back leaves the app.
            pressBackUnconditionally()
            awaitState(scenario, Lifecycle.State.DESTROYED)
        }
        assertEquals(1, db.eventDAO().getEventsForVacation(vacation.id).size)
    }

    // A ListView ignores a tap that comes before it has laid out the rows a search left, and
    // Espresso doesn't always wait for that layout.
    private fun searchTimeZones(query: String) {
        onView(withId(R.id.timeZoneSearch)).inRoot(isDialog()).perform(replaceText(query))
        val deadline = SystemClock.uptimeMillis() + 5000
        var laidOut = false
        while (!laidOut && SystemClock.uptimeMillis() < deadline) {
            onView(withId(R.id.timeZoneList)).inRoot(isDialog()).check { view, _ ->
                val list = view as ListView
                // With no matches the list is hidden behind its "no matches" text.
                laidOut = list.visibility != View.VISIBLE || !list.isLayoutRequested
            }
            if (!laidOut) SystemClock.sleep(20)
        }
        assertTrue("The time zone list didn't lay out its rows", laidOut)
    }

    // Exact alarms off, notifications on (without them no reminder shows, so nothing is offered),
    // and nothing offered yet in this launch.
    private fun offerExactAlarmsOnNextSave(): Vacation {
        assumeFalse("Exact alarms are already allowed", ReminderScheduler.canScheduleExactAlarms(context))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
        ExactAlarmOfferDialog.offered = false
        return insertVacation()
    }

    // An event in 2031, so its reminder is still to come.
    private fun fillAndSave(title: String) {
        onView(withId(R.id.editEventTitle)).perform(replaceText(title))
        onView(withId(R.id.editEventDate)).perform(setDate("07/06/2031"))
        pickDefaultTime()
        onView(withId(R.id.buttonSaveEvent)).perform(scrollTo(), click())
    }

    // A finishing activity is destroyed after Espresso's click returns.
    private fun awaitState(scenario: ActivityScenario<*>, state: Lifecycle.State) {
        val deadline = SystemClock.uptimeMillis() + 5000
        while (scenario.state != state && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(50)
        assertEquals(state, scenario.state)
    }

    private fun pickDefaultTime() {
        onView(withId(R.id.editEventTime)).perform(scrollTo(), click())
        onView(
            withId(com.google.android.material.R.id.material_timepicker_ok_button)
        ).inRoot(isDialog()).perform(click())
    }

    private fun assertPickOnly(view: View, nameId: Int) {
        val info = view.createAccessibilityNodeInfo()
        assertEquals(Spinner::class.java.name, info.className)
        assertFalse(info.isEditable)
        assertEquals("${context.getString(nameId)}, ${(view as TextView).text}", info.contentDescription)
        assertFalse(view.text.isNullOrEmpty())
    }

    private fun insertVacation(): Vacation {
        val vacation = Vacation("Trip-${UUID.randomUUID().toString().take(8)}", "", "07/04/2031", "07/09/2031")
        return vacation.copy(id = db.vacationDAO().insert(vacation).toInt()).also { insertedVacations += it }
    }

    private fun insertEvent(event: Event): Event = event.copy(id = db.eventDAO().insert(event).toInt())

    private fun newEventIntent(vacation: Vacation) =
        Intent(context, EventDetailActivity::class.java).putExtra("vacationId", vacation.id)

    // What the vacation page and a reminder send; the form reads the event itself.
    private fun editEventIntent(event: Event) = Intent(context, EventDetailActivity::class.java)
        .putExtra("eventId", event.id)
        .putExtra("vacationId", event.vacationId)
}
