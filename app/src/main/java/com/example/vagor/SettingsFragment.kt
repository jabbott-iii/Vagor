package com.example.vagor

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
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

        // Before Android 12, Android doesn't stop an unused app, so there's nothing to turn off.
        view.findViewById<Button>(R.id.buttonAppInfo).apply {
            isVisible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            setOnClickListener { requireContext().openAppInfo() }
        }

        view.findViewById<Button>(R.id.buttonPrivacyPolicy).setOnClickListener {
            MessageDialog.show(
                childFragmentManager,
                "privacyPolicy",
                R.string.privacy_policy,
                R.string.privacy_policy_text
            )
        }

        // The bundled libraries' Apache License asks for its text to go with the app.
        view.findViewById<Button>(R.id.buttonThirdPartyLicenses).setOnClickListener {
            MessageDialog.show(
                childFragmentManager,
                "thirdPartyLicenses",
                R.string.third_party_licenses,
                R.string.third_party_licenses_text,
                R.raw.apache_license_2_0
            )
        }

        val packageInfo = requireContext().ownPackageInfo()
        view.findViewById<TextView>(R.id.appVersion).text =
            getString(R.string.app_version, packageInfo.versionName, PackageInfoCompat.getLongVersionCode(packageInfo))
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
        // Back from App info, the unused app setting may have changed.
        requireView().findViewById<TextView>(R.id.hibernationStatus).apply {
            isVisible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            setText(if (context.mayHibernate()) R.string.hibernation_on else R.string.hibernation_off)
        }
    }
}

// Android 13 replaced getPackageInfo's Int flags with PackageInfoFlags.
private fun Context.ownPackageInfo(): PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
} else {
    @Suppress("DEPRECATION")
    packageManager.getPackageInfo(packageName, 0)
}
