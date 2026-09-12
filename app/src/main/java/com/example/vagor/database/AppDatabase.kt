package com.example.vagor.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.vagor.dao.ExcursionDAO
import com.example.vagor.dao.VacationDAO
import com.example.vagor.entities.Excursion
import com.example.vagor.entities.Vacation

@Database(entities = [Vacation::class, Excursion::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vacationDAO(): VacationDAO?
    abstract fun excursionDAO(): ExcursionDAO?
}