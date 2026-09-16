package com.example.vagor

import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasErrorText
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.vagor.database.AppDatabase
import com.example.vagor.entities.Excursion
import com.example.vagor.entities.Vacation
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AlertValidationInstrumentedTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db: AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "vacation_database"
    ).allowMainThreadQueries().build()

    @After
    fun tearDown() {
        db.clearAllTables()
    }

    @Test
    fun vacationAlertShowsValidationErrorForStoredInvalidDate() {
        db.clearAllTables()
        val title = "Trip-${UUID.randomUUID().toString().take(8)}"
        db.vacationDAO().insert(Vacation(title, "Hotel Vagor", "invalid-date", "09/18/2026"))
        val savedVacation = db.vacationDAO().getAllVacations().first { it.title == title }

        ActivityScenario.launch<VacationDetailActivity>(
            Intent(context, VacationDetailActivity::class.java).apply {
                putExtra("vacationId", savedVacation.id)
                putExtra("title", savedVacation.title)
                putExtra("hotel", savedVacation.hotel)
                putExtra("startDate", savedVacation.startDate)
                putExtra("endDate", savedVacation.endDate)
            }
        ).use {
            onView(withId(R.id.buttonStartAlert)).perform(click())
            onView(withId(R.id.editStartDate)).check(matches(hasErrorText(context.getString(R.string.invalid_date_format))))
        }
    }

    @Test
    fun excursionAlertShowsValidationErrorForStoredInvalidDate() {
        db.clearAllTables()
        val vacationTitle = "Trip-${UUID.randomUUID().toString().take(8)}"
        val excursionTitle = "Excursion-${UUID.randomUUID().toString().take(8)}"
        db.vacationDAO().insert(Vacation(vacationTitle, "Hotel Vagor", "09/16/2026", "09/18/2026"))
        val savedVacation = db.vacationDAO().getAllVacations().first { it.title == vacationTitle }
        db.excursionDAO().insert(Excursion(excursionTitle, "invalid-date", savedVacation.id))
        val savedExcursion = db.excursionDAO().getExcursionsForVacation(savedVacation.id).first { it.title == excursionTitle }

        ActivityScenario.launch<ExcursionDetailActivity>(
            Intent(context, ExcursionDetailActivity::class.java).apply {
                putExtra("excursionId", savedExcursion.id)
                putExtra("title", savedExcursion.title)
                putExtra("date", savedExcursion.date)
                putExtra("vacationId", savedVacation.id)
            }
        ).use {
            onView(withId(R.id.buttonExcursionAlert)).perform(click())
            onView(withId(R.id.editExcursionDate)).check(matches(hasErrorText(context.getString(R.string.invalid_date_format))))
        }
    }
}
