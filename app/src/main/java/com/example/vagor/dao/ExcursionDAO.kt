package com.example.vagor.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.vagor.entities.Excursion

@Dao
interface ExcursionDAO {
    @Insert
    fun insert(excursion: Excursion)

    @Update
    fun update(excursion: Excursion)

    @Delete
    fun delete(excursion: Excursion)

    @Query("SELECT * FROM excursions WHERE vacationId = :vacationId")
    fun getExcursionsForVacation(vacationId: Int): List<Excursion>

    @Query("SELECT COUNT(*) FROM excursions WHERE vacationId = :vacationId")
    fun getExcursionCountForVacation(vacationId: Int): Int
}
