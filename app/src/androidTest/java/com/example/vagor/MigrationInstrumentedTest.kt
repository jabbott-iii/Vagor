package com.example.vagor

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.vagor.database.AppDatabase
import com.example.vagor.entities.Event
import com.example.vagor.entities.Vacation
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Upgrades a database file of its own, built from the schemas exported to app/schemas; it never
// opens the app's vacation_database.
@RunWith(AndroidJUnit4::class)
class MigrationInstrumentedTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @After
    fun deleteTestDatabase() {
        context.deleteDatabase(TEST_DATABASE)
    }

    @Test
    fun version1To2KeepsEveryVacationAndEvent() {
        helper.createDatabase(TEST_DATABASE, 1).use { db ->
            db.execSQL(
                "INSERT INTO vacations (title, hotel, startDate, endDate, id) " +
                    "VALUES ('Rome', 'Hotel Roma', '07/04/2031', '07/09/2031', 3)"
            )
            db.execSQL("INSERT INTO excursions (title, date, vacationId, id) VALUES ('Colosseum', '07/05/2031', 3, 8)")
        }

        // Checks the upgraded tables against the exported version 2 schema.
        helper.runMigrationsAndValidate(TEST_DATABASE, 2, true, *AppDatabase.MIGRATIONS).close()

        // The app opens it as AppDatabase.open does, and the event has blank details and no time.
        val db = Room.databaseBuilder(context, AppDatabase::class.java, TEST_DATABASE)
            .addMigrations(*AppDatabase.MIGRATIONS)
            .allowMainThreadQueries()
            .build()
        try {
            assertEquals(
                Vacation("Rome", "Hotel Roma", "07/04/2031", "07/09/2031", 3),
                db.vacationDAO().getVacationById(3)
            )
            assertEquals(
                Event("Colosseum", "07/05/2031", 3, 8, details = "", time = null, timeZone = null),
                db.eventDAO().getEventById(8)
            )
        } finally {
            db.close()
        }
    }

    private companion object {
        const val TEST_DATABASE = "migration-test"
    }
}
