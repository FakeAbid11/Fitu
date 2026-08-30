package com.fitu.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.fitu.MainActivity
import com.fitu.R
import com.fitu.data.service.StepCounterService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class StepsWidget : AppWidgetProvider() {

    private val job = SupervisorJob()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + job)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Called when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Called when the last widget is removed
        job.cancel()
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        if (intent.action == ACTION_UPDATE_WIDGET) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, StepsWidget::class.java)
            )
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        coroutineScope.launch {
            val views = RemoteViews(context.packageName, R.layout.steps_widget)

            // Get step data
            val currentSteps = StepCounterService.stepCount.value
            val stepGoal = getStepGoal(context)
            
            // Calculate progress
            val progress = if (stepGoal > 0) {
                ((currentSteps.toFloat() / stepGoal) * 100).toInt().coerceIn(0, 100)
            } else 0

            // Format numbers
            val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())
            val formattedSteps = numberFormat.format(currentSteps)
            val formattedGoal = numberFormat.format(stepGoal)

            // Update views
            views.setTextViewText(R.id.steps_count, formattedSteps)
            views.setTextViewText(R.id.goal_text, "Goal: $formattedGoal steps")
            views.setTextViewText(R.id.percentage_text, "$progress%")
            views.setProgressBar(R.id.progress_bar, 100, progress, false)

            // Set click intent to open app
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

            // Update widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun getStepGoal(context: Context): Int {
        val prefs = context.getSharedPreferences("fitu_widget_prefs", Context.MODE_PRIVATE)
        return prefs.getInt("step_goal", 10000)
    }

    companion object {
        const val ACTION_UPDATE_WIDGET = "com.fitu.widget.ACTION_UPDATE_WIDGET"

        fun updateWidget(context: Context) {
            val intent = Intent(context, StepsWidget::class.java).apply {
                action = ACTION_UPDATE_WIDGET
            }
            context.sendBroadcast(intent)
        }

        fun saveStepGoal(context: Context, goal: Int) {
            val prefs = context.getSharedPreferences("fitu_widget_prefs", Context.MODE_PRIVATE)
            prefs.edit().putInt("step_goal", goal).apply()
            updateWidget(context)
        }
    }
}
