package com.fitu.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitu.ui.theme.OrangePrimary
import kotlin.math.sin

/**
 * Animated Fire Icon with flickering effect
 */
@Composable
fun AnimatedFireIcon(
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
    size: Int = 32
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fire")
    
    // Flicker animation
    val flicker by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flicker"
    )
    
    // Scale pulse
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    // Sway animation
    val sway by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sway"
    )

    val activeScale = if (isActive) scale else 1f
    
    Box(
        modifier = modifier
            .size(size.dp)
            .scale(activeScale),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = this.size.width
            val height = this.size.height
            val centerX = width / 2
            
            if (isActive) {
                // Outer glow
                val glowPath = Path().apply {
                    moveTo(centerX + sway, height * 0.95f)
                    cubicTo(
                        width * 0.15f, height * 0.7f,
                        width * 0.1f, height * 0.4f,
                        centerX + sway * 0.5f, height * 0.05f
                    )
                    cubicTo(
                        width * 0.9f, height * 0.4f,
                        width * 0.85f, height * 0.7f,
                        centerX + sway, height * 0.95f
                    )
                    close()
                }
                
                // Outer flame (orange-red)
                drawPath(
                    path = glowPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFF6B35).copy(alpha = 0.8f + flicker * 0.2f),
                            Color(0xFFFF4500).copy(alpha = 0.9f),
                            Color(0xFFDC143C).copy(alpha = 0.7f)
                        )
                    ),
                    style = Fill
                )
                
                // Inner flame (yellow-orange)
                val innerPath = Path().apply {
                    moveTo(centerX + sway * 0.5f, height * 0.9f)
                    cubicTo(
                        width * 0.3f, height * 0.65f,
                        width * 0.25f, height * 0.45f,
                        centerX + sway * 0.3f, height * 0.2f
                    )
                    cubicTo(
                        width * 0.75f, height * 0.45f,
                        width * 0.7f, height * 0.65f,
                        centerX + sway * 0.5f, height * 0.9f
                    )
                    close()
                }
                
                drawPath(
                    path = innerPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFD700).copy(alpha = 0.9f + flicker * 0.1f),
                            Color(0xFFFFA500).copy(alpha = 0.95f),
                            Color(0xFFFF6B35).copy(alpha = 0.8f)
                        )
                    ),
                    style = Fill
                )
                
                // Core flame (white-yellow)
                val corePath = Path().apply {
                    moveTo(centerX, height * 0.85f)
                    cubicTo(
                        width * 0.4f, height * 0.7f,
                        width * 0.38f, height * 0.55f,
                        centerX, height * 0.35f
                    )
                    cubicTo(
                        width * 0.62f, height * 0.55f,
                        width * 0.6f, height * 0.7f,
                        centerX, height * 0.85f
                    )
                    close()
                }
                
                drawPath(
                    path = corePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.95f),
                            Color(0xFFFFFF99).copy(alpha = 0.9f),
                            Color(0xFFFFD700).copy(alpha = 0.85f)
                        )
                    ),
                    style = Fill
                )
            } else {
                // Inactive gray flame
                val grayPath = Path().apply {
                    moveTo(centerX, height * 0.95f)
                    cubicTo(
                        width * 0.15f, height * 0.7f,
                        width * 0.1f, height * 0.4f,
                        centerX, height * 0.05f
                    )
                    cubicTo(
                        width * 0.9f, height * 0.4f,
                        width * 0.85f, height * 0.7f,
                        centerX, height * 0.95f
                    )
                    close()
                }
                
                drawPath(
                    path = grayPath,
                    color = Color.Gray.copy(alpha = 0.4f),
                    style = Fill
                )
            }
        }
    }
}

/**
 * Streak Counter Card Component
 */
@Composable
fun StreakCounterCard(
    currentStreak: Int,
    longestStreak: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val isActiveStreak = currentStreak > 0
    
    // Animate streak number
    val animatedStreak = remember { Animatable(0f) }
    LaunchedEffect(currentStreak) {
        animatedStreak.animateTo(
            targetValue = currentStreak.toFloat(),
            animationSpec = tween(1000, easing = FastOutSlowInEasing)
        )
    }

    GlassCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Fire icon with glow
            Box(
                contentAlignment = Alignment.Center
            ) {
                // Glow effect behind fire
                if (isActiveStreak) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .blur(12.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        OrangePrimary.copy(alpha = 0.6f),
                                        OrangePrimary.copy(alpha = 0.2f),
                                        Color.Transparent
                                    )
                                ),
                                CircleShape
                            )
                    )
                }
                
                AnimatedFireIcon(
                    modifier = Modifier.size(40.dp),
                    isActive = isActiveStreak,
                    size = 40
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Streak info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${animatedStreak.value.toInt()}",
                        color = if (isActiveStreak) OrangePrimary else Color.Gray,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (currentStreak == 1) "day" else "days",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                
                Text(
                    text = if (isActiveStreak) "Current Streak 🔥" else "Start your streak!",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }
            
            // Best streak badge
            if (longestStreak > 0) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(
                            Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "🏆",
                        fontSize = 16.sp
                    )
                    Text(
                        text = "$longestStreak",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Best",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

/**
 * Compact Streak Badge for header/navbar
 */
@Composable
fun StreakBadge(
    streak: Int,
    modifier: Modifier = Modifier
) {
    val isActive = streak > 0
    
    Row(
        modifier = modifier
            .background(
                if (isActive) OrangePrimary.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f),
                RoundedCornerShape(16.dp)
            )
            .border(
                1.dp,
                if (isActive) OrangePrimary.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.3f),
                RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedFireIcon(
            modifier = Modifier.size(18.dp),
            isActive = isActive,
            size = 18
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$streak",
            color = if (isActive) OrangePrimary else Color.Gray,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Mini streak indicator for dashboard
 */
@Composable
fun MiniStreakIndicator(
    streak: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedFireIcon(
            modifier = Modifier.size(16.dp),
            isActive = streak > 0,
            size = 16
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$streak day${if (streak != 1) "s" else ""}",
            color = if (streak > 0) OrangePrimary else Color.Gray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
