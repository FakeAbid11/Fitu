package com.fitu.data.repository

import com.fitu.data.local.dao.MealDao
import com.fitu.data.local.dao.StepDao
import com.fitu.data.local.dao.WorkoutDao
import com.fitu.data.local.entity.MealEntity
import com.fitu.data.local.entity.WorkoutEntity
import com.fitu.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DashboardRepositoryImpl @Inject constructor(
    private val mealDao: MealDao,
    private val workoutDao: WorkoutDao,
    private val stepDao: StepDao
) : DashboardRepository {

    override fun getMealsForDay(startOfDay: Long, endOfDay: Long): Flow<List<MealEntity>> {
        return mealDao.getMealsInRange(startOfDay, endOfDay)
    }

    override fun getWorkoutsForDay(startOfDay: Long, endOfDay: Long): Flow<List<WorkoutEntity>> {
        return workoutDao.getWorkoutsInRange(startOfDay, endOfDay)
    }

    override fun getCaloriesConsumedForDay(startOfDay: Long, endOfDay: Long): Flow<Int> {
        return mealDao.getCaloriesConsumedForDay(startOfDay, endOfDay)
    }

    override fun getCaloriesBurnedForDay(startOfDay: Long, endOfDay: Long): Flow<Int?> {
        return workoutDao.getWorkoutsInRange(startOfDay, endOfDay).map { workouts ->
            workouts.sumOf { it.caloriesBurned }
        }
    }

    override fun getProteinForDay(startOfDay: Long, endOfDay: Long): Flow<Int> {
        return mealDao.getProteinForDay(startOfDay, endOfDay)
    }

    override fun getCarbsForDay(startOfDay: Long, endOfDay: Long): Flow<Int> {
        return mealDao.getCarbsForDay(startOfDay, endOfDay)
    }

    override fun getFatsForDay(startOfDay: Long, endOfDay: Long): Flow<Int> {
        return mealDao.getFatsForDay(startOfDay, endOfDay)
    }

    override suspend fun insertMeal(meal: MealEntity): Long {
        return mealDao.insertMeal(meal)
    }

    override suspend fun deleteMeal(id: Long) {
        mealDao.deleteMeal(id)
    }

    override suspend fun insertWorkout(workout: WorkoutEntity): Long {
        return workoutDao.insertWorkout(workout)
    }
}
