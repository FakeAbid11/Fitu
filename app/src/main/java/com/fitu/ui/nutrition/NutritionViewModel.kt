package com.fitu.ui.nutrition

import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitu.data.local.UserPreferencesRepository
import com.fitu.data.local.dao.MealDao
import com.fitu.data.local.entity.MealEntity
import com.fitu.di.GeminiErrorType
import com.fitu.di.GeminiException
import com.fitu.di.GeminiModelProvider
import com.fitu.domain.repository.DashboardRepository
import com.google.ai.client.generativeai.type.content
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class NutritionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val geminiModelProvider: GeminiModelProvider,
    private val repository: DashboardRepository,
    private val mealDao: MealDao,
    private val foodCacheDao: com.fitu.data.local.dao.FoodCacheDao,
    userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    companion object {
        private const val TAG = "NutritionViewModel"
        private const val MAX_IMAGE_WIDTH = 1024
        private const val MAX_IMAGE_HEIGHT = 1024
        private const val JPEG_QUALITY = 85
        private const val THUMBNAIL_SIZE = 200
        private const val THUMBNAIL_QUALITY = 80
        private const val MIN_REQUEST_INTERVAL_MS = 2000L
        private const val DUPLICATE_DETECTION_WINDOW_MS = 5000L
    }

    // ==================== GLOBAL SNACKBAR EVENT (NEW) ====================
    private val _globalSnackbarEvent = MutableSharedFlow<GlobalSnackbarEvent>()
    val globalSnackbarEvent: SharedFlow<GlobalSnackbarEvent> = _globalSnackbarEvent.asSharedFlow()

    // ==================== BACKGROUND ANALYSIS STATE ====================
    private val _backgroundAnalysisState = MutableStateFlow<BackgroundAnalysisState>(BackgroundAnalysisState.Idle)
    val backgroundAnalysisState: StateFlow<BackgroundAnalysisState> = _backgroundAnalysisState.asStateFlow()

    private val _pendingAnalyzedFood = MutableStateFlow<PendingFoodAnalysis?>(null)
    val pendingAnalyzedFood: StateFlow<PendingFoodAnalysis?> = _pendingAnalyzedFood.asStateFlow()

    private val _showReviewSheet = MutableStateFlow(false)
    val showReviewSheet: StateFlow<Boolean> = _showReviewSheet.asStateFlow()

    private var backgroundAnalysisJob: Job? = null

    // ==================== EXISTING STATE ====================
    private val _uiState = MutableStateFlow<NutritionUiState>(NutritionUiState.Idle)
    val uiState: StateFlow<NutritionUiState> = _uiState

    private val _selectedMealType = MutableStateFlow(getMealTypeByTime())
    val selectedMealType: StateFlow<String> = _selectedMealType

    private val _showAddFoodSheet = MutableStateFlow(false)
    val showAddFoodSheet: StateFlow<Boolean> = _showAddFoodSheet

    private val _portion = MutableStateFlow(1f)
    val portion: StateFlow<Float> = _portion

    private val _analyzedFood = MutableStateFlow<AnalyzedFood?>(null)
    val analyzedFood: StateFlow<AnalyzedFood?> = _analyzedFood

    private val _textSearch = MutableStateFlow("")
    val textSearch: StateFlow<String> = _textSearch

    private val _mealToDelete = MutableStateFlow<MealEntity?>(null)
    val mealToDelete: StateFlow<MealEntity?> = _mealToDelete

    private val _showDeleteConfirmDialog = MutableStateFlow(false)
    val showDeleteConfirmDialog: StateFlow<Boolean> = _showDeleteConfirmDialog

    private val _currentPhotoBitmap = MutableStateFlow<Bitmap?>(null)
    val currentPhotoBitmap: StateFlow<Bitmap?> = _currentPhotoBitmap

    // Pull-to-refresh state
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private fun setPhotoBitmap(newBitmap: Bitmap?) {
        val oldBitmap = _currentPhotoBitmap.value
        if (oldBitmap != null && oldBitmap != newBitmap && !oldBitmap.isRecycled) {
            oldBitmap.recycle()
        }
        _currentPhotoBitmap.value = newBitmap
    }

    private var lastRequestTime = 0L
    private var currentAnalysisJob: Job? = null

    private var lastAddedFoodName: String? = null
    private var lastAddedFoodTime: Long = 0L
    private var lastAddedMealType: String? = null

    private val _showDuplicateWarning = MutableStateFlow(false)
    val showDuplicateWarning: StateFlow<Boolean> = _showDuplicateWarning

    private val _duplicateWarningMessage = MutableStateFlow("")
    val duplicateWarningMessage: StateFlow<String> = _duplicateWarningMessage

    val dailyCalorieGoal: StateFlow<Int> = userPreferencesRepository.dailyCalorieGoal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2000)

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
        val end = calendar.timeInMillis

        return Pair(start, end)
    }

    val todayMeals: StateFlow<List<MealEntity>> = mealDao.getMealsForDay(
        getTodayRange().first,
        getTodayRange().second
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val caloriesConsumed: StateFlow<Int> = mealDao.getCaloriesConsumedForDay(
        getTodayRange().first,
        getTodayRange().second
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val proteinConsumed: StateFlow<Int> = mealDao.getProteinForDay(
        getTodayRange().first,
        getTodayRange().second
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val carbsConsumed: StateFlow<Int> = mealDao.getCarbsForDay(
        getTodayRange().first,
        getTodayRange().second
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val fatsConsumed: StateFlow<Int> = mealDao.getFatsForDay(
        getTodayRange().first,
        getTodayRange().second
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val calorieProgress: StateFlow<Float> = combine(caloriesConsumed, dailyCalorieGoal) { consumed, goal ->
        if (goal > 0) (consumed.toFloat() / goal.toFloat()).coerceIn(0f, 1f) else 0f
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    init {
        cleanOldFoodCache()
        cleanOldFoodPhotos()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                delay(500)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun getMealTypeByTime(): String {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val currentMinutes = hour * 60 + minute

        val breakfastStart = 5 * 60
        val breakfastEnd = 11 * 60 + 59
        val lunchStart = 12 * 60
        val lunchEnd = 16 * 60
        val dinnerStart = 19 * 60
        val dinnerEnd = 22 * 60

        return when {
            currentMinutes in breakfastStart..breakfastEnd -> "breakfast"
            currentMinutes in lunchStart..lunchEnd -> "lunch"
            currentMinutes in dinnerStart..dinnerEnd -> "dinner"
            else -> "snacks"
        }
    }

    fun getMealTypeTimeRange(mealType: String): String {
        return when (mealType) {
            "breakfast" -> "5:00 AM - 12:00 PM"
            "lunch" -> "12:00 PM - 4:00 PM"
            "dinner" -> "7:00 PM - 10:00 PM"
            "snacks" -> "Anytime"
            else -> ""
        }
    }

    fun isSuggestedMealType(mealType: String): Boolean {
        return mealType == getMealTypeByTime()
    }

    private fun cleanOldFoodCache() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
                foodCacheDao.clearOldCache(sevenDaysAgo)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clean food cache", e)
            }
        }
    }

    private fun cleanOldFoodPhotos() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val photosDir = getFoodPhotosDir()
                if (photosDir.exists()) {
                    val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
                    photosDir.listFiles()?.forEach { file ->
                        if (file.lastModified() < thirtyDaysAgo) {
                            file.delete()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clean old food photos", e)
            }
        }
    }

    private fun getFoodPhotosDir(): File {
        val dir = File(context.filesDir, "food_photos")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private suspend fun saveFoodPhoto(bitmap: Bitmap): String? {
        return withContext(Dispatchers.IO) {
            try {
                val thumbnail = createThumbnail(bitmap)
                val filename = "food_${UUID.randomUUID()}.jpg"
                val file = File(getFoodPhotosDir(), filename)

                FileOutputStream(file).use { out ->
                    thumbnail.compress(Bitmap.CompressFormat.JPEG, THUMBNAIL_QUALITY, out)
                }

                if (thumbnail != bitmap && !thumbnail.isRecycled) {
                    thumbnail.recycle()
                }

                file.absolutePath
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save food photo", e)
                null
            }
        }
    }

    private fun createThumbnail(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val scaleFactor = minOf(
            THUMBNAIL_SIZE.toFloat() / width,
            THUMBNAIL_SIZE.toFloat() / height,
            1f
        )

        if (scaleFactor >= 1f) {
            return bitmap
        }

        val newWidth = (width * scaleFactor).toInt()
        val newHeight = (height * scaleFactor).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun deleteFoodPhoto(photoUri: String?) {
        if (photoUri.isNullOrBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(photoUri)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete food photo", e)
            }
        }
    }

    private fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun isRateLimited(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastRequestTime < MIN_REQUEST_INTERVAL_MS) {
            return true
        }
        lastRequestTime = now
        return false
    }

    private fun compressBitmap(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val scaleFactor = minOf(
            MAX_IMAGE_WIDTH.toFloat() / width,
            MAX_IMAGE_HEIGHT.toFloat() / height,
            1f
        )

        if (scaleFactor >= 1f) {
            return bitmap
        }

        val newWidth = (width * scaleFactor).toInt()
        val newHeight = (height * scaleFactor).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun getBitmapSizeKB(bitmap: Bitmap): Int {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
        return stream.size() / 1024
    }

    fun selectMealType(type: String) {
        _selectedMealType.value = type
    }

    fun showAddFood() {
        _selectedMealType.value = getMealTypeByTime()
        _showAddFoodSheet.value = true
    }

    fun hideAddFood() {
        _showAddFoodSheet.value = false
        _analyzedFood.value = null
        setPhotoBitmap(null)
        _portion.value = 1f
        _textSearch.value = ""
        _uiState.value = NutritionUiState.Idle
        _showDuplicateWarning.value = false
        _duplicateWarningMessage.value = ""
        currentAnalysisJob?.cancel()
    }

    fun updatePortion(value: Float) {
        _portion.value = value
    }

    fun updateTextSearch(value: String) {
        _textSearch.value = value
    }

    private fun isDuplicateFood(foodName: String, mealType: String): Boolean {
        val now = System.currentTimeMillis()
        val timeSinceLastAdd = now - lastAddedFoodTime

        if (lastAddedFoodName != null &&
            timeSinceLastAdd < DUPLICATE_DETECTION_WINDOW_MS &&
            lastAddedFoodName.equals(foodName, ignoreCase = true) &&
            lastAddedMealType == mealType
        ) {
            return true
        }
        return false
    }

    private fun findSimilarRecentMeal(foodName: String, mealType: String): MealEntity? {
        val recentMeals = todayMeals.value
        val fiveMinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000L)

        return recentMeals.find { meal ->
            meal.name.equals(foodName, ignoreCase = true) &&
                    meal.mealType == mealType &&
                    meal.timestamp > fiveMinutesAgo
        }
    }

    fun dismissDuplicateWarning() {
        _showDuplicateWarning.value = false
        _duplicateWarningMessage.value = ""
    }

    fun forceAddFoodToMeal() {
        _showDuplicateWarning.value = false
        _duplicateWarningMessage.value = ""
        addFoodToMealInternal(ignoreDuplicate = true)
    }

    // ==================== EMIT GLOBAL SNACKBAR EVENTS ====================
    private fun emitGlobalSnackbar(event: GlobalSnackbarEvent) {
        viewModelScope.launch {
            _globalSnackbarEvent.emit(event)
        }
    }

    // ==================== BACKGROUND ANALYSIS METHODS ====================
    fun analyzeFood(bitmap: Bitmap) {
        if (isRateLimited()) {
            _backgroundAnalysisState.value = BackgroundAnalysisState.Error(
                message = "Please wait a moment before scanning again",
                canRetry = true,
                retryBitmap = bitmap,
                retryQuery = null
            )
            emitGlobalSnackbar(GlobalSnackbarEvent.Error("Please wait a moment before scanning again", canRetry = true))
            return
        }

        if (!isOnline()) {
            _backgroundAnalysisState.value = BackgroundAnalysisState.Error(
                message = "No internet connection",
                canRetry = true,
                retryBitmap = bitmap,
                retryQuery = null
            )
            emitGlobalSnackbar(GlobalSnackbarEvent.Error("No internet connection", canRetry = true))
            return
        }

        backgroundAnalysisJob?.cancel()
        hideAddFood()

        _backgroundAnalysisState.value = BackgroundAnalysisState.Analyzing("Analyzing your food...")
        emitGlobalSnackbar(GlobalSnackbarEvent.Analyzing("Analyzing your food..."))

        backgroundAnalysisJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val compressedBitmap = compressBitmap(bitmap)

                val inputContent = content {
                    image(compressedBitmap)
                    text("""
                        Analyze this food image and estimate its nutritional content.
                        Return ONLY a valid JSON object with these exact fields:
                        {"name": "food name", "calories": number, "protein": number, "carbs": number, "fats": number}
                        Rules:
                        - "name" should be a short, descriptive name
                        - "calories" is in kcal (integer)
                        - "protein", "carbs", "fats" are in grams (integers)
                        - Return ONLY the JSON object
                    """.trimIndent())
                }

                val response = geminiModelProvider.generateContentWithRetry(
                    prompt = inputContent,
                    useFallback = true
                )

                val text = response.text?.trim() ?: throw GeminiException(
                    GeminiErrorType.EMPTY_RESPONSE,
                    "No response from AI"
                )

                val food = parseJsonResponse(text)
                val photoUri = saveFoodPhoto(bitmap)

                if (compressedBitmap != bitmap && !compressedBitmap.isRecycled) {
                    compressedBitmap.recycle()
                }

                _pendingAnalyzedFood.value = PendingFoodAnalysis(
                    food = food,
                    photoUri = photoUri,
                    bitmap = bitmap
                )

                _backgroundAnalysisState.value = BackgroundAnalysisState.Success(
                    "\"${food.name}\" analyzed! Tap to add."
                )
                emitGlobalSnackbar(GlobalSnackbarEvent.Success("\"${food.name}\" analyzed! Tap to add."))

                withContext(Dispatchers.Main) {
                    _showReviewSheet.value = true
                    _portion.value = 1f
                    _selectedMealType.value = getMealTypeByTime()
                }

            } catch (e: GeminiException) {
                val errorState = mapGeminiExceptionToBackgroundState(e, bitmap, null)
                _backgroundAnalysisState.value = errorState
                emitGlobalSnackbar(GlobalSnackbarEvent.Error(errorState.message, errorState.canRetry))
            } catch (e: Exception) {
                Log.e(TAG, "Background analysis failed", e)
                _backgroundAnalysisState.value = BackgroundAnalysisState.Error(
                    message = "Analysis failed. Tap to retry.",
                    canRetry = true,
                    retryBitmap = bitmap,
                    retryQuery = null
                )
                emitGlobalSnackbar(GlobalSnackbarEvent.Error("Analysis failed. Tap to retry.", canRetry = true))
            }
        }
    }

    fun searchFood(query: String) {
        if (query.isBlank()) return

        if (isRateLimited()) {
            _backgroundAnalysisState.value = BackgroundAnalysisState.Error(
                message = "Please wait a moment before searching again",
                canRetry = true,
                retryBitmap = null,
                retryQuery = query
            )
            emitGlobalSnackbar(GlobalSnackbarEvent.Error("Please wait a moment before searching again", canRetry = true))
            return
        }

        if (!isOnline()) {
            _backgroundAnalysisState.value = BackgroundAnalysisState.Error(
                message = "No internet connection",
                canRetry = true,
                retryBitmap = null,
                retryQuery = query
            )
            emitGlobalSnackbar(GlobalSnackbarEvent.Error("No internet connection", canRetry = true))
            return
        }

        backgroundAnalysisJob?.cancel()
        hideAddFood()

        _backgroundAnalysisState.value = BackgroundAnalysisState.Analyzing("Searching for \"$query\"...")
        emitGlobalSnackbar(GlobalSnackbarEvent.Analyzing("Searching for \"$query\"..."))

        backgroundAnalysisJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val cached = foodCacheDao.getCache(query.lowercase().trim())
                if (cached != null && System.currentTimeMillis() - cached.timestamp < 7 * 24 * 60 * 60 * 1000) {
                    try {
                        val food = parseJsonResponse(cached.resultJson)
                        _pendingAnalyzedFood.value = PendingFoodAnalysis(
                            food = food,
                            photoUri = null,
                            bitmap = null
                        )
                        _backgroundAnalysisState.value = BackgroundAnalysisState.Success(
                            "\"${food.name}\" found! Tap to add."
                        )
                        emitGlobalSnackbar(GlobalSnackbarEvent.Success("\"${food.name}\" found! Tap to add."))

                        withContext(Dispatchers.Main) {
                            _showReviewSheet.value = true
                            _portion.value = 1f
                            _selectedMealType.value = getMealTypeByTime()
                        }
                        return@launch
                    } catch (e: Exception) {
                        Log.w(TAG, "Cache invalid, fetching fresh")
                    }
                }

                val inputContent = content {
                    text("""
                        Estimate the nutritional information for: "$query"
                        Return ONLY a valid JSON object with these exact fields:
                        {"name": "food name", "calories": number, "protein": number, "carbs": number, "fats": number}
                    """.trimIndent())
                }

                val response = geminiModelProvider.generateContentWithRetry(
                    prompt = inputContent,
                    useFallback = true
                )

                val text = response.text?.trim() ?: throw GeminiException(
                    GeminiErrorType.EMPTY_RESPONSE,
                    "No response from AI"
                )

                val food = parseJsonResponse(text)

                val cleanJson = """{"name":"${food.name}","calories":${food.calories},"protein":${food.protein},"carbs":${food.carbs},"fats":${food.fats}}"""
                foodCacheDao.insertCache(
                    com.fitu.data.local.entity.FoodCacheEntity(
                        query = query.lowercase().trim(),
                        resultJson = cleanJson,
                        timestamp = System.currentTimeMillis()
                    )
                )

                _pendingAnalyzedFood.value = PendingFoodAnalysis(
                    food = food,
                    photoUri = null,
                    bitmap = null
                )

                _backgroundAnalysisState.value = BackgroundAnalysisState.Success(
                    "\"${food.name}\" found! Tap to add."
                )
                emitGlobalSnackbar(GlobalSnackbarEvent.Success("\"${food.name}\" found! Tap to add."))

                withContext(Dispatchers.Main) {
                    _showReviewSheet.value = true
                    _portion.value = 1f
                    _selectedMealType.value = getMealTypeByTime()
                }

            } catch (e: GeminiException) {
                val errorState = mapGeminiExceptionToBackgroundState(e, null, query)
                _backgroundAnalysisState.value = errorState
                emitGlobalSnackbar(GlobalSnackbarEvent.Error(errorState.message, errorState.canRetry))
            } catch (e: Exception) {
                Log.e(TAG, "Background search failed", e)
                _backgroundAnalysisState.value = BackgroundAnalysisState.Error(
                    message = "Search failed. Tap to retry.",
                    canRetry = true,
                    retryBitmap = null,
                    retryQuery = query
                )
                emitGlobalSnackbar(GlobalSnackbarEvent.Error("Search failed. Tap to retry.", canRetry = true))
            }
        }
    }

    fun retryBackgroundAnalysis() {
        val currentState = _backgroundAnalysisState.value
        if (currentState is BackgroundAnalysisState.Error && currentState.canRetry) {
            if (currentState.retryBitmap != null) {
                lastRequestTime = 0L
                analyzeFood(currentState.retryBitmap)
            } else if (currentState.retryQuery != null) {
                lastRequestTime = 0L
                searchFood(currentState.retryQuery)
            }
        }
    }

    fun openReviewSheet() {
        if (_pendingAnalyzedFood.value != null) {
            _showReviewSheet.value = true
            _portion.value = 1f
            _selectedMealType.value = getMealTypeByTime()
        }
    }

    fun closeReviewSheet() {
        _showReviewSheet.value = false
    }

    fun dismissBackgroundAnalysis() {
        _backgroundAnalysisState.value = BackgroundAnalysisState.Idle
        emitGlobalSnackbar(GlobalSnackbarEvent.Dismiss)
    }

    fun clearPendingAnalysis() {
        _backgroundAnalysisState.value = BackgroundAnalysisState.Idle
        _pendingAnalyzedFood.value = null
        _showReviewSheet.value = false
        emitGlobalSnackbar(GlobalSnackbarEvent.Dismiss)
    }

    fun addPendingFoodToMeal() {
        val pending = _pendingAnalyzedFood.value ?: return
        val food = pending.food
        val mealType = _selectedMealType.value
        val portionMultiplier = _portion.value

        viewModelScope.launch {
            val meal = MealEntity(
                name = food.name,
                calories = (food.calories * portionMultiplier).toInt(),
                protein = (food.protein * portionMultiplier).toInt(),
                carbs = (food.carbs * portionMultiplier).toInt(),
                fat = (food.fats * portionMultiplier).toInt(),
                date = System.currentTimeMillis(),
                timestamp = System.currentTimeMillis(),
                mealType = mealType,
                portion = portionMultiplier,
                photoUri = pending.photoUri
            )

            mealDao.insertMeal(meal)

            lastAddedFoodName = food.name
            lastAddedFoodTime = System.currentTimeMillis()
            lastAddedMealType = mealType

            clearPendingAnalysis()
        }
    }

    private fun mapGeminiExceptionToBackgroundState(
        e: GeminiException,
        bitmap: Bitmap?,
        query: String?
    ): BackgroundAnalysisState.Error {
        val message = when (e.errorType) {
            GeminiErrorType.API_KEY_MISSING -> "API key not set. Check Profile settings."
            GeminiErrorType.API_KEY_INVALID -> "Invalid API key. Check Profile settings."
            GeminiErrorType.RATE_LIMITED -> "Too many requests. Try again later."
            GeminiErrorType.NETWORK_ERROR -> "No internet connection."
            else -> e.message
        }

        val canRetry = e.errorType != GeminiErrorType.API_KEY_MISSING &&
                e.errorType != GeminiErrorType.API_KEY_INVALID

        return BackgroundAnalysisState.Error(
            message = message,
            canRetry = canRetry,
            retryBitmap = if (canRetry) bitmap else null,
            retryQuery = if (canRetry) query else null
        )
    }

    private fun parseJsonResponse(text: String): AnalyzedFood {
        val cleanText = text
            .removePrefix("```json")
            .removePrefix("```JSON")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        try {
            val json = JSONObject(cleanText)

            val name = json.optString("name", "").ifBlank {
                throw IllegalArgumentException("Missing food name")
            }

            val calories = json.optInt("calories", -1).let {
                if (it < 0) throw IllegalArgumentException("Invalid calories")
                it
            }

            val protein = json.optInt("protein", 0).coerceAtLeast(0)
            val carbs = json.optInt("carbs", 0).coerceAtLeast(0)
            val fats = json.optInt("fats", 0).coerceAtLeast(0)

            return AnalyzedFood(
                name = name.take(100),
                calories = calories.coerceIn(0, 10000),
                protein = protein.coerceIn(0, 1000),
                carbs = carbs.coerceIn(0, 1000),
                fats = fats.coerceIn(0, 1000)
            )
        } catch (e: Exception) {
            throw GeminiException(
                GeminiErrorType.INVALID_REQUEST,
                "Could not understand the AI response. Please try again."
            )
        }
    }

    private fun mapGeminiExceptionToUiState(e: GeminiException): NutritionUiState.Error {
        return when (e.errorType) {
            GeminiErrorType.API_KEY_MISSING -> NutritionUiState.Error(
                message = "API key not set. Please add your Gemini API key in Profile settings.",
                errorType = NutritionErrorType.API_KEY,
                canRetry = false
            )
            GeminiErrorType.API_KEY_INVALID -> NutritionUiState.Error(
                message = "Invalid API key. Please check your Gemini API key in Profile settings.",
                errorType = NutritionErrorType.API_KEY,
                canRetry = false
            )
            GeminiErrorType.RATE_LIMITED -> NutritionUiState.Error(
                message = "Too many requests. Please wait a moment and try again.",
                errorType = NutritionErrorType.RATE_LIMIT,
                canRetry = true
            )
            GeminiErrorType.NETWORK_ERROR -> NutritionUiState.Error(
                message = "No internet connection. Please check your network.",
                errorType = NutritionErrorType.NETWORK,
                canRetry = true
            )
            else -> NutritionUiState.Error(
                message = e.message,
                errorType = NutritionErrorType.UNKNOWN,
                canRetry = true
            )
        }
    }

    fun addFoodToMeal() {
        addFoodToMealInternal(ignoreDuplicate = false)
    }

    private fun addFoodToMealInternal(ignoreDuplicate: Boolean) {
        val food = _analyzedFood.value ?: return
        val mealType = _selectedMealType.value
        val portionMultiplier = _portion.value

        if (!ignoreDuplicate) {
            if (isDuplicateFood(food.name, mealType)) {
                _duplicateWarningMessage.value = "You just added \"${food.name}\" to ${mealType}. Add again?"
                _showDuplicateWarning.value = true
                return
            }

            val similarMeal = findSimilarRecentMeal(food.name, mealType)
            if (similarMeal != null) {
                _duplicateWarningMessage.value = "\"${food.name}\" was already added to ${mealType} recently. Add another serving?"
                _showDuplicateWarning.value = true
                return
            }
        }

        viewModelScope.launch {
            val photoUri = _currentPhotoBitmap.value?.let { bitmap ->
                saveFoodPhoto(bitmap)
            }

            val meal = MealEntity(
                name = food.name,
                calories = (food.calories * portionMultiplier).toInt(),
                protein = (food.protein * portionMultiplier).toInt(),
                carbs = (food.carbs * portionMultiplier).toInt(),
                fat = (food.fats * portionMultiplier).toInt(),
                date = System.currentTimeMillis(),
                timestamp = System.currentTimeMillis(),
                mealType = mealType,
                portion = portionMultiplier,
                photoUri = photoUri
            )

            mealDao.insertMeal(meal)

            lastAddedFoodName = food.name
            lastAddedFoodTime = System.currentTimeMillis()
            lastAddedMealType = mealType

            hideAddFood()
        }
    }

    fun requestDeleteMeal(meal: MealEntity) {
        _mealToDelete.value = meal
        _showDeleteConfirmDialog.value = true
    }

    fun cancelDeleteMeal() {
        _mealToDelete.value = null
        _showDeleteConfirmDialog.value = false
    }

    fun confirmDeleteMeal() {
        val meal = _mealToDelete.value ?: return
        viewModelScope.launch {
            deleteFoodPhoto(meal.photoUri)
            mealDao.deleteMeal(meal.id)
            _mealToDelete.value = null
            _showDeleteConfirmDialog.value = false
        }
    }

    fun reset() {
        _uiState.value = NutritionUiState.Idle
        _analyzedFood.value = null
        setPhotoBitmap(null)
    }

    fun retry() {
        _uiState.value = NutritionUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        setPhotoBitmap(null)
        currentAnalysisJob?.cancel()
        backgroundAnalysisJob?.cancel()
    }
}

// ==================== DATA CLASSES ====================

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

enum class NutritionErrorType {
    NETWORK, API_KEY, RATE_LIMIT, CONTENT, SERVICE, UNKNOWN
}

sealed class NutritionUiState {
    object Idle : NutritionUiState()
    object Analyzing : NutritionUiState()
    data class Success(val food: AnalyzedFood) : NutritionUiState()
    data class Error(
        val message: String,
        val errorType: NutritionErrorType = NutritionErrorType.UNKNOWN,
        val canRetry: Boolean = true
    ) : NutritionUiState()
}

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

// ==================== GLOBAL SNACKBAR EVENT (NEW) ====================
sealed class GlobalSnackbarEvent {
    data class Analyzing(val message: String) : GlobalSnackbarEvent()
    data class Success(val message: String) : GlobalSnackbarEvent()
    data class Error(val message: String, val canRetry: Boolean) : GlobalSnackbarEvent()
    object Dismiss : GlobalSnackbarEvent()
}
