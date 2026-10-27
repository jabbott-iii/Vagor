package com.example.vagor

import android.app.Application

class VagorApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Before any activity is created, so the app's first frame already uses the saved theme.
        // On Android 8-11, and on the first launch on 12+, the launch window the system draws
        // before this runs follows the system light/dark setting (see ThemeSettings).
        ThemeSettings.applySaved(this)
        // Every new process rebuilds the reminders, whatever starts it: a launch, a screen restored
        // after Android stopped the app (taking exact alarms away does that, and deletes them), or
        // an alarm or broadcast. A force stop or an app update also ends the process.
        ReminderScheduler.rebuildIfNeeded(this)
    }
}
