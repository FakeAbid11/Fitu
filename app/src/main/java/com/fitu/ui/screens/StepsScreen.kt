package com.fitu.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.fitu.ui.components.AnimatedDecimalCounter
import com.fitu.ui.components.AnimatedFormattedCounter
import com.fitu.ui.components.FootprintsIcon
import com.fitu.ui.components.GlassCard
import com.fitu.ui.steps.DaySteps
import com.fitu.ui.steps.StepsViewModel
import com.fitu.ui.theme.OrangePrimary
import com.fitu.util.AutoStartManager
import com.fitu.util.BatteryOptimizationHelper

private val WarningOrange = Color(0xFFFF9800)
private val SuccessGreen = Color(0xFF4CAF50)
private val PermissionBlue = Color(0xFF2196F3)

@Composable
fun StepsScreen(
    viewModel: StepsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val currentSteps by viewModel.stepCount.collectAsState()
    val dailyGoal by viewModel.dailyStepGoal.collectAsState()
    val caloriesBurned by viewModel.caloriesBurned.collectAsState()
    val weeklySteps by viewModel.weeklySteps.collectAsState()
    val isServiceRunning by viewModel.isServiceRunning.collectAsState()
    val usesHardwareCounter by viewModel.usesHardwareCounter.collectAsState()
    val isWeeklyDataLoading by viewModel.isWeeklyDataLoading.collectAsState()
    val formattedDistance by viewModel.formattedDistance.collectAsState()
    val distanceUnit by viewModel.distanceUnit.collectAsState()

    var isBatteryOptimized by remember {
        mutableStateOf(!BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context))
    }
    var showAutoStartWarning by remember {
        mutableStateOf(AutoStartManager.shouldShowAutoStartWarning(context))
    }
    val manufacturerName = AutoStartManager.getManufacturerName()

    var hasActivityPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val activityPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasActivityPermission = granted
        if (granted) {
            viewModel.startService()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isBatteryOptimized = !BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
                showAutoStartWarning = AutoStartManager.shouldShowAutoStartWarning(context)
                hasActivityPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACTIVITY_RECOGNITION
                    ) == PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val progress = if (dailyGoal > 0) currentSteps.toFloat() / dailyGoal.toFloat() else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(1000),
        label = "progress"
    )
    val goalProgress = if (dailyGoal > 0) (currentSteps.toFloat() / dailyGoal * 100).toInt() else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 32.dp, bottom = 120.dp)
    ) {
        Text(
            text = "Steps",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Track your daily activity",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (!hasActivityPermission || isBatteryOptimized || showAutoStartWarning) {
            PermissionWarningCard(
                hasActivityPermission = hasActivityPermission,
                isBatteryOptimized = isBatteryOptimized,
                needsAutoStart = showAutoStartWarning,
                manufacturerName = manufacturerName,
                onActivityPermissionClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        activityPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                    }
                },
                onBatteryClick = { BatteryOptimizationHelper.requestIgnoreBatteryOptimization(context) },
                onAutoStartClick = {
                    AutoStartManager.openAutoStartSettings(context)
                    showAutoStartWarning = false
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // ✨ IMPROVED: Gradient Progress Ring with Glow Effect
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            // Background track
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawArc(
                    color = Color.White.copy(alpha = 0.1f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            
            // Gradient progress arc with glow effect
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Glow layer (wider, more transparent)
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            OrangePrimary.copy(alpha = 0.3f),
                            Color(0xFFFF8A65).copy(alpha = 0.3f),
                            Color(0xFFFFAB91).copy(alpha = 0.3f),
                            OrangePrimary.copy(alpha = 0.3f)
                        )
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
                )
                
                // Main gradient progress
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            OrangePrimary,
                            Color(0xFFFF6B35),
                            Color(0xFFFFAB91),
                            OrangePrimary
                        )
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            
            // Icon at top
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-8).dp)
                    .size(48.dp)
                    .background(Color(0xFF1A1A1F), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    SneakerIcon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(28.dp)
                )
            }
            
            // Center content
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedFormattedCounter(
                    count = currentSteps,
                    style = TextStyle(
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = "STEPS TAKEN",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TrackingStatusSection(
            isServiceRunning = isServiceRunning,
            usesHardwareCounter = usesHardwareCounter,
            hasActivityPermission = hasActivityPermission,
            onStartTracking = {
                if (hasActivityPermission) {
                    viewModel.startService()
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    activityPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                } else {
                    viewModel.startService()
                }
            },
            onStopTracking = { viewModel.stopService() }
        )

        Spacer(modifier = Modifier.height(24.dp))

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
                        FootprintsIcon,
                        contentDescription = null,
                        tint = OrangePrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AnimatedDecimalCounter(
                        value = formattedDistance,
                        style = TextStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        distanceUnit,
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
                        "KCAL",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "TODAY'S GOAL",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        AnimatedFormattedCounter(
                            count = currentSteps,
                            style = TextStyle(
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            " / ${String.format("%,d", dailyGoal)}",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 16.sp
                        )
                    }
                }
                
                // Small progress indicator with gradient
                Box(
                    modifier = Modifier.size(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawArc(
                            color = Color.White.copy(alpha = 0.1f),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 4.dp.toPx())
                        )
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    OrangePrimary,
                                    Color(0xFFFF8A65),
                                    OrangePrimary
                                )
                            ),
                            startAngle = -90f,
                            sweepAngle = 360f * animatedProgress,
                            useCenter = false,
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Text(
                        "${goalProgress}%",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Weekly Activity",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                "STEPS",
                color = OrangePrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            if (isWeeklyDataLoading) {
                WeeklyChartLoading()
            } else {
                WeeklyChartContent(weeklySteps, currentSteps)
            }
        }
    }
}

