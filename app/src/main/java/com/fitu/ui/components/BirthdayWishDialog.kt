package com.fitu.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fitu.ui.theme.OrangePrimary
import kotlin.random.Random

/**
 * Birthday wish dialog with confetti animation.
 */
@Composable
fun BirthdayWishDialog(
    userName: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Confetti animation layer
            ConfettiAnimation(
                modifier = Modifier.fillMaxSize()
            )

            // Dialog card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1A1A1F)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Cake emoji
                    Text(
                        text = "🎂",
                        fontSize = 64.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Balloons and party emojis
                    Text(
                        text = "🎈🎉🎊",
                        fontSize = 32.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Headline
                    Text(
                        text = "Happy Birthday, ${userName.ifBlank { "User" }}! 🎉",
                        color = OrangePrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Subtext
                    Text(
                        text = "Wishing you a healthy and active year ahead!",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "May all your fitness goals come true! 💪",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Thank you button
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
                            text = "Thank you! 🙏",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Simple confetti animation effect.
 */
@Composable
private fun ConfettiAnimation(
    modifier: Modifier = Modifier
) {
    val confettiColors = listOf(
        OrangePrimary,
        Color(0xFFFF6B6B),
        Color(0xFF4ECDC4),
        Color(0xFFFFE66D),
        Color(0xFF95E1D3),
        Color(0xFFF38181),
        Color(0xFFAA96DA)
    )

    // Create multiple confetti particles
    val particles = remember {
        List(50) {
            ConfettiParticle(
                x = Random.nextFloat(),
                y = Random.nextFloat() * -1f, // Start above screen
                speed = 0.002f + Random.nextFloat() * 0.003f,
                size = 6f + Random.nextFloat() * 8f,
                color = confettiColors[Random.nextInt(confettiColors.size)]
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confetti_time"
    )

    Canvas(modifier = modifier) {
        particles.forEach { particle ->
            val yOffset = (particle.y + time * particle.speed * 200f) % 1.2f
            val xOffset = particle.x + kotlin.math.sin(time * 10f + particle.x * 10f) * 0.02f

            if (yOffset > 0) {
                drawCircle(
                    color = particle.color,
                    radius = particle.size,
                    center = Offset(
                        x = xOffset * size.width,
                        y = yOffset * size.height
                    )
                )
            }
        }
    }
}

private data class ConfettiParticle(
    val x: Float,
    val y: Float,
    val speed: Float,
    val size: Float,
    val color: Color
)

/**
 * Preview-friendly version without animation for testing.
 */
@Composable
fun BirthdayGreeting(
    userName: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "🎂",
            fontSize = 24.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Happy Birthday, ${userName.ifBlank { "User" }}!",
            color = OrangePrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
