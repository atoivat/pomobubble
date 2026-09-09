package com.pomobubble.model

data class PomodoroState(
    val phase: PomodoroPhase = PomodoroPhase.FOCUS,
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

    val currentPhaseProgressRatio: Float
        get() {
            val total = phase.defaultDurationSeconds
            if (total <= 0) return 0.0f
            val elapsed = (total - remainingSeconds).coerceAtLeast(0)
            return (elapsed.toFloat() / total.toFloat()).coerceIn(0.0f, 1.0f)
        }
}
