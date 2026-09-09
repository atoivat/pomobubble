package com.pomobubble.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pomobubble.model.PomodoroPhase
import com.pomobubble.model.PomodoroState
import java.util.Locale

@Composable
fun PomodoroBubbleContent(
    state: PomodoroState,
    onTogglePlayPause: () -> Unit,
    onRewind: () -> Unit,
    onSkip: () -> Unit,
    onFullReset: () -> Unit,
    onDragDelta: (dx: Float, dy: Float) -> Unit
) {
    val phaseColor = when (state.phase) {
        PomodoroPhase.FOCUS, PomodoroPhase.WAIT_FOCUS -> Color(0xFFC62828) // Deep Crimson Red
        PomodoroPhase.SHORT_REST, PomodoroPhase.WAIT_SHORT_REST -> Color(0xFF1565C0) // Sapphire Blue
        PomodoroPhase.LONG_REST, PomodoroPhase.WAIT_LONG_REST -> Color(0xFF2E7D32) // Emerald Green
        PomodoroPhase.IDLE -> Color(0xFF212121) // Dark Gray
    }

    val dragModifier = Modifier.pointerInput(Unit) {
        detectDragGestures { change, dragAmount ->
            change.consume()
            onDragDelta(dragAmount.x, dragAmount.y)
        }
    }

    Crossfade(targetState = state.isCollapsed, label = "BubbleStateTransition") { collapsed ->
        if (collapsed) {
            CollapsedBubble(
                state = state,
                backgroundColor = phaseColor,
                onTap = onTogglePlayPause,
                modifier = dragModifier
            )
        } else {
            ExpandedPill(
                state = state,
                backgroundColor = phaseColor,
                onTogglePlayPause = onTogglePlayPause,
                onRewind = onRewind,
                onSkip = onSkip,
                onFullReset = onFullReset,
                modifier = dragModifier
            )
        }
    }
}

@Composable
private fun CollapsedBubble(
    state: PomodoroState,
    backgroundColor: Color,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable { onTap() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = formatTime(state.remainingSeconds),
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExpandedPill(
    state: PomodoroState,
    backgroundColor: Color,
    onTogglePlayPause: () -> Unit,
    onRewind: () -> Unit,
    onSkip: () -> Unit,
    onFullReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Rewind Button
            Text(
                text = " < ",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onRewind() }
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Center Play/Pause Timer (Long press for Full Reset)
            Text(
                text = formatTime(state.remainingSeconds),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                textDecoration = if (state.isPaused) TextDecoration.LineThrough else TextDecoration.None,
                modifier = Modifier.combinedClickable(
                    onClick = { onTogglePlayPause() },
                    onLongClick = { onFullReset() }
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Skip Button
            Text(
                text = " > ",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onSkip() }
            )
        }

        // Bottom Progress Line (2dp white bar)
        if (state.phase != PomodoroPhase.IDLE) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color.White.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = state.progressRatio)
                        .fillMaxHeight()
                        .background(Color.White)
                )
            }
        }
    }
}

private fun formatTime(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format(Locale.US, "%02d:%02d", mins, secs)
}
