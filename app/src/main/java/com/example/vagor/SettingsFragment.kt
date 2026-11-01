package com.example.vagor

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch

// The Settings tab on the home screen.
class SettingsFragment : Fragment(R.layout.fragment_settings) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val switchDarkMode = view.findViewById<MaterialSwitch>(R.id.switchDarkMode)
        switchDarkMode.isChecked = ThemeSettings.isDarkMode(requireContext())
        // Changing the night mode recreates every open screen in the new theme; the home screen
        // comes back on this tab.
        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            ThemeSettings.setDarkMode(requireContext(), isChecked)
        }

        view.findViewById<Button>(R.id.buttonNotificationSettings).setOnClickListener {
            requireContext().openNotificationSettings()
        }

        // Before Android 12 there's no Alarms & reminders screen, and event reminders are always
        // exact.
        view.findViewById<Button>(R.id.buttonAlarmsAndReminders).apply {
            isVisible = requireContext().exactAlarmSettingsIntent() != null
            setOnClickListener { requireContext().openExactAlarmSettings() }
        }

        view.findViewById<Button>(R.id.buttonPrivacyPolicy).setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.privacy_policy)
                .setMessage(R.string.privacy_policy_text)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }

        // The bundled libraries' Apache License asks for its text to go with the app.
        view.findViewById<Button>(R.id.buttonThirdPartyLicenses).setOnClickListener {
            val license = resources.openRawResource(R.raw.apache_license_2_0).bufferedReader().use { it.readText() }
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.third_party_licenses)
                .setMessage(getString(R.string.third_party_licenses_text) + "\n\n" + reflowParagraphs(license))
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        // Back from Android's settings, notifications or exact alarms may have been turned on or off.
        val context = requireContext()
        val notificationsOn = ReminderScheduler.notificationsOn(context)
        val status = when {
            !notificationsOn -> R.string.reminders_need_notifications
            ReminderScheduler.canScheduleExactAlarms(context) -> R.string.event_reminders_exact
            else -> R.string.event_reminders_inexact
        }
        requireView().findViewById<TextView>(R.id.eventRemindersStatus).setText(status)
        requireView().findViewById<Button>(R.id.buttonNotificationSettings).isVisible = !notificationsOn
    }
}
