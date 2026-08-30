package com.fitu.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Reusable page transition configurations
 */
object PageTransitions {
    
    private const val DURATION_FAST = 200
    private const val DURATION_NORMAL = 300
    private const val DURATION_SLOW = 400
    
    // ============== SLIDE TRANSITIONS ==============
    
    /**
     * Slide in from right with fade
     */
    fun slideInRight(duration: Int = DURATION_NORMAL): EnterTransition {
        return slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(duration, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(duration))
    }
    
    /**
     * Slide out to left with fade
     */
    fun slideOutLeft(duration: Int = DURATION_NORMAL): ExitTransition {
        return slideOutHorizontally(
            targetOffsetX = { -it / 3 },
            animationSpec = tween(duration, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(duration))
    }
    
    /**
     * Slide in from left with fade
     */
    fun slideInLeft(duration: Int = DURATION_NORMAL): EnterTransition {
        return slideInHorizontally(
            initialOffsetX = { -it },
            animationSpec = tween(duration, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(duration))
    }
    
    /**
     * Slide out to right with fade
     */
    fun slideOutRight(duration: Int = DURATION_NORMAL): ExitTransition {
        return slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(duration, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(duration))
    }
    
    /**
     * Slide in from bottom with fade
     */
    fun slideInBottom(duration: Int = DURATION_NORMAL): EnterTransition {
        return slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(duration, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(duration))
    }
    
    /**
     * Slide out to bottom with fade
     */
    fun slideOutBottom(duration: Int = DURATION_NORMAL): ExitTransition {
        return slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(duration, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(duration))
    }
    
    /**
     * Slide in from top with fade
     */
    fun slideInTop(duration: Int = DURATION_NORMAL): EnterTransition {
        return slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(duration, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(duration))
    }
    
    /**
     * Slide out to top with fade
     */
    fun slideOutTop(duration: Int = DURATION_NORMAL): ExitTransition {
        return slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(duration, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(duration))
    }
    
    // ============== SCALE TRANSITIONS ==============
    
    /**
     * Scale in with fade (zoom in effect)
     */
    fun scaleIn(
        initialScale: Float = 0.85f,
        duration: Int = DURATION_NORMAL
    ): EnterTransition {
        return scaleIn(
            initialScale = initialScale,
            animationSpec = tween(duration, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(duration))
    }
    
    /**
     * Scale out with fade (zoom out effect)
     */
    fun scaleOut(
        targetScale: Float = 0.85f,
        duration: Int = DURATION_NORMAL
    ): ExitTransition {
        return scaleOut(
            targetScale = targetScale,
            animationSpec = tween(duration, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(duration))
    }
    
    // ============== FADE TRANSITIONS ==============
    
    /**
     * Simple fade in
     */
    fun fadeIn(duration: Int = DURATION_NORMAL): EnterTransition {
        return fadeIn(animationSpec = tween(duration))
    }
    
    /**
     * Simple fade out
     */
    fun fadeOut(duration: Int = DURATION_NORMAL): ExitTransition {
        return fadeOut(animationSpec = tween(duration))
    }
    
    // ============== EXPAND/SHRINK TRANSITIONS ==============
    
    /**
     * Expand vertically from top
     */
    fun expandFromTop(duration: Int = DURATION_NORMAL): EnterTransition {
        return expandVertically(
            expandFrom = Alignment.Top,
            animationSpec = tween(duration, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(duration))
    }
    
    /**
     * Shrink vertically to top
     */
    fun shrinkToTop(duration: Int = DURATION_NORMAL): ExitTransition {
        return shrinkVertically(
            shrinkTowards = Alignment.Top,
            animationSpec = tween(duration, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(duration))
    }
    
    /**
     * Expand vertically from bottom
     */
    fun expandFromBottom(duration: Int = DURATION_NORMAL): EnterTransition {
        return expandVertically(
            expandFrom = Alignment.Bottom,
            animationSpec = tween(duration, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(duration))
    }
    
    /**
     * Shrink vertically to bottom
     */
    fun shrinkToBottom(duration: Int = DURATION_NORMAL): ExitTransition {
        return shrinkVertically(
            shrinkTowards = Alignment.Bottom,
            animationSpec = tween(duration, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(duration))
    }
}

/**
 * Animated visibility wrapper with preset transitions
 */
@Composable
fun AnimatedContent(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: EnterTransition = PageTransitions.fadeIn() + PageTransitions.scaleIn(),
    exit: ExitTransition = PageTransitions.fadeOut() + PageTransitions.scaleOut(),
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = enter,
        exit = exit,
        content = content
    )
}

/**
 * Slide up animated content
 */
@Composable
fun SlideUpContent(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = PageTransitions.slideInBottom(),
        exit = PageTransitions.slideOutBottom(),
        content = content
    )
}

/**
 * Scale animated content
 */
@Composable
fun ScaleContent(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = PageTransitions.scaleIn(),
        exit = PageTransitions.scaleOut(),
        content = content
    )
}
