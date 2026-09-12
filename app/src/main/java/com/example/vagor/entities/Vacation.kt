package com.example.vagor.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vacations")
class Vacation(
    var title: String?,
    var hotel: String?,
    var startDate: String?,
    var endDate: String?
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}