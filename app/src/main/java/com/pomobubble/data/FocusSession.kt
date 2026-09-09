package com.pomobubble.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_sessions")
data class FocusSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestampMillis: Long = System.currentTimeMillis(),
    val durationMinutes: Int = 25,
    val completed: Boolean = true
)
