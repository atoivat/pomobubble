package com.pomobubble.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HeatmapCalendarGrid(
    dailySummaries: Map<String, Int>,
    weeksCount: Int = 12,
    modifier: Modifier = Modifier
) {
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    // Compute grid dates for the last N weeks (7 days per week)
    val today = calendar.time
    val currentDayOfWeek = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Monday = 0, Sunday = 6

    val totalDays = weeksCount * 7
    calendar.add(Calendar.DAY_OF_YEAR, -(totalDays - 1 - currentDayOfWeek))

    val dayGrid = List(weeksCount) { _ ->
        List(7) { _ ->
            val dateStr = dateFormat.format(calendar.time)
            val minutes = dailySummaries[dateStr] ?: 0
            val isFuture = calendar.time.after(today)
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            HeatmapDay(dateStr, minutes, isFuture)
        }
    }

    val dayLabels = listOf("M", "", "W", "", "F", "", "S")

    Column(modifier = modifier) {
        Text(
            text = "Focus Activity (Last 12 Weeks)",
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Day of week labels on left
            Column(
                modifier = Modifier.padding(end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                dayLabels.forEach { label ->
                    Box(
                        modifier = Modifier.size(width = 14.dp, height = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Heatmap columns (weeks)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                dayGrid.forEach { week ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        week.forEach { day ->
                            HeatmapSquare(day = day)
                        }
                    }
                }
            }
        }
    }
}

private data class HeatmapDay(
    val dateStr: String,
    val minutes: Int,
    val isFuture: Boolean
)

@Composable
private fun HeatmapSquare(day: HeatmapDay) {
    val maxMinutes = 180f // 3 hours = 100% white opacity
    val alpha = if (day.minutes > 0) {
        (day.minutes.toFloat() / maxMinutes).coerceIn(0.2f, 1.0f)
    } else {
        0.0f
    }

    val boxColor = if (day.isFuture) {
        Color.Transparent
    } else {
        Color.White.copy(alpha = alpha)
    }

    val borderColor = if (day.isFuture) {
        Color.Transparent
    } else {
        Color.White.copy(alpha = 0.18f)
    }

    Box(
        modifier = Modifier
            .size(14.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(boxColor)
            .border(1.dp, borderColor, RoundedCornerShape(3.dp))
    )
}
