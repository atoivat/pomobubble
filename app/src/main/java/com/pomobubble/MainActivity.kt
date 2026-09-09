package com.pomobubble

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pomobubble.data.AppDatabase
import com.pomobubble.data.FocusSession
import com.pomobubble.model.PomodoroPhase
import com.pomobubble.model.PomodoroState
import com.pomobubble.service.PomodoroOverlayService
import com.pomobubble.state.PomodoroStateMachine
import com.pomobubble.ui.HeatmapCalendarGrid
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private val localStateMachine = PomodoroStateMachine()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = AppDatabase.getDatabase(this)
        localStateMachine.onSessionLog = { durationMinutes, completed ->
            CoroutineScope(Dispatchers.IO).launch {
                database.focusSessionDao().insertSession(
                    FocusSession(durationMinutes = durationMinutes, completed = completed)
                )
            }
        }

        setContent {
            val context = LocalContext.current

            // Observables for DB stats & summaries
            val startOfDayMillis = remember {
                Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }

            val heatmapStartMillis = remember {
                Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -84)
                }.timeInMillis
            }

            val todayMinutes by database.focusSessionDao().getTodayTotalMinutes(startOfDayMillis).collectAsState(initial = 0)
            val todaySessionsCount by database.focusSessionDao().getTodaySessionCount(startOfDayMillis).collectAsState(initial = 0)
            val recentSessions by database.focusSessionDao().getRecentSessions().collectAsState(initial = emptyList())
            val dailySummariesList by database.focusSessionDao().getDailySummaries(heatmapStartMillis).collectAsState(initial = emptyList())

            val summariesMap = remember(dailySummariesList) {
                dailySummariesList.associate { it.dayDate to it.totalMinutes }
            }

            var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
            var isServiceRunning by remember { mutableStateOf(isOverlayServiceRunning(context)) }

            val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        hasOverlayPermission = Settings.canDrawOverlays(context)
                        isServiceRunning = isOverlayServiceRunning(context)
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            val state by localStateMachine.state.collectAsState()

            val targetColor = when (state.phase) {
                PomodoroPhase.FOCUS, PomodoroPhase.WAIT_FOCUS -> Color(0xFFC62828)
                PomodoroPhase.SHORT_REST, PomodoroPhase.WAIT_SHORT_REST -> Color(0xFF1565C0)
                PomodoroPhase.LONG_REST, PomodoroPhase.WAIT_LONG_REST -> Color(0xFF2E7D32)
            }

            val animatedBackgroundColor by animateColorAsState(
                targetValue = targetColor,
                animationSpec = tween(durationMillis = 500),
                label = "BackgroundColorAnimation"
            )

            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = animatedBackgroundColor
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Title Header
                        Text(
                            text = "PomoBubble",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 16.dp, bottom = 32.dp)
                        )

                        // Hero Timer Controls (Large Size)
                        HeroTimerSection(
                            state = state,
                            onTogglePlayPause = { localStateMachine.togglePlayPause() },
                            onRewind = { localStateMachine.rewind() },
                            onSkip = { localStateMachine.skip() },
                            onFullReset = { localStateMachine.fullReset() }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Floating Overlay Controller Button
                        OverlayControlButton(
                            hasPermission = hasOverlayPermission,
                            isRunning = isServiceRunning,
                            onRequestPermission = {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:$packageName")
                                )
                                startActivity(intent)
                            },
                            onToggleService = {
                                if (isServiceRunning) {
                                    stopService(Intent(this@MainActivity, PomodoroOverlayService::class.java))
                                    isServiceRunning = false
                                } else {
                                    startForegroundService(Intent(this@MainActivity, PomodoroOverlayService::class.java))
                                    isServiceRunning = true
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(40.dp))

                        // Scroll Section 1: Today's Focus Stats
                        TodayStatsCard(
                            todayMinutes = todayMinutes ?: 0,
                            todaySessionsCount = todaySessionsCount
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Recent Sessions List
                        if (recentSessions.isNotEmpty()) {
                            RecentSessionsCard(recentSessions = recentSessions)
                            Spacer(modifier = Modifier.height(32.dp))
                        }

                        // Scroll Section 2: GitHub-Style Contribution Heatmap
                        HeatmapCalendarGrid(
                            dailySummaries = summariesMap,
                            weeksCount = 12,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }


    private fun isOverlayServiceRunning(context: Context): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (PomodoroOverlayService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeroTimerSection(
    state: PomodoroState,
    onTogglePlayPause: () -> Unit,
    onRewind: () -> Unit,
    onSkip: () -> Unit,
    onFullReset: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Rewind Button (Long press for reset)
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .combinedClickable(
                    onClick = { onRewind() },
                    onLongClick = { onFullReset() }
                )
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "<",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Large 52sp Hero Timer
        Text(
            text = formatTime(state.remainingSeconds),
            color = Color.White,
            fontSize = 52.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            style = TextStyle(
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                textAlign = TextAlign.Center
            ),
            textDecoration = if (state.isPaused) TextDecoration.LineThrough else TextDecoration.None,
            modifier = Modifier.clickable { onTogglePlayPause() }
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Skip Button (Long press for reset)
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .combinedClickable(
                    onClick = { onSkip() },
                    onLongClick = { onFullReset() }
                )
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = ">",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
            )
        }
    }
}

@Composable
private fun OverlayControlButton(
    hasPermission: Boolean,
    isRunning: Boolean,
    onRequestPermission: () -> Unit,
    onToggleService: () -> Unit
) {
    if (!hasPermission) {
        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.25f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text("Grant Overlay Permission", color = Color.White)
        }
    } else {
        Button(
            onClick = onToggleService,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRunning) Color.White.copy(alpha = 0.35f) else Color.White
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text(
                text = if (isRunning) "Dismiss Floating Bubble" else "Launch Floating Bubble",
                color = if (isRunning) Color.White else Color(0xFFC62828),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TodayStatsCard(
    todayMinutes: Int,
    todaySessionsCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .padding(20.dp)
    ) {
        Text(
            text = "Today's Summary",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Focus Time",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
                val hours = todayMinutes / 60
                val mins = todayMinutes % 60
                val timeStr = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
                Text(
                    text = timeStr,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column {
                Text(
                    text = "Completed Sessions",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
                Text(
                    text = "$todaySessionsCount sessions",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun RecentSessionsCard(recentSessions: List<FocusSession>) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.US) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .padding(20.dp)
    ) {
        Text(
            text = "Recent Finished Sessions",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        recentSessions.take(5).forEach { session ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Focus Session (${session.durationMinutes}m)",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp
                )
                Text(
                    text = timeFormat.format(Date(session.timestampMillis)),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
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
