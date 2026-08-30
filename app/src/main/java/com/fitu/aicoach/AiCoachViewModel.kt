package com.fitu.aicoach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitu.data.local.UserPreferencesRepository
import com.fitu.data.local.dao.WorkoutDao
import com.fitu.data.local.entity.WorkoutEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiCoachViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val workoutDao: WorkoutDao
) : ViewModel() {

    val userWeightKg: StateFlow<Int> = userPreferencesRepository.userWeightKg
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 70)

    private val _selectedExercise = MutableStateFlow(ExerciseType.PUSH_UP)
    val selectedExercise: StateFlow<ExerciseType> = _selectedExercise.asStateFlow()

    private val _repCount = MutableStateFlow(0)
    val repCount: StateFlow<Int> = _repCount.asStateFlow()

    private val _holdTimeMs = MutableStateFlow(0L)
    val holdTimeMs: StateFlow<Long> = _holdTimeMs.asStateFlow()

    private val _bestHoldTimeMs = MutableStateFlow(0L)
    val bestHoldTimeMs: StateFlow<Long> = _bestHoldTimeMs.asStateFlow()

    private val _formScore = MutableStateFlow(0f)
    val formScore: StateFlow<Float> = _formScore.asStateFlow()

    private val _currentAngle = MutableStateFlow(0f)
    val currentAngle: StateFlow<Float> = _currentAngle.asStateFlow()

    private val _feedback = MutableStateFlow("")
    val feedback: StateFlow<String> = _feedback.asStateFlow()

    private val _hasCameraPermission = MutableStateFlow(false)
    val hasCameraPermission: StateFlow<Boolean> = _hasCameraPermission.asStateFlow()

    private val _isWorkoutActive = MutableStateFlow(false)
    val isWorkoutActive: StateFlow<Boolean> = _isWorkoutActive.asStateFlow()

    private val _workoutSaved = MutableStateFlow(false)
    val workoutSaved: StateFlow<Boolean> = _workoutSaved.asStateFlow()

    val caloriesBurned: StateFlow<Float> = combine(
        _selectedExercise,
        _repCount,
        _holdTimeMs,
        userWeightKg
    ) { exercise, reps, holdTime, weight ->
        if (exercise.isTimeBased) {
            ExerciseType.calculateCaloriesFromTime(exercise, holdTime, weight)
        } else {
            ExerciseType.calculateCaloriesFromReps(exercise, reps, weight)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    fun selectExercise(type: ExerciseType) {
        _selectedExercise.value = type
        resetStats()
    }

    fun updateRepCount(count: Int) {
        _repCount.value = count
    }

    fun updateHoldTime(currentMs: Long, bestMs: Long) {
        _holdTimeMs.value = currentMs
        if (bestMs > _bestHoldTimeMs.value) {
            _bestHoldTimeMs.value = bestMs
        }
    }

    fun updateFormScore(score: Float) {
        _formScore.value = score
    }

    fun updateAngle(angle: Float) {
        _currentAngle.value = angle
    }

    fun updateFeedback(message: String) {
        _feedback.value = message
    }

    fun setCameraPermission(granted: Boolean) {
        _hasCameraPermission.value = granted
    }

    fun startWorkout() {
        _isWorkoutActive.value = true
        _workoutSaved.value = false
        resetStats()
    }

    fun stopWorkout() {
        _isWorkoutActive.value = false
        saveWorkoutToDatabase()
    }

    private fun saveWorkoutToDatabase() {
        val exercise = _selectedExercise.value
        val reps = _repCount.value
        val holdTime = _holdTimeMs.value
        val calories = caloriesBurned.value.toInt()

        if (reps > 0 || holdTime > 0) {
            viewModelScope.launch {
                try {
                    val workout = WorkoutEntity(
                        exerciseType = exercise.displayName,
                        type = exercise.displayName,
                        reps = reps,
                        durationMs = holdTime,
                        durationSeconds = (holdTime / 1000).toInt(),
                        date = System.currentTimeMillis(),
                        timestamp = System.currentTimeMillis(),
                        caloriesBurned = calories
                    )
                    workoutDao.insertWorkout(workout)
                    _workoutSaved.value = true
                } catch (e: Exception) {
                    android.util.Log.e("AiCoachViewModel", "Failed to save workout", e)
                    _workoutSaved.value = false
                }
            }
        }
    }

    fun saveCurrentWorkout() {
        val reps = _repCount.value
        val holdTime = _holdTimeMs.value
        
        if (reps > 0 || holdTime > 0) {
            saveWorkoutToDatabase()
        }
    }

    fun resetStats() {
        _repCount.value = 0
        _holdTimeMs.value = 0L
        _formScore.value = 0f
        _currentAngle.value = 0f
        _feedback.value = ""
        _workoutSaved.value = false
    }

    fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}
