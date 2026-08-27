package com.example.data.repository

import com.example.data.local.AlarmDao
import com.example.data.local.WorldClockDao
import com.example.data.model.Alarm
import com.example.data.model.WorldClock
import kotlinx.coroutines.flow.Flow

class ClockRepository(
    private val alarmDao: AlarmDao,
    private val worldClockDao: WorldClockDao
) {
    val allAlarms: Flow<List<Alarm>> = alarmDao.getAllAlarms()
    val allWorldClocks: Flow<List<WorldClock>> = worldClockDao.getAllWorldClocks()

    suspend fun insertAlarm(alarm: Alarm): Long = alarmDao.insertAlarm(alarm)
    suspend fun updateAlarm(alarm: Alarm) = alarmDao.updateAlarm(alarm)
    suspend fun deleteAlarm(alarm: Alarm) = alarmDao.deleteAlarm(alarm)
    suspend fun deleteAlarmById(id: Int) = alarmDao.deleteAlarmById(id)

    suspend fun insertWorldClock(clock: WorldClock): Long = worldClockDao.insertWorldClock(clock)
    suspend fun updateWorldClock(clock: WorldClock) = worldClockDao.updateWorldClock(clock)
    suspend fun deleteWorldClock(clock: WorldClock) = worldClockDao.deleteWorldClock(clock)
    suspend fun deleteWorldClockById(id: Int) = worldClockDao.deleteWorldClockById(id)

    suspend fun prepopulateIfEmpty() {
        if (worldClockDao.getCount() == 0) {
            val defaults = listOf(
                WorldClock(cityName = "London", timezoneId = "Europe/London", country = "UK", isFavorite = true),
                WorldClock(cityName = "New York", timezoneId = "America/New_York", country = "USA", isFavorite = true),
                WorldClock(cityName = "Tokyo", timezoneId = "Asia/Tokyo", country = "Japan", isFavorite = true),
                WorldClock(cityName = "Sydney", timezoneId = "Australia/Sydney", country = "Australia", isFavorite = true),
                WorldClock(cityName = "Paris", timezoneId = "Europe/Paris", country = "France", isFavorite = true)
            )
            for (clock in defaults) {
                worldClockDao.insertWorldClock(clock)
            }
        }
    }
}
