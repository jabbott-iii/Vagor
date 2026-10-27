package com.example.vagor

import android.content.Context
import android.app.LocaleManager
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.LocaleList
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.isSelected
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.vagor.database.AppDatabase
import com.example.vagor.entities.Vacation
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.hamcrest.TypeSafeMatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.util.Locale
import java.util.UUID

// Uses the app's vacation_database but deletes only the rows it inserts.
@RunWith(AndroidJUnit4::class)
class CalendarDropdownInstrumentedTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db: AppDatabase = AppDatabase.open(context)
    private val insertedVacations = mutableListOf<Vacation>()

    @After
    fun tearDown() {
        insertedVacations.forEach {
            ReminderScheduler.cancelVacation(context, it.id)
            db.vacationDAO().delete(it)
        }
        db.close()
    }

    @Test
    fun tappingADayFillsTheField() {
        ActivityScenario.launch(VacationDetailActivity::class.java).use {
            onView(withId(R.id.editStartDate)).perform(replaceText("03/10/2030"))
            onView(withId(R.id.editStartDate)).perform(click())
            day("10").check(matches(isSelected()))

            day("20").perform(click())

            onView(withId(R.id.editStartDate)).check(matches(withText("03/20/2030")))
            onView(withId(R.id.monthTitle)).check(doesNotExist())
        }
    }

    @Test
    fun backClosesTheCalendarWithoutChangingTheDate() {
        ActivityScenario.launch(VacationDetailActivity::class.java).use {
            onView(withId(R.id.editStartDate)).perform(replaceText("03/10/2030"))
            onView(withId(R.id.editStartDate)).perform(click())
            onView(withId(R.id.monthTitle)).inRoot(isPlatformPopup()).check(matches(isDisplayed()))

            pressBack()

            onView(withId(R.id.monthTitle)).check(doesNotExist())
            onView(withId(R.id.editStartDate)).check(matches(withText("03/10/2030")))
        }
    }

    @Test
    fun monthButtonsMoveBetweenMonths() {
        ActivityScenario.launch(VacationDetailActivity::class.java).use {
            onView(withId(R.id.editStartDate)).perform(replaceText("01/31/2030"))
            onView(withId(R.id.editStartDate)).perform(click())

            onView(withId(R.id.nextMonth)).inRoot(isPlatformPopup()).perform(click())
            // February 2030 has 28 days.
            day("28").check(matches(isEnabled()))
            onView(allOf(withText("29"), isDayCell())).inRoot(isPlatformPopup()).check(doesNotExist())
            day("14").perform(click())

            onView(withId(R.id.editStartDate)).check(matches(withText("02/14/2030")))
        }
    }

    @Test
    fun endDateCalendarStartsAtTheStartDate() {
        ActivityScenario.launch(VacationDetailActivity::class.java).use {
            onView(withId(R.id.editStartDate)).perform(replaceText("03/10/2030"))
            onView(withId(R.id.editEndDate)).perform(click())

            day("10").check(matches(allOf(isSelected(), isEnabled())))
            day("9").check(matches(not(isEnabled())))
            onView(withId(R.id.previousMonth)).inRoot(isPlatformPopup()).check(matches(not(isDisplayed())))
        }
    }

    @Test
    fun emptyEndDateOpensAtAPastStartDate() {
        ActivityScenario.launch(VacationDetailActivity::class.java).use {
            onView(withId(R.id.editStartDate)).perform(replaceText("01/15/2020"))
            onView(withId(R.id.editEndDate)).perform(click())
            day("15").check(matches(isSelected()))

            day("20").perform(click())

            onView(withId(R.id.editEndDate)).check(matches(withText("01/20/2020")))
        }
    }

    @Test
    fun eventCalendarOffersOnlyTheVacationsDays() {
        val vacation = Vacation("Trip-${UUID.randomUUID().toString().take(8)}", "Hotel Vagor", "07/04/2031", "07/09/2031")
        val saved = vacation.copy(id = db.vacationDAO().insert(vacation).toInt())
        insertedVacations += saved
        val intent = Intent(context, EventDetailActivity::class.java).putExtra("vacationId", saved.id)

        ActivityScenario.launch<EventDetailActivity>(intent).use {
            onView(withId(R.id.editEventDate)).perform(click())

            day("4").check(matches(isSelected()))
            day("3").check(matches(not(isEnabled())))
            day("10").check(matches(not(isEnabled())))
            day("9").check(matches(isEnabled()))
            onView(withId(R.id.previousMonth)).inRoot(isPlatformPopup()).check(matches(not(isDisplayed())))
            onView(withId(R.id.nextMonth)).inRoot(isPlatformPopup()).check(matches(not(isDisplayed())))

            day("6").perform(click())
            onView(withId(R.id.editEventDate)).check(matches(withText("07/06/2031")))
        }
    }

    @Test
    fun todayIsMarkedForScreenReaders() {
        val today = LocalDate.now()
        ActivityScenario.launch(VacationDetailActivity::class.java).use {
            onView(withId(R.id.editStartDate)).perform(click())

            day(today.dayOfMonth.toString()).check { view, _ ->
                assertEquals(context.getString(R.string.today), ViewCompat.getStateDescription(view))
                assertTrue(view.isSelected)
            }
        }
    }

    // Today's ring is what marks today without colour, so it must stay a ring, not a filled circle.
    @Test
    fun todayIsDrawnAsARingAndThePickedDayAsAFilledCircle() {
        val size = 100
        val blue = ColorStateList.valueOf(Color.BLUE)
        fun render(drawable: Drawable): Bitmap {
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            drawable.setBounds(0, 0, size, size)
            drawable.draw(Canvas(bitmap))
            return bitmap
        }

        val ring = render(DayCircle(blue, strokeWidth = 4f, maxDiameter = 80f, clearance = 0f))
        assertEquals(Color.TRANSPARENT, ring.getPixel(50, 50))
        assertEquals(Color.BLUE, ring.getPixel(50, 12))

        val circle = render(DayCircle(blue, strokeWidth = 0f, maxDiameter = 80f, clearance = 0f))
        assertEquals(Color.BLUE, circle.getPixel(50, 50))

        // In a cell narrower than the circle's full size, it shrinks to fit instead of being cut off.
        val narrow = Bitmap.createBitmap(60, size, Bitmap.Config.ARGB_8888)
        DayCircle(blue, strokeWidth = 4f, maxDiameter = 80f, clearance = 3f).apply { setBounds(0, 0, 60, size) }.draw(Canvas(narrow))
        assertEquals(Color.BLUE, narrow.getPixel(5, 50))
        assertEquals(Color.TRANSPARENT, narrow.getPixel(1, 50))
    }

    // The framework calendar followed the phone's calendar system, so a Thai phone showed and
    // stored Buddhist-era years (2573 for 2030). The calendar and stored dates must stay Gregorian.
    @Test
    fun calendarStaysGregorianInAThaiLocale() {
        val original = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag("th-TH"))
        setAppLanguage("th-TH")
        try {
            ActivityScenario.launch(VacationDetailActivity::class.java).use { scenario ->
                // Fails if the app language didn't actually change, so the test can't pass vacuously.
                scenario.onActivity {
                    assertEquals("th", ConfigurationCompat.getLocales(it.resources.configuration)[0]?.language)
                }
                onView(withId(R.id.editStartDate)).perform(replaceText("03/10/2030"))
                onView(withId(R.id.editStartDate)).perform(click())

                onView(withId(R.id.monthTitle)).inRoot(isPlatformPopup()).check { view, _ ->
                    val title = (view as TextView).text.toString()
                    assertTrue("Month title \"$title\" should show 2030", title.contains("2030"))
                }
                day("20").perform(click())

                onView(withId(R.id.editStartDate)).check(matches(withText("03/20/2030")))
            }
        } finally {
            setAppLanguage(null)
            Locale.setDefault(original)
        }
    }

    // The app's own language, as set in Android's per-app language settings; null goes back to the
    // phone's language. From Android 13 the system keeps it; before that, AppCompat does.
    private fun setAppLanguage(languageTag: String?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java).applicationLocales =
                if (languageTag == null) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(languageTag)
        } else {
            val locales = if (languageTag == null) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(languageTag)
            InstrumentationRegistry.getInstrumentation().runOnMainSync { AppCompatDelegate.setApplicationLocales(locales) }
        }
    }

    @Test
    fun dateFieldIsReadAsADropDownListThatCantBeTypedInto() {
        ActivityScenario.launch(VacationDetailActivity::class.java).use {
            onView(withId(R.id.editStartDate)).perform(replaceText("03/10/2030"))
            onView(withId(R.id.editStartDate)).check { view, _ ->
                val info = view.createAccessibilityNodeInfo()
                assertEquals(Spinner::class.java.name, info.className)
                assertFalse(info.isEditable)
                assertFalse(info.actionList.contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_TEXT))
                assertEquals("${context.getString(R.string.start_date)}, 03/10/2030", info.contentDescription)
                assertFalse(view.performAccessibilityAction(AccessibilityNodeInfo.ACTION_SET_TEXT, null))
            }
        }
    }

    // A day of the shown month in the open calendar. Blank cells have no text, so a day number
    // matches exactly one cell.
    private fun day(number: String): ViewInteraction =
        onView(allOf(withText(number), isDisplayed(), isDayCell())).inRoot(isPlatformPopup())

    private fun isDayCell(): Matcher<View> = object : TypeSafeMatcher<View>() {
        override fun describeTo(description: Description) {
            description.appendText("is a day in the calendar")
        }

        override fun matchesSafely(item: View) = (item.parent?.parent as? View)?.id == R.id.days
    }
}
