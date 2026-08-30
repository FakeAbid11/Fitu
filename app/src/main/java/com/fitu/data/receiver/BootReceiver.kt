package com.fitu.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.fitu.data.service.StepCounterService

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received broadcast: ${intent.action}")

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON",
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                startStepCounterService(context)
            }
        }
    }

    private fun startStepCounterService(context: Context) {
        try {
            val prefs = context.getSharedPreferences("fitu_preferences", Context.MODE_PRIVATE)
            val isOnboardingComplete = prefs.getBoolean("onboarding_complete", false)

            if (!isOnboardingComplete) {
                Log.d(TAG, "Onboarding not complete, skipping service start")
                return
            }

            val stepGoal = prefs.getInt("daily_step_goal", 10000)

            val serviceIntent = Intent(context, StepCounterService::class.java).apply {
                putExtra("step_goal", stepGoal)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }

            Log.d(TAG, "StepCounterService started after boot")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start StepCounterService after boot", e)
        }
    }
}
