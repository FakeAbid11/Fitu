package com.fitu.data.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.fitu.MainActivity
import com.fitu.R
import com.fitu.data.local.dao.StepDao
import com.fitu.data.local.entity.StepEntity
import com.fitu.widget.StepsWidget
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject

@AndroidEntryPoint
class StepCounterService : Service(), SensorEventListener {

    @Inject
    lateinit var sensorManager: SensorManager
    
    @Inject
    lateinit var stepDao: StepDao

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var stepCounterSensor: Sensor? = null
    private var stepDetectorSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null
    
    // Step tracking state
    private var initialStepCount: Int = -1
    private var lastKnownHardwareSteps: Int = -1
    private var stepsAtStartOfDay: Int = 0
    
    private var currentDate: String = ""
    private var dailyStepGoal: Int = 10000
    
    // Use synchronized set with lock object
    private val milestoneLock = Any()
    private val notifiedMilestones = mutableSetOf<Int>()

    // Persistent storage for step state
    private val stepStatePrefs: SharedPreferences by lazy {
        getSharedPreferences("fitu_step_state", Context.MODE_PRIVATE)
    }

    companion object {
        private const val TAG = "StepCounterService"
        
        private val _stepCount = MutableStateFlow(0)
        val stepCount: StateFlow<Int> = _stepCount
        
        private val _motionMagnitude = MutableStateFlow(0f)
        val motionMagnitude: StateFlow<Float> = _motionMagnitude
        
        private val _isInitialized = MutableStateFlow(false)
        val isInitialized: StateFlow<Boolean> = _isInitialized
        
        private val _usesHardwareCounter = MutableStateFlow(false)
        val usesHardwareCounter: StateFlow<Boolean> = _usesHardwareCounter
        
        private const val NOTIFICATION_ID = 1
        private const val MILESTONE_NOTIFICATION_ID = 2
        private const val CHANNEL_ID = "step_counter_channel"
        private const val MILESTONE_CHANNEL_ID = "step_milestone_channel"
        
        val MILESTONES = listOf(25, 50, 75, 100)
        
        // Pref keys for persisting step state
        private const val PREF_INITIAL_STEP_COUNT = "initial_step_count"
        private const val PREF_LAST_HARDWARE_STEPS = "last_hardware_steps"
        private const val PREF_STEPS_AT_START_OF_DAY = "steps_at_start_of_day"
        private const val PREF_CURRENT_DATE = "current_date"
        private const val PREF_LAST_SAVED_STEPS = "last_saved_steps"
        
        fun getTodayDate(): String {
            return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        }
        
        suspend fun preloadSteps(stepDao: StepDao) {
            val today = getTodayDate()
            val todaySteps = stepDao.getStepsForDate(today)
            _stepCount.value = todaySteps?.steps ?: 0
            _isInitialized.value = true
        }
    }

    override fun onCreate() {
        super.onCreate()
        currentDate = getTodayDate()
        
        // Restore persisted state first
        restorePersistedState()
        
        // Load today's steps from database
        loadTodayStepsAsync()
        
        loadStepGoal()
        loadNotifiedMilestones()
        
        startForegroundService()
        registerSensors()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val today = getTodayDate()
        if (today != currentDate) {
            handleDayChange(today)
        }
        
        intent?.getIntExtra("step_goal", -1)?.let { goal ->
            if (goal > 0) {
                dailyStepGoal = goal
                StepsWidget.saveStepGoal(applicationContext, goal)
            }
        }
        
        return START_STICKY
    }

    private fun restorePersistedState() {
        val savedDate = stepStatePrefs.getString(PREF_CURRENT_DATE, "") ?: ""
        val today = getTodayDate()
        
        if (savedDate == today) {
            initialStepCount = stepStatePrefs.getInt(PREF_INITIAL_STEP_COUNT, -1)
            lastKnownHardwareSteps = stepStatePrefs.getInt(PREF_LAST_HARDWARE_STEPS, -1)
            stepsAtStartOfDay = stepStatePrefs.getInt(PREF_STEPS_AT_START_OF_DAY, 0)
            val lastSavedSteps = stepStatePrefs.getInt(PREF_LAST_SAVED_STEPS, 0)
            _stepCount.value = lastSavedSteps
        } else {
            resetPersistedState()
        }
    }

