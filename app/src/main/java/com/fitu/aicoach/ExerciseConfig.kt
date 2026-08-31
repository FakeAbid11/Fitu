package com.fitu.aicoach

/**
 * Configuration for each exercise type.
 * Defines the landmarks to track, angle thresholds, and tracking mode.
 *
 * @property exerciseType The type of exercise
 * @property landmarks Triple of landmark indices: (first, mid, last) for angle calculation
 * @property angleName Human-readable name for the angle being measured
 * @property downThreshold Angle threshold for "down" position (degrees)
 * @property upThreshold Angle threshold for "up" position (degrees)
 * @property useLeftSide Whether to use left side landmarks (true) or right side (false)
 * @property gateSegment Optional segment that must be roughly horizontal (body-position gate)
 * @property gateToleranceDeg Tolerance of the body-position gate in degrees
 */
data class ExerciseConfig(
    val exerciseType: ExerciseType,
    val landmarks: Triple<Int, Int, Int>,
    val angleName: String,
    val downThreshold: Float,
    val upThreshold: Float,
    val useLeftSide: Boolean = true,
    val gateSegment: Pair<Int, Int>? = null,
    val gateToleranceDeg: Float = 60f
) {
    companion object {
        /**
         * Get the configuration for a specific exercise type.
         *
         * Angle calculation uses three landmarks:
         * - First: The starting point of the angle
         * - Mid: The vertex (joint) where angle is measured
         * - Last: The ending point of the angle
         *
         * Example: For elbow angle: Shoulder, Elbow, Wrist
         */
        fun forExercise(type: ExerciseType, useLeftSide: Boolean = true): ExerciseConfig {
            return when (type) {
                ExerciseType.PUSH_UP -> {
                    // Elbow angle: Shoulder, Elbow, Wrist
                    // Down: bent arm, Up: extended arm
                    ExerciseConfig(
                        exerciseType = type,
                        landmarks = if (useLeftSide) {
                            Triple(
                                LandmarkIndex.LEFT_SHOULDER,
                                LandmarkIndex.LEFT_ELBOW,
                                LandmarkIndex.LEFT_WRIST
                            )
                        } else {
                            Triple(
                                LandmarkIndex.RIGHT_SHOULDER,
                                LandmarkIndex.RIGHT_ELBOW,
                                LandmarkIndex.RIGHT_WRIST
                            )
                        },
                        angleName = "Elbow",
                        // Push-up: Need to bend arm significantly and extend fully
                        // 60 deg gap prevents phantom reps
                        downThreshold = 90f,
                        upThreshold = 150f,
                        gateSegment = if (useLeftSide) {
                            Pair(LandmarkIndex.LEFT_SHOULDER, LandmarkIndex.LEFT_HIP)
                        } else {
                            Pair(LandmarkIndex.RIGHT_SHOULDER, LandmarkIndex.RIGHT_HIP)
                        },
                        useLeftSide = useLeftSide
                    )
                }
                ExerciseType.SQUAT -> {
                    // Knee angle: Hip, Knee, Ankle
                    // Down: at least half squat, Up: standing straight
                    ExerciseConfig(
                        exerciseType = type,
                        landmarks = if (useLeftSide) {
                            Triple(
                                LandmarkIndex.LEFT_HIP,
                                LandmarkIndex.LEFT_KNEE,
                                LandmarkIndex.LEFT_ANKLE
                            )
                        } else {
                            Triple(
                                LandmarkIndex.RIGHT_HIP,
                                LandmarkIndex.RIGHT_KNEE,
                                LandmarkIndex.RIGHT_ANKLE
                            )
                        },
                        angleName = "Knee",
                        // Squat: Need to bend knee significantly and stand fully
                        downThreshold = 110f,
                        upThreshold = 165f,
                        useLeftSide = useLeftSide
                    )
                }
                ExerciseType.PLANK -> {
                    // Body line: Shoulder, Hip, Ankle
                    // Valid plank: 160-180 (straight body)
                    ExerciseConfig(
                        exerciseType = type,
                        landmarks = if (useLeftSide) {
                            Triple(
                                LandmarkIndex.LEFT_SHOULDER,
                                LandmarkIndex.LEFT_HIP,
                                LandmarkIndex.LEFT_ANKLE
                            )
                        } else {
                            Triple(
                                LandmarkIndex.RIGHT_SHOULDER,
                                LandmarkIndex.RIGHT_HIP,
                                LandmarkIndex.RIGHT_ANKLE
                            )
                        },
                        angleName = "Body Line",
                        downThreshold = 160f,
                        upThreshold = 180f,
                        gateSegment = if (useLeftSide) {
                            Pair(LandmarkIndex.LEFT_SHOULDER, LandmarkIndex.LEFT_HIP)
                        } else {
                            Pair(LandmarkIndex.RIGHT_SHOULDER, LandmarkIndex.RIGHT_HIP)
                        },
                        useLeftSide = useLeftSide
                    )
                }
                ExerciseType.DUMBBELL_CURL -> {
                    // Elbow angle: Shoulder, Elbow, Wrist
                    // Down: arm extended, Up: fully curled
                    // Note: Thresholds are INVERTED compared to push-up
                    ExerciseConfig(
                        exerciseType = type,
                        landmarks = if (useLeftSide) {
                            Triple(
                                LandmarkIndex.LEFT_SHOULDER,
                                LandmarkIndex.LEFT_ELBOW,
                                LandmarkIndex.LEFT_WRIST
                            )
                        } else {
                            Triple(
                                LandmarkIndex.RIGHT_SHOULDER,
                                LandmarkIndex.RIGHT_ELBOW,
                                LandmarkIndex.RIGHT_WRIST
                            )
                        },
                        angleName = "Elbow",
                        // Curl: Need to extend arm and curl tight
                        // 80 deg gap prevents phantom reps
                        downThreshold = 150f,
                        upThreshold = 70f,
                        useLeftSide = useLeftSide
                    )
                }
                ExerciseType.CRUNCH -> {
                    // Hip/Torso angle: Shoulder, Hip, Knee
                    // Down: lying flat, Up: crunched up
                    ExerciseConfig(
                        exerciseType = type,
                        landmarks = if (useLeftSide) {
                            Triple(
                                LandmarkIndex.LEFT_SHOULDER,
                                LandmarkIndex.LEFT_HIP,
                                LandmarkIndex.LEFT_KNEE
                            )
                        } else {
                            Triple(
                                LandmarkIndex.RIGHT_SHOULDER,
                                LandmarkIndex.RIGHT_HIP,
                                LandmarkIndex.RIGHT_KNEE
                            )
                        },
                        angleName = "Torso",
                        // Crunch: Need to lie flat and crunch hard
                        // 50 deg gap prevents phantom reps
                        downThreshold = 160f,
                        upThreshold = 110f,
                        useLeftSide = useLeftSide
                    )
                }
            }
        }
    }
}