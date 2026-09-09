package com.pomobubble.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {

    @Insert
    suspend fun insertSession(session: FocusSession): Long

    @Query("SELECT * FROM focus_sessions ORDER BY timestampMillis DESC")
    fun getAllSessions(): Flow<List<FocusSession>>

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE completed = 1")
    fun getTotalCompletedCount(): Flow<Int>
}
