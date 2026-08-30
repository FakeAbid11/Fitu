package com.fitu.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "steps",
    indices = [Index(value = ["date"], unique = true)]
)
data class StepEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,  // Format: "yyyy-MM-dd"
    val steps: Int,
    val goal: Int = 10000,
    val caloriesBurned: Int = 0,
    val distanceMeters: Float = 0f,
    val lastUpdated: Long = System.currentTimeMillis()
)
