package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "world_clocks")
data class WorldClock(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cityName: String,
    val timezoneId: String,
    val country: String = "",
    val isFavorite: Boolean = true
)
