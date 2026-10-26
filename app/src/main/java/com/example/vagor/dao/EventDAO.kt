package com.example.vagor.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.vagor.entities.Event

@Dao
interface EventDAO {
    @Insert
    fun insert(event: Event): Long

    @Update
    fun update(event: Event)

    @Delete
    fun delete(event: Event)

    @Query("SELECT * FROM excursions")
    fun getAllEvents(): List<Event>

    @Query("SELECT * FROM excursions WHERE vacationId = :vacationId")
    fun getEventsForVacation(vacationId: Int): List<Event>

    @Query("SELECT * FROM excursions WHERE id = :eventId")
    fun getEventById(eventId: Int): Event?

    @Query("SELECT COUNT(*) FROM excursions WHERE vacationId = :vacationId")
    fun getEventCountForVacation(vacationId: Int): Int
}
