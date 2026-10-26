package com.example.vagor

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import com.example.vagor.database.AppDatabase
import com.example.vagor.entities.Event
import com.example.vagor.entities.Vacation
import java.time.ZoneId

// The only place that schedules or cancels reminder alarms. MyReceiver posts the notification
// when an alarm fires.
object ReminderScheduler {
    const val CHANNEL_ID = "reminders"
    private const val ONE_HOUR_MILLIS = 60 * 60 * 1000L

    fun ensureChannel(context: Context) {
        val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
            .setName(context.getString(R.string.reminder_channel_name))
            .setDescription(context.getString(R.string.reminder_channel_description))
            .build()
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    fun scheduleVacation(context: Context, vacation: Vacation) {
        schedule(context, ReminderKey(ReminderType.VACATION_START, vacation.id), vacation.startDate)
        schedule(context, ReminderKey(ReminderType.VACATION_END, vacation.id), vacation.endDate)
    }

    fun cancelVacation(context: Context, vacationId: Int) {
        cancel(context, ReminderKey(ReminderType.VACATION_START, vacationId), dismissPosted = true)
        cancel(context, ReminderKey(ReminderType.VACATION_END, vacationId), dismissPosted = true)
    }

    fun scheduleEvent(context: Context, event: Event) {
        schedule(context, ReminderKey(ReminderType.EVENT, event.id), event.date)
    }

    fun cancelEvent(context: Context, eventId: Int) {
        cancel(context, ReminderKey(ReminderType.EVENT, eventId), dismissPosted = true)
    }

    // Alarms don't survive a reboot or force stop, and stored dates have no time zone, so every
    // reminder is rebuilt from the database.
    fun rescheduleAll(context: Context, db: AppDatabase) {
        db.vacationDAO().getAllVacations().forEach { scheduleVacation(context, it) }
        db.eventDAO().getAllEvents().forEach { scheduleEvent(context, it) }
    }

    private fun schedule(context: Context, key: ReminderKey, dateText: String) {
        val triggerAt = ReminderTimes.triggerAtMillis(dateText, ZoneId.systemDefault(), System.currentTimeMillis())
        if (triggerAt == null) {
            cancel(context, key, dismissPosted = false)
            return
        }
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pendingIntent = alarmIntent(context, key, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ caps an inexact alarm's delay at an hour, and allow-while-idle lets it
            // fire during Doze.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } else {
            // Older versions may defer an inexact alarm by up to 75% of the time left until it's
            // due, which is months for a trip planned far ahead, so cap the window at an hour.
            // Doze can still hold it until the phone wakes.
            alarmManager.setWindow(AlarmManager.RTC_WAKEUP, triggerAt, ONE_HOUR_MILLIS, pendingIntent)
        }
    }

    private fun cancel(context: Context, key: ReminderKey, dismissPosted: Boolean) {
        alarmIntent(context, key, PendingIntent.FLAG_NO_CREATE)?.let { pendingIntent ->
            context.getSystemService(AlarmManager::class.java)?.cancel(pendingIntent)
            pendingIntent.cancel()
        }
        if (dismissPosted) {
            NotificationManagerCompat.from(context).cancel(key.toUriString(), 0)
        }
    }

    private fun alarmIntent(context: Context, key: ReminderKey, flags: Int): PendingIntent? {
        val intent = Intent(context, MyReceiver::class.java).setData(key.toUriString().toUri())
        return PendingIntent.getBroadcast(context, 0, intent, flags or PendingIntent.FLAG_IMMUTABLE)
    }
}
