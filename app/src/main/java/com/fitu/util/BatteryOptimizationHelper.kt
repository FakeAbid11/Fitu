package com.fitu.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

object BatteryOptimizationHelper {

    private const val TAG = "BatteryOptimizationHelper"

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
    }

    fun requestIgnoreBatteryOptimization(context: Context): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request battery optimization exemption", e)
            openBatteryOptimizationSettings(context)
        }
    }

    fun openBatteryOptimizationSettings(context: Context): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            openAppBatterySettings(context)
        }
    }

    fun openAppBatterySettings(context: Context): Boolean {
        if (openOemBatterySettings(context)) return true
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun openOemBatterySettings(context: Context): Boolean {
        val oemIntents = when (AutoStartManager.getManufacturer()) {
            AutoStartManager.Manufacturer.XIAOMI -> listOf(
                Intent().setComponent(ComponentName("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity")),
                Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.powercenter.PowerSettings"))
            )
            AutoStartManager.Manufacturer.HUAWEI -> listOf(
                Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.power.ui.HwPowerManagerActivity"))
            )
            AutoStartManager.Manufacturer.SAMSUNG -> listOf(
                Intent().setComponent(ComponentName("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity"))
            )
            AutoStartManager.Manufacturer.OPPO -> listOf(
                Intent().setComponent(ComponentName("com.coloros.oppoguardelf", "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity"))
            )
            AutoStartManager.Manufacturer.VIVO -> listOf(
                Intent().setComponent(ComponentName("com.vivo.abe", "com.vivo.applicationbehaviorengine.ui.ExcessivePowerManagerActivity"))
            )
            else -> emptyList()
        }

        for (intent in oemIntents) {
            try {
                if (context.packageManager.queryIntentActivities(intent, 0).isNotEmpty()) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    return true
                }
            } catch (e: Exception) {
                Log.w(TAG, "OEM intent not available")
            }
        }
        return false
    }

    fun getBatteryOptimizationInstructions(): String {
        return when (AutoStartManager.getManufacturer()) {
            AutoStartManager.Manufacturer.XIAOMI -> "1. Find 'Fitu'\n2. Set 'Battery saver' to 'No restrictions'"
            AutoStartManager.Manufacturer.HUAWEI -> "1. Find 'Fitu'\n2. Disable 'Power-intensive prompt'\n3. Enable 'Keep running after screen off'"
            AutoStartManager.Manufacturer.SAMSUNG -> "1. Find 'Fitu'\n2. Tap 'Battery'\n3. Select 'Unrestricted'"
            AutoStartManager.Manufacturer.OPPO -> "1. Find 'Fitu'\n2. Enable 'Allow background activity'"
            AutoStartManager.Manufacturer.VIVO -> "1. Find 'Fitu'\n2. Enable 'Allow high background power consumption'"
            AutoStartManager.Manufacturer.ONEPLUS -> "1. Find 'Fitu'\n2. Set Battery to 'Don't optimize'"
            else -> "1. Find 'Fitu'\n2. Tap 'Battery'\n3. Select 'Unrestricted'"
        }
    }
}
