package com.fitu.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fitu.BuildConfig  // ✅ Import BuildConfig
import com.fitu.ui.components.GlassCard
import com.fitu.ui.profile.BackupState
import com.fitu.ui.profile.ProfileViewModel
import com.fitu.ui.profile.RestoreState
import com.fitu.ui.theme.OrangePrimary
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val ErrorRed = Color(0xFFF44336)
private val SuccessGreen = Color(0xFF4CAF50)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val userName by viewModel.userName.collectAsState()
    val userAge by viewModel.userAge.collectAsState()
    val userHeightCm by viewModel.userHeightCm.collectAsState()
    val userWeightKg by viewModel.userWeightKg.collectAsState()
    val dailyStepGoal by viewModel.dailyStepGoal.collectAsState()
    val dailyCalorieGoal by viewModel.dailyCalorieGoal.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    val bmi by viewModel.bmi.collectAsState()
    val bmiCategory by viewModel.bmiCategory.collectAsState()
    val showApiKeyDialog by viewModel.showApiKeyDialog.collectAsState()
    val showAboutDialog by viewModel.showAboutDialog.collectAsState()
    val apiKeyError by viewModel.apiKeyError.collectAsState()
    val profileValidationErrors by viewModel.profileValidationErrors.collectAsState()

    // Birth date
    val birthDay by viewModel.birthDay.collectAsState()
    val birthMonth by viewModel.birthMonth.collectAsState()
    val birthYear by viewModel.birthYear.collectAsState()
    val formattedBirthDate by viewModel.formattedBirthDate.collectAsState()
    val calculatedAge by viewModel.calculatedAge.collectAsState()
    val showBirthDatePicker by viewModel.showBirthDatePicker.collectAsState()

    // Unit preference
    val useImperialUnits by viewModel.useImperialUnits.collectAsState()
    val formattedHeight by viewModel.formattedHeight.collectAsState()
    val formattedWeight by viewModel.formattedWeight.collectAsState()

    // Backup & Restore states
    val backupState by viewModel.backupState.collectAsState()
    val restoreState by viewModel.restoreState.collectAsState()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var selectedRestoreUri by remember { mutableStateOf<Uri?>(null) }

    val context = LocalContext.current

    // File picker for restore
    val restoreFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            selectedRestoreUri = it
            showRestoreConfirmDialog = true
        }
    }

    // Share intent for backup file
    LaunchedEffect(backupState) {
        if (backupState is BackupState.Success) {
            val uri = (backupState as BackupState.Success).uri
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "application/json"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Backup"))
        }
    }

    // Safe date picker with proper year range
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = try {
            if (birthYear != null && birthMonth != null && birthDay != null) {
                val calendar = Calendar.getInstance()
                calendar.set(birthYear!!, birthMonth!! - 1, birthDay!!)
                calendar.timeInMillis
            } else null
        } catch (e: Exception) {
            null
        },
        yearRange = 1920..currentYear
    )

    // Birth Date Picker Dialog
    if (showBirthDatePicker) {
        DatePickerDialog(
            onDismissRequest = { viewModel.hideBirthDatePicker() },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val calendar = Calendar.getInstance()
                            calendar.timeInMillis = millis
                            viewModel.saveBirthDate(
                                day = calendar.get(Calendar.DAY_OF_MONTH),
                                month = calendar.get(Calendar.MONTH) + 1,
                                year = calendar.get(Calendar.YEAR)
                            )
                        }
                    }
                ) {
                    Text("Save", color = OrangePrimary)
                }
            },
            dismissButton = {
                Row {
                    if (birthDay != null) {
                        TextButton(onClick = { viewModel.clearBirthDate() }) {
                            Text("Clear", color = ErrorRed)
                        }
                    }
                    TextButton(onClick = { viewModel.hideBirthDatePicker() }) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                    }
                }
            },
            colors = DatePickerDefaults.colors(containerColor = Color(0xFF1A1A1F))
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = Color(0xFF1A1A1F),
                    titleContentColor = Color.White,
                    headlineContentColor = Color.White,
                    weekdayContentColor = Color.White.copy(alpha = 0.5f),
                    subheadContentColor = Color.White.copy(alpha = 0.7f),
                    yearContentColor = Color.White,
                    currentYearContentColor = OrangePrimary,
                    selectedYearContainerColor = OrangePrimary,
                    selectedYearContentColor = Color.White,
                    dayContentColor = Color.White,
                    selectedDayContainerColor = OrangePrimary,
                    selectedDayContentColor = Color.White,
                    todayContentColor = OrangePrimary,
                    todayDateBorderColor = OrangePrimary
                )
            )
        }
    }

    // Edit Profile Dialog with validation
    if (showEditProfileDialog) {
        EditProfileDialog(
            currentName = userName,
            currentAge = calculatedAge ?: userAge,
            currentHeightCm = userHeightCm,
            currentWeight = userWeightKg,
            currentStepGoal = dailyStepGoal,
            currentCalorieGoal = dailyCalorieGoal,
            validationErrors = profileValidationErrors,
            onSave = { name, age, heightCm, weight, stepGoal, calorieGoal ->
                viewModel.saveProfile(name, age, heightCm, weight, stepGoal, calorieGoal)
                if (profileValidationErrors.nameError == null &&
                    profileValidationErrors.ageError == null &&
                    profileValidationErrors.heightError == null &&
                    profileValidationErrors.weightError == null &&
                    profileValidationErrors.stepGoalError == null &&
                    profileValidationErrors.calorieGoalError == null
                ) {
                    showEditProfileDialog = false
                }
            },
            onDismiss = {
                showEditProfileDialog = false
                viewModel.clearValidationErrors()
            }
        )
    }

    // API Key Dialog with simple format validation
    if (showApiKeyDialog) {
        ApiKeyDialog(
            currentKey = apiKey,
            apiKeyError = apiKeyError,
            onSave = { viewModel.validateAndSaveApiKey(it) },
            onDismiss = { viewModel.hideApiKeyDialog() }
        )
    }

    // Reset Confirmation Dialog
    if (showResetConfirmDialog) {
        ResetConfirmDialog(
            onConfirm = {
                showResetConfirmDialog = false
                viewModel.resetOnboarding()
            },
            onDismiss = { showResetConfirmDialog = false }
        )
    }

    // Backup Options Dialog
    if (showBackupDialog) {
        BackupOptionsDialog(
            onBackup = { includeApiKey ->
                viewModel.exportData(includeApiKey)
                showBackupDialog = false
            },
            onDismiss = { showBackupDialog = false }
        )
    }

    // Restore Confirmation Dialog
    if (showRestoreConfirmDialog && selectedRestoreUri != null) {
        RestoreConfirmDialog(
            uri = selectedRestoreUri!!,
            viewModel = viewModel,
            onConfirm = {
                viewModel.importData(selectedRestoreUri!!)
                showRestoreConfirmDialog = false
                selectedRestoreUri = null
            },
            onDismiss = {
                showRestoreConfirmDialog = false
                selectedRestoreUri = null
            }
        )
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { viewModel.hideAboutDialog() })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 32.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header
        Text(
            text = "Profile",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        // User Info
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            Brush.linearGradient(listOf(OrangePrimary, Color(0xFFD94F00))),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = getInitials(userName),
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column {
                    Text(
                        text = userName.ifBlank { "User" },
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pro Member",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }
            }

            IconButton(
                onClick = { showEditProfileDialog = true },
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit Profile",
                    tint = OrangePrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // My Goals
        Text(
            text = "MY GOALS",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Daily Steps", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format("%,d", dailyStepGoal),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Daily Calories", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format("%,d", dailyCalorieGoal),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Body Stats
        Text(
            text = "BODY STATS",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // BMI
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("BMI", color = Color.White, fontSize = 16.sp)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (bmi.isNaN() || bmi == 0f) "--" else DecimalFormat("#.#").format(bmi),
                            color = getBmiColor(bmiCategory),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (bmiCategory.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .background(getBmiColor(bmiCategory).copy(alpha = 0.2f), RoundedCornerShape(100))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = bmiCategory.uppercase(),
                                    color = getBmiColor(bmiCategory),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)

                // Birth Date
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.showBirthDatePicker() },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Cake, null, tint = OrangePrimary, modifier = Modifier.size(18.dp))
                        Text("Birthday", color = Color.White, fontSize = 16.sp)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = formattedBirthDate ?: "Not set",
                            color = Color.White.copy(alpha = if (formattedBirthDate != null) 0.7f else 0.4f),
                            fontSize = 14.sp
                        )
                        Icon(Icons.Default.KeyboardArrowRight, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)

                // Age
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Age", color = Color.White, fontSize = 16.sp)
                    Text("${calculatedAge ?: userAge} years", color = Color.White.copy(alpha = 0.7f), fontSize = 16.sp)
                }

                Divider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)

                // Height with unit conversion
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Height", color = Color.White, fontSize = 16.sp)
                    Text(formattedHeight, color = Color.White.copy(alpha = 0.7f), fontSize = 16.sp)
                }

                Divider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)

                // Weight with unit conversion
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Weight", color = Color.White, fontSize = 16.sp)
                    Text(formattedWeight, color = Color.White.copy(alpha = 0.7f), fontSize = 16.sp)
                }

                Divider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)

                // API Key
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.showApiKeyDialog() },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("API Key", color = Color.White, fontSize = 16.sp)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (apiKey.isNotBlank()) "••••••••" else "Not set",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 16.sp
                        )
                        Icon(Icons.Default.KeyboardArrowRight, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // App Settings
        Text(
            text = "APP SETTINGS",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                // Unit Toggle Setting
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF2196F3).copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Straighten,
                                contentDescription = null,
                                tint = Color(0xFF2196F3),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Use Imperial Units",
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp
                            )
                            Text(
                                text = if (useImperialUnits) "ft/in, lbs, miles" else "cm, kg, km",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Switch(
                        checked = useImperialUnits,
                        onCheckedChange = { viewModel.setUnitPreference(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = OrangePrimary,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                }

                Divider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)

                SettingsItem(
                    icon = Icons.Default.Cake,
                    iconBgColor = Color(0xFFE91E63),
                    title = "Update Birthday",
                    subtitle = "Change your birth date",
                    onClick = { viewModel.showBirthDatePicker() }
                )

                Divider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)

                SettingsItem(
                    icon = Icons.Default.Key,
                    iconBgColor = OrangePrimary,
                    title = "Update API Key",
                    subtitle = "Change your Gemini API key",
                    onClick = { viewModel.showApiKeyDialog() }
                )

                Divider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)

                SettingsItem(
                    icon = Icons.Default.Delete,
                    iconBgColor = ErrorRed,
                    title = "Reset App",
                    subtitle = "Delete all local data",
                    titleColor = ErrorRed,
                    onClick = { showResetConfirmDialog = true }
                )
            }
        }

        // Backup & Restore
        Text(
            text = "BACKUP & RESTORE",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                // Backup Data
                SettingsItem(
                    icon = Icons.Default.CloudUpload,
                    iconBgColor = Color(0xFF4CAF50),
                    title = "Backup Data",
                    subtitle = "Export all your data to a file",
                    onClick = { showBackupDialog = true }
                )

                Divider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)

                // Restore Data
                SettingsItem(
                    icon = Icons.Default.CloudDownload,
                    iconBgColor = Color(0xFF2196F3),
                    title = "Restore Data",
                    subtitle = "Import data from a backup file",
                    onClick = { restoreFilePicker.launch(arrayOf("application/json")) }
                )
            }
        }

        // Loading/Success/Error Feedback for Backup
        when (backupState) {
            is BackupState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(16.dp),
                    color = OrangePrimary
                )
            }
            is BackupState.Success -> {
                Text(
                    text = "✓ Backup created successfully!",
                    color = SuccessGreen,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(8.dp)
                )
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(3000)
                    viewModel.resetBackupState()
                }
            }
            is BackupState.Error -> {
                Text(
                    text = "✗ ${(backupState as BackupState.Error).message}",
                    color = ErrorRed,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(8.dp)
                )
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(5000)
                    viewModel.resetBackupState()
                }
            }
            is BackupState.Idle -> {}
        }

        // Loading/Success/Error Feedback for Restore
        when (restoreState) {
            is RestoreState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(16.dp),
                    color = OrangePrimary
                )
            }
            is RestoreState.Success -> {
                Text(
                    text = "✓ Data restored successfully!",
                    color = SuccessGreen,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(8.dp)
                )
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(3000)
                    viewModel.resetRestoreState()
                }
            }
            is RestoreState.Error -> {
                Text(
                    text = "✗ ${(restoreState as RestoreState.Error).message}",
                    color = ErrorRed,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(8.dp)
                )
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(5000)
                    viewModel.resetRestoreState()
                }
            }
            is RestoreState.Idle -> {}
        }

        // ✅ FIX: Use BuildConfig.VERSION_NAME instead of hardcoded string
        Text(
            text = "Fitu v${BuildConfig.VERSION_NAME}",
            color = Color.White.copy(alpha = 0.3f),
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

/**
 * Simple API Key Dialog - format validation only (no API call)
 */
@Composable
private fun ApiKeyDialog(
    currentKey: String,
    apiKeyError: String?,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var editKey by remember { mutableStateOf(currentKey) }
    var showKey by remember { mutableStateOf(false) }

    val isFormatValid = editKey.trim().let { it.length >= 20 && it.startsWith("AIza") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1F),
        title = { Text("Update API Key", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "Enter your Google AI Studio API key:",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = editKey,
                    onValueChange = { editKey = it },
                    label = { Text("API Key", color = Color.White.copy(alpha = 0.5f)) },
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    isError = apiKeyError != null,
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { showKey = !showKey }) {
                                Icon(
                                    if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            if (isFormatValid && apiKeyError == null) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Valid format",
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = when {
                            apiKeyError != null -> ErrorRed
                            isFormatValid -> SuccessGreen
                            else -> OrangePrimary
                        },
                        unfocusedBorderColor = when {
                            apiKeyError != null -> ErrorRed
                            isFormatValid -> SuccessGreen
                            else -> Color.White.copy(alpha = 0.2f)
                        }
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (apiKeyError != null) {
                    Text(apiKeyError, color = ErrorRed, fontSize = 13.sp)
                } else if (isFormatValid) {
                    Text("✓ Format looks good!", color = SuccessGreen, fontSize = 13.sp)
                } else {
                    Text(
                        "Key should start with 'AIza' and be 20+ characters",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(editKey) },
                enabled = editKey.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
            ) {
                Text("Save", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.7f))
            }
        }
    )
}

/**
 * Edit Profile Dialog with feet/inches height input
 */
@Composable
private fun EditProfileDialog(
    currentName: String,
    currentAge: Int,
    currentHeightCm: Int,
    currentWeight: Int,
    currentStepGoal: Int,
    currentCalorieGoal: Int,
    validationErrors: com.fitu.ui.profile.ProfileValidationErrors,
    onSave: (String, Int, Int, Int, Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    // Convert current height from cm to feet and inches for display
    val totalInches = (currentHeightCm / 2.54).toInt()
    val initialFeet = totalInches / 12
    val initialInches = totalInches % 12

    var name by remember { mutableStateOf(currentName) }
    var age by remember { mutableStateOf(currentAge.toString()) }
    var heightFeet by remember { mutableStateOf(initialFeet.toString()) }
    var heightInches by remember { mutableStateOf(initialInches.toString()) }
    var weight by remember { mutableStateOf(currentWeight.toString()) }
    var stepGoal by remember { mutableStateOf(currentStepGoal.toString()) }
    var calorieGoal by remember { mutableStateOf(currentCalorieGoal.toString()) }

    // Convert feet/inches back to cm for saving
    fun getHeightInCm(): Int {
        val feet = heightFeet.toIntOrNull() ?: 0
        val inches = heightInches.toIntOrNull() ?: 0
        val totalInchesVal = (feet * 12) + inches
        return (totalInchesVal * 2.54).toInt()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1F),
        title = { Text("Edit Profile", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                ProfileTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Name",
                    error = validationErrors.nameError
                )

                ProfileTextField(
                    value = age,
                    onValueChange = { age = it.filter { c -> c.isDigit() } },
                    label = "Age",
                    isNumber = true,
                    error = validationErrors.ageError
                )

                // Height in Feet and Inches
                Text(
                    text = "Height",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = heightFeet,
                            onValueChange = { heightFeet = it.filter { c -> c.isDigit() } },
                            label = { Text("Feet", color = Color.White.copy(alpha = 0.5f)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            isError = validationErrors.heightError != null,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = if (validationErrors.heightError != null) ErrorRed else OrangePrimary,
                                unfocusedBorderColor = if (validationErrors.heightError != null) ErrorRed else Color.White.copy(alpha = 0.2f),
                                errorBorderColor = ErrorRed
                            )
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = heightInches,
                            onValueChange = {
                                val filtered = it.filter { c -> c.isDigit() }
                                val inchesVal = filtered.toIntOrNull()
                                if (inchesVal == null || inchesVal <= 11) {
                                    heightInches = filtered
                                }
                            },
                            label = { Text("Inches", color = Color.White.copy(alpha = 0.5f)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            isError = validationErrors.heightError != null,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = if (validationErrors.heightError != null) ErrorRed else OrangePrimary,
                                unfocusedBorderColor = if (validationErrors.heightError != null) ErrorRed else Color.White.copy(alpha = 0.2f),
                                errorBorderColor = ErrorRed
                            )
                        )
                    }
                }

                if (validationErrors.heightError != null) {
                    Text(
                        text = validationErrors.heightError!!,
                        color = ErrorRed,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                ProfileTextField(
                    value = weight,
                    onValueChange = { weight = it.filter { c -> c.isDigit() } },
                    label = "Weight (kg)",
                    isNumber = true,
                    error = validationErrors.weightError
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProfileTextField(
                        value = stepGoal,
                        onValueChange = { stepGoal = it.filter { c -> c.isDigit() } },
                        label = "Step Goal",
                        isNumber = true,
                        modifier = Modifier.weight(1f),
                        error = validationErrors.stepGoalError
                    )

                    ProfileTextField(
                        value = calorieGoal,
                        onValueChange = { calorieGoal = it.filter { c -> c.isDigit() } },
                        label = "Calorie Goal",
                        isNumber = true,
                        modifier = Modifier.weight(1f),
                        error = validationErrors.calorieGoalError
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        name,
                        age.toIntOrNull() ?: currentAge,
                        getHeightInCm(),
                        weight.toIntOrNull() ?: currentWeight,
                        stepGoal.toIntOrNull() ?: currentStepGoal,
                        calorieGoal.toIntOrNull() ?: currentCalorieGoal
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
            ) {
                Text("Save", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.7f))
            }
        }
    )
}

@Composable
private fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isNumber: Boolean = false,
    modifier: Modifier = Modifier,
    error: String? = null
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, color = Color.White.copy(alpha = 0.5f)) },
            keyboardOptions = KeyboardOptions(keyboardType = if (isNumber) KeyboardType.Number else KeyboardType.Text),
            singleLine = true,
            isError = error != null,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = if (error != null) ErrorRed else OrangePrimary,
                unfocusedBorderColor = if (error != null) ErrorRed else Color.White.copy(alpha = 0.2f),
                errorBorderColor = ErrorRed
            )
        )
        if (error != null) {
            Text(
                text = error,
                color = ErrorRed,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
        }
    }
}

