package com.example.mindtrack.ui

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

private val Context.dataStore by preferencesDataStore(name = "mindtrack_prefs")
private val LAST_RESET_WEEK = intPreferencesKey("last_reset_week")

object WeeklyResetStore {

    private fun currentYearWeek(): Int {
        val today = LocalDate.now()
        val week = today.get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear())
        val year = today.year
        // Combine year+week so it’s unique
        return year * 100 + week
    }

    suspend fun shouldReset(context: Context): Boolean {
        val prefs = context.dataStore.data.first()
        val last = prefs[LAST_RESET_WEEK] ?: -1
        return last != currentYearWeek()
    }

    suspend fun markReset(context: Context) {
        val now = currentYearWeek()
        context.dataStore.edit { it[LAST_RESET_WEEK] = now }
    }
}
