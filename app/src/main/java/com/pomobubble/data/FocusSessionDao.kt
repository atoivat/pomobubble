package com.pomobubble.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class DailyFocusSummary(
    val dayDate: String,
    val totalMinutes: Int
)

@Dao
interface FocusSessionDao {

    @Insert
    suspend fun insertSession(session: FocusSession): Long

    @Query("SELECT * FROM focus_sessions ORDER BY timestampMillis DESC LIMIT 10")
    fun getRecentSessions(): Flow<List<FocusSession>>

    @Query("SELECT SUM(durationMinutes) FROM focus_sessions WHERE completed = 1 AND timestampMillis >= :startOfDayMillis")
    fun getTodayTotalMinutes(startOfDayMillis: Long): Flow<Int?>

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE completed = 1 AND timestampMillis >= :startOfDayMillis")
    fun getTodaySessionCount(startOfDayMillis: Long): Flow<Int>

    @Query("""
        SELECT date(timestampMillis / 1000, 'unixepoch', 'localtime') as dayDate,
               SUM(durationMinutes) as totalMinutes
        FROM focus_sessions
        WHERE completed = 1 AND timestampMillis >= :startMillis
        GROUP BY dayDate
    """)
    fun getDailySummaries(startMillis: Long): Flow<List<DailyFocusSummary>>
}
