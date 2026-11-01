package com.example.vagor

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.vagor.entities.Event
import org.junit.After
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

// Android lets an app hold 500 alarms at a time. Sets real alarms for events that aren't in the
// database (ids far above any real one) and cancels them all afterwards; a run killed partway leaves
// them set until they fire 30 days later or the app is uninstalled.
@RunWith(AndroidJUnit4::class)
class ReminderLimitInstrumentedTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val ids = FIRST_ID until FIRST_ID + COUNT

    @After
    fun cancelReminders() {
        ids.forEach { ReminderScheduler.cancelEvent(context, it) }
    }

    @Test
    fun remindersPastTheAlarmLimitAreLeftUnsetInsteadOfCrashing() {
        val date = DateValidators.formatDate(LocalDate.now().plusDays(30))
        val events = ids.map { Event("Limit test", date, 0, it, time = "10:00", timeZone = "UTC") }

        events.forEach { ReminderScheduler.scheduleEvent(context, it) }
        val before = alarm(ids.first)
        assertNotNull("The first reminder wasn't set", before)
        // Replacing a reminder that is already set works at the limit too.
        ReminderScheduler.scheduleEvent(context, events.first().copy(time = "11:00"))

        // Android 12+ has the limit (older versions may not): the 11:00 reminder replaced the first
        // one only by cancelling it and setting it again, and the last one stayed unset, with no
        // alarm record left behind.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            assertNotEquals(before, alarm(ids.first))
            assertNotNull(alarm(ids.first))
            assertNull(alarm(ids.last))
        }
    }

    private fun alarm(eventId: Int): PendingIntent? {
        val intent = Intent(context, MyReceiver::class.java)
            .setData(ReminderKey(ReminderType.EVENT, eventId).toUriString().toUri())
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private companion object {
        const val FIRST_ID = 900_000_000
        const val COUNT = 520
    }
}