    private fun persistState() {
        stepStatePrefs.edit()
            .putString(PREF_CURRENT_DATE, currentDate)
            .putInt(PREF_INITIAL_STEP_COUNT, initialStepCount)
            .putInt(PREF_LAST_HARDWARE_STEPS, lastKnownHardwareSteps)
            .putInt(PREF_STEPS_AT_START_OF_DAY, stepsAtStartOfDay)
            .putInt(PREF_LAST_SAVED_STEPS, _stepCount.value)
            .apply()
    }

    private fun resetPersistedState() {
        initialStepCount = -1
        lastKnownHardwareSteps = -1
        stepsAtStartOfDay = 0
        
        stepStatePrefs.edit()
            .putString(PREF_CURRENT_DATE, currentDate)
            .putInt(PREF_INITIAL_STEP_COUNT, -1)
            .putInt(PREF_LAST_HARDWARE_STEPS, -1)
            .putInt(PREF_STEPS_AT_START_OF_DAY, 0)
            .putInt(PREF_LAST_SAVED_STEPS, 0)
            .apply()
    }

    private fun handleDayChange(newDate: String) {
        saveSteps(_stepCount.value)
        currentDate = newDate
        _stepCount.value = 0
        stepsAtStartOfDay = 0
        initialStepCount = -1
        lastKnownHardwareSteps = -1
        
        synchronized(milestoneLock) {
            notifiedMilestones.clear()
        }
        clearNotifiedMilestones()
        resetPersistedState()
        loadTodayStepsAsync()
        StepsWidget.updateWidget(applicationContext)
    }

