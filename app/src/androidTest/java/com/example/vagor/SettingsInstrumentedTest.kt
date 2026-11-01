package com.example.vagor

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.IntentFilter
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.isSelected
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.Matchers.containsString
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// Changes the app's dark mode setting and puts it back afterwards.
@RunWith(AndroidJUnit4::class)
class SettingsInstrumentedTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private var originalDarkMode = true

    // The home screen asks for notifications when trips exist; the prompt would cover the tabs.
    @Before
    fun rememberSettingAndGrantNotifications() {
        originalDarkMode = ThemeSettings.isDarkMode(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    @After
    fun restoreSetting() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            ThemeSettings.setDarkMode(context, originalDarkMode)
        }
    }

    @Test
    fun darkModeIsOnByDefault() {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().remove("dark_mode").commit()

        assertTrue(ThemeSettings.isDarkMode(context))
    }

    @Test
    fun switchTurnsDarkModeOffAndOn() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { ThemeSettings.setDarkMode(context, true) }

        ActivityScenario.launch(HomeActivity::class.java).use {
            onView(withId(R.id.nav_settings)).perform(click())
            onView(withId(R.id.switchDarkMode)).check(matches(isChecked()))

            onView(withId(R.id.switchDarkMode)).perform(click())
            assertFalse(ThemeSettings.isDarkMode(context))
            assertEquals(AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.getDefaultNightMode())
            // The screen is recreated in the light theme, still on the Settings tab, with the
            // switch off.
            onView(withId(R.id.switchDarkMode)).check(matches(isNotChecked()))
            onView(withId(R.id.nav_settings)).check(matches(isSelected()))

            onView(withId(R.id.switchDarkMode)).perform(click())
            assertTrue(ThemeSettings.isDarkMode(context))
            assertEquals(AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.getDefaultNightMode())
            onView(withId(R.id.switchDarkMode)).check(matches(isChecked()))
        }
    }

    @Test
    fun backFromSettingsReturnsToVacations() {
        ActivityScenario.launch(HomeActivity::class.java).use {
            onView(withId(R.id.nav_settings)).perform(click())
            onView(withId(R.id.switchDarkMode)).check(matches(isDisplayed()))

            pressBack()

            onView(withId(R.id.buttonAddVacation)).check(matches(isDisplayed()))
            onView(withId(R.id.nav_vacations)).check(matches(isSelected()))
        }
    }

    @Test
    fun settingsSaysWhetherEventRemindersAreExact() {
        ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
            onView(withId(R.id.nav_settings)).perform(click())
            val status = when {
                !ReminderScheduler.notificationsOn(context) -> R.string.reminders_need_notifications
                ReminderScheduler.canScheduleExactAlarms(context) -> R.string.event_reminders_exact
                else -> R.string.event_reminders_inexact
            }
            onView(withId(R.id.eventRemindersStatus)).check(matches(withText(status)))

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                // Exact alarms need no permission, so there's nothing to open.
                onView(withId(R.id.buttonAlarmsAndReminders)).check(matches(withEffectiveVisibility(Visibility.GONE)))
                return@use
            }
            // Android's Alarms & reminders screen opens over Vagor, and Back returns to the tab.
            onView(withId(R.id.buttonAlarmsAndReminders)).perform(scrollTo(), click())
            awaitState(scenario, Lifecycle.State.CREATED)
            InstrumentationRegistry.getInstrumentation().uiAutomation.performGlobalAction(
                AccessibilityService.GLOBAL_ACTION_BACK
            )
            awaitState(scenario, Lifecycle.State.RESUMED)
            onView(withId(R.id.eventRemindersStatus)).check(matches(withText(status)))
        }
    }

    @Test
    fun settingsSaysWhetherAndroidMayPauseVagor() {
        ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
            onView(withId(R.id.nav_settings)).perform(click())

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                // Android doesn't pause unused apps, so there's nothing to say or open.
                onView(withId(R.id.hibernationStatus)).check(matches(withEffectiveVisibility(Visibility.GONE)))
                onView(withId(R.id.buttonAppInfo)).check(matches(withEffectiveVisibility(Visibility.GONE)))
                return@use
            }
            val status = if (context.mayHibernate()) R.string.hibernation_on else R.string.hibernation_off
            onView(withId(R.id.hibernationStatus)).perform(scrollTo()).check(matches(withText(status)))

            // Vagor's App info opens over Vagor, and Back returns to the tab.
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val appInfo = watchForOwnAppInfo()
            try {
                onView(withId(R.id.buttonAppInfo)).perform(scrollTo(), click())
                awaitState(scenario, Lifecycle.State.CREATED)
                assertTrue("Vagor's App info didn't open", instrumentation.checkMonitorHit(appInfo, 1))
            } finally {
                instrumentation.removeMonitor(appInfo)
            }
            instrumentation.uiAutomation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            awaitState(scenario, Lifecycle.State.RESUMED)
            onView(withId(R.id.hibernationStatus)).check(matches(withText(status)))
        }
    }

    @Test
    fun notificationSettingsButtonShowsOnlyWhileNotificationsAreOff() {
        ActivityScenario.launch(HomeActivity::class.java).use {
            onView(withId(R.id.nav_settings)).perform(click())

            val visibility = if (ReminderScheduler.notificationsOn(context)) Visibility.GONE else Visibility.VISIBLE
            onView(withId(R.id.buttonNotificationSettings)).check(matches(withEffectiveVisibility(visibility)))
        }
    }

    // The dashboard shows it when notifications are off. Open Settings asks for Android's
    // notification settings for Vagor; the request is caught, so no settings screen opens.
    @Test
    fun notificationsOffDialogOpensNotificationSettings() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val monitor = Instrumentation.ActivityMonitor(
            IntentFilter(Settings.ACTION_APP_NOTIFICATION_SETTINGS),
            Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null),
            true
        )
        instrumentation.addMonitor(monitor)
        try {
            ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
                scenario.onActivity { NotificationsOffDialog.show(it.supportFragmentManager) }
                onView(withText(R.string.notifications_off_title)).inRoot(isDialog()).check(matches(isDisplayed()))

                onView(withText(R.string.open_settings)).inRoot(isDialog()).perform(click())

                // Notifications are on in this test, so not only the channel is off: Vagor's own page.
                assertTrue(instrumentation.checkMonitorHit(monitor, 1))
                onView(withId(R.id.buttonAddVacation)).check(matches(isDisplayed()))
            }
        } finally {
            instrumentation.removeMonitor(monitor)
        }
    }

    @Test
    fun privacyPolicyOpensFromSettings() {
        ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
            onView(withId(R.id.nav_settings)).perform(click())
            onView(withId(R.id.buttonPrivacyPolicy)).perform(scrollTo(), click())

            onView(withText(R.string.privacy_policy)).inRoot(isDialog()).check(matches(isDisplayed()))

            // It stays open through a rotation, which recreates the screen.
            scenario.recreate()
            onView(withText(R.string.privacy_policy)).inRoot(isDialog()).check(matches(isDisplayed()))
        }
    }

    @Test
    fun thirdPartyLicensesOpenFromSettingsWithTheLicenseText() {
        ActivityScenario.launch(HomeActivity::class.java).use {
            onView(withId(R.id.nav_settings)).perform(click())
            onView(withId(R.id.buttonThirdPartyLicenses)).perform(scrollTo(), click())

            onView(withText(R.string.third_party_licenses)).inRoot(isDialog()).check(matches(isDisplayed()))
            onView(withText(containsString("Material Components for Android"))).inRoot(isDialog())
                .check(
                    matches(withText(containsString("TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION")))
                )
        }
    }

    // For reporting a problem: the version name and code, as Google Play shows them.
    @Test
    fun settingsShowsTheAppVersion() {
        @Suppress("DEPRECATION")
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        val version = "Version ${info.versionName} (${PackageInfoCompat.getLongVersionCode(info)})"

        ActivityScenario.launch(HomeActivity::class.java).use {
            onView(withId(R.id.nav_settings)).perform(click())

            onView(withId(R.id.appVersion)).perform(scrollTo()).check(matches(withText(version)))
        }
    }

    private fun awaitState(scenario: ActivityScenario<*>, state: Lifecycle.State) {
        val deadline = SystemClock.uptimeMillis() + 5000
        while (scenario.state != state && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(50)
        assertEquals(state, scenario.state)
    }
}
