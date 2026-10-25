package com.example.vagor

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.service.notification.StatusBarNotification
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.vagor.database.AppDatabase
import com.example.vagor.entities.Excursion
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
import java.time.format.DateTimeFormatter
import java.util.UUID

// Uses the app's vacation_database but deletes only the rows it inserts.
@RunWith(AndroidJUnit4::class)
class ReminderInstrumentedTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db: AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "vacation_database"
    ).allowMainThreadQueries().build()
    private val insertedVacations = mutableListOf<Vacation>()
    private val insertedExcursions = mutableListOf<Excursion>()

    @Before
    fun grantNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    @After
    fun tearDown() {
        insertedExcursions.forEach {
            ReminderScheduler.cancelExcursion(context, it.id)
            db.excursionDAO().delete(it)
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
        val excursion = insertExcursion(vacation, daysFromNow(11))

        ReminderScheduler.scheduleVacation(context, vacation)
        ReminderScheduler.scheduleExcursion(context, excursion)

        assertNotNull(existingAlarm(ReminderKey(ReminderType.VACATION_START, vacation.id)))
        assertNotNull(existingAlarm(ReminderKey(ReminderType.VACATION_END, vacation.id)))
        assertNotNull(existingAlarm(ReminderKey(ReminderType.EXCURSION, excursion.id)))

        ReminderScheduler.cancelExcursion(context, excursion.id)
        ReminderScheduler.cancelVacation(context, vacation.id)

        assertNull(existingAlarm(ReminderKey(ReminderType.VACATION_START, vacation.id)))
        assertNull(existingAlarm(ReminderKey(ReminderType.VACATION_END, vacation.id)))
        assertNull(existingAlarm(ReminderKey(ReminderType.EXCURSION, excursion.id)))
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
        val excursion = insertExcursion(vacation, daysFromNow(11))

        ReminderScheduler.rescheduleAll(context, db)

        assertNotNull(existingAlarm(ReminderKey(ReminderType.VACATION_START, vacation.id)))
        assertNotNull(existingAlarm(ReminderKey(ReminderType.EXCURSION, excursion.id)))
    }

    @Test
    fun firedReminderPostsANotification() {
        val vacation = insertVacation(daysFromNow(0), daysFromNow(2))
        val excursion = insertExcursion(vacation, daysFromNow(1))

        val vacationKey = ReminderKey(ReminderType.VACATION_START, vacation.id)
        val excursionKey = ReminderKey(ReminderType.EXCURSION, excursion.id)
        val tags = listOf(vacationKey.toUriString(), excursionKey.toUriString())
        MyReceiver().onReceive(context, alarmIntentFor(vacationKey))
        MyReceiver().onReceive(context, alarmIntentFor(excursionKey))

        // The notification service posts and cancels asynchronously.
        val posted = awaitNotifications { active -> active.count { it.tag in tags } == 2 }.associateBy { it.tag }
        val vacationNotification = posted[vacationKey.toUriString()]
        val excursionNotification = posted[excursionKey.toUriString()]
        assertNotNull(vacationNotification)
        assertNotNull(excursionNotification)
        assertEquals(ReminderScheduler.CHANNEL_ID, vacationNotification!!.notification.channelId)
        assertEquals(
            context.getString(R.string.vacation_starting_today, vacation.title),
            vacationNotification.notification.extras.getString("android.title")
        )
        assertEquals(
            context.getString(R.string.excursion_happening_today, excursion.title),
            excursionNotification!!.notification.extras.getString("android.title")
        )
        assertNotNull(vacationNotification.notification.contentIntent)

        ReminderScheduler.cancelExcursion(context, excursion.id)
        ReminderScheduler.cancelVacation(context, vacation.id)
        val remaining = awaitNotifications { active -> active.none { it.tag in tags } }
        assertFalse(remaining.any { it.tag in tags })
    }

    @Test
    fun reminderForDeletedItemPostsNothing() {
        val missingKey = ReminderKey(ReminderType.EXCURSION, Int.MAX_VALUE)

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

    private fun insertExcursion(vacation: Vacation, date: String): Excursion {
        val excursion = Excursion("Excursion-${UUID.randomUUID().toString().take(8)}", date, vacation.id)
        val saved = excursion.copy(id = db.excursionDAO().insert(excursion).toInt())
        insertedExcursions += saved
        return saved
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
