package com.example.vagor

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit

// The dark mode switch on the Settings screen. Dark is the default. This is a UI preference,
// not trip data, so it lives in SharedPreferences rather than vacation_database.
object ThemeSettings {
    private const val PREFS_NAME = "settings"
    private const val KEY_DARK_MODE = "dark_mode"

    fun isDarkMode(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_DARK_MODE, true)

    fun setDarkMode(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit { putBoolean(KEY_DARK_MODE, enabled) }
        applyNightMode(context, enabled)
    }

    // AppCompat doesn't persist the night mode, so this runs on every process start.
    fun applySaved(context: Context) {
        applyNightMode(context, isDarkMode(context))
    }

    private fun applyNightMode(context: Context, dark: Boolean) {
        AppCompatDelegate.setDefaultNightMode(
            if (dark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
        // The system draws the launch window before any app code runs. On Android 12+ it
        // remembers this setting, so later launches' splash screens match the app; Android 8-11
        // has no equivalent and uses the system light/dark setting.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(UiModeManager::class.java)?.setApplicationNightMode(
                if (dark) UiModeManager.MODE_NIGHT_YES else UiModeManager.MODE_NIGHT_NO
            )
        }
    }
}
