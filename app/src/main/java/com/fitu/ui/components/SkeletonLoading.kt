package com.fitu.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Skeleton colors
private val SkeletonBaseColor = Color(0xFF1A1A1F)
private val SkeletonHighlightColor = Color(0xFF2A2A2F)
private val SkeletonShimmerColor = Color(0xFF3A3A3F)

/**
 * Creates a shimmer brush for skeleton loading effect
 */
@Composable
fun shimmerBrush(): Brush {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    
    val shimmerProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_progress"
    )
    
    return Brush.linearGradient(
        colors = listOf(
            SkeletonBaseColor,
            SkeletonHighlightColor,
            SkeletonShimmerColor,
            SkeletonHighlightColor,
            SkeletonBaseColor
        ),
        start = Offset(
            x = shimmerProgress * 1000f - 500f,
            y = shimmerProgress * 1000f - 500f
        ),
        end = Offset(
            x = shimmerProgress * 1000f,
            y = shimmerProgress * 1000f
        )
    )
}

/**
 * Basic skeleton box with shimmer effect
 */
@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp = 16.dp,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp)
) {
    val brush = shimmerBrush()
    
    Box(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .height(height)
            .clip(shape)
            .background(brush)
    )
}

/**
 * Circular skeleton for avatars/icons
 */
@Composable
fun SkeletonCircle(
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    val brush = shimmerBrush()
    
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(brush)
    )
}

/**
 * Skeleton text line
 */
@Composable
fun SkeletonText(
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp = 14.dp
) {
    SkeletonBox(
        modifier = modifier,
        width = width,
        height = height,
        shape = RoundedCornerShape(4.dp)
    )
}

/**
 * Multiple skeleton text lines
 */
@Composable
fun SkeletonTextLines(
    lines: Int = 3,
    modifier: Modifier = Modifier,
    lineHeight: Dp = 14.dp,
    spacing: Dp = 8.dp,
    lastLineWidth: Float = 0.7f
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        repeat(lines) { index ->
            val isLastLine = index == lines - 1
            SkeletonText(
                modifier = if (isLastLine) Modifier.fillMaxWidth(lastLineWidth) else Modifier.fillMaxWidth(),
                height = lineHeight
            )
        }
    }
}

/**
 * Skeleton card with glass effect
 */
@Composable
fun SkeletonCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(20.dp)
    ) {
        content()
    }
}

/**
 * Skeleton for a stat card (number + label)
 */
@Composable
fun SkeletonStatCard(
    modifier: Modifier = Modifier
) {
    SkeletonCard(modifier = modifier) {
        Column {
            SkeletonText(width = 80.dp, height = 12.dp)
            Spacer(modifier = Modifier.height(12.dp))
            SkeletonBox(width = 120.dp, height = 36.dp)
            Spacer(modifier = Modifier.height(8.dp))
            SkeletonText(width = 100.dp, height = 12.dp)
        }
    }
}

/**
 * Skeleton for a list item with icon
 */
@Composable
fun SkeletonListItem(
    modifier: Modifier = Modifier,
    showTrailing: Boolean = true
) {
    SkeletonCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonCircle(size = 48.dp)
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                SkeletonText(width = 140.dp, height = 16.dp)
                Spacer(modifier = Modifier.height(8.dp))
                SkeletonText(width = 100.dp, height = 12.dp)
            }
            
            if (showTrailing) {
                Spacer(modifier = Modifier.width(12.dp))
                SkeletonBox(width = 60.dp, height = 32.dp, shape = RoundedCornerShape(16.dp))
            }
        }
    }
}

/**
 * Skeleton for meal/food item
 */
@Composable
fun SkeletonMealItem(
    modifier: Modifier = Modifier
) {
    SkeletonCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Food thumbnail
            SkeletonBox(
                width = 48.dp,
                height = 48.dp,
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Name and calories
            Column(modifier = Modifier.weight(1f)) {
                SkeletonText(width = 120.dp, height = 14.dp)
                Spacer(modifier = Modifier.height(6.dp))
                SkeletonText(width = 80.dp, height = 12.dp)
            }
            
            // Macro pills
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) {
                    SkeletonBox(
                        width = 40.dp,
                        height = 20.dp,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Delete button
            SkeletonCircle(size = 32.dp)
        }
    }
}

/**
 * Skeleton for weekly chart
 */
@Composable
fun SkeletonWeeklyChart(
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Chart bars
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            val heights = listOf(30, 50, 40, 70, 60, 45, 55)
            heights.forEach { height ->
                SkeletonBox(
                    width = 24.dp,
                    height = height.dp,
                    shape = RoundedCornerShape(4.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Day labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            repeat(7) {
                SkeletonText(width = 20.dp, height = 10.dp)
            }
        }
    }
}

/**
 * Skeleton for progress ring
 */
@Composable
fun SkeletonProgressRing(
    size: Dp = 64.dp,
    modifier: Modifier = Modifier
) {
    val brush = shimmerBrush()
    
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(brush),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size - 12.dp)
                .clip(CircleShape)
                .background(Color(0xFF0A0A0F))
        )
    }
}
