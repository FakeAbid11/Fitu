package com.fitu.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.fitu.ui.onboarding.OnboardingScreen
import com.fitu.ui.screens.CoachScreen
import com.fitu.ui.screens.DashboardScreen
import com.fitu.ui.screens.GeneratorScreen
import com.fitu.ui.screens.NutritionScreen
import com.fitu.ui.screens.ProfileScreen
import com.fitu.ui.screens.StepsScreen
import com.fitu.ui.components.PageTransitions
import com.fitu.ui.splash.SplashScreen

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Dashboard : Screen("dashboard")
    object Steps : Screen("steps")
    object Nutrition : Screen("nutrition")
    object Coach : Screen("coach")
    object Generator : Screen("generator")
    object Profile : Screen("profile")
}

// Fast animation durations
private const val POPUP_DURATION = 200
private const val FADE_DURATION = 150


/**
 * Simple fade in (for splash/onboarding)
 */
private fun fadeInOnly(): EnterTransition {
    return fadeIn(animationSpec = tween(FADE_DURATION))
}

/**
 * Simple fade out (for splash/onboarding)
 */
private fun fadeOutOnly(): ExitTransition {
    return fadeOut(animationSpec = tween(FADE_DURATION))
}

private fun routeOrder(route: String?): Int = when (route) {
    Screen.Splash.route -> 0
    Screen.Onboarding.route -> 1
    Screen.Dashboard.route -> 2
    Screen.Steps.route -> 3
    Screen.Nutrition.route -> 4
    Screen.Coach.route -> 5
    Screen.Generator.route -> 6
    Screen.Profile.route -> 7
    else -> 99
}

private fun isFadedRoute(route: String?): Boolean =
    route == Screen.Splash.route || route == Screen.Onboarding.route

// Forward = navigating toward a later destination in the tab order
private fun isForward(initialRoute: String?, targetRoute: String?): Boolean =
    routeOrder(targetRoute) >= routeOrder(initialRoute)

private fun enterFor(initialRoute: String?, targetRoute: String?): EnterTransition =
    if (isFadedRoute(initialRoute) || isFadedRoute(targetRoute)) {
        fadeInOnly()
    } else if (isForward(initialRoute, targetRoute)) {
        PageTransitions.slideInRight(220)
    } else {
        PageTransitions.slideInLeft(220)
    }

private fun exitFor(initialRoute: String?, targetRoute: String?): ExitTransition =
    if (isFadedRoute(initialRoute) || isFadedRoute(targetRoute)) {
        fadeOutOnly()
    } else if (isForward(initialRoute, targetRoute)) {
        PageTransitions.slideOutLeft(220)
    } else {
        PageTransitions.slideOutRight(220)
    }

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        // Direction-aware transitions: forward navigation slides content in
        // from the right, going back slides it in from the left. Splash and
        // onboarding keep a simple fade.
        enterTransition = { enterFor(initialState.destination.route, targetState.destination.route) },
        exitTransition = { exitFor(initialState.destination.route, targetState.destination.route) },
        popEnterTransition = { enterFor(initialState.destination.route, targetState.destination.route) },
        popExitTransition = { exitFor(initialState.destination.route, targetState.destination.route) }
    ) {
        // Splash Screen - Fade only
        composable(
            route = Screen.Splash.route,
            enterTransition = { fadeInOnly() },
            exitTransition = { fadeOutOnly() }
        ) {
            SplashScreen(
                onSplashFinished = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // Onboarding - Fade transition
        composable(
            route = Screen.Onboarding.route,
            enterTransition = { fadeInOnly() },
            exitTransition = { fadeOutOnly() }
        ) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        // Dashboard - Fast popup (no navigation parameters needed)
        composable(
            route = Screen.Dashboard.route,
        ) {
            DashboardScreen()
        }

        // Steps Screen - Fast popup
        composable(
            route = Screen.Steps.route,
        ) {
            StepsScreen()
        }

        // Nutrition Screen - Fast popup
        composable(
            route = Screen.Nutrition.route,
        ) {
            NutritionScreen()
        }

        // Coach Screen - Fast popup
        composable(
            route = Screen.Coach.route,
        ) {
            CoachScreen()
        }

        // Generator Screen - Fast popup
        composable(
            route = Screen.Generator.route,
        ) {
            GeneratorScreen()
        }

        // Profile Screen - Fast popup
        composable(
            route = Screen.Profile.route,
        ) {
            ProfileScreen()
        }
    }
}
