package com.example.mindtrack.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindtrack.data.MoodDatabase
import com.example.mindtrack.data.MoodEntry
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

data class DayMood(val dayLabel: String, val avgMood: Double)

class MoodViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = MoodDatabase.getDatabase(application).moodDao()

    val moodList = dao.getAllMoods()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addMood(mood: Int, note: String) {
        viewModelScope.launch {
            dao.insertMood(MoodEntry(mood = mood, note = note))
        }
    }

    // ✅ Manual reset (button)
    fun resetWeekManual() {
        viewModelScope.launch {
            dao.deleteAll()
            WeeklyResetStore.markReset(getApplication())
        }
    }

    // ✅ Auto reset when a new week starts
    fun autoResetIfNewWeek() {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            if (WeeklyResetStore.shouldReset(ctx)) {
                dao.deleteAll()
                WeeklyResetStore.markReset(ctx)
            }
        }
    }

    // ✅ Weekly average (based on last 7 days data)
    val weeklyAverageMood: StateFlow<Double> =
        moodList.map { list ->
            val sevenDaysAgo = LocalDate.now().minusDays(6)
            val recent = list.filter { entry ->
                entry.toLocalDate() >= sevenDaysAgo
            }
            if (recent.isNotEmpty()) recent.map { it.mood }.average() else 0.0
        }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    // ✅ Weekly series: Mon–Sun labels + average mood per day (line chart)
// ✅ Daily series: last 7 days (Mon–Sun labels), 0.0 if no mood logged that day
    val dailySeries: StateFlow<List<DayMood>> =
        moodList.map { list ->
            val today = LocalDate.now()
            val start = today.minusDays(6) // last 7 days

            val byDay = list
                .filter { it.toLocalDate() in start..today }
                .groupBy { it.toLocalDate() }

            (0..6).map { i ->
                val d = start.plusDays(i.toLong())
                val entries = byDay[d].orEmpty()
                val avg = if (entries.isNotEmpty()) entries.map { it.mood }.average() else 0.0
                DayMood(
                    dayLabel = d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    avgMood = avg
                )
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

}

// helper
private fun MoodEntry.toLocalDate(): LocalDate {
    return Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}