@Composable
private fun PermissionWarningCard(
    hasActivityPermission: Boolean,
    isBatteryOptimized: Boolean,
    needsAutoStart: Boolean,
    manufacturerName: String,
    onActivityPermissionClick: () -> Unit,
    onBatteryClick: () -> Unit,
    onAutoStartClick: () -> Unit
) {
    val batteryHelperText = when (AutoStartManager.getManufacturer()) {
        AutoStartManager.Manufacturer.XIAOMI -> " Find Fitu → Battery saver → Select 'No restrictions'"
        AutoStartManager.Manufacturer.SAMSUNG -> " Find Fitu → Battery → Select 'Unrestricted'"
        AutoStartManager.Manufacturer.HUAWEI -> " Find Fitu → Disable 'Power-intensive prompt'"
        AutoStartManager.Manufacturer.OPPO -> " Find Fitu → Enable 'Allow background activity'"
        AutoStartManager.Manufacturer.VIVO -> " Find Fitu → Enable 'High background power consumption'"
        AutoStartManager.Manufacturer.ONEPLUS -> " Find Fitu → Battery → Select 'Don't optimize'"
        else -> " Set Fitu to 'Unrestricted' in battery settings"
    }

    val autoStartHelperText = when (AutoStartManager.getManufacturer()) {
        AutoStartManager.Manufacturer.XIAOMI -> " Find 'Fitu' and turn ON the autostart toggle"
        AutoStartManager.Manufacturer.HUAWEI -> " Find 'Fitu' → Manage manually → Turn on all toggles"
        AutoStartManager.Manufacturer.OPPO -> " Find 'Fitu' → Enable 'Allow Auto-startup'"
        AutoStartManager.Manufacturer.VIVO -> " Find 'Fitu' → Enable autostart permission"
        AutoStartManager.Manufacturer.SAMSUNG -> " Find 'Fitu' → Allow background activity"
        AutoStartManager.Manufacturer.ONEPLUS -> " Find 'Fitu' → Enable 'Allow auto-launch'"
        else -> " Find 'Fitu' in the list and enable autostart"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WarningOrange.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = WarningOrange,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "Permissions needed",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Enable these for accurate step tracking",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!hasActivityPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Text(
                    text = " Allow Fitu to access your physical activity data",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onActivityPermissionClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PermissionBlue),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(PermissionBlue))
                ) {
                    Icon(Icons.Default.DirectionsWalk, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enable Activity Tracking")
                }

                if (isBatteryOptimized || needsAutoStart) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            if (isBatteryOptimized) {
                Text(
                    text = batteryHelperText,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onBatteryClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningOrange),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(WarningOrange))
                ) {
                    Icon(Icons.Default.BatteryAlert, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Disable Battery Restrictions")
                }
                if (needsAutoStart) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            if (needsAutoStart) {
                Text(
                    text = autoStartHelperText,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onAutoStartClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningOrange),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(WarningOrange))
                ) {
                    Icon(Icons.Default.Settings, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enable Auto-Start ($manufacturerName)")
                }
            }
        }
    }
}

