package com.fitu.ui.generator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitu.data.local.dao.WorkoutPlanDao
import com.fitu.data.local.entity.WorkoutPlanEntity
import com.fitu.di.GeminiModelProvider
import com.google.ai.client.generativeai.type.content
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GeneratorViewModel @Inject constructor(
    private val geminiModelProvider: GeminiModelProvider,
    private val workoutPlanDao: WorkoutPlanDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<GeneratorUiState>(GeneratorUiState.Idle)
    val uiState: StateFlow<GeneratorUiState> = _uiState

    private val _selectedMuscles = MutableStateFlow<Set<String>>(emptySet())
    val selectedMuscles: StateFlow<Set<String>> = _selectedMuscles

    private val _difficulty = MutableStateFlow("Intermediate")
    val difficulty: StateFlow<String> = _difficulty

    private val _duration = MutableStateFlow(30)
    val duration: StateFlow<Int> = _duration

    private val _equipment = MutableStateFlow("Bodyweight")
    val equipment: StateFlow<String> = _equipment

    val savedPlans = workoutPlanDao.getAllPlans()

    fun toggleMuscle(muscle: String) {
        _selectedMuscles.value = if (_selectedMuscles.value.contains(muscle)) {
            _selectedMuscles.value - muscle
        } else {
            _selectedMuscles.value + muscle
        }
    }

    fun setDifficulty(value: String) {
        _difficulty.value = value
    }

    fun setDuration(value: Int) {
        _duration.value = value
    }

    fun setEquipment(value: String) {
        _equipment.value = value
    }

    fun generateWorkout() {
        if (_selectedMuscles.value.isEmpty()) {
            _uiState.value = GeneratorUiState.Error("Please select at least one muscle group")
            return
        }

        _uiState.value = GeneratorUiState.Generating
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val generativeModel = geminiModelProvider.getModel()
                if (generativeModel == null) {
                    _uiState.value = GeneratorUiState.Error("API key not configured. Please set up your Gemini API key in settings.")
                    return@launch
                }

                val prompt = """
                    Generate a workout plan with the following parameters:
                    - Target Muscles: ${_selectedMuscles.value.joinToString(", ")}
                    - Difficulty: ${_difficulty.value}
                    - Duration: ${_duration.value} minutes
                    - Equipment: ${_equipment.value}
                    
                    Return a JSON object with fields:
                    - name: string (creative workout name)
                    - exercises: array of objects with:
                        - name: string
                        - sets: int
                        - reps: int or "duration" string for time-based
                        - restSeconds: int
                        
                    Only return JSON, no markdown.
                """.trimIndent()

                val response = generativeModel.generateContent(content { text(prompt) })
                val resultText = response.text

                if (resultText != null) {
                    // Save to DB
                    val plan = WorkoutPlanEntity(
                        name = "Generated Workout",
                        muscleGroups = _selectedMuscles.value.joinToString(","),
                        difficulty = _difficulty.value,
                        duration = _duration.value,
                        equipment = _equipment.value,
                        exercises = resultText,
                        createdAt = System.currentTimeMillis()
                    )
                    workoutPlanDao.insertPlan(plan)
                    _uiState.value = GeneratorUiState.Success(resultText)
                } else {
                    _uiState.value = GeneratorUiState.Error("Failed to generate workout")
                }
            } catch (e: Exception) {
                _uiState.value = GeneratorUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun reset() {
        _uiState.value = GeneratorUiState.Idle
    }
}

sealed class GeneratorUiState {
    object Idle : GeneratorUiState()
    object Generating : GeneratorUiState()
    data class Success(val result: String) : GeneratorUiState()
    data class Error(val message: String) : GeneratorUiState()
}
