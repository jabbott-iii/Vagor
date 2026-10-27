package com.example.vagor.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vacations")
data class Vacation(
    var title: String,
    // Trip details replaced the hotel field on 2026-10-26. The column keeps its old name, so
    // renaming it needed no migration. Blank when the user left it empty.
    @ColumnInfo(name = "hotel")
    var details: String,
    var startDate: String,
    var endDate: String,
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
)