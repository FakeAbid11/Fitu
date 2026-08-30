package com.fitu.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fitu.ui.theme.OrangePrimary

enum class GoalType {
    STEPS,
    CALORIES_BURNED,
    CALORIES_CONSUMED,
    WORKOUT
}

@Composable
fun GoalCelebrationDialog(
    show: Boolean,
    goalType: GoalType,
    achievedValue: String,
    goalValue: String,
    onDismiss: () -> Unit
) {
    if (show) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Confetti background
                ConfettiExplosion(
                    particleCount = 150,
                    durationMillis = 5000
                )

                // Celebration card
                CelebrationCard(
                    goalType = goalType,
                    achievedValue = achievedValue,
                    goalValue = goalValue,
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@Composable
private fun CelebrationCard(
    goalType: GoalType,
    achievedValue: String,
    goalValue: String,
    onDismiss: () -> Unit
) {
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(key1 = true) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(300)
        )
    }

    val (emoji, title, subtitle) = when (goalType) {
        GoalType.STEPS -> Triple(
            "🏃‍♂️",
            "Step Goal Crushed!",
            "You've walked $achievedValue steps today!"
        )
        GoalType.CALORIES_BURNED -> Triple(
            "🔥",
            "Calories Torched!",
            "You've burned $achievedValue kcal today!"
        )
        GoalType.CALORIES_CONSUMED -> Triple(
            "🍎",
            "Nutrition Goal Met!",
            "You've consumed $achievedValue kcal today!"
        )
        GoalType.WORKOUT -> Triple(
            "💪",
            "Workout Complete!",
            "Amazing effort today!"
        )
    }

    Box(
        modifier = Modifier
            .padding(32.dp)
            .scale(scale.value)
            .alpha(alpha.value)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A1F),
                        Color(0xFF252530)
                    )
                ),
                RoundedCornerShape(32.dp)
            )
            .padding(32.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Trophy animation
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                OrangePrimary.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emoji,
                    fontSize = 64.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Celebration emoji row
            Text(
                text = "🎉🎊✨🎉🎊",
                fontSize = 24.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                text = title,
                color = OrangePrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Goal info
            Text(
                text = "Goal: $goalValue",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Motivational message
            Text(
                text = getMotivationalMessage(),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Dismiss button
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrangePrimary
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Keep Going! 💪",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun getMotivationalMessage(): String {
    val messages = listOf(
        "You're unstoppable! 🚀",
        "Champions are made of this! 🏆",
        "Your dedication is inspiring! ⭐",
        "Nothing can stop you now! 💥",
        "You're on fire today! 🔥",
        "Excellence is a habit! 👑",
        "Keep pushing your limits! 💯",
        "You're a fitness warrior! ⚔️"
    )
    return messages.random()
}
