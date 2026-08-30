package com.fitu.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.fitu.BuildConfig
import com.fitu.data.local.SecureStorage
import com.fitu.data.local.UserPreferencesRepository
import com.fitu.data.local.dao.MealDao
import com.fitu.data.local.dao.StepDao
import com.fitu.data.local.dao.WorkoutDao
import com.fitu.data.local.dao.WorkoutPlanDao
import com.fitu.data.local.entity.MealEntity
import com.fitu.data.local.entity.StepEntity
import com.fitu.data.local.entity.WorkoutEntity
import com.fitu.data.local.entity.WorkoutPlanEntity
import com.fitu.data.model.*
import com.fitu.domain.repository.BackupInfo
import com.fitu.domain.repository.BackupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class BackupRepositoryImpl @Inject constructor(
    private val context: Context,
    private val stepDao: StepDao,
    private val mealDao: MealDao,
    private val workoutDao: WorkoutDao,
    private val workoutPlanDao: WorkoutPlanDao,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val secureStorage: SecureStorage
) : BackupRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    override suspend fun exportData(includeApiKey: Boolean): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            // Gather all data
            val steps = getAllSteps().map { it.toBackup() }
            val meals = getAllMeals().map { it.toBackup() }
            val workouts = getAllWorkouts().map { it.toBackup() }
            val workoutPlans = getAllWorkoutPlans().map { it.toBackup() }
            
            // Get user profile
            val userProfile = UserProfileBackup(
                name = userPreferencesRepository.userName.first(),
                age = userPreferencesRepository.userAge.first(),
                heightCm = userPreferencesRepository.userHeightCm.first(),
                weightKg = userPreferencesRepository.userWeightKg.first(),
                dailyStepGoal = userPreferencesRepository.dailyStepGoal.first(),
                dailyCalorieGoal = userPreferencesRepository.dailyCalorieGoal.first(),
                birthDay = userPreferencesRepository.birthDay.first(),
                birthMonth = userPreferencesRepository.birthMonth.first(),
                birthYear = userPreferencesRepository.birthYear.first(),
                useImperialUnits = userPreferencesRepository.useImperialUnits.first()
            )
            
            // Get API key if requested
            val apiKey = if (includeApiKey) {
                secureStorage.apiKeyFlow.first().takeIf { it.isNotBlank() }
            } else null

            // Create backup data
            val backupData = BackupData(
                appVersion = BuildConfig.VERSION_NAME,
                exportDate = System.currentTimeMillis(),
                userProfile = userProfile,
                apiKey = apiKey,
                steps = steps,
                meals = meals,
                workouts = workouts,
                workoutPlans = workoutPlans
            )

            // Convert to JSON
            val jsonString = json.encodeToString(backupData)

            // Create file
            val fileName = "fitu_backup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.json"
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/Fitu")
            }

            val uri = context.contentResolver.insert(
                MediaStore.Files.getContentUri("external"),
                contentValues
            ) ?: return@withContext Result.failure(IOException("Failed to create backup file"))

            // Write data
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(jsonString.toByteArray())
            } ?: return@withContext Result.failure(IOException("Failed to write backup data"))

            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun importData(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Read file
            val jsonString = context.contentResolver.openInputStream(uri)?.use { input ->
                input.readBytes().decodeToString()
            } ?: return@withContext Result.failure(IOException("Failed to read backup file"))

            // Parse JSON
            val backupData = json.decodeFromString<BackupData>(jsonString)

            // Restore user profile
            backupData.userProfile?.let { profile ->
                userPreferencesRepository.saveUserProfile(
                    name = profile.name,
                    age = profile.age,
                    heightCm = profile.heightCm,
                    weightKg = profile.weightKg,
                    stepGoal = profile.dailyStepGoal,
                    calorieGoal = profile.dailyCalorieGoal
                )
                userPreferencesRepository.saveBirthDate(
                    profile.birthDay,
                    profile.birthMonth,
                    profile.birthYear
                )
                userPreferencesRepository.setUseImperialUnits(profile.useImperialUnits)
            }

            // Restore API key
            backupData.apiKey?.let { key ->
                if (key.isNotBlank()) {
                    secureStorage.saveApiKey(key)
                }
            }

            // Restore steps
            backupData.steps.forEach { step ->
                stepDao.insertOrUpdate(step.toEntity())
            }

            // Restore meals
            backupData.meals.forEach { meal ->
                mealDao.insert(meal.toEntity())
            }

            // Restore workouts
            backupData.workouts.forEach { workout ->
                workoutDao.insert(workout.toEntity())
            }

            // Restore workout plans
            backupData.workoutPlans.forEach { plan ->
                workoutPlanDao.insertPlan(plan.toEntity())
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getBackupInfo(uri: Uri): Result<BackupInfo> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { input ->
                input.readBytes().decodeToString()
            } ?: return@withContext Result.failure(IOException("Failed to read backup file"))

            val backupData = json.decodeFromString<BackupData>(jsonString)

            val info = BackupInfo(
                appVersion = backupData.appVersion,
                exportDate = backupData.exportDate,
                stepRecords = backupData.steps.size,
                mealRecords = backupData.meals.size,
                workoutRecords = backupData.workouts.size,
                workoutPlans = backupData.workoutPlans.size,
                hasUserProfile = backupData.userProfile != null,
                hasApiKey = !backupData.apiKey.isNullOrBlank()
            )

            Result.success(info)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Helper functions to get all data
    private suspend fun getAllSteps(): List<StepEntity> {
        // Get all steps from the beginning of time to far future
        return stepDao.getStepsInRange(0L, Long.MAX_VALUE)
    }

    private suspend fun getAllMeals(): List<MealEntity> {
        return mealDao.getMealsInRangeSync(0L, Long.MAX_VALUE)
    }

    private suspend fun getAllWorkouts(): List<WorkoutEntity> {
        return workoutDao.getWorkoutsInRangeSync(0L, Long.MAX_VALUE)
    }

    private suspend fun getAllWorkoutPlans(): List<WorkoutPlanEntity> {
        return workoutPlanDao.getAllPlans().first()
    }
}

// Extension functions for entity conversions
private fun StepEntity.toBackup() = StepBackup(
    date = date,
    steps = steps,
    goal = goal,
    caloriesBurned = caloriesBurned,
    distanceMeters = distanceMeters,
    lastUpdated = lastUpdated
)

private fun StepBackup.toEntity() = StepEntity(
    id = 0, // Room will auto-generate
    date = date,
    steps = steps,
    goal = goal,
    caloriesBurned = caloriesBurned,
    distanceMeters = distanceMeters,
    lastUpdated = lastUpdated
)

private fun MealEntity.toBackup() = MealBackup(
    id = id,
    name = name,
    description = description,
    calories = calories,
    protein = protein,
    carbs = carbs,
    fat = fat,
    fiber = fiber,
    mealType = mealType,
    portion = portion,
    photoUri = photoUri,
    date = date,
    timestamp = timestamp,
    createdAt = createdAt
)

private fun MealBackup.toEntity() = MealEntity(
    id = 0, // Room will auto-generate
    name = name,
    description = description,
    calories = calories,
    protein = protein,
    carbs = carbs,
    fat = fat,
    fiber = fiber,
    mealType = mealType,
    portion = portion,
    photoUri = photoUri,
    date = date,
    timestamp = timestamp,
    createdAt = createdAt
)

private fun WorkoutEntity.toBackup() = WorkoutBackup(
    id = id,
    exerciseType = exerciseType,
    type = type,
    reps = reps,
    sets = sets,
    durationSeconds = durationSeconds,
    durationMs = durationMs,
    caloriesBurned = caloriesBurned,
    date = date,
    timestamp = timestamp,
    createdAt = createdAt
)

private fun WorkoutBackup.toEntity() = WorkoutEntity(
    id = 0, // Room will auto-generate
    exerciseType = exerciseType,
    type = type,
    reps = reps,
    sets = sets,
    durationSeconds = durationSeconds,
    durationMs = durationMs,
    caloriesBurned = caloriesBurned,
    date = date,
    timestamp = timestamp,
    createdAt = createdAt
)

private fun WorkoutPlanEntity.toBackup() = WorkoutPlanBackup(
    id = id,
    name = name,
    description = description,
    exercises = exercises,
    difficulty = difficulty,
    durationMinutes = durationMinutes,
    duration = duration,
    targetMuscles = targetMuscles,
    muscleGroups = muscleGroups,
    equipment = equipment,
    createdAt = createdAt
)

private fun WorkoutPlanBackup.toEntity() = WorkoutPlanEntity(
    id = 0, // Room will auto-generate
    name = name,
    description = description,
    exercises = exercises,
    difficulty = difficulty,
    durationMinutes = durationMinutes,
    duration = duration,
    targetMuscles = targetMuscles,
    muscleGroups = muscleGroups,
    equipment = equipment,
    createdAt = createdAt
)
