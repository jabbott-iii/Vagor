package com.example.vagor

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.AlarmManagerCompat
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

    // With [keepDue], a reminder whose moment has passed but which may still be on its way is left
    // alone instead of cancelled (ReminderTimes.dateReminderMayBeDue).
    fun scheduleVacation(context: Context, vacation: Vacation, keepDue: Boolean = false) {
        schedule(context, ReminderKey(ReminderType.VACATION_START, vacation.id), vacation.startDate, keepDue)
        schedule(context, ReminderKey(ReminderType.VACATION_END, vacation.id), vacation.endDate, keepDue)
    }

    fun cancelVacation(context: Context, vacationId: Int) {
        cancel(context, ReminderKey(ReminderType.VACATION_START, vacationId), dismissPosted = true)
        cancel(context, ReminderKey(ReminderType.VACATION_END, vacationId), dismissPosted = true)
    }

    // An hour before the event starts, or 9:00 AM on its date for an event saved before events had
    // a time. [keepDue] works as for scheduleVacation (ReminderTimes.eventReminderMayBeDue).
    fun scheduleEvent(context: Context, event: Event, keepDue: Boolean = false) {
        val key = ReminderKey(ReminderType.EVENT, event.id)
        val start = EventTimes.startOf(event, ZoneId.systemDefault())
        if (start == null) {
            schedule(context, key, event.date, keepDue)
            return
        }
        val now = System.currentTimeMillis()
        val triggerAt = ReminderTimes.eventTriggerAtMillis(start, now)
        if (triggerAt == null) {
            if (!keepDue || !ReminderTimes.eventReminderMayBeDue(start, now)) {
                cancel(context, key, dismissPosted = false)
            }
            return
        }
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pendingIntent = alarmIntent(context, key, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        if (AlarmManagerCompat.canScheduleExactAlarms(alarmManager)) {
            try {
                AlarmManagerCompat.setExactAndAllowWhileIdle(alarmManager, AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                return
            } catch (_: SecurityException) {
                // The permission was taken away since the check.
            }
        }
        // Without exact alarms (Android 12 and later, until the user allows them): 50 to 60 minutes
        // before. Doze and Battery Saver can hold it until the phone wakes or Vagor is opened, which
        // may be after the event has started.
        alarmManager.setWindow(AlarmManager.RTC_WAKEUP, triggerAt, ReminderTimes.INEXACT_EVENT_WINDOW.toMillis(), pendingIntent)
    }

    // After the event form saves [event]. When its date, time or time zone changed, a reminder
    // already posted for it gives the old time, so it's dismissed and a late alarm isn't kept.
    // When they didn't, an alarm that may still be on its way is kept.
    fun eventSaved(context: Context, event: Event, timingChanged: Boolean) {
        if (timingChanged) {
            NotificationManagerCompat.from(context).cancel(ReminderKey(ReminderType.EVENT, event.id).toUriString(), 0)
        }
        scheduleEvent(context, event, keepDue = !timingChanged)
    }

    // Exact alarms need no permission before Android 12. From Android 14 a new install starts
    // without it, and the user can allow it, or take it away, in Alarms & reminders. Android also
    // allows them for an app whose battery use is set to Unrestricted.
    fun canScheduleExactAlarms(context: Context): Boolean {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return false
        return AlarmManagerCompat.canScheduleExactAlarms(alarmManager)
    }

    // Whether a reminder would show: notifications are on for Vagor and for its reminder channel
    // (a channel not created yet will be created on).
    fun notificationsOn(context: Context): Boolean {
        val manager = NotificationManagerCompat.from(context)
        return manager.areNotificationsEnabled() &&
            manager.getNotificationChannelCompat(CHANNEL_ID)?.importance != NotificationManagerCompat.IMPORTANCE_NONE
    }

    fun cancelEvent(context: Context, eventId: Int) {
        cancel(context, ReminderKey(ReminderType.EVENT, eventId), dismissPosted = true)
    }

    // Whether exact alarms were allowed when this process last rebuilt every reminder; null until
    // it has.
    @Volatile
    private var builtWithExact: Boolean? = null

    // Alarms don't survive a reboot or force stop, dates saved without a time zone follow the
    // phone's, and allowing exact alarms makes event reminders exact, so every reminder is rebuilt
    // from the database. A rebuild keeps the alarms of reminders that may still be on their way.
    fun rescheduleAll(context: Context, db: AppDatabase) {
        builtWithExact = canScheduleExactAlarms(context)
        db.vacationDAO().getAllVacations().forEach { scheduleVacation(context, it, keepDue = true) }
        db.eventDAO().getAllEvents().forEach { scheduleEvent(context, it, keepDue = true) }
    }

    // Rebuilds every reminder once in each new process, and again whenever exact alarms have been
    // allowed or taken away since. Taking them away in Alarms & reminders deletes the exact alarms
    // and kills the app, which may come back on any screen, and nothing is broadcast; setting
    // Vagor's battery use to Unrestricted allows them, and nothing is broadcast either.
    fun rebuildIfNeeded(context: Context) {
        if (builtWithExact == canScheduleExactAlarms(context)) return
        val db = AppDatabase.open(context.applicationContext)
        try {
            rescheduleAll(context, db)
        } finally {
            db.close()
        }
    }

    private fun schedule(context: Context, key: ReminderKey, dateText: String, keepDue: Boolean) {
        val zone = ZoneId.systemDefault()
        val now = System.currentTimeMillis()
        val triggerAt = ReminderTimes.triggerAtMillis(dateText, zone, now)
        if (triggerAt == null) {
            if (!keepDue || !ReminderTimes.dateReminderMayBeDue(dateText, zone, now)) {
                cancel(context, key, dismissPosted = false)
            }
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
