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
import com.example.vagor.entities.Excursion
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
                    .setContentIntent(
                        TaskStackBuilder.create(context)
                            .addNextIntent(Intent(context, HomeActivity::class.java))
                            .addNextIntent(Intent(context, VacationListActivity::class.java))
                            .addNextIntent(vacationDetailIntent(context, vacation).setData(key.toUriString().toUri()))
                            .getPendingIntent(0, PENDING_INTENT_FLAGS)
                    )
            }
            ReminderType.EXCURSION -> {
                val excursion = db.excursionDAO().getExcursionById(key.itemId) ?: return
                val vacation = db.vacationDAO().getVacationById(excursion.vacationId) ?: return
                builder.setContentTitle(context.getString(R.string.excursion_happening_today, excursion.title))
                    .setContentText(context.getString(R.string.reminder_part_of_vacation, vacation.title))
                    .setContentIntent(
                        TaskStackBuilder.create(context)
                            .addNextIntent(Intent(context, HomeActivity::class.java))
                            .addNextIntent(Intent(context, VacationListActivity::class.java))
                            .addNextIntent(vacationDetailIntent(context, vacation))
                            .addNextIntent(
                                Intent(context, ExcursionListActivity::class.java)
                                    .putExtra("vacationId", vacation.id)
                            )
                            .addNextIntent(excursionDetailIntent(context, excursion).setData(key.toUriString().toUri()))
                            .getPendingIntent(0, PENDING_INTENT_FLAGS)
                    )
            }
        }

        ReminderScheduler.ensureChannel(context)
        NotificationManagerCompat.from(context).notify(key.toUriString(), 0, builder.build())
    }

    private fun vacationDetailIntent(context: Context, vacation: Vacation) =
        Intent(context, VacationDetailActivity::class.java)
            .putExtra("vacationId", vacation.id)
            .putExtra("title", vacation.title)
            .putExtra("hotel", vacation.hotel)
            .putExtra("startDate", vacation.startDate)
            .putExtra("endDate", vacation.endDate)

    private fun excursionDetailIntent(context: Context, excursion: Excursion) =
        Intent(context, ExcursionDetailActivity::class.java)
            .putExtra("excursionId", excursion.id)
            .putExtra("title", excursion.title)
            .putExtra("date", excursion.date)
            .putExtra("vacationId", excursion.vacationId)

    private companion object {
        const val PENDING_INTENT_FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    }
}
