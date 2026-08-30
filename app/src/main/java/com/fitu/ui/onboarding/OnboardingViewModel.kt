package com.fitu.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitu.data.local.SecureStorage
import com.fitu.data.local.UserPreferencesRepository
import com.fitu.util.BirthdayUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val secureStorage: SecureStorage
) : ViewModel() {

    // Page 1 - Personal Info
    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name

    // Height in Feet and Inches
    private val _heightFeet = MutableStateFlow("5")
    val heightFeet: StateFlow<String> = _heightFeet

    private val _heightInches = MutableStateFlow("7")
    val heightInches: StateFlow<String> = _heightInches

    private val _weightKg = MutableStateFlow("70")
    val weightKg: StateFlow<String> = _weightKg

    private val _stepGoal = MutableStateFlow("10000")
    val stepGoal: StateFlow<String> = _stepGoal

    private val _calorieGoal = MutableStateFlow("2000")
    val calorieGoal: StateFlow<String> = _calorieGoal

    // Birth date (optional)
    private val _birthDay = MutableStateFlow<Int?>(null)
    val birthDay: StateFlow<Int?> = _birthDay

    private val _birthMonth = MutableStateFlow<Int?>(null)
    val birthMonth: StateFlow<Int?> = _birthMonth

    private val _birthYear = MutableStateFlow<Int?>(null)
    val birthYear: StateFlow<Int?> = _birthYear

    private val _showDatePicker = MutableStateFlow(false)
    val showDatePicker: StateFlow<Boolean> = _showDatePicker

    // Page 2 - API Setup
    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey

    private val _showApiKey = MutableStateFlow(false)
    val showApiKey: StateFlow<Boolean> = _showApiKey

    private val _apiKeyError = MutableStateFlow<String?>(null)
    val apiKeyError: StateFlow<String?> = _apiKeyError

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage

    // Validation errors
    private val _nameError = MutableStateFlow<String?>(null)
    val nameError: StateFlow<String?> = _nameError

    private val _heightError = MutableStateFlow<String?>(null)
    val heightError: StateFlow<String?> = _heightError

    private val _weightError = MutableStateFlow<String?>(null)
    val weightError: StateFlow<String?> = _weightError

    private val _stepGoalError = MutableStateFlow<String?>(null)
    val stepGoalError: StateFlow<String?> = _stepGoalError

    private val _calorieGoalError = MutableStateFlow<String?>(null)
    val calorieGoalError: StateFlow<String?> = _calorieGoalError

    fun updateName(value: String) {
        _name.value = value
        _nameError.value = null
    }

    fun updateHeightFeet(value: String) {
        // Allow only digits, max 1 digit for feet (3-8 range)
        _heightFeet.value = value.filter { it.isDigit() }.take(1)
        _heightError.value = null
    }

    fun updateHeightInches(value: String) {
        // Allow only digits, max 2 digits for inches (0-11 range)
        _heightInches.value = value.filter { it.isDigit() }.take(2)
        _heightError.value = null
    }

    /**
     * Convert feet and inches to centimeters
     */
    fun getHeightInCm(): Int {
        val feet = _heightFeet.value.toIntOrNull() ?: 5
        val inches = _heightInches.value.toIntOrNull() ?: 7
        return ((feet * 12 + inches) * 2.54).toInt()
    }

    fun updateWeightKg(value: String) {
        _weightKg.value = value.filter { it.isDigit() }
        _weightError.value = null
    }

    fun updateStepGoal(value: String) {
        _stepGoal.value = value.filter { it.isDigit() }
        _stepGoalError.value = null
    }

    fun updateCalorieGoal(value: String) {
        _calorieGoal.value = value.filter { it.isDigit() }
        _calorieGoalError.value = null
    }

    fun updateBirthDate(day: Int, month: Int, year: Int) {
        _birthDay.value = day
        _birthMonth.value = month
        _birthYear.value = year
    }

    fun clearBirthDate() {
        _birthDay.value = null
        _birthMonth.value = null
        _birthYear.value = null
    }

    fun showDatePicker() {
        _showDatePicker.value = true
    }

    fun hideDatePicker() {
        _showDatePicker.value = false
    }

    fun updateApiKey(value: String) {
        _apiKey.value = value
        _apiKeyError.value = null
    }

    fun toggleShowApiKey() {
        _showApiKey.value = !_showApiKey.value
    }

    /**
     * Validate Page 1 inputs with realistic ranges
     */
    fun validatePage1(): Boolean {
        var isValid = true

        // Name validation
        val trimmedName = _name.value.trim()
        when {
            trimmedName.isEmpty() -> {
                _nameError.value = "Name is required"
                isValid = false
            }
            trimmedName.length < 2 -> {
                _nameError.value = "Name must be at least 2 characters"
                isValid = false
            }
            trimmedName.length > 50 -> {
                _nameError.value = "Name must be less than 50 characters"
                isValid = false
            }
            !trimmedName.matches(Regex("^[a-zA-Z\\s]+$")) -> {
                _nameError.value = "Name can only contain letters and spaces"
                isValid = false
            }
        }

        // Height validation (feet: 3-8, inches: 0-11)
        val feet = _heightFeet.value.toIntOrNull()
        val inches = _heightInches.value.toIntOrNull() ?: 0
        when {
            feet == null || _heightFeet.value.isEmpty() -> {
                _heightError.value = "Feet is required"
                isValid = false
            }
            feet < 3 || feet > 8 -> {
                _heightError.value = "Feet must be between 3 and 8"
                isValid = false
            }
            inches < 0 || inches > 11 -> {
                _heightError.value = "Inches must be between 0 and 11"
                isValid = false
            }
        }

        // Weight validation (realistic range: 10kg - 500kg)
        val weight = _weightKg.value.toIntOrNull()
        when {
            weight == null || _weightKg.value.isEmpty() -> {
                _weightError.value = "Weight is required"
                isValid = false
            }
            weight < 10 -> {
                _weightError.value = "Weight must be at least 10 kg"
                isValid = false
            }
            weight > 500 -> {
                _weightError.value = "Weight must be less than 500 kg"
                isValid = false
            }
        }

        // Step goal validation (realistic range: 1000 - 100000)
        val steps = _stepGoal.value.toIntOrNull()
        when {
            steps == null || _stepGoal.value.isEmpty() -> {
                _stepGoalError.value = "Step goal is required"
                isValid = false
            }
            steps < 1000 -> {
                _stepGoalError.value = "Step goal must be at least 1,000"
                isValid = false
            }
            steps > 100000 -> {
                _stepGoalError.value = "Step goal must be less than 100,000"
                isValid = false
            }
        }

        return isValid
    }

    /**
     * Simple API key format validation - NO API call
     * Just checks if key starts with "AIza" and is longer than 20 characters
     */
    fun validateApiKeyFormat(): Boolean {
        val key = _apiKey.value.trim()

        when {
            key.isEmpty() -> {
                _apiKeyError.value = "API key is required"
                return false
            }
            key.length < 20 -> {
                _apiKeyError.value = "API key is too short"
                return false
            }
            !key.startsWith("AIza") -> {
                _apiKeyError.value = "Invalid API key format. Google AI keys start with 'AIza'"
                return false
            }
        }

        _apiKeyError.value = null
        return true
    }

    fun nextPage() {
        if (_currentPage.value == 0 && validatePage1()) {
            _currentPage.value = 1
        }
    }

    fun previousPage() {
        if (_currentPage.value > 0) {
            _currentPage.value -= 1
        }
    }

    /**
     * Get formatted birth date for display.
     */
    fun getFormattedBirthDate(): String? {
        return BirthdayUtils.formatBirthDate(_birthDay.value, _birthMonth.value, _birthYear.value)
    }

    /**
     * Calculate user's age from birth date.
     */
    fun getCalculatedAge(): Int {
        return BirthdayUtils.calculateAge(_birthDay.value, _birthMonth.value, _birthYear.value) ?: 25
    }

    /**
     * Complete onboarding - validates API key format and saves data
     */
    fun completeOnboarding(onComplete: () -> Unit) {
        // Validate API key format
        if (!validateApiKeyFormat()) {
            return
        }

        viewModelScope.launch {
            // Calculate age from birth date if available
            val calculatedAge = getCalculatedAge()

            // Save user profile (height converted to cm internally)
            userPreferencesRepository.saveUserProfile(
                name = _name.value.trim(),
                age = calculatedAge,
                heightCm = getHeightInCm(),
                weightKg = _weightKg.value.toIntOrNull() ?: 70,
                stepGoal = _stepGoal.value.toIntOrNull() ?: 10000,
                calorieGoal = _calorieGoal.value.toIntOrNull() ?: 2000
            )

            // Save birth date (optional)
            userPreferencesRepository.saveBirthDate(
                day = _birthDay.value,
                month = _birthMonth.value,
                year = _birthYear.value
            )

            // Save API key to SecureStorage (encrypted)
            secureStorage.saveApiKey(_apiKey.value.trim())

            // Mark onboarding complete
            userPreferencesRepository.setOnboardingComplete(true)

            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }
}
