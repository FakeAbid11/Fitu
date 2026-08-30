package com.fitu.ui.steps

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitu.data.local.UserPreferencesRepository
import com.fitu.data.local.dao.StepDao
import com.fitu.data.local.entity.StepEntity
import com.fitu.data.service.StepCounterService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class StepsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val stepDao: StepDao,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val stepCount: StateFlow<Int> = StepCounterService.stepCount
    val motionMagnitude: StateFlow<Float> = StepCounterService.motionMagnitude

    val dailyStepGoal: StateFlow<Int> = userPreferencesRepository.dailyStepGoal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10000)

    val userHeightCm: StateFlow<Int> = userPreferencesRepository.userHeightCm
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 170)

    val userWeightKg: StateFlow<Int> = userPreferencesRepository.userWeightKg
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 70)

    val useImperialUnits: StateFlow<Boolean> = userPreferencesRepository.useImperialUnits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning

    val usesHardwareCounter: StateFlow<Boolean> = StepCounterService.usesHardwareCounter

    val progress: StateFlow<Float> = combine(stepCount, dailyStepGoal) { steps, goal ->
        if (goal > 0) (steps.toFloat() / goal.toFloat()).coerceIn(0f, 1f) else 0f
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val distanceKm: StateFlow<Float> = combine(stepCount, userHeightCm) { steps, heightCm ->
        val strideLengthKm = (heightCm * 0.415f) / 100_000f
        steps * strideLengthKm
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val formattedDistance: StateFlow<String> = combine(distanceKm, useImperialUnits) { km, useImperial ->
        if (useImperial) {
            val miles = km * 0.621371f
            String.format("%.2f", miles)
        } else {
            String.format("%.2f", km)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "0.00")

    val distanceUnit: StateFlow<String> = useImperialUnits.map { useImperial ->
        if (useImperial) "MI" else "KM"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "KM")

    val caloriesBurned: StateFlow<Int> = combine(stepCount, userWeightKg) { steps, weightKg ->
        (steps * weightKg * 0.00057f).toInt()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _isWeeklyDataLoading = MutableStateFlow(true)
    val isWeeklyDataLoading: StateFlow<Boolean> = _isWeeklyDataLoading

    private val _weeklyStepsFromDb = MutableStateFlow<List<StepEntity>>(emptyList())

    val weeklySteps: StateFlow<List<DaySteps>> = combine(
        _weeklyStepsFromDb,
        stepCount
    ) { stepEntities, todaySteps ->
        buildWeeklyDaySteps(stepEntities, todaySteps)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        checkServiceRunning()
        if (hasStepPermission()) {
            startService()
        }
        loadWeeklySteps()

        viewModelScope.launch {
            userPreferencesRepository.dailyStepGoal.collect { goal ->
                val prefs = context.getSharedPreferences("fitu_service_prefs", Context.MODE_PRIVATE)
                prefs.edit().putInt("daily_step_goal", goal).apply()
            }
        }

        viewModelScope.launch {
            while (true) {
                delay(5000)
                checkServiceRunning()
            }
        }
    }

    private fun hasStepPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    private fun checkServiceRunning() {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        val running = am.getRunningServices(Integer.MAX_VALUE)?.any {
            it.service.className == StepCounterService::class.java.name
        } ?: false
        _isServiceRunning.value = running
    }

    fun startService() {
        viewModelScope.launch {
            val goal = userPreferencesRepository.dailyStepGoal.first()
            val intent = Intent(context, StepCounterService::class.java).apply {
                putExtra("step_goal", goal)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            delay(500)
            checkServiceRunning()
        }
    }

    fun stopService() {
        context.stopService(Intent(context, StepCounterService::class.java))
        viewModelScope.launch {
            delay(500)
            checkServiceRunning()
        }
    }

    private fun loadWeeklySteps() {
        viewModelScope.launch {
            _isWeeklyDataLoading.value = true
            val df = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val cal = Calendar.getInstance()
            val end = df.format(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, -6)
            val start = df.format(cal.time)

            stepDao.getStepsBetweenDates(start, end).collect {
                _weeklyStepsFromDb.value = it
                _isWeeklyDataLoading.value = false
            }
        }
    }

    private fun buildWeeklyDaySteps(stepEntities: List<StepEntity>, todaySteps: Int): List<DaySteps> {
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dnf = SimpleDateFormat("EEE", Locale.US)
        val stepMap = stepEntities.associateBy { it.date }
        val today = StepCounterService.getTodayDate()
        val weekData = mutableListOf<DaySteps>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -6)
        for (i in 0..6) {
            val date = df.format(cal.time)
            val steps = if (date == today) todaySteps else stepMap[date]?.steps ?: 0
            weekData.add(DaySteps(dnf.format(cal.time), date, steps, date == today))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return weekData
    }
}