@Composable
private fun ResetConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1F),
        icon = {
            Box(
                modifier = Modifier.size(56.dp).background(ErrorRed.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Warning, null, tint = ErrorRed, modifier = Modifier.size(28.dp))
            }
        },
        title = {
            Text(
                "Reset App?",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "This will permanently delete all your data including:",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "Profile information",
                        "Step history",
                        "Meal tracking data",
                        "Workout records",
                        "API key"
                    ).forEach {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).background(ErrorRed, CircleShape))
                            Text(it, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "This action cannot be undone.",
                    color = ErrorRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Yes, Reset Everything", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel", color = Color.White.copy(alpha = 0.7f))
            }
        }
    )
}

@Composable
private fun BackupOptionsDialog(
    onBackup: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var includeApiKey by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1F),
        icon = {
            Box(
                modifier = Modifier.size(56.dp).background(Color(0xFF4CAF50).copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.FileUpload, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(28.dp))
            }
        },
        title = {
            Text(
                "Backup Your Data",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Create a backup file with all your fitness data:",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "Step history",
                        "Meal tracking data",
                        "Workout records",
                        "Workout plans",
                        "Profile settings"
                    ).forEach {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).background(Color(0xFF4CAF50), CircleShape))
                            Text(it, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // API Key Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                        .clickable { includeApiKey = !includeApiKey },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Include API Key", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("Save your Gemini API key in backup", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                    }
                    Checkbox(
                        checked = includeApiKey,
                        onCheckedChange = { includeApiKey = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = OrangePrimary,
                            uncheckedColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                }

                if (includeApiKey) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "⚠️ Security Note: Your API key will be stored in plain text in the backup file. Keep it safe!",
                        color = Color(0xFFFF9800),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onBackup(includeApiKey) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create Backup", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel", color = Color.White.copy(alpha = 0.7f))
            }
        }
    )
}