    private fun registerSensors() {
        // ✅ FIX #1: Guard against sensor access without permission (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) 
                != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Permission missing. Using accelerometer fallback.")
                _usesHardwareCounter.value = false
                registerAccelerometerFallback()
                return
            }
        }

        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        
        if (stepCounterSensor != null) {
            Log.d(TAG, "Using hardware TYPE_STEP_COUNTER")
            _usesHardwareCounter.value = true
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
            if (stepDetectorSensor != null) {
                _usesHardwareCounter.value = true
                sensorManager.registerListener(this, stepDetectorSensor, SensorManager.SENSOR_DELAY_UI)
            } else {
                _usesHardwareCounter.value = false
                registerAccelerometerFallback()
            }
        }
    }

    private fun registerAccelerometerFallback() {
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accelerometerSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        val today = getTodayDate()
        if (today != currentDate) handleDayChange(today)

        when (event.sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> handleStepCounter(event)
            Sensor.TYPE_STEP_DETECTOR -> handleStepDetector()
            Sensor.TYPE_ACCELEROMETER -> handleAccelerometer(event)
        }
    }

    private fun handleStepCounter(event: SensorEvent) {
        val totalStepsSinceBoot = event.values[0].toInt()
        if (initialStepCount < 0) {
            if (lastKnownHardwareSteps > 0 && totalStepsSinceBoot >= lastKnownHardwareSteps) {
                val stepsDuringDowntime = totalStepsSinceBoot - lastKnownHardwareSteps
                _stepCount.value += stepsDuringDowntime
                initialStepCount = totalStepsSinceBoot
                stepsAtStartOfDay = _stepCount.value
            } else {
                initialStepCount = totalStepsSinceBoot
                stepsAtStartOfDay = _stepCount.value
            }
        }
        
        val stepsSinceServiceStart = totalStepsSinceBoot - initialStepCount
        val todaySteps = stepsAtStartOfDay + stepsSinceServiceStart
        lastKnownHardwareSteps = totalStepsSinceBoot
        
        if (todaySteps != _stepCount.value && todaySteps >= 0) {
            _stepCount.value = todaySteps
            checkMilestones(todaySteps)
            saveStepsPeriodically(todaySteps)
            persistState()
            StepsWidget.updateWidget(applicationContext)
        }
    }

    private fun handleStepDetector() {
        val newSteps = _stepCount.value + 1
        _stepCount.value = newSteps
        checkMilestones(newSteps)
        saveStepsPeriodically(newSteps)
        persistState()
        StepsWidget.updateWidget(applicationContext)
    }

    // Accelerometer math logic
    private val gravity = FloatArray(3)
    private var smoothedMag = 0.0
    private var isBelowReset = true
    private var lastStepTime = 0L
    private val ALPHA = 0.92f
    private val SMOOTH_FACTOR = 0.7f
    private val STEP_THRESHOLD = 2.4f
    private val RESET_THRESHOLD = 1.2f
    private val MIN_STEP_TIME = 420L

    private fun handleAccelerometer(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        gravity[0] = ALPHA * gravity[0] + (1 - ALPHA) * x
        gravity[1] = ALPHA * gravity[1] + (1 - ALPHA) * y
        gravity[2] = ALPHA * gravity[2] + (1 - ALPHA) * z
        val lx = x - gravity[0]
        val ly = y - gravity[1]
        val lz = z - gravity[2]
        val rawMag = Math.sqrt((lx * lx + ly * ly + lz * lz).toDouble()).toFloat()
        smoothedMag = (SMOOTH_FACTOR * smoothedMag) + ((1 - SMOOTH_FACTOR) * rawMag)
        _motionMagnitude.value = smoothedMag.toFloat()

        val now = System.currentTimeMillis()
        if (smoothedMag > STEP_THRESHOLD && isBelowReset) {
            if (now - lastStepTime > MIN_STEP_TIME) {
                val newSteps = _stepCount.value + 1
                _stepCount.value = newSteps
                lastStepTime = now
                isBelowReset = false
                checkMilestones(newSteps)
                saveStepsPeriodically(newSteps)
                persistState()
                StepsWidget.updateWidget(applicationContext)
            }
        } else if (smoothedMag < RESET_THRESHOLD) {
            isBelowReset = true
        }
    }

    private val lastSaveTime = AtomicLong(0L)
    private val SAVE_INTERVAL = 10000L 

    private fun saveStepsPeriodically(steps: Int) {
        val now = System.currentTimeMillis()
        val lastSave = lastSaveTime.get()
        if (now - lastSave > SAVE_INTERVAL) {
            if (lastSaveTime.compareAndSet(lastSave, now)) {
                saveSteps(steps)
            }
        }
    }

    private fun saveSteps(steps: Int) {
        serviceScope.launch {
            try {
                val entity = StepEntity(date = currentDate, steps = steps, lastUpdated = System.currentTimeMillis())
                stepDao.insertOrUpdate(entity)
                updateNotification(steps)
                stepStatePrefs.edit().putInt(PREF_LAST_SAVED_STEPS, steps).apply()
            } catch (e: Exception) {
                Log.e(TAG, "Error saving steps", e)
            }
        }
    }

    private fun loadStepGoal() {
        val prefs = getSharedPreferences("fitu_service_prefs", Context.MODE_PRIVATE)
        dailyStepGoal = prefs.getInt("daily_step_goal", 10000)
    }

    private fun loadNotifiedMilestones() {
        val prefs = getSharedPreferences("fitu_service_prefs", Context.MODE_PRIVATE)
        val savedDate = prefs.getString("milestone_date", "")
        synchronized(milestoneLock) {
            notifiedMilestones.clear()
            if (savedDate == currentDate) {
                val savedMilestones = prefs.getStringSet("notified_milestones", emptySet()) ?: emptySet()
                savedMilestones.mapNotNull { it.toIntOrNull() }.forEach { notifiedMilestones.add(it) }
            }
        }
    }

    private fun saveNotifiedMilestones() {
        val prefs = getSharedPreferences("fitu_service_prefs", Context.MODE_PRIVATE)
        val milestoneCopy: Set<String>
        synchronized(milestoneLock) {
            milestoneCopy = notifiedMilestones.map { it.toString() }.toSet()
        }
        prefs.edit().putString("milestone_date", currentDate).putStringSet("notified_milestones", milestoneCopy).apply()
    }

    private fun clearNotifiedMilestones() {
        val prefs = getSharedPreferences("fitu_service_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("milestone_date", currentDate).putStringSet("notified_milestones", emptySet()).apply()
    }

    private fun loadTodayStepsAsync() {
        serviceScope.launch {
            try {
                val todaySteps = stepDao.getStepsForDate(currentDate)
                val dbSteps = todaySteps?.steps ?: 0
                if (dbSteps > _stepCount.value || _stepCount.value == 0) {
                    _stepCount.value = dbSteps
                    stepsAtStartOfDay = dbSteps
                    stepStatePrefs.edit().putInt(PREF_STEPS_AT_START_OF_DAY, dbSteps).putInt(PREF_LAST_SAVED_STEPS, dbSteps).apply()
                    StepsWidget.updateWidget(applicationContext)
                }
                _isInitialized.value = true
            } catch (e: Exception) {
                _isInitialized.value = true
            }
        }
    }

    private fun startForegroundService() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Step Counter", NotificationManager.IMPORTANCE_LOW))
            notificationManager.createNotificationChannel(NotificationChannel(MILESTONE_CHANNEL_ID, "Step Milestones", NotificationManager.IMPORTANCE_HIGH))
        }

        val notification = buildNotification(_stepCount.value)

        // ✅ FIX #2: Android 14 (API 34) Foreground Service Type Fix
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(steps: Int): Notification {
        val pendingIntent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val progress = if (dailyStepGoal > 0) ((steps.toFloat() / dailyStepGoal) * 100).toInt() else 0
        val sensorType = if (_usesHardwareCounter.value) "HW" else "SW"
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🏃 Fitu Step Counter")
            .setContentText("$steps steps today ($progress%) [$sensorType]")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true).setSilent(true).build()
    }

    private fun updateNotification(steps: Int) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(steps))
    }

    private fun checkMilestones(steps: Int) {
        if (dailyStepGoal <= 0) return
        val currentProgress = ((steps.toFloat() / dailyStepGoal) * 100).toInt()
        for (milestone in MILESTONES) {
            val shouldNotify: Boolean
            synchronized(milestoneLock) {
                shouldNotify = currentProgress >= milestone && !notifiedMilestones.contains(milestone)
                if (shouldNotify) notifiedMilestones.add(milestone)
            }
            if (shouldNotify) {
                sendMilestoneNotification(steps, milestone)
                saveNotifiedMilestones()
            }
        }
    }

    private fun sendMilestoneNotification(steps: Int, milestone: Int) {
        val pendingIntent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val (title, message) = when (milestone) {
            25 -> "🏃 25% Complete!" to "You've taken $steps steps. Keep moving!"
            50 -> "💪 50% Complete!" to "You've taken $steps steps. Halfway there!"
            75 -> "🔥 75% Complete!" to "You've taken $steps steps. Almost at your goal!"
            100 -> "🎉 GOAL REACHED!" to "Congratulations! You've completed $steps steps today!"
            else -> "Step Progress" to "You've taken $steps steps"
        }
        val notification = NotificationCompat.Builder(this, MILESTONE_CHANNEL_ID)
            .setContentTitle(title).setContentText(message).setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent).setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_VIBRATE).build()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(MILESTONE_NOTIFICATION_ID + milestone, notification)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() {
        super.onDestroy()
        saveSteps(_stepCount.value)
        persistState()
        StepsWidget.updateWidget(applicationContext)
        serviceScope.cancel()
    }
}
