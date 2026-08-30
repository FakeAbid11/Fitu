package com.fitu.data.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val appVersion: String,
    val exportDate: Long,
    val userProfile: UserProfileBackup?,
    val apiKey: String?,
    val steps: List<StepBackup>,
    val meals: List<MealBackup>,
    val workouts: List<WorkoutBackup>,
    val workoutPlans: List<WorkoutPlanBackup>
)

@Serializable
data class UserProfileBackup(
    val name: String,
    val age: Int,
    val heightCm: Int,
    val weightKg: Int,
    val dailyStepGoal: Int,
    val dailyCalorieGoal: Int,
    val birthDay: Int?,
    val birthMonth: Int?,
    val birthYear: Int?,
    val useImperialUnits: Boolean
)

@Serializable
data class StepBackup(
    val date: String,
    val steps: Int,
    val goal: Int,
    val caloriesBurned: Int,
    val distanceMeters: Float,
    val lastUpdated: Long
)

@Serializable
data class MealBackup(
    val id: Long,
    val name: String,
    val description: String,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
    val fiber: Int,
    val mealType: String,
    val portion: Float,
    val photoUri: String?,
    val date: Long,
    val timestamp: Long,
    val createdAt: Long
)

@Serializable
data class WorkoutBackup(
    val id: Long,
    val exerciseType: String,
    val type: String,
    val reps: Int,
    val sets: Int,
    val durationSeconds: Int,
    val durationMs: Long,
    val caloriesBurned: Int,
    val date: Long,
    val timestamp: Long,
    val createdAt: Long
)

@Serializable
data class WorkoutPlanBackup(
    val id: Long,
    val name: String,
    val description: String,
    val exercises: String,
    val difficulty: String,
    val durationMinutes: Int,
    val duration: Int,
    val targetMuscles: String,
    val muscleGroups: String,
    val equipment: String,
    val createdAt: Long
)
