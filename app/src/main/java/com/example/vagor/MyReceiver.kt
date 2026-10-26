package com.example.vagor

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.room.Room.databaseBuilder
import com.example.vagor.database.AppDatabase
import com.example.vagor.entities.Event
import com.example.vagor.entities.Vacation

// Fires for alarms set by ReminderScheduler and posts the reminder notification.
class MyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Alarms left by older versions have no reminder URI. Their items get new reminders after
        // the update, so these are ignored.
        val key = ReminderKey.fromUriString(intent.dataString) ?: return

        val db = databaseBuilder<AppDatabase>(
            context.applicationContext,
            AppDatabase::class.java, "vacation_database"
        )
            .allowMainThreadQueries()
            .build()
        try {
            showReminder(context, db, key)
        } finally {
            db.close()
        }
    }

    private fun showReminder(context: Context, db: AppDatabase, key: ReminderKey) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val builder = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)

        // The item may have been deleted since the alarm was set.
        when (key.type) {
            ReminderType.VACATION_START, ReminderType.VACATION_END -> {
                val vacation = db.vacationDAO().getVacationById(key.itemId) ?: return
                val message = if (key.type == ReminderType.VACATION_START) {
                    R.string.vacation_starting_today
                } else {
                    R.string.vacation_ending_today
                }
                builder.setContentTitle(context.getString(message, vacation.title))
                    .setContentText(context.getString(R.string.reminder_hotel, vacation.hotel))
                    .setContentIntent(vacationReminderStack(context, vacation, key).getPendingIntent(0, PENDING_INTENT_FLAGS))
            }
            ReminderType.EVENT -> {
                val event = db.eventDAO().getEventById(key.itemId) ?: return
                val vacation = db.vacationDAO().getVacationById(event.vacationId) ?: return
                builder.setContentTitle(context.getString(R.string.event_happening_today, event.title))
                    .setContentText(context.getString(R.string.reminder_part_of_vacation, vacation.title))
                    .setContentIntent(eventReminderStack(context, vacation, event, key).getPendingIntent(0, PENDING_INTENT_FLAGS))
            }
        }

        ReminderScheduler.ensureChannel(context)
        NotificationManagerCompat.from(context).notify(key.toUriString(), 0, builder.build())
    }

    companion object {
        private const val PENDING_INTENT_FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        // What tapping a reminder opens, above the screens Back returns to. The last intent carries
        // the reminder's URI, which keeps each reminder's PendingIntent separate.
        internal fun vacationReminderStack(context: Context, vacation: Vacation, key: ReminderKey): TaskStackBuilder =
            TaskStackBuilder.create(context)
                .addNextIntent(Intent(context, HomeActivity::class.java))
                .addNextIntent(vacationPageIntent(context, vacation).setData(key.toUriString().toUri()))

        internal fun eventReminderStack(context: Context, vacation: Vacation, event: Event, key: ReminderKey): TaskStackBuilder =
            TaskStackBuilder.create(context)
                .addNextIntent(Intent(context, HomeActivity::class.java))
                .addNextIntent(vacationPageIntent(context, vacation))
                .addNextIntent(eventDetailIntent(context, event).setData(key.toUriString().toUri()))

        // The vacation's page, with its details and events.
        private fun vacationPageIntent(context: Context, vacation: Vacation) =
            Intent(context, EventListActivity::class.java)
                .putExtra("vacationId", vacation.id)

        private fun eventDetailIntent(context: Context, event: Event) =
            Intent(context, EventDetailActivity::class.java)
                .putExtra("eventId", event.id)
                .putExtra("title", event.title)
                .putExtra("date", event.date)
                .putExtra("vacationId", event.vacationId)
    }
}
