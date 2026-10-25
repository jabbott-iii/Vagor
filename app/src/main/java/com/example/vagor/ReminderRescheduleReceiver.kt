package com.example.vagor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.room.Room.databaseBuilder
import com.example.vagor.database.AppDatabase

// Alarms are cleared on reboot, and a clock or time zone change moves 9:00 AM local time.
class ReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> Unit
            else -> return
        }

        val db = databaseBuilder<AppDatabase>(
            context.applicationContext,
            AppDatabase::class.java, "vacation_database"
        )
            .allowMainThreadQueries()
            .build()
        try {
            ReminderScheduler.rescheduleAll(context, db)
        } finally {
            db.close()
        }
    }
}
