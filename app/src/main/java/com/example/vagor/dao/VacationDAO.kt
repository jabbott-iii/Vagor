package com.example.vagor.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.vagor.entities.Vacation

@Dao
interface VacationDAO {
    @Insert
    fun insert(vacation: Vacation?)

    @Update
    fun update(vacation: Vacation?)

    @Delete
    fun delete(vacation: Vacation?)

    @get:Query("SELECT * FROM vacations")
    val allVacations: MutableList<Vacation?>?

    @Query("SELECT * FROM vacations WHERE id = :vacationId")
    fun getVacationById(vacationId: Int): Vacation?
}