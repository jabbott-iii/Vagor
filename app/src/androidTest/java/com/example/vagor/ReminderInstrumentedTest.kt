package com.example.vagor

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.service.notification.StatusBarNotification
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.vagor.database.AppDatabase
import com.example.vagor.entities.Event
import com.example.vagor.entities.Vacation
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

// Uses the app's vacation_database but deletes only the rows it inserts.
@RunWith(AndroidJUnit4::class)
class ReminderInstrumentedTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db: AppDatabase = AppDatabase.open(context)
    private val insertedVacations = mutableListOf<Vacation>()
    private val insertedEvents = mutableListOf<Event>()

    @Before
    fun grantNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    @After
    fun tearDown() {
        insertedEvents.forEach {
            ReminderScheduler.cancelEvent(context, it.id)
            db.eventDAO().delete(it)
        }
        insertedVacations.forEach {
            ReminderScheduler.cancelVacation(context, it.id)
            db.vacationDAO().delete(it)
        }
        db.close()
    }

    @Test
    fun savingSchedulesAndDeletingCancelsReminders() {
        val vacation = insertVacation(daysFromNow(10), daysFromNow(12))
        val event = insertEvent(vacation, daysFromNow(11))

        ReminderScheduler.scheduleVacation(context, vacation)
        ReminderScheduler.scheduleEvent(context, event)

        assertNotNull(existingAlarm(ReminderKey(ReminderType.VACATION_START, vacation.id)))
        assertNotNull(existingAlarm(ReminderKey(ReminderType.VACATION_END, vacation.id)))
        assertNotNull(existingAlarm(ReminderKey(ReminderType.EVENT, event.id)))

        ReminderScheduler.cancelEvent(context, event.id)
        ReminderScheduler.cancelVacation(context, vacation.id)

        assertNull(existingAlarm(ReminderKey(ReminderType.VACATION_START, vacation.id)))
        assertNull(existingAlarm(ReminderKey(ReminderType.VACATION_END, vacation.id)))
        assertNull(existingAlarm(ReminderKey(ReminderType.EVENT, event.id)))
    }

    // Whether the alarm is exact depends on Alarms & reminders, which a test can't turn off
    // without stopping the app; either way there's one alarm per event.
    @Test
    fun anEventWithATimeGetsAReminderUntilAnHourBeforeItStarts() {
        val vacation = insertVacation(daysFromNow(0), daysFromNow(12))
        val later = insertEvent(vacation, daysFromNow(11), time = "10:00", zone = "Asia/Tokyo")
        // Saved 30 minutes before it starts: the hour before has passed.
        val soon = insertEventAt(vacation, ZonedDateTime.now().plusMinutes(30))

        ReminderScheduler.scheduleEvent(context, later)
        ReminderScheduler.scheduleEvent(context, soon)

        assertNotNull(existingAlarm(ReminderKey(ReminderType.EVENT, later.id)))
        assertNull(existingAlarm(ReminderKey(ReminderType.EVENT, soon.id)))
    }

    // An inexact alarm can come after its moment (the end of its window, Doze, Battery Saver), so
    // a rebuild mustn't cancel one whose event is still ahead. Saving the event with a new time
    // does cancel it.
    @Test
    fun aRebuildKeepsAnAlarmThatMayStillBeOnItsWay() {
        val vacation = insertVacation(daysFromNow(-1), daysFromNow(1))
        val soon = insertEventAt(vacation, ZonedDateTime.now().plusMinutes(30))
        val started = insertEventAt(vacation, ZonedDateTime.now().minusMinutes(30))
        // Stand-ins for alarms that haven't fired yet.
        listOf(soon, started).forEach { pendingAlarm(ReminderKey(ReminderType.EVENT, it.id)) }

        ReminderScheduler.rescheduleAll(context, db)

        assertNotNull(existingAlarm(ReminderKey(ReminderType.EVENT, soon.id)))
        assertNull(existingAlarm(ReminderKey(ReminderType.EVENT, started.id)))

        ReminderScheduler.eventSaved(context, soon, timingChanged = false)
        assertNotNull(existingAlarm(ReminderKey(ReminderType.EVENT, soon.id)))
        ReminderScheduler.eventSaved(context, soon, timingChanged = true)
        assertNull(existingAlarm(ReminderKey(ReminderType.EVENT, soon.id)))
    }

    // A posted reminder gives the event's start time, which a new time makes wrong.
    @Test
    fun savingAnEventWithANewTimeDismissesItsPostedReminder() {
        val vacation = insertVacation(daysFromNow(0), daysFromNow(2))
        val event = insertEvent(vacation, daysFromNow(1), time = "19:30", zone = "Asia/Tokyo")
        val tag = ReminderKey(ReminderType.EVENT, event.id).toUriString()
        MyReceiver().onReceive(context, alarmIntentFor(ReminderKey(ReminderType.EVENT, event.id)))
        assertTrue(awaitNotifications { active -> active.any { it.tag == tag } }.any { it.tag == tag })

        ReminderScheduler.eventSaved(context, event.copy(details = "New details"), timingChanged = false)
        SystemClock.sleep(500)
        assertTrue(activeNotifications().any { it.tag == tag })

        ReminderScheduler.eventSaved(context, event.copy(time = "20:00"), timingChanged = true)
        assertTrue(awaitNotifications { active -> active.none { it.tag == tag } }.none { it.tag == tag })
    }

    @Test
    fun movingADateIntoThePastCancelsItsReminder() {
        val vacation = insertVacation(daysFromNow(10), daysFromNow(12))
        ReminderScheduler.scheduleVacation(context, vacation)
        assertNotNull(existingAlarm(ReminderKey(ReminderType.VACATION_START, vacation.id)))

        val moved = vacation.copy(startDate = daysFromNow(-3))
        db.vacationDAO().update(moved)
        ReminderScheduler.scheduleVacation(context, moved)

        assertNull(existingAlarm(ReminderKey(ReminderType.VACATION_START, vacation.id)))
        assertNotNull(existingAlarm(ReminderKey(ReminderType.VACATION_END, vacation.id)))
    }

    @Test
    fun rescheduleAllRestoresRemindersFromTheDatabase() {
        val vacation = insertVacation(daysFromNow(10), daysFromNow(12))
        val event = insertEvent(vacation, daysFromNow(11))

        ReminderScheduler.rescheduleAll(context, db)

        assertNotNull(existingAlarm(ReminderKey(ReminderType.VACATION_START, vacation.id)))
        assertNotNull(existingAlarm(ReminderKey(ReminderType.EVENT, event.id)))
    }

    @Test
    fun firedReminderPostsANotification() {
        val vacation = insertVacation(daysFromNow(0), daysFromNow(2))
        val event = insertEvent(vacation, daysFromNow(1))

        val vacationKey = ReminderKey(ReminderType.VACATION_START, vacation.id)
        val eventKey = ReminderKey(ReminderType.EVENT, event.id)
        val tags = listOf(vacationKey.toUriString(), eventKey.toUriString())
        MyReceiver().onReceive(context, alarmIntentFor(vacationKey))
        MyReceiver().onReceive(context, alarmIntentFor(eventKey))

        // The notification service posts and cancels asynchronously.
        val posted = awaitNotifications { active -> active.count { it.tag in tags } == 2 }.associateBy { it.tag }
        val vacationNotification = posted[vacationKey.toUriString()]
        val eventNotification = posted[eventKey.toUriString()]
        assertNotNull(vacationNotification)
        assertNotNull(eventNotification)
        assertEquals(ReminderScheduler.CHANNEL_ID, vacationNotification!!.notification.channelId)
        assertEquals(
            context.getString(R.string.vacation_starting_today, vacation.title),
            vacationNotification.notification.extras.getString("android.title")
        )
        assertEquals(
            context.vacationDateRange(vacation),
            vacationNotification.notification.extras.getCharSequence("android.text")?.toString()
        )
        assertEquals(
            context.getString(R.string.event_happening_today, event.title),
            eventNotification!!.notification.extras.getString("android.title")
        )
        assertNotNull(vacationNotification.notification.contentIntent)
        assertNotNull(eventNotification.notification.contentIntent)

        ReminderScheduler.cancelEvent(context, event.id)
        ReminderScheduler.cancelVacation(context, vacation.id)
        val remaining = awaitNotifications { active -> active.none { it.tag in tags } }
        assertFalse(remaining.any { it.tag in tags })
    }

    @Test
    fun anEventReminderGivesItsStartTimeAndDetails() {
        val vacation = insertVacation(daysFromNow(0), daysFromNow(2))
        val event = insertEvent(vacation, daysFromNow(1), time = "19:30", zone = "Asia/Tokyo", details = "Table for two")
        val key = ReminderKey(ReminderType.EVENT, event.id)

        MyReceiver().onReceive(context, alarmIntentFor(key))

        val posted = awaitNotifications { active -> active.any { it.tag == key.toUriString() } }
            .single { it.tag == key.toUriString() }.notification.extras
        val partOf = context.getString(R.string.reminder_part_of_vacation, vacation.title)
        assertEquals(
            context.getString(R.string.event_starts_at, event.title, context.eventTimeText(event)),
            posted.getCharSequence("android.title")?.toString()
        )
        assertEquals(partOf, posted.getCharSequence("android.text")?.toString())
        assertEquals(
            context.getString(R.string.reminder_vacation_and_details, partOf, "Table for two"),
            posted.getCharSequence("android.bigText")?.toString()
        )

        ReminderScheduler.cancelEvent(context, event.id)
        awaitNotifications { active -> active.none { it.tag == key.toUriString() } }
    }

    @Test
    fun tappingAReminderOpensTheVacationPageOrTheEvent() {
        val vacation = Vacation("Trip", "Hotel Vagor", daysFromNow(10), daysFromNow(12), 41)
        val event = Event("Event", daysFromNow(11), vacation.id, 42)
        val vacationKey = ReminderKey(ReminderType.VACATION_START, vacation.id)
        val eventKey = ReminderKey(ReminderType.EVENT, event.id)

        val vacationStack = MyReceiver.vacationReminderStack(context, vacation, vacationKey).intents
        assertEquals(
            listOf(HomeActivity::class.java.name, EventListActivity::class.java.name),
            vacationStack.map { it.component!!.className }
        )
        assertEquals(vacation.id, vacationStack[1].getIntExtra("vacationId", -1))
        assertEquals(vacationKey.toUriString(), vacationStack[1].dataString)

        val eventStack = MyReceiver.eventReminderStack(context, vacation, event, eventKey).intents
        assertEquals(
            listOf(HomeActivity::class.java.name, EventListActivity::class.java.name, EventDetailActivity::class.java.name),
            eventStack.map { it.component!!.className }
        )
        assertEquals(vacation.id, eventStack[1].getIntExtra("vacationId", -1))
        // The event form reads the event from the database, so a reminder tapped after an edit
        // can't bring back what the event held when the reminder was posted.
        assertEquals(event.id, eventStack[2].getIntExtra("eventId", -1))
        assertEquals(vacation.id, eventStack[2].getIntExtra("vacationId", -1))
        assertNull(eventStack[2].getStringExtra("title"))
        assertEquals(eventKey.toUriString(), eventStack[2].dataString)
    }

    @Test
    fun reminderForDeletedItemPostsNothing() {
        val missingKey = ReminderKey(ReminderType.EVENT, Int.MAX_VALUE)

        MyReceiver().onReceive(context, alarmIntentFor(missingKey))
        MyReceiver().onReceive(context, Intent(context, MyReceiver::class.java).putExtra("message", "old alert"))

        SystemClock.sleep(1000)
        assertTrue(activeNotifications().isEmpty())
    }

    private fun activeNotifications(): List<StatusBarNotification> =
        context.getSystemService(NotificationManager::class.java).activeNotifications.toList()

    private fun awaitNotifications(condition: (List<StatusBarNotification>) -> Boolean): List<StatusBarNotification> {
        val deadline = SystemClock.uptimeMillis() + 5000
        var active = activeNotifications()
        while (!condition(active) && SystemClock.uptimeMillis() < deadline) {
            SystemClock.sleep(100)
            active = activeNotifications()
        }
        return active
    }

    private fun insertVacation(start: String, end: String): Vacation {
        val vacation = Vacation("Trip-${UUID.randomUUID().toString().take(8)}", "Hotel Vagor", start, end)
        val saved = vacation.copy(id = db.vacationDAO().insert(vacation).toInt())
        insertedVacations += saved
        return saved
    }

    // Without a time, like an event saved before events had one.
    private fun insertEvent(
        vacation: Vacation,
        date: String,
        time: String? = null,
        zone: String? = null,
        details: String = "",
    ): Event {
        val event = Event("Event-${UUID.randomUUID().toString().take(8)}", date, vacation.id, details = details, time = time, timeZone = zone)
        val saved = event.copy(id = db.eventDAO().insert(event).toInt())
        insertedEvents += saved
        return saved
    }

    private fun insertEventAt(vacation: Vacation, start: ZonedDateTime): Event = insertEvent(
        vacation,
        start.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")),
        time = start.format(DateTimeFormatter.ofPattern("HH:mm")),
        zone = start.zone.id
    )

    private fun pendingAlarm(key: ReminderKey) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            alarmIntentFor(key),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        context.getSystemService(AlarmManager::class.java)
            .setWindow(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 24 * 60 * 60 * 1000L, 10 * 60 * 1000L, pendingIntent)
    }

    private fun daysFromNow(days: Long): String =
        LocalDate.now().plusDays(days).format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))

    private fun alarmIntentFor(key: ReminderKey) =
        Intent(context, MyReceiver::class.java).setData(Uri.parse(key.toUriString()))

    private fun existingAlarm(key: ReminderKey): PendingIntent? = PendingIntent.getBroadcast(
        context,
        0,
        alarmIntentFor(key),
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    )
}
