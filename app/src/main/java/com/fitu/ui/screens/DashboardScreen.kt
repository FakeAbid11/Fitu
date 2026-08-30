package com.fitu.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fitu.ui.components.AnimatedFormattedCounter
import com.fitu.ui.components.DumbbellIcon
import com.fitu.ui.components.GlassCard
import com.fitu.ui.components.GoalCelebrationDialog
import com.fitu.ui.components.GoalType
import com.fitu.ui.components.PullToRefreshContainer
import com.fitu.ui.components.StreakCounterCard
import com.fitu.ui.components.skeletons.DashboardSkeleton
import com.fitu.ui.dashboard.DashboardViewModel
import com.fitu.ui.theme.OrangePrimary
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val userName by viewModel.userName.collectAsState()
    val currentSteps by viewModel.currentSteps.collectAsState()
    val dailyStepGoal by viewModel.dailyStepGoal.collectAsState()
    val caloriesBurned by viewModel.caloriesBurned.collectAsState()
    val caloriesConsumed by viewModel.caloriesConsumed.collectAsState()
    val dailyCalorieGoal by viewModel.dailyCalorieGoal.collectAsState()
    val todayTotalReps by viewModel.todayTotalReps.collectAsState()
    val isStepsInitialized by viewModel.isStepsInitialized.collectAsState()
    val streakData by viewModel.streakData.collectAsState()
    val showStepGoalCelebration by viewModel.showStepGoalCelebration.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val progress = if (dailyStepGoal > 0) currentSteps.toFloat() / dailyStepGoal.toFloat() else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(1000),
        label = "progress"
    )

    val today = LocalDate.now()
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())
    val formattedDate = today.format(dateFormatter)

    val greeting = when (LocalTime.now().hour) {
        in 5..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..20 -> "Good Evening"
        else -> "Good Night"
    }

    val motivationalMessage = when {
        currentSteps >= dailyStepGoal -> "🎉 Goal achieved! Amazing work!"
        currentSteps >= dailyStepGoal * 0.75 -> "🔥 Almost there! Keep pushing!"
        currentSteps >= dailyStepGoal * 0.5 -> "💪 Halfway there! You got this!"
        currentSteps >= dailyStepGoal * 0.25 -> "🚶 Great start! Keep moving!"
        else -> "👟 Let's get moving today!"
    }

    // Goal celebration dialog
    GoalCelebrationDialog(
        show = showStepGoalCelebration,
        goalType = GoalType.STEPS,
        achievedValue = String.format("%,d", currentSteps),
        goalValue = String.format("%,d", dailyStepGoal),
        onDismiss = { viewModel.dismissStepGoalCelebration() }
    )

    if (!isStepsInitialized) {
        DashboardSkeleton()
        return
    }

    PullToRefreshContainer(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refresh() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0A0F))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 32.dp, bottom = 120.dp)
        ) {
            // Header
            Text(
                text = "$greeting,",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp
            )

            Text(
                text = userName.ifEmpty { "Athlete" },
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = formattedDate,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Motivational message
            Text(
                text = motivationalMessage,
                color = OrangePrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Streak Card
            if (streakData.currentStreak > 0 || streakData.longestStreak > 0) {
                StreakCounterCard(
                    currentStreak = streakData.currentStreak,
                    longestStreak = streakData.longestStreak
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Steps Card with circular progress
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Today's Steps",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Goal: ${String.format("%,d", dailyStepGoal)}",
                            color = OrangePrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Box(
                        modifier = Modifier.size(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawArc(
                                color = Color.White.copy(alpha = 0.1f),
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = OrangePrimary,
                                startAngle = -90f,
                                sweepAngle = 360f * animatedProgress,
                                useCenter = false,
                                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AnimatedFormattedCounter(
                                count = currentSteps,
                                style = TextStyle(
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "steps",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Reps & Calories Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GlassCard(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            DumbbellIcon,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        AnimatedFormattedCounter(
                            count = todayTotalReps,
                            style = TextStyle(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "reps",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    }
                }

                GlassCard(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        AnimatedFormattedCounter(
                            count = caloriesBurned,
                            style = TextStyle(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "burned",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Nutrition Card
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Nutrition",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Goal: ${String.format("%,d", dailyCalorieGoal)} kcal",
                            color = OrangePrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AnimatedFormattedCounter(
                                count = caloriesConsumed,
                                style = TextStyle(
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "consumed",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 12.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(40.dp)
                                .background(Color.White.copy(alpha = 0.1f))
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val remaining = (dailyCalorieGoal - caloriesConsumed).coerceAtLeast(0)
                            AnimatedFormattedCounter(
                                count = remaining,
                                style = TextStyle(
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (remaining > 0) Color.White else Color(0xFF4CAF50)
                                )
                            )
                            Text(
                                text = "remaining",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Progress bar
                    val calorieProgress = if (dailyCalorieGoal > 0) {
                        (caloriesConsumed.toFloat() / dailyCalorieGoal).coerceIn(0f, 1f)
                    } else 0f

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(calorieProgress)
                                .height(8.dp)
                                .background(OrangePrimary, RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        }
    }
}
