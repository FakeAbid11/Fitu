package com.fitu.aicoach

/**
 * Enum representing supported exercise types for AI Coach.
 * 
 * @property displayName Human-readable name for the exercise
 * @property isTimeBased True for plank (time tracking), false for rep-based exercises
 * @property emoji Icon emoji for the exercise
 * @property caloriesPerRep Base calories burned per rep (for rep-based exercises)
 * @property caloriesPerMinute Base calories burned per minute (for time-based exercises)
 * 
 * Calorie values are base estimates for a 70kg person and will be adjusted
 * based on the user's actual weight.
 */
enum class ExerciseType(
    val displayName: String,
    val isTimeBased: Boolean,
    val emoji: String,
    val caloriesPerRep: Float,
    val caloriesPerMinute: Float
) {
    PUSH_UP(
        displayName = "Push-up",
        isTimeBased = false,
        emoji = "💪",
        caloriesPerRep = 0.4f,    // ~0.3-0.5 kcal per push-up
        caloriesPerMinute = 0f
    ),
    SQUAT(
        displayName = "Squat",
        isTimeBased = false,
        emoji = "🦵",
        caloriesPerRep = 0.35f,   // ~0.3-0.4 kcal per squat
        caloriesPerMinute = 0f
    ),
    PLANK(
        displayName = "Plank",
        isTimeBased = true,
        emoji = "🧘",
        caloriesPerRep = 0f,
        caloriesPerMinute = 4f    // ~3-5 kcal per minute
    ),
    DUMBBELL_CURL(
        displayName = "Curl",
        isTimeBased = false,
        emoji = "🏋️",
        caloriesPerRep = 0.2f,    // ~0.15-0.25 kcal per curl
        caloriesPerMinute = 0f
    ),
    CRUNCH(
        displayName = "Crunch",
        isTimeBased = false,
        emoji = "🔥",
        caloriesPerRep = 0.15f,   // ~0.1-0.2 kcal per crunch
        caloriesPerMinute = 0f
    );

    companion object {
        private const val BASE_WEIGHT_KG = 70f

        /**
         * Calculate calories burned for rep-based exercises
         * Adjusts based on user's weight
         */
        fun calculateCaloriesFromReps(
            exerciseType: ExerciseType,
            reps: Int,
            userWeightKg: Int
        ): Float {
            val weightMultiplier = userWeightKg.toFloat() / BASE_WEIGHT_KG
            return exerciseType.caloriesPerRep * reps * weightMultiplier
        }

        /**
         * Calculate calories burned for time-based exercises
         * Adjusts based on user's weight
         */
        fun calculateCaloriesFromTime(
            exerciseType: ExerciseType,
            durationMs: Long,
            userWeightKg: Int
        ): Float {
            val weightMultiplier = userWeightKg.toFloat() / BASE_WEIGHT_KG
            val minutes = durationMs / 60000f
            return exerciseType.caloriesPerMinute * minutes * weightMultiplier
        }

        /**
         * Get all rep-based exercises
         */
        fun getRepBasedExercises(): List<ExerciseType> = entries.filter { !it.isTimeBased }

        /**
         * Get all time-based exercises
         */
        fun getTimeBasedExercises(): List<ExerciseType> = entries.filter { it.isTimeBased }
    }
}
