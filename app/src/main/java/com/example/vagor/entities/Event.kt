package com.example.vagor.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

// The table keeps its original name, so renaming the class needs no migration.
@Entity(tableName = "excursions")
data class Event(
    var title: String,
    var date: String,
    var vacationId: Int,
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
)