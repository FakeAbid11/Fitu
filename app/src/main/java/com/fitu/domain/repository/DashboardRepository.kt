package com.fitu.domain.repository

import com.fitu.data.local.entity.MealEntity
import com.fitu.data.local.entity.WorkoutEntity
import kotlinx.coroutines.flow.Flow

interface DashboardRepository {
    fun getMealsForDay(startOfDay: Long, endOfDay: Long): Flow<List<MealEntity>>
    fun getWorkoutsForDay(startOfDay: Long, endOfDay: Long): Flow<List<WorkoutEntity>>
    fun getCaloriesConsumedForDay(startOfDay: Long, endOfDay: Long): Flow<Int>
    fun getCaloriesBurnedForDay(startOfDay: Long, endOfDay: Long): Flow<Int?>
    fun getProteinForDay(startOfDay: Long, endOfDay: Long): Flow<Int>
    fun getCarbsForDay(startOfDay: Long, endOfDay: Long): Flow<Int>
    fun getFatsForDay(startOfDay: Long, endOfDay: Long): Flow<Int>
    suspend fun insertMeal(meal: MealEntity): Long
    suspend fun deleteMeal(id: Long)
    suspend fun insertWorkout(workout: WorkoutEntity): Long
}
