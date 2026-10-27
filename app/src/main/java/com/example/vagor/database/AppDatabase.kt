package com.example.vagor.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.vagor.dao.EventDAO
import com.example.vagor.dao.VacationDAO
import com.example.vagor.entities.Event
import com.example.vagor.entities.Vacation

// Each version's schema is exported to app/schemas. A schema change needs a new version and a
// Migration added to MIGRATIONS; the database holds the user's only copy of their trips.
@Database(entities = [Vacation::class, Event::class], version = 2, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vacationDAO(): VacationDAO
    abstract fun eventDAO(): EventDAO

    companion object {
        const val NAME = "vacation_database"

        // Version 2 (2026-10-27): events get details, a time and a time zone. Existing events get
        // blank details and no time.
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `excursions` ADD COLUMN `details` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `excursions` ADD COLUMN `time` TEXT")
                db.execSQL("ALTER TABLE `excursions` ADD COLUMN `timeZone` TEXT")
            }
        }

        val MIGRATIONS = arrayOf(MIGRATION_1_2)

        // A new instance of the app's database, with every migration. Each screen and receiver
        // still opens its own; this only keeps them from missing a migration.
        fun open(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, NAME)
                .addMigrations(*MIGRATIONS)
                .allowMainThreadQueries()
                .build()
    }
}
