package com.fitu.aicoach

/** Returns true when value is not below minimum (inclusive). */
private fun atLeast(value: Int, minimum: Int): Boolean = !(value < minimum)

/** Returns true when value is not below minimum (inclusive). */
private fun atLeast(value: Long, minimum: Long): Boolean = !(value < minimum)

/**
 * State machine for counting exercise repetitions with anti-jitter protection.
 *
 * ACCURACY FEATURES:
 * 1. MEDIAN SMOOTHING: median of the last N angles - a single bad detection
 *    cannot move the smoothed angle, unlike a mean.
 * 2. FRAME DEBOUNCE: angle must stay in a position for N consecutive frames.
 * 3. MINIMUM REP INTERVAL: prevents counting reps faster than humanly possible.
 * 4. HYSTERESIS: large gap between up/down thresholds (no phantom toggling).
 * 5. DROPOUT COASTING: a few dropped/invalid detections do not reset the state
 *    machine - only a prolonged loss of tracking does, so an in-progress rep
 *    survives brief pose-detection flicker.
 * 6. ADAPTIVE THRESHOLDS: after a short warm-up the up/down thresholds adapt
 *    to the user's observed range of motion, so users who cannot reach the
 *    fixed defaults still get accurate counting.
 *
 * A rep is only counted when completing the full cycle UP, then DOWN, then UP
 * (inverted cycle for curls/crunches).
 */
