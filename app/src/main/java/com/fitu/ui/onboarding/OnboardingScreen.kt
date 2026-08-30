package com.fitu.ui.onboarding

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fitu.ui.theme.OrangePrimary
import java.util.Calendar

private val DarkBackground = Color(0xFF0A0A0F)
private val InputBackground = Color(0xFF1A1A1F)
private val BorderColor = Color(0xFF2A2A2F)
private val ErrorRed = Color(0xFFF44336)
private val SuccessGreen = Color(0xFF4CAF50)

// ✅ FIX: Define constants for date picker range
private const val MIN_BIRTH_YEAR = 1920  // Allows users up to ~105 years old
private const val MIN_AGE_YEARS = 5       // Minimum age to use the app

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onComplete: () -> Unit
) {
    val currentPage by viewModel.currentPage.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        ProgressBar(currentPage = currentPage, pageCount = 2)
        Spacer(modifier = Modifier.height(32.dp))

        AnimatedContent(targetState = currentPage, label = "page") { page ->
            when (page) {
                0 -> PersonalInfoPage(viewModel)
                1 -> ApiSetupPage(viewModel, onComplete)
            }
        }
    }
}

@Composable
private fun ProgressBar(currentPage: Int, pageCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .background(
                        if (index <= currentPage) OrangePrimary else Color.White.copy(alpha = 0.1f),
                        RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalInfoPage(viewModel: OnboardingViewModel) {
    val name by viewModel.name.collectAsState()
    val heightFeet by viewModel.heightFeet.collectAsState()
    val heightInches by viewModel.heightInches.collectAsState()
    val weightKg by viewModel.weightKg.collectAsState()
    val stepGoal by viewModel.stepGoal.collectAsState()
    val birthDay by viewModel.birthDay.collectAsState()
    val birthMonth by viewModel.birthMonth.collectAsState()
    val birthYear by viewModel.birthYear.collectAsState()
    val showDatePicker by viewModel.showDatePicker.collectAsState()

    // Validation errors
    val nameError by viewModel.nameError.collectAsState()
    val heightError by viewModel.heightError.collectAsState()
    val weightError by viewModel.weightError.collectAsState()
    val stepGoalError by viewModel.stepGoalError.collectAsState()

    // ✅ FIX: Calculate dynamic year range based on current year
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val maxBirthYear = currentYear - MIN_AGE_YEARS  // e.g., 2026 - 5 = 2021

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = if (birthYear != null && birthMonth != null && birthDay != null) {
            try {
                val calendar = Calendar.getInstance()
                calendar.set(birthYear!!, birthMonth!! - 1, birthDay!!)
                calendar.timeInMillis
            } catch (e: Exception) {
                null
            }
        } else null,
        // ✅ FIX: Dynamic year range instead of hardcoded 1940..2015
        yearRange = MIN_BIRTH_YEAR..maxBirthYear
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { viewModel.hideDatePicker() },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val calendar = Calendar.getInstance()
                            calendar.timeInMillis = millis
                            viewModel.updateBirthDate(
                                day = calendar.get(Calendar.DAY_OF_MONTH),
                                month = calendar.get(Calendar.MONTH) + 1,
                                year = calendar.get(Calendar.YEAR)
                            )
                        }
                        viewModel.hideDatePicker()
                    }
                ) {
                    Text("OK", color = OrangePrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDatePicker() }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f))
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Tell us about you",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "We use this to personalize your AI coach.",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Name
        InputLabel("NAME")
        Spacer(modifier = Modifier.height(8.dp))
        DarkTextField(
            value = name,
            onValueChange = { viewModel.updateName(it) },
            placeholder = "Your name",
            isError = nameError != null,
            errorMessage = nameError
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Birthday
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            InputLabel("BIRTHDAY")
            Text(
                text = "(Optional)",
                color = Color.White.copy(alpha = 0.3f),
                fontSize = 11.sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(InputBackground)
                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                .clickable { viewModel.showDatePicker() }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = viewModel.getFormattedBirthDate() ?: "Select your birthday",
                    color = if (birthDay != null) Color.White else Color.White.copy(alpha = 0.3f),
                    fontSize = 16.sp
                )
                Icon(
                    Icons.Default.Cake,
                    contentDescription = "Select birthday",
                    tint = OrangePrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Used to personalize your fitness goals and wish you on your birthday 🎂",
            color = Color.White.copy(alpha = 0.3f),
            fontSize = 11.sp,
            lineHeight = 14.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Height (Feet & Inches) & Weight
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Height in Feet and Inches
            Column(modifier = Modifier.weight(1f)) {
                InputLabel("HEIGHT")
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Feet input
                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = heightFeet,
                            onValueChange = { viewModel.updateHeightFeet(it) },
                            placeholder = { Text("5", color = Color.White.copy(alpha = 0.3f)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = heightError != null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp)),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = InputBackground,
                                unfocusedContainerColor = InputBackground,
                                focusedBorderColor = if (heightError != null) ErrorRed else BorderColor,
                                unfocusedBorderColor = if (heightError != null) ErrorRed else BorderColor,
                                errorBorderColor = ErrorRed,
                                cursorColor = OrangePrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        Text(
                            text = "ft",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                    }

                    // Inches input
                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = heightInches,
                            onValueChange = { viewModel.updateHeightInches(it) },
                            placeholder = { Text("7", color = Color.White.copy(alpha = 0.3f)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = heightError != null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp)),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = InputBackground,
                                unfocusedContainerColor = InputBackground,
                                focusedBorderColor = if (heightError != null) ErrorRed else BorderColor,
                                unfocusedBorderColor = if (heightError != null) ErrorRed else BorderColor,
                                errorBorderColor = ErrorRed,
                                cursorColor = OrangePrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        Text(
                            text = "in",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                    }
                }

                // Show height error below both fields
                if (heightError != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = heightError!!,
                        color = ErrorRed,
                        fontSize = 12.sp
                    )
                }
            }

            // Weight
            Column(modifier = Modifier.weight(1f)) {
                InputLabel("WEIGHT (KG)")
                Spacer(modifier = Modifier.height(8.dp))
                DarkTextField(
                    value = weightKg,
                    onValueChange = { viewModel.updateWeightKg(it) },
                    placeholder = "70",
                    keyboardType = KeyboardType.Number,
                    isError = weightError != null,
                    errorMessage = weightError
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Step Goal
        InputLabel("DAILY STEP GOAL")
        Spacer(modifier = Modifier.height(8.dp))
        DarkTextField(
            value = stepGoal,
            onValueChange = { viewModel.updateStepGoal(it) },
            placeholder = "10000",
            keyboardType = KeyboardType.Number,
            isError = stepGoalError != null,
            errorMessage = stepGoalError
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { viewModel.nextPage() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary.copy(alpha = 0.8f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Continue", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = Color.White)
        }
    }
}

@Composable
fun ApiSetupPage(viewModel: OnboardingViewModel, onComplete: () -> Unit) {
    val apiKey by viewModel.apiKey.collectAsState()
    val showApiKey by viewModel.showApiKey.collectAsState()
    val apiKeyError by viewModel.apiKeyError.collectAsState()

    val context = LocalContext.current

    // Check if key format is valid for enabling button
    val isKeyFormatValid = apiKey.trim().let { it.length >= 20 && it.startsWith("AIza") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Power up AI",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Enter your Gemini API key to enable nutrition vision and smart coaching.",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Info Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, OrangePrimary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .background(OrangePrimary.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(OrangePrimary.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Key,
                        contentDescription = null,
                        tint = OrangePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Google AI Studio (Free)",
                        color = OrangePrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Get a free API key from Google AI Studio. Your key is stored securely on your device.",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // API Key Input
        InputLabel("API KEY")
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = apiKey,
            onValueChange = { viewModel.updateApiKey(it) },
            placeholder = { Text("AIzaSy...", color = Color.White.copy(alpha = 0.3f)) },
            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = apiKeyError != null,
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.toggleShowApiKey() }) {
                        Icon(
                            if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showApiKey) "Hide" else "Show",
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    // Show checkmark if format is valid
                    if (isKeyFormatValid && apiKeyError == null) {
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
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = InputBackground,
                unfocusedContainerColor = InputBackground,
                focusedBorderColor = if (apiKeyError != null) ErrorRed else if (isKeyFormatValid) SuccessGreen else BorderColor,
                unfocusedBorderColor = if (apiKeyError != null) ErrorRed else if (isKeyFormatValid) SuccessGreen else BorderColor,
                cursorColor = OrangePrimary
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        // Error or hint message
        Spacer(modifier = Modifier.height(8.dp))
        if (apiKeyError != null) {
            Text(
                text = apiKeyError!!,
                color = ErrorRed,
                fontSize = 13.sp
            )
        } else if (isKeyFormatValid) {
            Text(
                text = "✓ API key format looks good!",
                color = SuccessGreen,
                fontSize = 13.sp
            )
        } else {
            Text(
                text = "Key should start with 'AIza' and be at least 20 characters",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Get API Key Link
        Text(
            text = "Get a free API key here →",
            color = OrangePrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "💡 Free tier includes 15 requests/minute, 1 million tokens/month",
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 12.sp,
            lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.weight(1f))

        // Start Journey Button
        Button(
            onClick = { viewModel.completeOnboarding(onComplete) },
            enabled = apiKey.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = OrangePrimary.copy(alpha = 0.8f),
                disabledContainerColor = OrangePrimary.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Start Journey", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
        }
    }
}

@Composable
private fun InputLabel(text: String) {
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.5f),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
    )
}

@Composable
private fun DarkTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color.White.copy(alpha = 0.3f)) },
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            isError = isError,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = InputBackground,
                unfocusedContainerColor = InputBackground,
                focusedBorderColor = if (isError) ErrorRed else BorderColor,
                unfocusedBorderColor = if (isError) ErrorRed else BorderColor,
                errorBorderColor = ErrorRed,
                cursorColor = OrangePrimary
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        if (isError && errorMessage != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(errorMessage, color = ErrorRed, fontSize = 12.sp)
        }
    }
}
