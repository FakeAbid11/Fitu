package com.fitu.ui.shared

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Activity-scoped ViewModel that persists across navigation.
 * Holds background analysis state and pending food data.
 */
@HiltViewModel
class SharedAnalysisViewModel @Inject constructor() : ViewModel() {

    private val _backgroundAnalysisState = MutableStateFlow<BackgroundAnalysisState>(BackgroundAnalysisState.Idle)
    val backgroundAnalysisState: StateFlow<BackgroundAnalysisState> = _backgroundAnalysisState.asStateFlow()

    private val _pendingAnalyzedFood = MutableStateFlow<PendingFoodAnalysis?>(null)
    val pendingAnalyzedFood: StateFlow<PendingFoodAnalysis?> = _pendingAnalyzedFood.asStateFlow()

    fun setAnalysisState(state: BackgroundAnalysisState) {
        _backgroundAnalysisState.value = state
    }

    fun setPendingFood(food: PendingFoodAnalysis?) {
        _pendingAnalyzedFood.value = food
    }

    fun dismissBackgroundAnalysis() {
        _backgroundAnalysisState.value = BackgroundAnalysisState.Idle
    }

    fun clearPendingAnalysis() {
        _backgroundAnalysisState.value = BackgroundAnalysisState.Idle
        _pendingAnalyzedFood.value = null
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up bitmaps to prevent memory leaks
        (_pendingAnalyzedFood.value?.bitmap as? Bitmap)?.let { bitmap ->
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }
}

// Data classes
data class AnalyzedFood(
    val name: String,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fats: Int
)

data class PendingFoodAnalysis(
    val food: AnalyzedFood,
    val photoUri: String?,
    val bitmap: Bitmap?
)

sealed class BackgroundAnalysisState {
    object Idle : BackgroundAnalysisState()
    data class Analyzing(val message: String) : BackgroundAnalysisState()
    data class Success(val message: String) : BackgroundAnalysisState()
    data class Error(
        val message: String,
        val canRetry: Boolean,
        val retryBitmap: Bitmap?,
        val retryQuery: String?
    ) : BackgroundAnalysisState()
}
