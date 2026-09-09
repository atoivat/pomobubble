package com.pomobubble

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pomobubble.service.PomodoroOverlayService

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PermissionScreen(
                        hasOverlayPermission = Settings.canDrawOverlays(this),
                        onRequestPermission = { requestOverlayPermission() },
                        onStartService = { startOverlayService() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // If user returned from settings and permission was granted, auto-start service
        if (Settings.canDrawOverlays(this)) {
            startOverlayService()
        }
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun startOverlayService() {
        val intent = Intent(this, PomodoroOverlayService::class.java)
        startForegroundService(intent)
    }
}

@Composable
fun PermissionScreen(
    hasOverlayPermission: Boolean,
    onRequestPermission: () -> Unit,
    onStartService: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "PomoBubble",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (!hasOverlayPermission) {
            Text(
                text = "PomoBubble requires Display Over Other Apps permission to render the floating Pomodoro widget.",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = onRequestPermission) {
                Text("Grant Overlay Permission")
            }
        } else {
            Text(
                text = "Overlay permission granted! The floating timer service is active.",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = onStartService) {
                Text("Start Floating Bubble")
            }
        }
    }
}
