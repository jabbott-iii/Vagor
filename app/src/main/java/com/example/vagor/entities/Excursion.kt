package com.example.vagor.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "excursions")
class Excursion(var title: String?, var date: String?, var vacationId: Int) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}