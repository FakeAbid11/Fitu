 package com.fitu.ui.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitu.data.local.UserPreferencesRepository
import com.fitu.data.local.dao.StepDao
import com.fitu.data.local.dao.WorkoutDao
import com.fitu.data.local.entity.StepEntity
import com.fitu.data.repository.StreakRepository
import com.fitu.data.repository.StreakData
import com.fitu.data.service.StepCounterService
import com.fitu.domain.repository.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: DashboardRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val stepDao: StepDao,
    private val streakRepository: StreakRepository,
    private val workoutDao: WorkoutDao
) : ViewModel() {

    companion object {
        private const val PREFS_NAME = "fitu_dashboard_prefs"
        private const val KEY_CELEBRATION_SHOWN_DATE = "celebration_shown_date"
    }

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // User info
    val userName: StateFlow<String> = userPreferencesRepository.userName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val dailyStepGoal: StateFlow<Int> = userPreferencesRepository.dailyStepGoal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10000)

    val dailyCalorieGoal: StateFlow<Int> = userPreferencesRepository.dailyCalorieGoal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2000)

    // User's physical stats for personalized calculations
    val userHeightCm: StateFlow<Int> = userPreferencesRepository.userHeightCm
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 170)

    val userWeightKg: StateFlow<Int> = userPreferencesRepository.userWeightKg
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 70)

    // Unit preference
    val useImperialUnits: StateFlow<Boolean> = userPreferencesRepository.useImperialUnits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Today's date formatted
    val todayDate: String = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())

    // Steps - Read directly from StepCounterService (real-time data)
    val currentSteps: StateFlow<Int> = StepCounterService.stepCount

    // Track if steps are initialized (to prevent showing 0)
    val isStepsInitialized: StateFlow<Boolean> = StepCounterService.isInitialized

    // Pull-to-refresh state
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Loading state for weekly steps chart
    private val _isWeeklyDataLoading = MutableStateFlow(true)
    val isWeeklyDataLoading: StateFlow<Boolean> = _isWeeklyDataLoading.asStateFlow()

    // Weekly progress - Raw data from database
    private val _weeklyStepsFromDb = MutableStateFlow<List<StepEntity>>(emptyList())

    // Weekly steps - Combines DB data with live step count for reactive updates
    val weeklySteps: StateFlow<List<Int>> = combine(
        _weeklyStepsFromDb,
        currentSteps
    ) { stepEntities, todaySteps ->
        buildWeeklyData(stepEntities, todaySteps)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf(0, 0, 0, 0, 0, 0, 0))

    // Calories
    private val _caloriesBurned = MutableStateFlow(0)
    val caloriesBurned: StateFlow<Int> = _caloriesBurned.asStateFlow()

    private val _caloriesConsumed = MutableStateFlow(0)
    val caloriesConsumed: StateFlow<Int> = _caloriesConsumed.asStateFlow()

    // Workouts completed today
    private val _workoutsCompleted = MutableStateFlow(0)
    val workoutsCompleted: StateFlow<Int> = _workoutsCompleted.asStateFlow()

    // Today's total reps from AI Coach workouts
    val todayTotalReps: StateFlow<Int> = run {
        val todayRange = getTodayRange()
        workoutDao.getTotalRepsInRange(todayRange.first, todayRange.second)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Birthday feature - simplified
    private val _showBirthdayDialog = MutableStateFlow(false)
    val showBirthdayDialog: StateFlow<Boolean> = _showBirthdayDialog.asStateFlow()

    private val _isBirthday = MutableStateFlow(false)
    val isBirthday: StateFlow<Boolean> = _isBirthday.asStateFlow()

    // Goal Celebration
    private val _showStepGoalCelebration = MutableStateFlow(false)
    val showStepGoalCelebration: StateFlow<Boolean> = _showStepGoalCelebration.asStateFlow()

    // Streak data
    private val _streakData = MutableStateFlow(StreakData())
    val streakData: StateFlow<StreakData> = _streakData.asStateFlow()

    private val _stepsNeededForStreak = MutableStateFlow(0)
    val stepsNeededForStreak: StateFlow<Int> = _stepsNeededForStreak.asStateFlow()

    init {
        loadDashboardData()
        loadWeeklySteps()
        loadStreakData()
        observeStepGoalCompletion()
    }

    /**
     * Check if celebration was already shown today
     */
    private fun hasCelebrationBeenShownToday(): Boolean {
        val today = getTodayDateString()
        val shownDate = prefs.getString(KEY_CELEBRATION_SHOWN_DATE, null)
        return shownDate == today
    }

    /**
     * Mark celebration as shown for today
     */
    private fun markCelebrationShown() {
        val today = getTodayDateString()
        prefs.edit().putString(KEY_CELEBRATION_SHOWN_DATE, today).apply()
    }

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                loadDashboardData()
                loadWeeklySteps()
                loadStreakData()
                delay(500)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            combine(currentSteps, userWeightKg) { steps, weight ->
                (steps * 0.04 * weight / 70).toInt()
            }.collect { burned ->
                _caloriesBurned.value = burned
            }
        }

        viewModelScope.launch {
            try {
                val todayRange = getTodayRange()
                repository.getMealsForDay(todayRange.first, todayRange.second).collect { meals ->
                    _caloriesConsumed.value = meals.sumOf { it.calories }
                }
            } catch (e: Exception) {
                _caloriesConsumed.value = 0
            }
        }

        viewModelScope.launch {
            try {
                val todayRange = getTodayRange()
                repository.getWorkoutsForDay(todayRange.first, todayRange.second).collect { workouts ->
                    _workoutsCompleted.value = workouts.size
                }
            } catch (e: Exception) {
                _workoutsCompleted.value = 0
            }
        }
    }

    private fun loadWeeklySteps() {
        viewModelScope.launch(Dispatchers.IO) {
            _isWeeklyDataLoading.value = true
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val calendar = Calendar.getInstance()
                val endDate = dateFormat.format(calendar.time)

                calendar.add(Calendar.DAY_OF_YEAR, -6)
                val startDate = dateFormat.format(calendar.time)

                val steps = stepDao.getStepsBetweenDatesSync(startDate, endDate)
                _weeklyStepsFromDb.value = steps
            } catch (e: Exception) {
                _weeklyStepsFromDb.value = emptyList()
            } finally {
                _isWeeklyDataLoading.value = false
            }
        }
    }

    private fun buildWeeklyData(stepEntities: List<StepEntity>, todaySteps: Int): List<Int> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayDate = dateFormat.format(Date())

        val stepsMap = stepEntities.associate { it.date to it.steps }.toMutableMap()
        stepsMap[todayDate] = todaySteps

        val weeklyList = mutableListOf<Int>()
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -6)

        for (i in 0..6) {
            val date = dateFormat.format(calendar.time)
            weeklyList.add(stepsMap[date] ?: 0)
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return weeklyList
    }

    private fun loadStreakData() {
        viewModelScope.launch {
            streakRepository.getStreakData().collect { data ->
                _streakData.value = data
            }
        }

        viewModelScope.launch {
            _stepsNeededForStreak.value = streakRepository.getStepsNeededForStreak()
        }
    }

    private fun observeStepGoalCompletion() {
        viewModelScope.launch {
            combine(currentSteps, dailyStepGoal) { steps, goal ->
                steps >= goal && goal > 0
            }
                .distinctUntilChanged()
                .collect { goalReached ->
                    // Only show celebration if:
                    // 1. Goal is reached
                    // 2. Celebration hasn't been shown today yet
                    if (goalReached && !hasCelebrationBeenShownToday()) {
                        _showStepGoalCelebration.value = true
                        markCelebrationShown()
                        loadStreakData()
                    }
                }
        }
    }

    fun dismissBirthdayDialog() {
        _showBirthdayDialog.value = false
    }

    fun dismissStepGoalCelebration() {
        _showStepGoalCelebration.value = false
    }

    fun refreshStreakData() {
        loadStreakData()
    }

    fun refreshData() {
        loadDashboardData()
        loadWeeklySteps()
        loadStreakData()
    }

    private fun getTodayRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val end = calendar.timeInMillis

        return Pair(start, end)
    }
} 
