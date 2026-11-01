package com.example.vagor

import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.LocaleList
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.children
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
import kotlin.math.abs

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
            onView(withId(R.id.editStartDate)).perform(setDate("03/10/2030"))
            onView(withId(R.id.editStartDate)).perform(click())
            day("10").check(matches(isSelected()))

            day("20").perform(click())

            onView(withId(R.id.editStartDate)).check(matches(withDate("03/20/2030")))
            onView(withId(R.id.monthTitle)).check(doesNotExist())
        }
    }

    @Test
    fun backClosesTheCalendarWithoutChangingTheDate() {
        ActivityScenario.launch(VacationDetailActivity::class.java).use {
            onView(withId(R.id.editStartDate)).perform(setDate("03/10/2030"))
            onView(withId(R.id.editStartDate)).perform(click())
            onView(withId(R.id.monthTitle)).inRoot(isPlatformPopup()).check(matches(isDisplayed()))

            pressBack()

            onView(withId(R.id.monthTitle)).check(doesNotExist())
            onView(withId(R.id.editStartDate)).check(matches(withDate("03/10/2030")))
        }
    }

    @Test
    fun monthButtonsMoveBetweenMonths() {
        ActivityScenario.launch(VacationDetailActivity::class.java).use {
            onView(withId(R.id.editStartDate)).perform(setDate("01/31/2030"))
            onView(withId(R.id.editStartDate)).perform(click())

            onView(withId(R.id.nextMonth)).inRoot(isPlatformPopup()).perform(click())
            // February 2030 has 28 days.
            day("28").check(matches(isEnabled()))
            onView(allOf(withText("29"), isDayCell())).inRoot(isPlatformPopup()).check(doesNotExist())
            day("14").perform(click())

            onView(withId(R.id.editStartDate)).check(matches(withDate("02/14/2030")))
        }
    }

    @Test
    fun endDateCalendarStartsAtTheStartDate() {
        ActivityScenario.launch(VacationDetailActivity::class.java).use {
            onView(withId(R.id.editStartDate)).perform(setDate("03/10/2030"))
            onView(withId(R.id.editEndDate)).perform(click())

            day("10").check(matches(allOf(isSelected(), isEnabled())))
            day("9").check(matches(not(isEnabled())))
            onView(withId(R.id.previousMonth)).inRoot(isPlatformPopup()).check(matches(not(isDisplayed())))
        }
    }

    @Test
    fun emptyEndDateOpensAtAPastStartDate() {
        ActivityScenario.launch(VacationDetailActivity::class.java).use {
            onView(withId(R.id.editStartDate)).perform(setDate("01/15/2020"))
            onView(withId(R.id.editEndDate)).perform(click())
            day("15").check(matches(isSelected()))

            day("20").perform(click())

            onView(withId(R.id.editEndDate)).check(matches(withDate("01/20/2020")))
        }
    }

    @Test
    fun eventCalendarOffersOnlyTheVacationsDays() {
        val vacation =
            Vacation("Trip-${UUID.randomUUID().toString().take(8)}", "Hotel Vagor", "07/04/2031", "07/09/2031")
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
            onView(withId(R.id.editEventDate)).check(matches(withDate("07/06/2031")))
        }
    }

    // Android's smallest touch target is 48dp each way, at every font size. On a 360dp-wide phone
    // the field is too narrow for seven of those, so the calendar is wider than its field and
    // centred under it. Narrower screens (CI's emulator is 320dp wide) can't fit them, and the
    // calendar is as wide as allowed.
    @Test
    fun daysAreAtLeast48dpEachWayWhereTheScreenAllows() {
        val density = context.resources.displayMetrics.density
        val touchTarget = (48 * density).toInt()
        ActivityScenario.launch(VacationDetailActivity::class.java).use { scenario ->
            var screenDp = 0f
            var screenWidth = 0
            var fieldWidth = 0
            val fieldLocation = IntArray(2)
            scenario.onActivity {
                val screen = Rect()
                it.window.decorView.getWindowVisibleDisplayFrame(screen)
                screenWidth = screen.width()
                screenDp = screenWidth / density
                val field = it.findViewById<View>(R.id.editStartDate)
                fieldWidth = field.width
                field.getLocationOnScreen(fieldLocation)
            }
            onView(withId(R.id.editStartDate)).perform(click())

            onView(withId(R.id.days)).inRoot(isPlatformPopup()).check { view, _ ->
                val days = (view as ViewGroup).children.flatMap { (it as ViewGroup).children }.toList()
                assertEquals(CalendarMonth.CELLS, days.size)
                days.forEach { day ->
                    assertTrue("A day is ${day.height / density}dp tall", day.height >= touchTarget)
                    if (screenDp >= 360) assertTrue("A day is ${day.width / density}dp wide", day.width >= touchTarget)
                }
                val popup = view.rootView
                if (screenDp < 360) {
                    val margin = context.resources.getDimensionPixelSize(R.dimen.calendar_dropdown_screen_margin)
                    assertEquals(screenWidth - 2 * margin, popup.width)
                    assertTrue(popup.width > fieldWidth)
                }
                if (popup.width > fieldWidth) {
                    val popupLocation = IntArray(2)
                    popup.getLocationOnScreen(popupLocation)
                    val centredLeft = fieldLocation[0] - (popup.width - fieldWidth) / 2
                    assertTrue("Not centred under the field", abs(popupLocation[0] - centredLeft) <= 1)
                }
            }
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
        DayCircle(blue, strokeWidth = 4f, maxDiameter = 80f, clearance = 3f).apply {
            setBounds(0, 0, 60, size)
        }.draw(Canvas(narrow))
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
                onView(withId(R.id.editStartDate)).perform(setDate("03/10/2030"))
                onView(withId(R.id.editStartDate)).perform(click())

                onView(withId(R.id.monthTitle)).inRoot(isPlatformPopup()).check { view, _ ->
                    val title = (view as TextView).text.toString()
                    assertTrue("Month title \"$title\" should show 2030", title.contains("2030"))
                }
                day("20").perform(click())

                onView(withId(R.id.editStartDate)).check(matches(withDate("03/20/2030")))
                onView(withId(R.id.editStartDate)).check { view, _ ->
                    val shown = (view as TextView).text.toString()
                    assertTrue("Field \"$shown\" should show 2030", shown.contains("2030"))
                }
            }
        } finally {
            setAppLanguage(null)
            Locale.setDefault(original)
        }
    }

    // Dates are shown the way the phone writes them, and still stored as MM/dd/yyyy.
    @Test
    fun datesAreShownInThePhonesFormat() {
        class Format(val language: String, val picked: String, val range: String, val beforeJulianSwitch: String)
        val formats = listOf(
            Format("en-US", "Mar 20, 2030", "Jul 4, 2031 – Jul 9, 2031", "Oct 10, 1582"),
            Format("en-GB", "20 Mar 2030", "4 Jul 2031 – 9 Jul 2031", "10 Oct 1582")
        )
        for (format in formats) {
            val language = format.language
            setAppLanguage(language)
            try {
                ActivityScenario.launch(VacationDetailActivity::class.java).use { scenario ->
                    scenario.onActivity {
                        assertEquals(Locale.forLanguageTag(language), it.displayLocale())
                        val trip = Vacation("Trip", "", "07/04/2031", "07/09/2031")
                        assertEquals(format.range, it.vacationDateRange(trip))
                        // Gregorian like the stored dates, where ICU would switch to Julian.
                        assertEquals(format.beforeJulianSwitch, it.dateText("10/10/1582"))
                        // A date saved before dates were checked strictly is shown as it was saved.
                        assertEquals("7/5/2031", it.dateText("7/5/2031"))
                    }
                    onView(withId(R.id.editStartDate)).perform(setDate("03/10/2030"))
                    onView(withId(R.id.editStartDate)).perform(click())
                    day("20").perform(click())

                    onView(
                        withId(R.id.editStartDate)
                    ).check(matches(allOf(withText(format.picked), withDate("03/20/2030"))))
                }
            } finally {
                setAppLanguage(null)
            }
        }
    }

    // The field keeps its date when the screen is recreated, as it is on rotation.
    @Test
    fun aPickedDateSurvivesRotation() {
        ActivityScenario.launch(VacationDetailActivity::class.java).use { scenario ->
            onView(withId(R.id.editStartDate)).perform(setDate("03/10/2030"))
            onView(withId(R.id.editStartDate)).perform(click())
            day("20").perform(click())

            scenario.recreate()

            onView(withId(R.id.editStartDate)).check { view, _ ->
                assertEquals("03/20/2030", (view as DateField).date)
                assertEquals(view.context.dateText("03/20/2030"), view.text.toString())
            }
        }
    }

    // The app's own language, as set in Android's per-app language settings; null goes back to the
    // phone's language. From Android 13 the system keeps it; before that, AppCompat does.
    private fun setAppLanguage(languageTag: String?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java).applicationLocales =
                if (languageTag == null) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(languageTag)
        } else {
            val locales = if (languageTag ==
                null
            ) {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(languageTag)
            }
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                AppCompatDelegate.setApplicationLocales(locales)
            }
        }
    }

    @Test
    fun dateFieldIsReadAsADropDownListThatCantBeTypedInto() {
        ActivityScenario.launch(VacationDetailActivity::class.java).use {
            onView(withId(R.id.editStartDate)).perform(setDate("03/10/2030"))
            onView(withId(R.id.editStartDate)).check { view, _ ->
                val info = view.createAccessibilityNodeInfo()
                assertEquals(Spinner::class.java.name, info.className)
                assertFalse(info.isEditable)
                assertFalse(info.actionList.contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_TEXT))
                assertEquals(
                    "${context.getString(R.string.start_date)}, ${view.context.dateText("03/10/2030")}",
                    info.contentDescription
                )
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
