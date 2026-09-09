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
    fun initialState_isFocusPhasePausedAndCollapsed() {
        val state = stateMachine.state.value
        assertEquals(PomodoroPhase.FOCUS, state.phase)
        assertEquals(25 * 60L, state.remainingSeconds)
        assertTrue(state.isPaused)
        assertTrue(state.isCollapsed)
        assertEquals(0, state.focusCount)
    }

    @Test
    fun singleClickOnCollapsedBubble_togglesPauseWithoutExpanding() {
        stateMachine.togglePlayPause()

        val state = stateMachine.state.value
        assertEquals(PomodoroPhase.FOCUS, state.phase)
        assertFalse(state.isPaused)
        assertTrue(state.isCollapsed) // MUST remain collapsed!
    }

    @Test
    fun toggleCollapse_expandsOrCollapsesWithoutChangingTimerOrPauseState() {
        stateMachine.togglePlayPause() // Start timer
        stateMachine.onTick(300) // 300s elapsed -> 1200s remaining

        // Expand to pill view
        stateMachine.toggleCollapse()
        var state = stateMachine.state.value
        assertFalse(state.isCollapsed)
        assertEquals(1200L, state.remainingSeconds)
        assertFalse(state.isPaused)

        // Collapse back to bubble view
        stateMachine.toggleCollapse()
        state = stateMachine.state.value
        assertTrue(state.isCollapsed)
        assertEquals(1200L, state.remainingSeconds)
        assertFalse(state.isPaused)
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
    fun fullReset_resetsToFocusState() {
        stateMachine.togglePlayPause()
        stateMachine.skip()

        stateMachine.fullReset()

        val state = stateMachine.state.value
        assertEquals(PomodoroPhase.FOCUS, state.phase)
        assertEquals(25 * 60L, state.remainingSeconds)
        assertEquals(0, state.focusCount)
        assertTrue(state.isPaused)
        assertTrue(state.isCollapsed)
    }
}
