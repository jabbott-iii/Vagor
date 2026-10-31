package com.example.vagor

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.example.vagor.entities.Event
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.time.ZoneId

// From Android 12 the user allows exact alarms for Vagor in Alarms & reminders, and only then do
// event reminders come exactly an hour before (ReminderScheduler.scheduleEvent).

// The Alarms & reminders screen for Vagor, or null before Android 12, where exact alarms need no
// permission.
fun Context.exactAlarmSettingsIntent(): Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.fromParts("package", packageName, null))
} else {
    null
}

// Opens Alarms & reminders, or Vagor's App info screen on a phone that lacks it.
fun Context.openExactAlarmSettings() {
    val appInfo = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
    for (intent in listOfNotNull(exactAlarmSettingsIntent(), appInfo)) {
        try {
            startActivity(intent)
            return
        } catch (_: ActivityNotFoundException) {
            // Try the next one.
        }
    }
}

// Offered after saving an event whose reminder can't be exact. Whichever way it's closed, the
// event form behind it closes too, as it does after any save.
class ExactAlarmOfferDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.exact_reminders_title)
        .setMessage(R.string.exact_reminders_message)
        .setPositiveButton(R.string.open_settings) { _, _ ->
            requireContext().openExactAlarmSettings()
            requireActivity().finish()
        }
        .setNegativeButton(R.string.not_now) { _, _ -> requireActivity().finish() }
        .create()

    // Back, or a tap outside the dialog.
    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        activity?.finish()
    }

    companion object {
        private const val TAG = "exactAlarmOffer"

        // Offered once per app launch, so saving more events doesn't ask again.
        internal var offered = false

        // Shows the offer when [saved]'s reminder is still to come and can't be exact, and
        // notifications are on (without them no reminder shows). False when it isn't shown.
        fun showAfterSave(manager: FragmentManager, context: Context, saved: Event): Boolean {
            if (offered || ReminderScheduler.canScheduleExactAlarms(context)) return false
            if (!ReminderScheduler.notificationsOn(context)) return false
            val start = EventTimes.startOf(saved, ZoneId.systemDefault()) ?: return false
            if (ReminderTimes.eventTriggerAtMillis(start, System.currentTimeMillis()) == null) return false
            if (manager.isStateSaved || manager.findFragmentByTag(TAG) != null) return false
            offered = true
            ExactAlarmOfferDialog().show(manager, TAG)
            return true
        }
    }
}
