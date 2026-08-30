package com.fitu.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log

object AutoStartManager {

    private const val TAG = "AutoStartManager"
    private const val PREFS_NAME = "auto_start_prefs"
    private const val KEY_AUTO_START_CONFIGURED = "auto_start_configured"

    enum class Manufacturer {
        XIAOMI, HUAWEI, OPPO, VIVO, SAMSUNG, ONEPLUS, LETV, ASUS, NOKIA, MEIZU, OTHER
    }

    fun getManufacturer(): Manufacturer {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") -> Manufacturer.XIAOMI
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> Manufacturer.HUAWEI
            manufacturer.contains("oppo") -> Manufacturer.OPPO
            manufacturer.contains("vivo") -> Manufacturer.VIVO
            manufacturer.contains("samsung") -> Manufacturer.SAMSUNG
            manufacturer.contains("oneplus") -> Manufacturer.ONEPLUS
            manufacturer.contains("letv") || manufacturer.contains("leeco") -> Manufacturer.LETV
            manufacturer.contains("asus") -> Manufacturer.ASUS
            manufacturer.contains("nokia") -> Manufacturer.NOKIA
            manufacturer.contains("meizu") -> Manufacturer.MEIZU
            else -> Manufacturer.OTHER
        }
    }

    fun hasAutoStartFeature(): Boolean = getManufacturer() != Manufacturer.OTHER

    fun getManufacturerName(): String {
        return when (getManufacturer()) {
            Manufacturer.XIAOMI -> "Xiaomi/Redmi"
            Manufacturer.HUAWEI -> "Huawei/Honor"
            Manufacturer.OPPO -> "Oppo"
            Manufacturer.VIVO -> "Vivo"
            Manufacturer.SAMSUNG -> "Samsung"
            Manufacturer.ONEPLUS -> "OnePlus"
            Manufacturer.LETV -> "LeEco"
            Manufacturer.ASUS -> "Asus"
            Manufacturer.NOKIA -> "Nokia"
            Manufacturer.MEIZU -> "Meizu"
            Manufacturer.OTHER -> Build.MANUFACTURER
        }
    }

    fun isAutoStartConfigured(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUTO_START_CONFIGURED, false)
    }

    fun setAutoStartConfigured(context: Context, configured: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_START_CONFIGURED, configured).apply()
    }

    fun shouldShowAutoStartWarning(context: Context): Boolean {
        return hasAutoStartFeature() && !isAutoStartConfigured(context)
    }

    fun getAutoStartInstructions(): String {
        return when (getManufacturer()) {
            Manufacturer.XIAOMI -> "1. Find 'Fitu' in the app list\n2. Enable 'Autostart' toggle"
            Manufacturer.HUAWEI -> "1. Find 'Fitu'\n2. Enable 'Manage manually'\n3. Turn on all toggles"
            Manufacturer.OPPO -> "1. Find 'Fitu'\n2. Enable 'Allow Auto-startup'"
            Manufacturer.VIVO -> "1. Find 'Fitu'\n2. Enable autostart permission"
            Manufacturer.SAMSUNG -> "1. Find 'Fitu'\n2. Tap 'Battery'\n3. Select 'Unrestricted'"
            Manufacturer.ONEPLUS -> "1. Find 'Fitu'\n2. Enable 'Allow auto-launch'"
            else -> "1. Open Settings\n2. Find 'Fitu'\n3. Enable auto-start"
        }
    }

    private val autoStartIntents = listOf(
        Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
        Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.powercenter.PowerSettings")),
        Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")),
        Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")),
        Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")),
        Intent().setComponent(ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")),
        Intent().setComponent(ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")),
        Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager")),
        Intent().setComponent(ComponentName("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity")),
        Intent().setComponent(ComponentName("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity")),
        Intent().setComponent(ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.autostart.AutoStartActivity")),
        Intent().setComponent(ComponentName("com.evenwell.powersaving.g3", "com.evenwell.powersaving.g3.exception.PowerSaverExceptionActivity")),
        Intent().setComponent(ComponentName("com.meizu.safe", "com.meizu.safe.permission.SmartBGActivity"))
    )

    fun openAutoStartSettings(context: Context): Boolean {
        for (intent in autoStartIntents) {
            if (isIntentAvailable(context, intent)) {
                try {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    setAutoStartConfigured(context, true)
                    return true
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to open auto-start settings", e)
                }
            }
        }
        val result = openAppInfoSettings(context)
        if (result) setAutoStartConfigured(context, true)
        return result
    }

    fun openAppInfoSettings(context: Context): Boolean {
        return try {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun isIntentAvailable(context: Context, intent: Intent): Boolean {
        return try {
            context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}
