package com.fitu.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitu.data.local.SecureStorage
import com.fitu.data.local.UserPreferencesRepository
import com.fitu.domain.repository.BackupRepository
import com.fitu.util.BirthdayUtils
import com.fitu.util.UnitConverter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.pow

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val secureStorage: SecureStorage,
    private val backupRepository: BackupRepository
) : ViewModel() {

    val userName: StateFlow<String> = userPreferencesRepository.userName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val userAge: StateFlow<Int> = userPreferencesRepository.userAge
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 25)

    val userHeightCm: StateFlow<Int> = userPreferencesRepository.userHeightCm
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 170)

    val userWeightKg: StateFlow<Int> = userPreferencesRepository.userWeightKg
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 70)

    val dailyStepGoal: StateFlow<Int> = userPreferencesRepository.dailyStepGoal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10000)

    val dailyCalorieGoal: StateFlow<Int> = userPreferencesRepository.dailyCalorieGoal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2000)

    val apiKey: StateFlow<String> = secureStorage.apiKeyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // Birth date fields
    val birthDay: StateFlow<Int?> = userPreferencesRepository.birthDay
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val birthMonth: StateFlow<Int?> = userPreferencesRepository.birthMonth
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val birthYear: StateFlow<Int?> = userPreferencesRepository.birthYear
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val formattedBirthDate: StateFlow<String?> = combine(birthDay, birthMonth, birthYear) { day, month, year ->
        BirthdayUtils.formatBirthDate(day, month, year)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val calculatedAge: StateFlow<Int?> = combine(birthDay, birthMonth, birthYear) { day, month, year ->
        BirthdayUtils.calculateAge(day, month, year)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val useImperialUnits: StateFlow<Boolean> = userPreferencesRepository.useImperialUnits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val formattedHeight: StateFlow<String> = combine(userHeightCm, useImperialUnits) { heightCm, useImperial ->
        UnitConverter.formatHeight(heightCm, useImperial)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "170 cm")

    val formattedWeight: StateFlow<String> = combine(userWeightKg, useImperialUnits) { weightKg, useImperial ->
        UnitConverter.formatWeight(weightKg, useImperial)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "70 kg")

    // BMI Calculation
    val bmi: StateFlow<Float> = combine(userHeightCm, userWeightKg) { height, weight ->
        if (height > 0) {
            val heightM = height / 100f
            weight / heightM.pow(2)
        } else 0f
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val bmiCategory: StateFlow<String> = bmi.combine(flowOf(Unit)) { bmiValue, _ ->
        when {
            bmiValue < 18.5f -> "Underweight"
            bmiValue < 25f -> "Normal"
            bmiValue < 30f -> "Overweight"
            else -> "Obese"
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing

    private val _showApiKeyDialog = MutableStateFlow(false)
    val showApiKeyDialog: StateFlow<Boolean> = _showApiKeyDialog

    private val _showAboutDialog = MutableStateFlow(false)
    val showAboutDialog: StateFlow<Boolean> = _showAboutDialog

    private val _showBirthDatePicker = MutableStateFlow(false)
    val showBirthDatePicker: StateFlow<Boolean> = _showBirthDatePicker

    private val _apiKeyError = MutableStateFlow<String?>(null)
    val apiKeyError: StateFlow<String?> = _apiKeyError

    private val _profileValidationErrors = MutableStateFlow<ProfileValidationErrors>(ProfileValidationErrors())
    val profileValidationErrors: StateFlow<ProfileValidationErrors> = _profileValidationErrors

    fun toggleEdit() {
        _isEditing.value = !_isEditing.value
    }

    fun showApiKeyDialog() {
        _apiKeyError.value = null
        _showApiKeyDialog.value = true
    }

    fun hideApiKeyDialog() {
        _showApiKeyDialog.value = false
        _apiKeyError.value = null
    }

    fun showAboutDialog() {
        _showAboutDialog.value = true
    }

    fun hideAboutDialog() {
        _showAboutDialog.value = false
    }

    fun showBirthDatePicker() {
        _showBirthDatePicker.value = true
    }

    fun hideBirthDatePicker() {
        _showBirthDatePicker.value = false
    }

    fun toggleUnitPreference() {
        viewModelScope.launch {
            val currentValue = useImperialUnits.value
            userPreferencesRepository.setUseImperialUnits(!currentValue)
        }
    }

    fun setUnitPreference(useImperial: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setUseImperialUnits(useImperial)
        }
    }

    fun validateProfile(
        name: String,
        age: Int,
        heightCm: Int,
        weightKg: Int,
        stepGoal: Int,
        calorieGoal: Int
    ): Boolean {
        val errors = ProfileValidationErrors()
        var isValid = true

        val trimmedName = name.trim()
        when {
            trimmedName.isEmpty() -> {
                errors.nameError = "Name is required"
                isValid = false
            }
            trimmedName.length < 2 -> {
                errors.nameError = "Name must be at least 2 characters"
                isValid = false
            }
            trimmedName.length > 50 -> {
                errors.nameError = "Name must be less than 50 characters"
                isValid = false
            }
        }

        when {
            age < 1 -> {
                errors.ageError = "Age must be at least 1"
                isValid = false
            }
            age > 150 -> {
                errors.ageError = "Age must be less than 150"
                isValid = false
            }
        }

        when {
            heightCm < 50 -> {
                errors.heightError = "Height must be at least 50 cm"
                isValid = false
            }
            heightCm > 300 -> {
                errors.heightError = "Height must be less than 300 cm"
                isValid = false
            }
        }

        when {
            weightKg < 10 -> {
                errors.weightError = "Weight must be at least 10 kg"
                isValid = false
            }
            weightKg > 500 -> {
                errors.weightError = "Weight must be less than 500 kg"
                isValid = false
            }
        }

        when {
            stepGoal < 1000 -> {
                errors.stepGoalError = "Step goal must be at least 1,000"
                isValid = false
            }
            stepGoal > 100000 -> {
                errors.stepGoalError = "Step goal must be less than 100,000"
                isValid = false
            }
        }

        when {
            calorieGoal < 500 -> {
                errors.calorieGoalError = "Calorie goal must be at least 500"
                isValid = false
            }
            calorieGoal > 10000 -> {
                errors.calorieGoalError = "Calorie goal must be less than 10,000"
                isValid = false
            }
        }

        _profileValidationErrors.value = errors
        return isValid
    }

    fun clearValidationErrors() {
        _profileValidationErrors.value = ProfileValidationErrors()
    }

    fun saveProfile(
        name: String,
        age: Int,
        heightCm: Int,
        weightKg: Int,
        stepGoal: Int,
        calorieGoal: Int
    ) {
        if (!validateProfile(name, age, heightCm, weightKg, stepGoal, calorieGoal)) {
            return
        }

        viewModelScope.launch {
            userPreferencesRepository.saveUserProfile(
                name = name.trim(),
                age = age,
                heightCm = heightCm,
                weightKg = weightKg,
                stepGoal = stepGoal,
                calorieGoal = calorieGoal
            )
            _isEditing.value = false
            clearValidationErrors()
        }
    }

    fun saveBirthDate(day: Int, month: Int, year: Int) {
        viewModelScope.launch {
            userPreferencesRepository.saveBirthDate(day, month, year)
            val calculatedAge = BirthdayUtils.calculateAge(day, month, year) ?: 25
            userPreferencesRepository.saveUserProfile(
                name = userName.value,
                age = calculatedAge,
                heightCm = userHeightCm.value,
                weightKg = userWeightKg.value,
                stepGoal = dailyStepGoal.value,
                calorieGoal = dailyCalorieGoal.value
            )
            _showBirthDatePicker.value = false
        }
    }

    fun clearBirthDate() {
        viewModelScope.launch {
            userPreferencesRepository.saveBirthDate(null, null, null)
            _showBirthDatePicker.value = false
        }
    }

    fun validateAndSaveApiKey(newApiKey: String) {
        val key = newApiKey.trim()

        when {
            key.isEmpty() -> {
                _apiKeyError.value = "API key is required"
                return
            }
            key.length < 20 -> {
                _apiKeyError.value = "API key is too short"
                return
            }
            !key.startsWith("AIza") -> {
                _apiKeyError.value = "Invalid format. Google AI keys start with 'AIza'"
                return
            }
        }

        viewModelScope.launch {
            secureStorage.saveApiKey(key)
            _apiKeyError.value = null
            _showApiKeyDialog.value = false
        }
    }

    fun resetOnboarding() {
        viewModelScope.launch {
            userPreferencesRepository.clearAllData()
            secureStorage.clearApiKey()
            userPreferencesRepository.setOnboardingComplete(false)
        }
    }

    // ==================== BACKUP & RESTORE ====================

    private val _backupState = MutableStateFlow<BackupState>(BackupState.Idle)
    val backupState: StateFlow<BackupState> = _backupState

    private val _restoreState = MutableStateFlow<RestoreState>(RestoreState.Idle)
    val restoreState: StateFlow<RestoreState> = _restoreState

    fun exportData(includeApiKey: Boolean = false) {
        viewModelScope.launch {
            _backupState.value = BackupState.Loading
            val result = backupRepository.exportData(includeApiKey)
            _backupState.value = result.fold(
                onSuccess = { uri -> BackupState.Success(uri) },
                onFailure = { error -> BackupState.Error(error.message ?: "Failed to export data") }
            )
        }
    }

    fun importData(uri: Uri) {
        viewModelScope.launch {
            _restoreState.value = RestoreState.Loading
            val result = backupRepository.importData(uri)
            _restoreState.value = result.fold(
                onSuccess = { RestoreState.Success },
                onFailure = { error -> RestoreState.Error(error.message ?: "Failed to import data") }
            )
        }
    }

    suspend fun getBackupInfo(uri: Uri) = backupRepository.getBackupInfo(uri)

    fun resetBackupState() {
        _backupState.value = BackupState.Idle
    }

    fun resetRestoreState() {
        _restoreState.value = RestoreState.Idle
    }
}

sealed class BackupState {
    object Idle : BackupState()
    object Loading : BackupState()
    data class Success(val uri: Uri) : BackupState()
    data class Error(val message: String) : BackupState()
}

sealed class RestoreState {
    object Idle : RestoreState()
    object Loading : RestoreState()
    object Success : RestoreState()
    data class Error(val message: String) : RestoreState()
}

data class ProfileValidationErrors(
    var nameError: String? = null,
    var ageError: String? = null,
    var heightError: String? = null,
    var weightError: String? = null,
    var stepGoalError: String? = null,
    var calorieGoalError: String? = null
)