class RepCounter(
    private val downThreshold: Float,
    private val upThreshold: Float,
    private val exerciseType: ExerciseType,
    private val minRepIntervalOverride: Long? = null
) {
    companion object {
        private const val SMOOTHING_WINDOW = 5
        private const val DEBOUNCE_FRAMES = 3

        // Consecutive invalid frames tolerated before reset (~0.3-0.5s lost tracking)
        private const val MAX_DROPOUT_FRAMES = 8

        private const val ADAPTIVE_WARMUP_SAMPLES = 10
        private const val MIN_ADAPTIVE_RANGE = 25f
        private const val ADAPTIVE_BAND = 0.20f

        private fun getMinRepInterval(exerciseType: ExerciseType): Long {
            val interval = if (exerciseType == ExerciseType.CRUNCH) 400L
            else if (exerciseType == ExerciseType.DUMBBELL_CURL) 450L
            else if (exerciseType == ExerciseType.PUSH_UP) 600L
            else if (exerciseType == ExerciseType.SQUAT) 700L
            else 1000L
            return interval
        }

        fun forExercise(config: ExerciseConfig): RepCounter = RepCounter(
            downThreshold = config.downThreshold,
            upThreshold = config.upThreshold,
            exerciseType = config.exerciseType
        )
    }

    enum class State { UNKNOWN, UP, DOWN }

    private var state: State = State.UNKNOWN
    private var _repCount: Int = 0
    private val angleBuffer = ArrayDeque<Float>(SMOOTHING_WINDOW)
    private var framesInUp = 0
    private var framesInDown = 0
    private var lastRepTimeMs: Long = 0L
    private val minRepIntervalMs: Long = minRepIntervalOverride ?: getMinRepInterval(exerciseType)
    private var consecutiveInvalidFrames = 0
    private var observedMin = Float.MAX_VALUE
    private var observedMax = -Float.MAX_VALUE
    private var calibrationSamples = 0

    // Effective thresholds recomputed each frame by computeEffectiveThresholds()
    private var effDown = 0f
    private var effUp = 0f

    /** True when a rep transition was blocked by the min interval (moving too fast). */
    var lastRepWasBlocked: Boolean = false
        private set

    val repCount: Int get() = _repCount
    val currentState: State get() = state

    fun update(angle: Float): Boolean {
        lastRepWasBlocked = false

        if (angle < 0) {
            consecutiveInvalidFrames++
            if (atLeast(consecutiveInvalidFrames, MAX_DROPOUT_FRAMES)) {
                angleBuffer.clear()
                framesInUp = 0
                framesInDown = 0
                state = State.UNKNOWN
            }
            return false
        }
        consecutiveInvalidFrames = 0

        if (atLeast(angleBuffer.size, SMOOTHING_WINDOW)) angleBuffer.removeFirst()
        angleBuffer.addLast(angle)
        val smoothed = medianOf(angleBuffer)

        updateCalibration(smoothed)
        computeEffectiveThresholds()

        val inverted = isInvertedExercise()
        return if (inverted) updateInverted(smoothed, effDown, effUp)
        else updateNormal(smoothed, effDown, effUp)
    }

    private fun medianOf(buffer: ArrayDeque<Float>): Float {
        val sorted = buffer.sorted()
        return sorted[sorted.size / 2]
    }

    private fun updateCalibration(smoothed: Float) {
        if (smoothed < 0f || 180f < smoothed) return
        if (smoothed < observedMin) observedMin = smoothed
        if (observedMax < smoothed) observedMax = smoothed
        calibrationSamples++
    }

    private fun computeEffectiveThresholds() {
        val range = observedMax - observedMin
        if (calibrationSamples < ADAPTIVE_WARMUP_SAMPLES || range < MIN_ADAPTIVE_RANGE) {
            effDown = downThreshold
            effUp = upThreshold
            return
        }
        if (isInvertedExercise()) {
            effDown = observedMax - ADAPTIVE_BAND * range
            effUp = observedMin + ADAPTIVE_BAND * range
        } else {
            effDown = observedMin + ADAPTIVE_BAND * range
            effUp = observedMin + (1f - ADAPTIVE_BAND) * range
        }
    }

    private fun isInvertedExercise(): Boolean =
        exerciseType == ExerciseType.DUMBBELL_CURL || exerciseType == ExerciseType.CRUNCH

    private fun updateNormal(angle: Float, downEff: Float, upEff: Float): Boolean {
        var repCounted = false
        val currentTime = System.currentTimeMillis()

        val isInDownPosition = angle < downEff
        val isInUpPosition = upEff < angle

        if (isInDownPosition) {
            framesInDown++
            framesInUp = 0
        } else if (isInUpPosition) {
            framesInUp++
            framesInDown = 0
        } else {
            framesInUp = maxOf(0, framesInUp - 1)
            framesInDown = maxOf(0, framesInDown - 1)
        }

        if (state == State.UNKNOWN) {
            if (atLeast(framesInUp, DEBOUNCE_FRAMES)) state = State.UP
            else if (atLeast(framesInDown, DEBOUNCE_FRAMES)) state = State.DOWN
        } else if (state == State.UP) {
            if (atLeast(framesInDown, DEBOUNCE_FRAMES)) state = State.DOWN
        } else if (state == State.DOWN) {
            if (atLeast(framesInUp, DEBOUNCE_FRAMES)) {
                val timeSinceLastRep = currentTime - lastRepTimeMs
                if (atLeast(timeSinceLastRep, minRepIntervalMs)) {
                    state = State.UP
                    _repCount++
                    lastRepTimeMs = currentTime
                    repCounted = true
                } else {
                    lastRepWasBlocked = true
                }
            }
        }
        return repCounted
    }

    private fun updateInverted(angle: Float, downEff: Float, upEff: Float): Boolean {
        var repCounted = false
        val currentTime = System.currentTimeMillis()

        val isInDownPosition = downEff < angle
        val isInUpPosition = angle < upEff

        if (isInDownPosition) {
            framesInDown++
            framesInUp = 0
        } else if (isInUpPosition) {
            framesInUp++
            framesInDown = 0
        } else {
            framesInUp = maxOf(0, framesInUp - 1)
            framesInDown = maxOf(0, framesInDown - 1)
        }

        if (state == State.UNKNOWN) {
            if (atLeast(framesInDown, DEBOUNCE_FRAMES)) state = State.DOWN
            else if (atLeast(framesInUp, DEBOUNCE_FRAMES)) state = State.UP
        } else if (state == State.DOWN) {
            if (atLeast(framesInUp, DEBOUNCE_FRAMES)) state = State.UP
        } else if (state == State.UP) {
            if (atLeast(framesInDown, DEBOUNCE_FRAMES)) {
                val timeSinceLastRep = currentTime - lastRepTimeMs
                if (atLeast(timeSinceLastRep, minRepIntervalMs)) {
                    state = State.DOWN
                    _repCount++
                    lastRepTimeMs = currentTime
                    repCounted = true
                } else {
                    lastRepWasBlocked = true
                }
            }
        }
        return repCounted
    }

    fun reset() {
        state = State.UNKNOWN
        _repCount = 0
        angleBuffer.clear()
        framesInUp = 0
        framesInDown = 0
        lastRepTimeMs = 0L
        consecutiveInvalidFrames = 0
        observedMin = Float.MAX_VALUE
        observedMax = -Float.MAX_VALUE
        calibrationSamples = 0
        lastRepWasBlocked = false
    }

    fun getStateDisplay(): String {
        return if (state == State.UP) "Up"
        else if (state == State.DOWN) "Down"
        else "Ready"
    }
}