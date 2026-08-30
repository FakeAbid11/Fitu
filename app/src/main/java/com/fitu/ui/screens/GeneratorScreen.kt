package com.fitu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fitu.ui.generator.GeneratorUiState
import com.fitu.ui.generator.GeneratorViewModel

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GeneratorScreen(
    viewModel: GeneratorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedMuscles by viewModel.selectedMuscles.collectAsState()
    val difficulty by viewModel.difficulty.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val equipment by viewModel.equipment.collectAsState()

    val muscleGroups = listOf(
        "Chest", "Back", "Shoulders", "Biceps", "Triceps",
        "Core", "Glutes", "Quads", "Hamstrings", "Calves"
    )
    val difficulties = listOf("Beginner", "Intermediate", "Advanced")
    val equipmentOptions = listOf("Bodyweight", "Dumbbells", "Full Gym")

    when (uiState) {
        is GeneratorUiState.Idle, is GeneratorUiState.Error -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "AI Workout Generator",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Muscle Groups Selection (Body Map Simplified)
                Text(
                    "Select Target Muscles",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    muscleGroups.forEach { muscle ->
                        val isSelected = selectedMuscles.contains(muscle)
                        MuscleChip(
                            text = muscle,
                            isSelected = isSelected,
                            onClick = { viewModel.toggleMuscle(muscle) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Difficulty
                Text(
                    "Difficulty",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    difficulties.forEach { diff ->
                        FilterChip(
                            selected = difficulty == diff,
                            onClick = { viewModel.setDifficulty(diff) },
                            label = { Text(diff) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Duration Slider
                Text(
                    "Duration: $duration min",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Slider(
                    value = duration.toFloat(),
                    onValueChange = { viewModel.setDuration(it.toInt()) },
                    valueRange = 15f..60f,
                    steps = 8
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Equipment
                Text(
                    "Equipment",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    equipmentOptions.forEach { eq ->
                        FilterChip(
                            selected = equipment == eq,
                            onClick = { viewModel.setEquipment(eq) },
                            label = { Text(eq) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Error message
                if (uiState is GeneratorUiState.Error) {
                    Text(
                        text = (uiState as GeneratorUiState.Error).message,
                        color = Color.Red,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Generate Button
                Button(
                    onClick = { viewModel.generateWorkout() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Generate Workout")
                }
            }
        }

        is GeneratorUiState.Generating -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("AI is crafting your workout...", color = MaterialTheme.colorScheme.onBackground)
                }
            }
        }

        is GeneratorUiState.Success -> {
            val result = (uiState as GeneratorUiState.Success).result
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Your Workout Plan",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text(
                        text = result,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.reset() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Create Another")
                }
            }
        }
    }
}

@Composable
fun MuscleChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                RoundedCornerShape(16.dp)
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.primary,
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
    }
}
