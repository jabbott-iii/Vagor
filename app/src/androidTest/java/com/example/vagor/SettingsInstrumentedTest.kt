package com.example.vagor

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.isSelected
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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

    @Before
    fun rememberSetting() {
        originalDarkMode = ThemeSettings.isDarkMode(context)
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
    fun privacyPolicyOpensFromSettings() {
        ActivityScenario.launch(HomeActivity::class.java).use {
            onView(withId(R.id.nav_settings)).perform(click())
            onView(withId(R.id.buttonPrivacyPolicy)).perform(click())

            onView(withText(R.string.privacy_policy)).inRoot(isDialog()).check(matches(isDisplayed()))
        }
    }
}
