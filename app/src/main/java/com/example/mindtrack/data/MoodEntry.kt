package com.example.mindtrack.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mood_entries")
data class MoodEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val mood: Int,
    val note: String,
    val timestamp: Long = System.currentTimeMillis()
)
