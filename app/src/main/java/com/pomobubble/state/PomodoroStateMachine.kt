package com.pomobubble.state

import com.pomobubble.model.PomodoroPhase
import com.pomobubble.model.PomodoroState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PomodoroStateMachine {

    private val _state = MutableStateFlow(PomodoroState())
    val state: StateFlow<PomodoroState> = _state.asStateFlow()

    fun togglePlayPause() {
        _state.update { current ->
            val nextPhase = when (current.phase) {
                PomodoroPhase.WAIT_FOCUS -> PomodoroPhase.FOCUS
                PomodoroPhase.WAIT_SHORT_REST -> PomodoroPhase.SHORT_REST
                PomodoroPhase.WAIT_LONG_REST -> PomodoroPhase.LONG_REST
                else -> current.phase
            }
            val isNewPhase = current.phase != nextPhase
            current.copy(
                phase = nextPhase,
                remainingSeconds = if (isNewPhase) nextPhase.defaultDurationSeconds else current.remainingSeconds,
                isPaused = if (isNewPhase) false else !current.isPaused
            )
        }
    }

    fun toggleCollapse() {
        _state.update { current ->
            current.copy(isCollapsed = !current.isCollapsed)
        }
    }

    fun onTick(elapsedSeconds: Long) {
        _state.update { current ->
            if (current.isPaused) return@update current

            val newRemaining = current.remainingSeconds - elapsedSeconds
            if (newRemaining <= 0) {
                when (current.phase) {
                    PomodoroPhase.FOCUS -> {
                        val newCount = current.focusCount + 1
                        if (newCount >= 4) {
                            current.copy(
                                phase = PomodoroPhase.WAIT_LONG_REST,
                                remainingSeconds = PomodoroPhase.LONG_REST.defaultDurationSeconds,
                                focusCount = newCount,
                                isPaused = true
                            )
                        } else {
                            current.copy(
                                phase = PomodoroPhase.WAIT_SHORT_REST,
                                remainingSeconds = PomodoroPhase.SHORT_REST.defaultDurationSeconds,
                                focusCount = newCount,
                                isPaused = true
                            )
                        }
                    }
                    PomodoroPhase.SHORT_REST -> {
                        current.copy(
                            phase = PomodoroPhase.WAIT_FOCUS,
                            remainingSeconds = PomodoroPhase.FOCUS.defaultDurationSeconds,
                            isPaused = true
                        )
                    }
                    PomodoroPhase.LONG_REST -> {
                        current.copy(
                            phase = PomodoroPhase.WAIT_FOCUS,
                            remainingSeconds = PomodoroPhase.FOCUS.defaultDurationSeconds,
                            focusCount = 0,
                            isPaused = true
                        )
                    }
                    else -> current
                }
            } else {
                current.copy(remainingSeconds = newRemaining)
            }
        }
    }

    fun skip() {
        _state.update { current ->
            val (nextPhase, nextCount) = when (current.phase) {
                PomodoroPhase.FOCUS, PomodoroPhase.WAIT_FOCUS -> {
                    val count = current.focusCount + 1
                    if (count >= 4) {
                        PomodoroPhase.WAIT_LONG_REST to count
                    } else {
                        PomodoroPhase.WAIT_SHORT_REST to count
                    }
                }
                PomodoroPhase.SHORT_REST, PomodoroPhase.WAIT_SHORT_REST -> {
                    PomodoroPhase.WAIT_FOCUS to current.focusCount
                }
                PomodoroPhase.LONG_REST, PomodoroPhase.WAIT_LONG_REST -> {
                    PomodoroPhase.WAIT_FOCUS to 0
                }
            }
            current.copy(
                phase = nextPhase,
                remainingSeconds = nextPhase.defaultDurationSeconds,
                focusCount = nextCount,
                isPaused = true
            )
        }
    }

    fun rewind() {
        _state.update { current ->
            val defaultDuration = current.phase.defaultDurationSeconds
            if (!current.isPaused || current.remainingSeconds < defaultDuration) {
                val pausedWaitPhase = when (current.phase) {
                    PomodoroPhase.FOCUS -> PomodoroPhase.WAIT_FOCUS
                    PomodoroPhase.SHORT_REST -> PomodoroPhase.WAIT_SHORT_REST
                    PomodoroPhase.LONG_REST -> PomodoroPhase.WAIT_LONG_REST
                    else -> current.phase
                }
                current.copy(
                    phase = pausedWaitPhase,
                    remainingSeconds = defaultDuration,
                    isPaused = true
                )
            } else {
                val (prevPhase, prevCount) = when (current.phase) {
                    PomodoroPhase.WAIT_FOCUS, PomodoroPhase.FOCUS -> {
                        if (current.focusCount > 0) {
                            PomodoroPhase.WAIT_SHORT_REST to (current.focusCount - 1)
                        } else {
                            PomodoroPhase.WAIT_LONG_REST to 0
                        }
                    }
                    PomodoroPhase.WAIT_SHORT_REST, PomodoroPhase.SHORT_REST -> {
                        PomodoroPhase.WAIT_FOCUS to current.focusCount
                    }
                    PomodoroPhase.WAIT_LONG_REST, PomodoroPhase.LONG_REST -> {
                        PomodoroPhase.WAIT_FOCUS to current.focusCount
                    }
                }
                current.copy(
                    phase = prevPhase,
                    remainingSeconds = prevPhase.defaultDurationSeconds,
                    focusCount = prevCount,
                    isPaused = true
                )
            }
        }
    }

    fun fullReset() {
        _state.value = PomodoroState(
            phase = PomodoroPhase.FOCUS,
            remainingSeconds = PomodoroPhase.FOCUS.defaultDurationSeconds,
            focusCount = 0,
            isPaused = true,
            isCollapsed = true
        )
    }

    fun onInactivityTimeout() {
        _state.update { current ->
            if (!current.isCollapsed) {
                current.copy(isCollapsed = true)
            } else {
                current
            }
        }
    }
}
