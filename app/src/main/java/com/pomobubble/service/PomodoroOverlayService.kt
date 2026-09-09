package com.pomobubble.service

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.pomobubble.MainActivity
import com.pomobubble.audio.SoundManager
import com.pomobubble.data.AppDatabase
import com.pomobubble.data.FocusSession
import com.pomobubble.model.PomodoroPhase
import com.pomobubble.state.PomodoroStateMachine
import com.pomobubble.ui.PomodoroBubbleContent
import kotlinx.coroutines.*

class PomodoroOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var composeView: ComposeView
    private lateinit var soundManager: SoundManager
    private lateinit var database: AppDatabase

    private val overlayLifecycleOwner = OverlayLifecycleOwner()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    val stateMachine = PomodoroStateMachine()
    private var tickerJob: Job? = null
    private var previousPhase: PomodoroPhase = PomodoroPhase.IDLE

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        soundManager = SoundManager(this)
        database = AppDatabase.getDatabase(this)

        overlayLifecycleOwner.onCreate()
        overlayLifecycleOwner.onStart()
        overlayLifecycleOwner.onResume()

        startForegroundNotification()
        setupOverlayView()
        observeStateChanges()
        startTickerLoop()
    }

    private fun startForegroundNotification() {
        val channelId = "pomodoro_overlay_channel"
        val channelName = "Pomodoro Timer Overlay"

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("PomoBubble Active")
            .setContentText("Floating Pomodoro timer is running")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun setupOverlayView() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(overlayLifecycleOwner)
            setViewTreeSavedStateRegistryOwner(overlayLifecycleOwner)
            setViewTreeViewModelStoreOwner(overlayLifecycleOwner)

            setContent {
                val state by stateMachine.state.collectAsState()
                PomodoroBubbleContent(
                    state = state,
                    onTogglePlayPause = { stateMachine.togglePlayPause() },
                    onRewind = { stateMachine.rewind() },
                    onSkip = { stateMachine.skip() },
                    onFullReset = { stateMachine.fullReset() },
                    onDragDelta = { dx, dy ->
                        params.x += dx.toInt()
                        params.y += dy.toInt()
                        windowManager.updateViewLayout(composeView, params)
                    }
                )
            }
        }

        windowManager.addView(composeView, params)
    }

    private fun observeStateChanges() {
        serviceScope.launch {
            stateMachine.state.collect { state ->
                val currentPhase = state.phase
                if (previousPhase != currentPhase) {
                    // Trigger alert when transitioning into a wait state (timer completed)
                    if (currentPhase == PomodoroPhase.WAIT_SHORT_REST ||
                        currentPhase == PomodoroPhase.WAIT_LONG_REST ||
                        currentPhase == PomodoroPhase.WAIT_FOCUS
                    ) {
                        soundManager.playPhaseCompleteSound()

                        // Log completed Focus session in Room DB
                        if (previousPhase == PomodoroPhase.FOCUS) {
                            launch(Dispatchers.IO) {
                                database.focusSessionDao().insertSession(
                                    FocusSession(durationMinutes = 25, completed = true)
                                )
                            }
                        }
                    }
                    previousPhase = currentPhase
                }
            }
        }
    }

    private fun startTickerLoop() {
        tickerJob = serviceScope.launch {
            var lastTime = SystemClock.elapsedRealtime()
            while (isActive) {
                delay(1000)
                val now = SystemClock.elapsedRealtime()
                val deltaSeconds = (now - lastTime) / 1000
                if (deltaSeconds >= 1) {
                    stateMachine.onTick(deltaSeconds)
                    lastTime = now
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tickerJob?.cancel()
        serviceScope.cancel()
        if (::composeView.isInitialized) {
            windowManager.removeView(composeView)
        }
        overlayLifecycleOwner.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}
