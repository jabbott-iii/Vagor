package com.example.vagor

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder

// No reminder shows while notifications are off for Vagor, or only for its reminder channel
// (ReminderScheduler.notificationsOn).

// Opens where the user turns them back on: the reminder channel's settings when only it is off,
// otherwise Vagor's notification settings, or its App info screen on a phone that lacks them.
fun Context.openNotificationSettings() {
    val manager = NotificationManagerCompat.from(this)
    val channelOnly = manager.areNotificationsEnabled() &&
        manager.getNotificationChannelCompat(ReminderScheduler.CHANNEL_ID)?.importance ==
        NotificationManagerCompat.IMPORTANCE_NONE
    val channel = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        .putExtra(Settings.EXTRA_CHANNEL_ID, ReminderScheduler.CHANNEL_ID)
    val app = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    val appInfo = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
    for (intent in listOfNotNull(channel.takeIf { channelOnly }, app, appInfo)) {
        try {
            startActivity(intent)
            return
        } catch (_: ActivityNotFoundException) {
            // Try the next one.
        }
    }
}

// Shown once per launch when the dashboard has a trip but notifications are off, for the app or
// only for the reminder channel, and, on Android 13+, after the user turns down the permission
// prompt. A dialog rather than a toast,
// because Android cuts a toast at two lines, and this one offers the way to turn them on.
class NotificationsOffDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.notifications_off_title)
        .setMessage(R.string.reminders_need_notifications)
        .setPositiveButton(R.string.open_settings) { _, _ -> requireContext().openNotificationSettings() }
        .setNegativeButton(R.string.not_now, null)
        .create()

    companion object {
        private const val TAG = "notificationsOff"

        fun show(manager: FragmentManager) {
            if (manager.isStateSaved || manager.findFragmentByTag(TAG) != null) return
            NotificationsOffDialog().show(manager, TAG)
        }
    }
}