@Composable
private fun TrackingStatusSection(
    isServiceRunning: Boolean,
    usesHardwareCounter: Boolean,
    hasActivityPermission: Boolean,
    onStartTracking: () -> Unit,
    onStopTracking: () -> Unit
) {
    if (isServiceRunning) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(SuccessGreen, CircleShape)
                    )
                    Column {
                        Text(
                            "Tracking Active",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (usesHardwareCounter) "Using hardware sensor" else "Using accelerometer",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    }
                }
                IconButton(
                    onClick = onStopTracking,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "Stop Tracking",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    } else {
        Button(
            onClick = onStartTracking,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (hasActivityPermission) "Start Tracking" else "Enable & Start Tracking",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun WeeklyChartLoading() {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            repeat(7) {
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height((20 + (it * 10)).dp)
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach {
                Text(
                    it,
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 11.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = OrangePrimary,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Loading weekly data...",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun WeeklyChartContent(
    weeklySteps: List<DaySteps>,
    currentSteps: Int
) {
    val maxSteps = remember(weeklySteps, currentSteps) {
        weeklySteps.maxOfOrNull { daySteps: DaySteps ->
            if (daySteps.isToday) currentSteps else daySteps.steps
        }?.coerceAtLeast(1) ?: 1
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            weeklySteps.forEach { dayData: DaySteps ->
                val steps = if (dayData.isToday) currentSteps else dayData.steps
                val barHeight = if (maxSteps > 0) (steps.toFloat() / maxSteps * 80).dp else 4.dp
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(barHeight.coerceAtLeast(4.dp))
                        .background(
                            if (steps > 0) {
                                if (dayData.isToday) OrangePrimary else OrangePrimary.copy(alpha = 0.6f)
                            } else Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(4.dp)
                        )
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            weeklySteps.forEach { dayData: DaySteps ->
                Text(
                    dayData.day,
                    color = if (dayData.isToday) OrangePrimary else Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontWeight = if (dayData.isToday) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

private val SneakerIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Sneaker",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 256f,
        viewportHeight = 256f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(231.16f, 166.63f)
            lineTo(202.53f, 152.32f)
            arcTo(47.74f, 47.74f, 0f, false, true, 176f, 109.39f)
            verticalLineTo(80f)
            arcTo(8f, 8f, 0f, false, false, 168f, 72f)
            arcTo(48.05f, 48.05f, 0f, false, true, 120f, 24f)
            arcTo(8f, 8f, 0f, false, false, 107.17f, 17.63f)
            lineTo(30.13f, 76f)
            lineTo(29.93f, 76.16f)
            arcTo(16f, 16f, 0f, false, false, 28.69f, 99.91f)
            lineTo(142.4f, 213.66f)
            arcTo(8f, 8f, 0f, false, false, 148.06f, 216f)
            horizontalLineTo(224f)
            arcTo(16f, 16f, 0f, false, false, 240f, 200f)
            verticalLineTo(180.94f)
            arcTo(15.92f, 15.92f, 0f, false, false, 231.16f, 166.63f)
            close()
        }
    }.build()
