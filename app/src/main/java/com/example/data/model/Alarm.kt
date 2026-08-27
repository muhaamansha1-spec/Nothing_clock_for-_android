package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val daysOfWeek: String, // Comma separated days, e.g., "Mon,Tue,Wed" or "" for one-time
    val label: String = "",
    val isEnabled: Boolean = true,
    val isVibrateEnabled: Boolean = true,
    val ringtone: String = "GLYPH RAPID"
) {
    val isRecurring: Boolean
        get() = daysOfWeek.isNotEmpty()

    fun isScheduledForDay(day: String): Boolean {
        return daysOfWeek.split(",").contains(day)
    }
}
