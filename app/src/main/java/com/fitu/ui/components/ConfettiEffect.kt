package com.fitu.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import com.fitu.ui.theme.OrangePrimary
import kotlin.math.sin
import kotlin.random.Random

private data class ConfettiParticleData(
    val id: Int,
    val x: Float,
    val initialY: Float,
    val size: Float,
    val color: Color,
    val rotation: Float,
    val rotationSpeed: Float,
    val fallSpeed: Float,
    val wobbleSpeed: Float,
    val wobbleAmount: Float,
    val shape: ConfettiShapeType
)

private enum class ConfettiShapeType {
    RECTANGLE,
    CIRCLE,
    TRIANGLE,
    STAR
}

@Composable
fun ConfettiExplosion(
    modifier: Modifier = Modifier,
    particleCount: Int = 100,
    durationMillis: Int = 4000,
    onComplete: () -> Unit = {}
) {
    val confettiColors = listOf(
        OrangePrimary,
        Color(0xFFFF6B6B),
        Color(0xFF4ECDC4),
        Color(0xFFFFE66D),
        Color(0xFF95E1D3),
        Color(0xFFF38181),
        Color(0xFFAA96DA),
        Color(0xFF81C784),
        Color(0xFF64B5F6),
        Color(0xFFFFB74D)
    )

    val particles = remember {
        List(particleCount) { index ->
            ConfettiParticleData(
                id = index,
                x = Random.nextFloat(),
                initialY = Random.nextFloat() * -0.5f - 0.1f,
                size = Random.nextFloat() * 12f + 6f,
                color = confettiColors[Random.nextInt(confettiColors.size)],
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = Random.nextFloat() * 360f - 180f,
                fallSpeed = Random.nextFloat() * 0.3f + 0.2f,
                wobbleSpeed = Random.nextFloat() * 4f + 2f,
                wobbleAmount = Random.nextFloat() * 0.1f + 0.02f,
                shape = ConfettiShapeType.entries[Random.nextInt(ConfettiShapeType.entries.size)]
            )
        }
    }

    val progress = remember { Animatable(0f) }

    LaunchedEffect(key1 = true) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = LinearEasing
            )
        )
        onComplete()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "confetti_wobble")
    val wobble by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wobble"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        particles.forEach { particle ->
            val currentY = particle.initialY + progress.value * (1.5f + particle.fallSpeed)

            if (currentY < 1.2f) {
                val wobbleOffset = sin(wobble * particle.wobbleSpeed) * particle.wobbleAmount
                val currentX = particle.x + wobbleOffset
                val currentRotation = particle.rotation + progress.value * particle.rotationSpeed * 3f

                val x = currentX * canvasWidth
                val y = currentY * canvasHeight

                rotate(
                    degrees = currentRotation,
                    pivot = Offset(x, y)
                ) {
                    when (particle.shape) {
                        ConfettiShapeType.RECTANGLE -> {
                            drawRect(
                                color = particle.color,
                                topLeft = Offset(x - particle.size / 2, y - particle.size / 4),
                                size = Size(particle.size, particle.size / 2)
                            )
                        }
                        ConfettiShapeType.CIRCLE -> {
                            drawCircle(
                                color = particle.color,
                                radius = particle.size / 2,
                                center = Offset(x, y)
                            )
                        }
                        ConfettiShapeType.TRIANGLE -> {
                            val path = Path().apply {
                                moveTo(x, y - particle.size / 2)
                                lineTo(x - particle.size / 2, y + particle.size / 2)
                                lineTo(x + particle.size / 2, y + particle.size / 2)
                                close()
                            }
                            drawPath(path, particle.color)
                        }
                        ConfettiShapeType.STAR -> {
                            drawCircle(
                                color = particle.color,
                                radius = particle.size / 3,
                                center = Offset(x, y)
                            )
                            drawRect(
                                color = particle.color,
                                topLeft = Offset(x - particle.size / 6, y - particle.size / 2),
                                size = Size(particle.size / 3, particle.size)
                            )
                            drawRect(
                                color = particle.color,
                                topLeft = Offset(x - particle.size / 2, y - particle.size / 6),
                                size = Size(particle.size, particle.size / 3)
                            )
                        }
                    }
                }
            }
        }
    }
}
