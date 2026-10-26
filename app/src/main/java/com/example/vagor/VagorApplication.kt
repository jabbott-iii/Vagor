package com.example.vagor

import android.app.Application

class VagorApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Before any activity is created, so the app's first frame already uses the saved theme.
        // On Android 8-11, and on the first launch on 12+, the launch window the system draws
        // before this runs follows the system light/dark setting (see ThemeSettings).
        ThemeSettings.applySaved(this)
    }
}
