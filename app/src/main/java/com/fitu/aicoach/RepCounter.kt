package com.fitu.aicoach

/**
 * State machine for counting exercise repetitions with anti-jitter protection.
 * 
 * ANTI-PHANTOM REP FEATURES:
 * 1. ANGLE SMOOTHING: Averages the last N angles to reduce noise from camera shake
 * 2. FRAME DEBOUNCE: Requires angle to stay in threshold for N consecutive frames
 * 3. MINIMUM REP INTERVAL: Prevents counting reps faster than humanly possible
 * 4. HYSTERESIS: Large gap between up/down thresholds
 * 
 * The state machine only counts a rep when completing: UP → DOWN → UP
 */
class RepCounter(
    private val downThreshold: Float,
    private val upThreshold: Float,
    private val exerciseType: ExerciseType
) {
    companion object {
        // Anti-jitter configuration
        private const val SMOOTHING_WINDOW = 5        // Average last 5 angles
        private const val DEBOUNCE_FRAMES = 3         // Require 3 consecutive frames in position
        
        /**
         * ✅ FIX #4: Get minimum rep interval based on exercise type
         * Fast exercises (crunches, curls) = 400ms
         * Medium exercises (push-ups) = 600ms
         * Slow exercises (squats) = 700ms
         */
        private fun getMinRepInterval(exerciseType: ExerciseType): Long {
            return when (exerciseType) {
                ExerciseType.CRUNCH -> 400L        // Fast exercise
                ExerciseType.DUMBBELL_CURL -> 450L // Fast exercise
                ExerciseType.PUSH_UP -> 600L       // Medium exercise
                ExerciseType.SQUAT -> 700L         // Slower, deeper movement
                ExerciseType.PLANK -> 1000L        // Time-based, not used for reps
            }
        }
        
        fun forExercise(config: ExerciseConfig): RepCounter {
            return RepCounter(
                downThreshold = config.downThreshold,
                upThreshold = config.upThreshold,
                exerciseType = config.exerciseType
            )
        }
    }

    enum class State {
        UNKNOWN,  // Initial state, position not yet determined
        UP,       // Extended position (start/end of rep)
        DOWN      // Contracted position (bottom of rep)
    }

    private var state: State = State.UNKNOWN
    private var _repCount: Int = 0
    
    // Angle smoothing buffer
    private val angleBuffer = ArrayDeque<Float>(SMOOTHING_WINDOW)
    
    // Frame debounce counters
    private var framesInUp = 0
    private var framesInDown = 0
    
    // Timestamp of last rep
    private var lastRepTimeMs: Long = 0L
    
    // ✅ FIX #4: Store the min interval for this exercise
    private val minRepIntervalMs: Long = getMinRepInterval(exerciseType)

    val repCount: Int get() = _repCount
    val currentState: State get() = state

    /**
     * Update the counter with a new angle measurement.
     * @param angle Current angle in degrees (0-180)
     * @return True if a new rep was counted this frame
     */
    fun update(angle: Float): Boolean {
        if (angle < 0) {
            // Invalid angle - clear buffer to avoid stale data
            angleBuffer.clear()
            framesInUp = 0
            framesInDown = 0
            return false
        }

        // Add to smoothing buffer
        if (angleBuffer.size >= SMOOTHING_WINDOW) {
            angleBuffer.removeFirst()
        }
        angleBuffer.addLast(angle)
        
        // Calculate smoothed angle (average of buffer)
        val smoothedAngle = angleBuffer.average().toFloat()
        
        // Use smoothed angle for state detection
        return when (exerciseType) {
            ExerciseType.DUMBBELL_CURL, ExerciseType.CRUNCH -> updateInverted(smoothedAngle)
            else -> updateNormal(smoothedAngle)
        }
    }

    /**
     * Normal update logic for push-ups and squats.
     * DOWN = angle < downThreshold (bent position)
     * UP = angle > upThreshold (extended position)
     */
    private fun updateNormal(angle: Float): Boolean {
        var repCounted = false
        val currentTime = System.currentTimeMillis()

        // Check current position
        val isInDownPosition = angle < downThreshold
        val isInUpPosition = angle > upThreshold

        // Update debounce counters
        if (isInDownPosition) {
            framesInDown++
            framesInUp = 0
        } else if (isInUpPosition) {
            framesInUp++
            framesInDown = 0
        } else {
            // In dead zone - slowly decay counters
            framesInUp = maxOf(0, framesInUp - 1)
            framesInDown = maxOf(0, framesInDown - 1)
        }

        when (state) {
            State.UNKNOWN -> {
                // Determine initial state (requires debounce)
                if (framesInUp >= DEBOUNCE_FRAMES) {
                    state = State.UP
                } else if (framesInDown >= DEBOUNCE_FRAMES) {
                    state = State.DOWN
                }
            }
            State.UP -> {
                // Check if user went DOWN (requires debounce)
                if (framesInDown >= DEBOUNCE_FRAMES) {
                    state = State.DOWN
                }
            }
            State.DOWN -> {
                // Check if user came back UP → count rep! (requires debounce + time check)
                if (framesInUp >= DEBOUNCE_FRAMES) {
                    val timeSinceLastRep = currentTime - lastRepTimeMs
                    // ✅ FIX #4: Use exercise-specific interval
                    if (timeSinceLastRep >= minRepIntervalMs) {
                        state = State.UP
                        _repCount++
                        lastRepTimeMs = currentTime
                        repCounted = true
                    }
                }
            }
        }

        return repCounted
    }

    /**
     * Inverted update logic for curls and crunches.
     * DOWN = angle > downThreshold (extended/relaxed position)
     * UP = angle < upThreshold (contracted position)
     */
    private fun updateInverted(angle: Float): Boolean {
        var repCounted = false
        val currentTime = System.currentTimeMillis()

        // Check current position (inverted logic)
        val isInDownPosition = angle > downThreshold
        val isInUpPosition = angle < upThreshold

        // Update debounce counters
        if (isInDownPosition) {
            framesInDown++
            framesInUp = 0
        } else if (isInUpPosition) {
            framesInUp++
            framesInDown = 0
        } else {
            // In dead zone - slowly decay counters
            framesInUp = maxOf(0, framesInUp - 1)
            framesInDown = maxOf(0, framesInDown - 1)
        }

        when (state) {
            State.UNKNOWN -> {
                // Determine initial state (requires debounce)
                if (framesInDown >= DEBOUNCE_FRAMES) {
                    state = State.DOWN  // Extended = start
                } else if (framesInUp >= DEBOUNCE_FRAMES) {
                    state = State.UP
                }
            }
            State.DOWN -> {
                // Check if user contracted (UP) (requires debounce)
                if (framesInUp >= DEBOUNCE_FRAMES) {
                    state = State.UP
                }
            }
            State.UP -> {
                // Check if user extended back (DOWN) → count rep! (requires debounce + time check)
                if (framesInDown >= DEBOUNCE_FRAMES) {
                    val timeSinceLastRep = currentTime - lastRepTimeMs
                    // ✅ FIX #4: Use exercise-specific interval
                    if (timeSinceLastRep >= minRepIntervalMs) {
                        state = State.DOWN
                        _repCount++
                        lastRepTimeMs = currentTime
                        repCounted = true
                    }
                }
            }
        }

        return repCounted
    }

    /**
     * Reset the counter to initial state
     */
    fun reset() {
        state = State.UNKNOWN
        _repCount = 0
        angleBuffer.clear()
        framesInUp = 0
        framesInDown = 0
        lastRepTimeMs = 0L
    }

    /**
     * Get state as a display string
     */
    fun getStateDisplay(): String {
        return when (state) {
            State.UNKNOWN -> "Ready"
            State.UP -> "Up"
            State.DOWN -> "Down"
        }
    }
}
