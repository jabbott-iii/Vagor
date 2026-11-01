package com.example.vagor

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.provider.CalendarContract
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.vagor.database.AppDatabase
import com.example.vagor.entities.Event
import com.example.vagor.entities.Vacation
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

// The Add to Calendar buttons, which open Google Calendar first. The intent each one sends is
// caught before it starts, so no calendar app opens. Uses the app's vacation_database but deletes
// only the rows it inserts, and cancels the alarms saving them set.
@RunWith(AndroidJUnit4::class)
class CalendarInsertInstrumentedTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val db: AppDatabase = AppDatabase.open(context)
    private val insertedVacations = mutableListOf<Vacation>()
    private val monitor = InsertMonitor()

    // Catches every calendar insert the app starts and stops it from opening anything.
    private class InsertMonitor : Instrumentation.ActivityMonitor() {
        @Volatile
        var started: Intent? = null

        @Volatile
        var count = 0

        override fun onStartActivity(intent: Intent): Instrumentation.ActivityResult? {
            if (intent.action != Intent.ACTION_INSERT) return null
            if (started == null) started = intent
            count++
            return Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null)
        }
    }

    // Each test may open a calendar app, which a second tap within 2 seconds wouldn't.
    @Before
    fun catchCalendarInserts() {
        calendarOpenedAt = Long.MIN_VALUE / 2
        instrumentation.addMonitor(monitor)
    }

    @After
    fun tearDown() {
        instrumentation.removeMonitor(monitor)
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
    fun aVacationGoesInAsAllDayOverItsDatesWithItsDetailsAndEvents() {
        val vacation = insertVacation(details = "Hotel Sakura")
        val dinner = insertEvent(Event("Dinner", "07/05/2031", vacation.id, time = "19:30", timeZone = "Asia/Tokyo"))
        val intent = Intent(context, EventListActivity::class.java).putExtra("vacationId", vacation.id)

        ActivityScenario.launch<EventListActivity>(intent).use {
            onView(withId(R.id.buttonVacationToCalendar)).perform(scrollTo(), click())
        }

        val sent = awaitInsert()
        assertEquals("com.google.android.calendar", sent.`package`)
        assertEquals(CalendarContract.Events.CONTENT_URI, sent.data)
        assertEquals("vnd.android.cursor.dir/event", sent.type)
        assertTrue(sent.getBooleanExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, false))
        assertNull(sent.getStringExtra(CalendarContract.Events.EVENT_TIMEZONE))
        val zone = ZoneId.systemDefault()
        assertEquals(
            CalendarTimes.allDayMillis(LocalDate.of(2031, 7, 4), zone),
            sent.getLongExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, 0)
        )
        assertEquals(
            CalendarTimes.allDayEndMillis(LocalDate.of(2031, 7, 9), zone),
            sent.getLongExtra(CalendarContract.EXTRA_EVENT_END_TIME, 0)
        )
        assertEquals(vacation.title, sent.getStringExtra(CalendarContract.Events.TITLE))
        val events = context.getString(
            R.string.calendar_vacation_events,
            context.getString(R.string.event_share_line, "Dinner", context.eventWhenText(dinner))
        )
        assertEquals("Hotel Sakura\n\n$events", sent.getStringExtra(CalendarContract.Events.DESCRIPTION))
    }

    @Test
    fun anEventGoesInAtItsTimeInItsZoneForAnHour() {
        val vacation = insertVacation()
        val event =
            insertEvent(
                Event(
                    "Dinner",
                    "07/05/2031",
                    vacation.id,
                    details = "Table for two",
                    time = "19:30",
                    timeZone = "Asia/Tokyo"
                )
            )
        val intent = Intent(context, EventDetailActivity::class.java)
            .putExtra("eventId", event.id)
            .putExtra("vacationId", vacation.id)

        ActivityScenario.launch<EventDetailActivity>(intent).use {
            onView(withId(R.id.buttonEventToCalendar)).perform(scrollTo(), click())
        }

        val sent = awaitInsert()
        assertEquals("com.google.android.calendar", sent.`package`)
        val begin = ZonedDateTime.of(2031, 7, 5, 19, 30, 0, 0, ZoneId.of("Asia/Tokyo")).toInstant().toEpochMilli()
        assertEquals(begin, sent.getLongExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, 0))
        assertEquals(begin + 60 * 60 * 1000L, sent.getLongExtra(CalendarContract.EXTRA_EVENT_END_TIME, 0))
        assertEquals("Asia/Tokyo", sent.getStringExtra(CalendarContract.Events.EVENT_TIMEZONE))
        assertEquals(false, sent.getBooleanExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, false))
        assertEquals("Dinner", sent.getStringExtra(CalendarContract.Events.TITLE))
        // Some calendar apps show the time in the phone's zone, so the description names the event's.
        val time = context.getString(
            R.string.event_share_time,
            context.eventTimeText(event),
            TimeZoneChoices.label(context, "Asia/Tokyo")
        )
        val partOf = context.getString(R.string.reminder_part_of_vacation, vacation.title)
        assertEquals(
            context.getString(R.string.calendar_event_description, "Table for two", "$time\n$partOf"),
            sent.getStringExtra(CalendarContract.Events.DESCRIPTION)
        )
    }

    // Like Share, the button takes what the form shows, so it needs a title, date and time.
    @Test
    fun anEventWithoutATimeIsNotSent() {
        val vacation = insertVacation()
        val intent = Intent(context, EventDetailActivity::class.java).putExtra("vacationId", vacation.id)

        ActivityScenario.launch<EventDetailActivity>(intent).use {
            onView(withId(R.id.editEventTitle)).perform(replaceText("Museum"))
            onView(withId(R.id.editEventDate)).perform(setDate("07/06/2031"))
            onView(withId(R.id.buttonEventToCalendar)).perform(scrollTo(), click())
            instrumentation.waitForIdleSync()
        }

        assertNull(monitor.started)
    }

    // Some calendar apps save every new-event screen the user leaves, so a quick second tap
    // mustn't open another.
    @Test
    fun aSecondTapWhileTheCalendarOpensIsIgnored() {
        val vacation = insertVacation()
        val intent = Intent(context, EventListActivity::class.java).putExtra("vacationId", vacation.id)

        ActivityScenario.launch<EventListActivity>(intent).use {
            onView(withId(R.id.buttonVacationToCalendar)).perform(scrollTo(), click())
            onView(withId(R.id.buttonVacationToCalendar)).perform(click())
        }

        awaitInsert()
        assertEquals(1, monitor.count)
    }

    // An event saved before dates were checked strictly can't be sent; Save would refuse it too.
    @Test
    fun anEventWithAnOldLooseDateIsNotSent() {
        val vacation = insertVacation()
        val event = insertEvent(Event("Old", "7/5/2031", vacation.id, time = "10:00", timeZone = "Asia/Tokyo"))
        val intent = Intent(context, EventDetailActivity::class.java)
            .putExtra("eventId", event.id)
            .putExtra("vacationId", vacation.id)

        ActivityScenario.launch<EventDetailActivity>(intent).use {
            onView(withId(R.id.buttonEventToCalendar)).perform(scrollTo(), click())
            instrumentation.waitForIdleSync()
        }

        assertNull(monitor.started)
    }

    private fun awaitInsert(): Intent {
        val deadline = SystemClock.uptimeMillis() + 5000
        while (monitor.started == null && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(50)
        val started = monitor.started
        assertNotNull("No calendar insert was started", started)
        return started!!
    }

    private fun insertVacation(details: String = ""): Vacation {
        val vacation = Vacation("Trip-${UUID.randomUUID().toString().take(8)}", details, "07/04/2031", "07/09/2031")
        return vacation.copy(id = db.vacationDAO().insert(vacation).toInt()).also { insertedVacations += it }
    }

    private fun insertEvent(event: Event): Event = event.copy(id = db.eventDAO().insert(event).toInt())
}
