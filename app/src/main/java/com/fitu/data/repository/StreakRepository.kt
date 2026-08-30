package com.fitu.data.repository

import com.fitu.data.local.UserPreferencesRepository
import com.fitu.data.local.dao.StepDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

data class StreakData(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastStreakDate: Long? = null,
    val streakHistory: List<Long> = emptyList()
)

@Singleton
class StreakRepository @Inject constructor(
    private val stepDao: StepDao,
    private val userPreferencesRepository: UserPreferencesRepository
) {

    /**
     * Calculate streak data based on step history and daily goal
     */
    fun getStreakData(): Flow<StreakData> = flow {
        val dailyGoal = userPreferencesRepository.dailyStepGoal.first()

        // Get last 365 days of step data
        val today = getStartOfDay(System.currentTimeMillis())
        val startDate = today - (365L * 24 * 60 * 60 * 1000)

        val stepRecords = stepDao.getStepsBetweenDatesSync(
            formatDateString(startDate),
            formatDateString(today + (24 * 60 * 60 * 1000) - 1)
        )

        // Convert to map of date string -> steps
        val stepsMap = stepRecords.associate { record ->
            record.date to record.steps
        }

        // Calculate current streak (consecutive days meeting goal, ending today or yesterday)
        var currentStreak = 0
        var checkDate = today

        // Check if today's goal is met, if not start from yesterday
        val todayDateStr = formatDateString(today)
        val todaySteps = stepsMap[todayDateStr] ?: 0
        if (todaySteps < dailyGoal) {
            checkDate = today - (24 * 60 * 60 * 1000) // yesterday
        }

        while (true) {
            val dateStr = formatDateString(checkDate)
            val steps = stepsMap[dateStr] ?: 0
            if (steps >= dailyGoal) {
                currentStreak++
                checkDate -= (24 * 60 * 60 * 1000) // go back one day
            } else {
                break
            }
        }

        // Calculate longest streak ever
        var longestStreak = 0
        var tempStreak = 0
        val streakDays = mutableListOf<Long>()

        var scanDate = startDate
        while (scanDate <= today) {
            val dateStr = formatDateString(scanDate)
            val steps = stepsMap[dateStr] ?: 0
            if (steps >= dailyGoal) {
                tempStreak++
                streakDays.add(scanDate)
            } else {
                if (tempStreak > longestStreak) {
                    longestStreak = tempStreak
                }
                tempStreak = 0
            }
            scanDate += (24 * 60 * 60 * 1000) // next day
        }

        // Check final streak
        if (tempStreak > longestStreak) {
            longestStreak = tempStreak
        }

        // Determine last streak date
        val lastStreakDate = if (currentStreak > 0) {
            if (todaySteps >= dailyGoal) today else today - (24 * 60 * 60 * 1000)
        } else {
            streakDays.lastOrNull()
        }

        emit(
            StreakData(
                currentStreak = currentStreak,
                longestStreak = longestStreak,
                lastStreakDate = lastStreakDate,
                streakHistory = streakDays
            )
        )
    }

    /**
     * Get steps needed to maintain/extend streak
     */
    suspend fun getStepsNeededForStreak(): Int {
        val dailyGoal = userPreferencesRepository.dailyStepGoal.first()
        val today = getStartOfDay(System.currentTimeMillis())
        val todayStr = formatDateString(today)

        val todayRecord = stepDao.getStepsForDate(todayStr)
        val todaySteps = todayRecord?.steps ?: 0

        return (dailyGoal - todaySteps).coerceAtLeast(0)
    }

    /**
     * Get start of day timestamp
     */
    private fun getStartOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * Format timestamp to date string "yyyy-MM-dd"
     */
    private fun formatDateString(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return String.format("%04d-%02d-%02d", year, month, day)
    }
}
