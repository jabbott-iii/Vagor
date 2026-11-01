package com.example.vagor

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Build
import android.os.SystemClock
import androidx.appcompat.app.AppCompatDelegate
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
    fun privacyPolicyOpensFromSettings() {
        ActivityScenario.launch(HomeActivity::class.java).use {
            onView(withId(R.id.nav_settings)).perform(click())
            onView(withId(R.id.buttonPrivacyPolicy)).perform(scrollTo(), click())

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

    private fun awaitState(scenario: ActivityScenario<*>, state: Lifecycle.State) {
        val deadline = SystemClock.uptimeMillis() + 5000
        while (scenario.state != state && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(50)
        assertEquals(state, scenario.state)
    }
}
