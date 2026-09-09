package com.pomobubble.model

enum class PomodoroPhase(val defaultDurationSeconds: Long) {
    FOCUS(25 * 60L),
    WAIT_SHORT_REST(5 * 60L),
    SHORT_REST(5 * 60L),
    WAIT_LONG_REST(15 * 60L),
    LONG_REST(15 * 60L),
    WAIT_FOCUS(25 * 60L);

    val isFocusPhase: Boolean
        get() = this == FOCUS || this == WAIT_FOCUS

    val isRestPhase: Boolean
        get() = this == SHORT_REST || this == WAIT_SHORT_REST || this == LONG_REST || this == WAIT_LONG_REST
}
