package com.pomobubble

import com.pomobubble.model.PomodoroPhase
import com.pomobubble.state.PomodoroStateMachine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PomodoroStateMachineTest {

    private lateinit var stateMachine: PomodoroStateMachine

    @Before
    fun setUp() {
        stateMachine = PomodoroStateMachine()
    }

    @Test
    fun initialState_isIdleAndPausedAndCollapsed() {
        val state = stateMachine.state.value
        assertEquals(PomodoroPhase.IDLE, state.phase)
        assertTrue(state.isPaused)
        assertTrue(state.isCollapsed)
        assertEquals(0, state.focusCount)
    }

    @Test
    fun togglePlayPause_whenIdle_startsFocusPhaseAndUncollapses() {
        stateMachine.togglePlayPause()

        val state = stateMachine.state.value
        assertEquals(PomodoroPhase.FOCUS, state.phase)
        assertFalse(state.isPaused)
        assertFalse(state.isCollapsed)
    }

    @Test
    fun tick_whenFocusTimerHitsZero_transitionsToWaitShortRestAndPauses() {
        stateMachine.togglePlayPause() // Start FOCUS (25 min = 1500s)

        // Simulate 1500 seconds passing
        stateMachine.onTick(1500)

        val state = stateMachine.state.value
        assertEquals(PomodoroPhase.WAIT_SHORT_REST, state.phase)
        assertEquals(5 * 60L, state.remainingSeconds)
        assertEquals(1, state.focusCount)
        assertTrue(state.isPaused)
    }

    @Test
    fun skip_fromFocus_advancesToWaitShortRestInPausedState() {
        stateMachine.togglePlayPause() // Start FOCUS

        stateMachine.skip()

        val state = stateMachine.state.value
        assertEquals(PomodoroPhase.WAIT_SHORT_REST, state.phase)
        assertEquals(1, state.focusCount)
        assertTrue(state.isPaused)
    }

    @Test
    fun fullReset_resetsToIdleState() {
        stateMachine.togglePlayPause()
        stateMachine.skip()

        stateMachine.fullReset()

        val state = stateMachine.state.value
        assertEquals(PomodoroPhase.IDLE, state.phase)
        assertEquals(0, state.focusCount)
        assertTrue(state.isPaused)
        assertTrue(state.isCollapsed)
    }
}
