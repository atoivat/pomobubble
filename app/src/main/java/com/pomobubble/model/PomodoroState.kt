package com.pomobubble.model

data class PomodoroState(
    val phase: PomodoroPhase = PomodoroPhase.IDLE,
    val remainingSeconds: Long = PomodoroPhase.FOCUS.defaultDurationSeconds,
    val focusCount: Int = 0,
    val isPaused: Boolean = true,
    val isCollapsed: Boolean = true
) {
    val progressRatio: Float
        get() = when {
            focusCount >= 4 -> 1.0f
            else -> (focusCount % 4) / 4.0f
        }
}
