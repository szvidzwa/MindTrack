package com.example.mindtrack.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import java.io.File
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.compose.ui.geometry.Size
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: MoodViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MoodScreen(viewModel)
            }
        }
    }
}

@Composable
fun MoodScreen(viewModel: MoodViewModel) {
    val context = LocalContext.current

    var mood by remember { mutableIntStateOf(3) }
    var note by remember { mutableStateOf("") }

    val moods by viewModel.moodList.collectAsState()
    val weeklyAverage by viewModel.weeklyAverageMood.collectAsState()
    val dailySeries by viewModel.dailySeries.collectAsState()

    //  Auto reset weekly when app opens
    LaunchedEffect(Unit) {
        viewModel.autoResetIfNewWeek()
    }

    Column(modifier = Modifier.padding(16.dp)) {

        //  App header
        Text("MindTrack", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Helping you keep track of your mood",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text("How are you feeling today?", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = mood.toFloat(),
            onValueChange = { mood = it.toInt() },
            valueRange = 1f..5f,
            steps = 3
        )
        Text("Mood level: $mood")

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Optional note") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            viewModel.addMood(mood, note)
            note = ""
        }) {
            Text("Save Mood")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Weekly Average Mood: ${"%.1f".format(weeklyAverage)}",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text("Daily Mood (Last 7 Days)", style = MaterialTheme.typography.titleMedium)
        DailyMoodBarChart(dailySeries)

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { viewModel.resetWeekManual() }) {
                Text("Reset Week")
            }
            Button(onClick = { exportMoodsCsvAndShare(context, moods) }) {
                Text("Export/Share CSV")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Mood History", style = MaterialTheme.typography.titleMedium)

        LazyColumn {
            items(moods) { entry ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {

                        Text(
                            text = "Mood: ${entry.mood}",
                            style = MaterialTheme.typography.titleMedium,
                            color = moodColor(entry.mood.toDouble())
                        )

                        if (entry.note.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = entry.note,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = formatDate(entry.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

    }
}


private fun moodColor(mood: Double): Color {
    return when {
        mood <= 0.0 -> Color(0xFFBDBDBD) // gray for "no data"
        mood <= 2.0 -> Color(0xFFE53935) // red
        mood < 4.0 -> Color(0xFFFB8C00)  // orange
        else -> Color(0xFF43A047)        // green
    }
}


@Composable
fun DailyMoodBarChart(series: List<DayMood>) {
    val yMin = 0f
    val yMax = 5f

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(vertical = 8.dp)
    ) {
        if (series.isEmpty()) return@Canvas

        val leftPad = 60f
        val bottomPad = 40f
        val topPad = 20f
        val rightPad = 20f

        val chartW = size.width - leftPad - rightPad
        val chartH = size.height - topPad - bottomPad

        // Y grid lines (0–5)
        for (y in 0..5) {
            val yPos = topPad + chartH * (1f - (y - yMin) / (yMax - yMin))
            drawLine(
                color = Color.LightGray,
                start = Offset(leftPad, yPos),
                end = Offset(size.width - rightPad, yPos),
                strokeWidth = 2f
            )
        }

        val barWidth = chartW / (series.size * 1.8f)

        series.forEachIndexed { index, dm ->
            val mood = dm.avgMood.coerceIn(0.0, 5.0)
            val barHeight = chartH * (mood.toFloat() / yMax) // 0 => zero bar

            val x = leftPad + index * (barWidth * 1.8f)
            val y = topPad + (chartH - barHeight)

            drawRect(
                color = moodColor(mood),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight)
            )
        }
    }

    // X-axis labels (Mon–Sun)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 60.dp, end = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        series.forEach { dm ->
            Text(dm.dayLabel, style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault())
    return formatter.format(Date(timestamp))
}


private fun exportMoodsCsvAndShare(context: android.content.Context, entries: List<com.example.mindtrack.data.MoodEntry>) {
    val csvHeader = "id,mood,note,timestamp\n"
    val csvBody = entries.joinToString("\n") { e ->
        "${e.id},${e.mood},\"${e.note.replace("\"", "\"\"")}\",${e.timestamp}"
    }
    val csv = csvHeader + csvBody

    val file = File(context.cacheDir, "mindtrack_moods.csv")
    file.writeText(csv)

    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(Intent.createChooser(shareIntent, "Share Mood Export"))
}


