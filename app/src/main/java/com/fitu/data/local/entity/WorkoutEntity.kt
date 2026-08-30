package com.fitu.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workouts",
    indices = [Index(value = ["date"])]
)
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val exerciseType: String = "",
    val type: String = "",
    val reps: Int = 0,
    val sets: Int = 0,
    val durationSeconds: Int = 0,
    val durationMs: Long = 0L,
    val caloriesBurned: Int = 0,
    val date: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)
