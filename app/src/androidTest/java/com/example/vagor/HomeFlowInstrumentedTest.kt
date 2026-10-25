package com.example.vagor

import android.Manifest
import android.os.Build
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class HomeFlowInstrumentedTest {
    // Saving a vacation leads to the notification permission prompt, which would cover the list.
    @Before
    fun grantNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            instrumentation.uiAutomation.grantRuntimePermission(
                instrumentation.targetContext.packageName,
                Manifest.permission.POST_NOTIFICATIONS
            )
        }
    }

    @Test
    fun canCreateVacationFromHomeFlow() {
        val uniqueTitle = "Trip-${UUID.randomUUID().toString().take(8)}"
        val hotelName = "Hotel Vagor"

        ActivityScenario.launch(HomeActivity::class.java).use {
            onView(withId(R.id.buttonGoToVacations)).perform(click())
            onView(withId(R.id.buttonAddVacation)).perform(click())
            onView(withId(R.id.editTitle)).perform(replaceText(uniqueTitle))
            onView(withId(R.id.editHotel)).perform(replaceText(hotelName))
            onView(withId(R.id.editStartDate)).perform(replaceText("09/16/2026"))
            onView(withId(R.id.editEndDate)).perform(replaceText("09/18/2026"))
            closeSoftKeyboard()
            onView(withId(R.id.buttonSave)).perform(click())

            onView(withText("$uniqueTitle - $hotelName")).check(matches(isDisplayed()))
        }
    }
}
