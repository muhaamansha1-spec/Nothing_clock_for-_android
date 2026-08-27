package com.example

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.example.data.local.AppDatabase
import com.example.data.model.Alarm
import com.example.ui.components.DotMatrix
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class AlarmWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_UPDATE_ALARM_WIDGET = "com.example.clock.UPDATE_ALARM_WIDGET"

        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, AlarmWidgetProvider::class.java).apply {
                action = ACTION_UPDATE_ALARM_WIDGET
            }
            context.sendBroadcast(intent)
        }

        fun getNextTriggerTime(alarm: Alarm): Long {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, alarm.hour)
            calendar.set(Calendar.MINUTE, alarm.minute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            val now = Calendar.getInstance()

            if (alarm.isRecurring) {
                val daysList = alarm.daysOfWeek.split(",").filter { it.isNotBlank() }
                if (calendar.timeInMillis <= now.timeInMillis) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
                for (i in 0..7) {
                    val dayOfWeekStr = when (calendar.get(Calendar.DAY_OF_WEEK)) {
                        Calendar.MONDAY -> "Mon"
                        Calendar.TUESDAY -> "Tue"
                        Calendar.WEDNESDAY -> "Wed"
                        Calendar.THURSDAY -> "Thu"
                        Calendar.FRIDAY -> "Fri"
                        Calendar.SATURDAY -> "Sat"
                        Calendar.SUNDAY -> "Sun"
                        else -> ""
                    }
                    if (daysList.contains(dayOfWeekStr)) {
                        return calendar.timeInMillis
                    }
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
                return calendar.timeInMillis
            } else {
                if (calendar.timeInMillis <= now.timeInMillis) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
                return calendar.timeInMillis
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        scheduleNextUpdate(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (action == ACTION_UPDATE_ALARM_WIDGET ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == "android.intent.action.TIME_SET" ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_USER_PRESENT ||
            action == AppWidgetManager.ACTION_APPWIDGET_UPDATE
        ) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, AlarmWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
            scheduleNextUpdate(context)
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleNextUpdate(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmWidgetProvider::class.java).apply {
            action = ACTION_UPDATE_ALARM_WIDGET
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1002,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun scheduleNextUpdate(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmWidgetProvider::class.java).apply {
            action = ACTION_UPDATE_ALARM_WIDGET
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1002,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Schedule next check on minute boundary
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MINUTE, 1)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val triggerAtMillis = calendar.timeInMillis

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            alarmManager.set(AlarmManager.RTC, triggerAtMillis, pendingIntent)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.alarm_widget_layout)

        // Launch MainActivity directly to ALARMS tab on tap
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_SECTION", "ALARMS")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            2002,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        // Asynchronously fetch current alarms from Room
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val allAlarms = db.alarmDao().getAllAlarmsList()
                val enabledAlarms = allAlarms.filter { it.isEnabled }

                val nextAlarmPair = enabledAlarms.map { it to getNextTriggerTime(it) }
                    .minByOrNull { it.second }

                val sharedPrefs = context.getSharedPreferences("nothing_clock_settings", Context.MODE_PRIVATE)
                val is24Hour = sharedPrefs.getBoolean("is_24_hour", true)

                if (nextAlarmPair != null) {
                    val (alarm, triggerTime) = nextAlarmPair
                    val diffMillis = triggerTime - System.currentTimeMillis()

                    val timeString: String
                    if (is24Hour) {
                        timeString = String.format(Locale.US, "%02d:%02d", alarm.hour, alarm.minute)
                    } else {
                        val displayHour = if (alarm.hour % 12 == 0) 12 else alarm.hour % 12
                        val amPm = if (alarm.hour < 12) "AM" else "PM"
                        timeString = String.format(Locale.US, "%02d:%02d", displayHour, alarm.minute)
                    }

                    val dotMatrixBitmap = createDotMatrixBitmap(
                        context = context,
                        text = timeString,
                        activeColor = Color.WHITE,
                        inactiveColor = Color.argb(20, 255, 255, 255)
                    )

                    val label = if (alarm.label.isNotBlank()) alarm.label.uppercase(Locale.US) else "ALARM"
                    val daysText = if (alarm.isRecurring) {
                        alarm.daysOfWeek.replace(",", ", ").uppercase(Locale.US)
                    } else {
                        val nowCal = Calendar.getInstance()
                        val triggerCal = Calendar.getInstance().apply { timeInMillis = triggerTime }
                        if (triggerCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)) {
                            "TODAY"
                        } else {
                            "TOMORROW"
                        }
                    }

                    val countdownText = if (diffMillis <= 0) {
                        "RINGING"
                    } else {
                        val totalMinutes = Math.max(1L, diffMillis / 60000L)
                        val hours = totalMinutes / 60
                        val mins = totalMinutes % 60
                        if (hours > 0) "IN ${hours}H ${mins}M" else "IN ${mins}M"
                    }

                    views.setViewVisibility(R.id.widget_active_alarm_container, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_no_alarm_container, View.GONE)
                    views.setImageViewBitmap(R.id.widget_alarm_dot_matrix, dotMatrixBitmap)
                    views.setTextViewText(R.id.widget_alarm_label, label)
                    views.setTextViewText(R.id.widget_alarm_days, daysText)
                    views.setTextViewText(R.id.widget_countdown_pill, countdownText)
                    views.setViewVisibility(R.id.widget_countdown_pill, View.VISIBLE)
                } else {
                    views.setViewVisibility(R.id.widget_active_alarm_container, View.GONE)
                    views.setViewVisibility(R.id.widget_no_alarm_container, View.VISIBLE)
                    views.setTextViewText(R.id.widget_countdown_pill, "OFF")
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                android.util.Log.e("AlarmWidgetProvider", "Error updating alarm widget", e)
            }
        }
    }

    private fun createDotMatrixBitmap(
        context: Context,
        text: String,
        activeColor: Int,
        inactiveColor: Int
    ): Bitmap {
        val dotSize = 14f
        val dotSpacing = 3.5f
        val charSpacing = 18f
        val paddingX = 12f
        val paddingY = 12f

        val charWidth = 5 * dotSize + 4 * dotSpacing
        val charHeight = 7 * dotSize + 6 * dotSpacing

        val totalWidth = (text.length * charWidth + (text.length - 1) * charSpacing + 2 * paddingX).toInt()
        val totalHeight = (charHeight + 2 * paddingY).toInt()

        val bitmap = Bitmap.createBitmap(Math.max(1, totalWidth), Math.max(1, totalHeight), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        canvas.drawColor(Color.TRANSPARENT)

        var currentX = paddingX
        for (i in text.indices) {
            val char = text[i]
            val grid = DotMatrix.getGrid(char)

            for (row in 0 until 7) {
                for (col in 0 until 5) {
                    val isActive = grid[row * 5 + col]
                    paint.color = if (isActive) activeColor else inactiveColor

                    val cx = currentX + col * (dotSize + dotSpacing) + dotSize / 2f
                    val cy = paddingY + row * (dotSize + dotSpacing) + dotSize / 2f

                    canvas.drawCircle(cx, cy, dotSize / 2f, paint)
                }
            }
            currentX += charWidth + charSpacing
        }

        return bitmap
    }
}
