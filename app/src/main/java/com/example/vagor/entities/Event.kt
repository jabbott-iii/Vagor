package com.example.vagor.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// The table keeps its original name, so renaming the class needs no migration.
@Entity(tableName = "excursions")
data class Event(
    var title: String,
    var date: String,
    var vacationId: Int,
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    // Details, time and time zone were added on 2026-10-27 (database version 2). Details are
    // optional and blank when empty. The time ("HH:mm", 24-hour) is required when an event is
    // saved, but events saved before then have none until they're next edited. The time zone is an
    // IANA zone ID such as "Asia/Tokyo", stored whenever the time is.
    @ColumnInfo(defaultValue = "")
    var details: String = "",
    var time: String? = null,
    var timeZone: String? = null,
)