@Composable
private fun RestoreConfirmDialog(
    uri: Uri,
    viewModel: ProfileViewModel,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var backupInfo by remember { mutableStateOf<com.fitu.domain.repository.BackupInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uri) {
        try {
            val result = viewModel.getBackupInfo(uri)
            result.fold(
                onSuccess = {
                    backupInfo = it
                    isLoading = false
                },
                onFailure = {
                    error = it.message
                    isLoading = false
                }
            )
        } catch (e: Exception) {
            error = e.message
            isLoading = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1F),
        icon = {
            Box(
                modifier = Modifier.size(56.dp).background(Color(0xFF2196F3).copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.FileDownload, null, tint = Color(0xFF2196F3), modifier = Modifier.size(28.dp))
            }
        },
        title = {
            Text(
                "Restore Data?",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(color = OrangePrimary, modifier = Modifier.padding(16.dp))
                        Text("Reading backup file...", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                    }
                    error != null -> {
                        Text("Error: $error", color = ErrorRed, fontSize = 14.sp, textAlign = TextAlign.Center)
                    }
                    backupInfo != null -> {
                        Text(
                            "This will restore the following data:",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            InfoRow("App Version", backupInfo!!.appVersion)
                            InfoRow(
                                "Backup Date",
                                SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(backupInfo!!.exportDate))
                            )
                            Divider(color = Color.White.copy(alpha = 0.1f))
                            InfoRow("Step Records", backupInfo!!.stepRecords.toString())
                            InfoRow("Meal Records", backupInfo!!.mealRecords.toString())
                            InfoRow("Workout Records", backupInfo!!.workoutRecords.toString())
                            InfoRow("Workout Plans", backupInfo!!.workoutPlans.toString())
                            if (backupInfo!!.hasUserProfile) {
                                InfoRow("Profile", "✓ Included")
                            }
                            if (backupInfo!!.hasApiKey) {
                                InfoRow("API Key", "✓ Included")
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "⚠️ Warning: This will add to your existing data. No data will be deleted.",
                            color = Color(0xFFFF9800),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (backupInfo != null) {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Restore Data", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel", color = Color.White.copy(alpha = 0.7f))
            }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    iconBgColor: Color,
    title: String,
    subtitle: String,
    titleColor: Color = Color.White,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconBgColor.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconBgColor, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = titleColor, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            Text(subtitle, color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
        }

        Icon(Icons.Default.KeyboardArrowRight, null, tint = Color.White.copy(alpha = 0.3f))
    }
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1F),
        title = { Text("About Fitu", color = Color.White) },
        text = {
            Column {
                Text("Fitu", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = OrangePrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Your AI-powered fitness companion", color = Color.White.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(16.dp))
                // ✅ FIX: Use BuildConfig.VERSION_NAME instead of hardcoded string
                Text("Version ${BuildConfig.VERSION_NAME}", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                Text("© 2026 Fitu", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = OrangePrimary)
            }
        }
    )
}

private fun getInitials(name: String): String {
    if (name.isBlank()) return "U"
    val parts = name.trim().split(" ").filter { it.isNotEmpty() }
    return when {
        parts.size >= 2 && parts[0].isNotEmpty() && parts[1].isNotEmpty() ->
            "${parts[0].first()}${parts[1].first()}".uppercase()
        parts.isNotEmpty() && parts[0].isNotEmpty() ->
            parts[0].take(2).uppercase()
        else -> "U"
    }
}

private fun getBmiColor(category: String): Color {
    return when (category) {
        "Underweight" -> Color(0xFF2196F3)
        "Normal" -> Color(0xFF4CAF50)
        "Overweight" -> Color(0xFFFF9800)
        "Obese" -> Color(0xFFF44336)
        else -> Color.Gray
    }
}
