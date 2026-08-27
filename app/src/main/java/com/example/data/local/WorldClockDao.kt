package com.example.data.local

import androidx.room.*
import com.example.data.model.WorldClock
import kotlinx.coroutines.flow.Flow

@Dao
interface WorldClockDao {
    @Query("SELECT * FROM world_clocks ORDER BY cityName ASC")
    fun getAllWorldClocks(): Flow<List<WorldClock>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorldClock(clock: WorldClock): Long

    @Update
    suspend fun updateWorldClock(clock: WorldClock)

    @Delete
    suspend fun deleteWorldClock(clock: WorldClock)

    @Query("DELETE FROM world_clocks WHERE id = :id")
    suspend fun deleteWorldClockById(id: Int)

    @Query("SELECT COUNT(*) FROM world_clocks")
    suspend fun getCount(): Int
}
