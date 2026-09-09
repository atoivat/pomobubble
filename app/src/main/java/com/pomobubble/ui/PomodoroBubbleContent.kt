package com.pomobubble.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    onToggleCollapse: () -> Unit,
    onRewind: () -> Unit,
    onSkip: () -> Unit,
    onFullReset: () -> Unit,
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragDelta: (dx: Float, dy: Float) -> Unit
) {
    val phaseColor = when (state.phase) {
        PomodoroPhase.FOCUS, PomodoroPhase.WAIT_FOCUS -> Color(0xFFC62828) // Deep Crimson Red
        PomodoroPhase.SHORT_REST, PomodoroPhase.WAIT_SHORT_REST -> Color(0xFF1565C0) // Sapphire Blue
        PomodoroPhase.LONG_REST, PomodoroPhase.WAIT_LONG_REST -> Color(0xFF2E7D32) // Emerald Green
    }

    val dragModifier = Modifier.pointerInput(Unit) {
        detectDragGestures(
            onDragStart = { onDragStart() },
            onDragEnd = { onDragEnd() },
            onDragCancel = { onDragEnd() },
            onDrag = { change, dragAmount ->
                change.consume()
                onDragDelta(dragAmount.x, dragAmount.y)
            }
        )
    }

    Crossfade(targetState = state.isCollapsed, label = "BubbleStateTransition") { collapsed ->
        if (collapsed) {
            CollapsedBubble(
                state = state,
                backgroundColor = phaseColor,
                onTogglePlayPause = onTogglePlayPause,
                onToggleCollapse = onToggleCollapse,
                modifier = dragModifier
            )
        } else {
            ExpandedPill(
                state = state,
                backgroundColor = phaseColor,
                onTogglePlayPause = onTogglePlayPause,
                onToggleCollapse = onToggleCollapse,
                onRewind = onRewind,
                onSkip = onSkip,
                onFullReset = onFullReset,
                modifier = dragModifier
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CollapsedBubble(
    state: PomodoroState,
    backgroundColor: Color,
    onTogglePlayPause: () -> Unit,
    onToggleCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progressRatio = state.currentPhaseProgressRatio

    Box(
        modifier = modifier
            .size(58.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .combinedClickable(
                onClick = { onTogglePlayPause() },
                onLongClick = { onToggleCollapse() }
            ),
        contentAlignment = Alignment.Center
    ) {
        // Circular progress stroke
        Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
            val strokeWidth = 3.dp.toPx()
            // Track background ring
            drawCircle(
                color = Color.White.copy(alpha = 0.25f),
                style = Stroke(width = strokeWidth)
            )
            // Active progress arc
            drawArc(
                color = Color.White,
                startAngle = -90f,
                sweepAngle = 360f * progressRatio,
                useCenter = false,
                style = Stroke(width = strokeWidth)
            )
        }

        Text(
            text = formatTime(state.remainingSeconds),
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            style = TextStyle(
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                textAlign = TextAlign.Center
            ),
            textDecoration = if (state.isPaused) TextDecoration.LineThrough else TextDecoration.None
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExpandedPill(
    state: PomodoroState,
    backgroundColor: Color,
    onTogglePlayPause: () -> Unit,
    onToggleCollapse: () -> Unit,
    onRewind: () -> Unit,
    onSkip: () -> Unit,
    onFullReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(IntrinsicSize.Min)
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .combinedClickable(
                onClick = { },
                onLongClick = { onToggleCollapse() }
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Rewind Button (Long press for Full Reset)
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .combinedClickable(
                        onClick = { onRewind() },
                        onLongClick = { onFullReset() }
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "<",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        textAlign = TextAlign.Center
                    )
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Center Play/Pause Timer (Long press to collapse to circle view)
            Text(
                text = formatTime(state.remainingSeconds),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    textAlign = TextAlign.Center
                ),
                textDecoration = if (state.isPaused) TextDecoration.LineThrough else TextDecoration.None,
                modifier = Modifier.combinedClickable(
                    onClick = { onTogglePlayPause() },
                    onLongClick = { onToggleCollapse() }
                )
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Skip Button
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .combinedClickable(
                        onClick = { onSkip() },
                        onLongClick = { onFullReset() }
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = ">",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        textAlign = TextAlign.Center
                    )
                )
            }
        }

        // Bottom Progress Line (2dp white bar)
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

private fun formatTime(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format(Locale.US, "%02d:%02d", mins, secs)
}
