package com.fitu.aicoach

/**
 * Tracker for time-based exercises like Plank.
 * 
 * Tracks:
 * - Current hold time (resets when form breaks)
 * - Best hold time (longest continuous hold)
 * - Form score (0-10 based on body line straightness)
 * 
 * Valid plank form is defined as body line angle between 160° and 180°.
 * Perfectly straight (180°) = form score 10
 * Minimum valid (160°) = form score 0
 */
class PlankTracker(
    private val minValidAngle: Float = 160f,
    private val maxValidAngle: Float = 180f
) {
    private var _currentHoldTimeMs: Long = 0L
    private var _bestHoldTimeMs: Long = 0L
    private var _formScore: Float = 0f
    private var _isHolding: Boolean = false
    
    private var lastUpdateTime: Long = 0L

    /**
     * Current hold time in milliseconds
     */
    val currentHoldTimeMs: Long get() = _currentHoldTimeMs

    /**
     * Best hold time in milliseconds (longest continuous hold)
     */
    val bestHoldTimeMs: Long get() = _bestHoldTimeMs

    /**
     * Current form score (0-10)
     * 10 = perfect form (180° body line)
     * 0 = minimum valid form (160° body line)
     */
    val formScore: Float get() = _formScore

    /**
     * Whether user is currently in valid plank position
     */
    val isHolding: Boolean get() = _isHolding

    /**
     * Update the tracker with a new angle measurement.
     * 
     * @param bodyLineAngle Current angle of body line (Shoulder → Hip → Ankle)
     * @param currentTimeMs Current system time in milliseconds
     */
    fun update(bodyLineAngle: Float, currentTimeMs: Long) {
        if (bodyLineAngle < 0) {
            // Invalid angle - pose not detected
            handleFormBreak(currentTimeMs)
            return
        }

        // Check if angle is within valid range
        val isValidForm = bodyLineAngle >= minValidAngle && bodyLineAngle <= maxValidAngle

        if (isValidForm) {
            // Calculate form score (0-10)
            // 180° = 10, 160° = 0
            _formScore = calculateFormScore(bodyLineAngle)

            if (_isHolding) {
                // Continue holding - add elapsed time
                val elapsed = currentTimeMs - lastUpdateTime
                _currentHoldTimeMs += elapsed
            } else {
                // Start new hold
                _isHolding = true
                _currentHoldTimeMs = 0L
            }
        } else {
            // Form broke
            handleFormBreak(currentTimeMs)
        }

        lastUpdateTime = currentTimeMs
    }

    /**
     * Calculate form score based on body line angle.
     * 
     * Score mapping:
     * - 180° = 10 (perfect)
     * - 170° = 5 (good)
     * - 160° = 0 (minimum valid)
     */
    private fun calculateFormScore(angle: Float): Float {
        // Clamp to valid range
        val clampedAngle = angle.coerceIn(minValidAngle, maxValidAngle)
        
        // Linear interpolation: 160° → 0, 180° → 10
        val ratio = (clampedAngle - minValidAngle) / (maxValidAngle - minValidAngle)
        return (ratio * 10f).coerceIn(0f, 10f)
    }

    /**
     * Handle when form breaks (angle outside valid range)
     */
    private fun handleFormBreak(currentTimeMs: Long) {
        if (_isHolding) {
            // Update best time if current was better
            if (_currentHoldTimeMs > _bestHoldTimeMs) {
                _bestHoldTimeMs = _currentHoldTimeMs
            }
        }
        
        // Reset current hold
        _isHolding = false
        _currentHoldTimeMs = 0L
        _formScore = 0f
        lastUpdateTime = currentTimeMs
    }

    /**
     * Reset all tracking data
     */
    fun reset() {
        _currentHoldTimeMs = 0L
        _bestHoldTimeMs = 0L
        _formScore = 0f
        _isHolding = false
        lastUpdateTime = 0L
    }

    /**
     * Get current hold time formatted as MM:SS
     */
    fun getCurrentTimeFormatted(): String {
        return formatTime(_currentHoldTimeMs)
    }

    /**
     * Get best hold time formatted as MM:SS
     */
    fun getBestTimeFormatted(): String {
        return formatTime(_bestHoldTimeMs)
    }

    /**
     * Format milliseconds to MM:SS string
     */
    private fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    /**
     * Get form feedback text based on current score
     */
    fun getFormFeedback(): String {
        return when {
            !_isHolding -> "Get in position"
            _formScore >= 9f -> "Perfect! 🔥"
            _formScore >= 7f -> "Great form! 👍"
            _formScore >= 5f -> "Good! Keep straight"
            _formScore >= 3f -> "Straighten up!"
            else -> "Adjust form"
        }
    }

    companion object {
        /**
         * Create a PlankTracker with default thresholds
         */
        fun create(): PlankTracker {
            return PlankTracker(
                minValidAngle = 160f,
                maxValidAngle = 180f
            )
        }

        /**
         * Create from ExerciseConfig
         */
        fun forExercise(config: ExerciseConfig): PlankTracker {
            return PlankTracker(
                minValidAngle = config.downThreshold,
                maxValidAngle = config.upThreshold
            )
        }
    }
}
