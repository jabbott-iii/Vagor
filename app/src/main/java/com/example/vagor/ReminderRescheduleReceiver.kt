package com.example.vagor

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.vagor.database.AppDatabase

// Alarms are cleared on reboot, a clock or time zone change moves 9:00 AM local time, and once the
// user allows exact alarms, event reminders can be set exactly an hour before.
class ReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> Unit

            else -> return
        }

        val db = AppDatabase.open(context.applicationContext)
        try {
            ReminderScheduler.rescheduleAll(context, db)
        } finally {
            db.close()
        }
    }
}
