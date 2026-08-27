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
import android.util.TypedValue
import android.widget.RemoteViews
import com.example.ui.components.DotMatrix
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ClockWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_UPDATE_CLOCK = "com.example.clock.UPDATE_WIDGET"

        fun saveDrawingDots(context: Context, dots: BooleanArray) {
            val stringBuilder = StringBuilder()
            for (dot in dots) {
                stringBuilder.append(if (dot) '1' else '0')
            }
            context.getSharedPreferences("nothing_clock_settings", Context.MODE_PRIVATE)
                .edit()
                .putString("custom_glyph_dots", stringBuilder.toString())
                .apply()

            // Trigger widget update
            val intent = Intent(context, ClockWidgetProvider::class.java).apply {
                action = ACTION_UPDATE_CLOCK
            }
            context.sendBroadcast(intent)
        }

        fun loadDrawingDots(context: Context): BooleanArray {
            val savedStr = context.getSharedPreferences("nothing_clock_settings", Context.MODE_PRIVATE)
                .getString("custom_glyph_dots", null)
            if (savedStr != null && savedStr.length == 35) {
                val dots = BooleanArray(35)
                for (i in 0 until 35) {
                    dots[i] = savedStr[i] == '1'
                }
                return dots
            }
            // Return default heart preset
            return booleanArrayOf(
                false, true,  false, true,  false,
                true,  true,  true,  true,  true,
                true,  true,  true,  true,  true,
                false, true,  true,  true,  false,
                false, false, true,  false, false,
                false, false, false, false, false,
                false, false, false, false, false
            )
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidgetWithOptions(context, appWidgetManager, appWidgetId, null)
        }
        scheduleNextUpdate(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        updateAppWidgetWithOptions(context, appWidgetManager, appWidgetId, newOptions)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (action == ACTION_UPDATE_CLOCK || 
            action == Intent.ACTION_TIME_CHANGED || 
            action == "android.intent.action.TIME_SET" ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_USER_PRESENT) {
            
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, ClockWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            
            for (appWidgetId in appWidgetIds) {
                updateAppWidgetWithOptions(context, appWidgetManager, appWidgetId, null)
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
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ClockWidgetProvider::class.java).apply {
            action = ACTION_UPDATE_CLOCK
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun scheduleNextUpdate(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ClockWidgetProvider::class.java).apply {
            action = ACTION_UPDATE_CLOCK
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Calculate time to the next minute boundary exactly
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.add(Calendar.MINUTE, 1)
        val nextTriggerTime = calendar.timeInMillis

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextTriggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextTriggerTime,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextTriggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    nextTriggerTime,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // Fallback for security exception if permission is denied or revoked
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextTriggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    nextTriggerTime,
                    pendingIntent
                )
            }
        }
    }

    private fun updateAppWidgetWithOptions(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        options: Bundle?
    ) {
        val views = RemoteViews(context.packageName, R.layout.clock_widget_layout)

        val finalOptions = options ?: appWidgetManager.getAppWidgetOptions(appWidgetId)
        val minWidth = finalOptions?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) ?: 140
        val minHeight = finalOptions?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) ?: 60

        // Calculate dynamic font sizes for the sub-text elements based on boundaries
        val widthFontFactor = minWidth * 0.35f
        val heightFontFactor = minHeight * 0.65f
        val baseFontSize = minOf(widthFontFactor, heightFontFactor).coerceIn(32f, 160f)
        val dateFontSize = (baseFontSize * 0.22f).coerceIn(10f, 24f)

        // Set sub-text sizes dynamically
        views.setTextViewTextSize(R.id.widget_date_clock, TypedValue.COMPLEX_UNIT_SP, dateFontSize)

        // Format time string for dot matrix display
        val is24Hour = android.text.format.DateFormat.is24HourFormat(context)
        val timeFormat = if (is24Hour) {
            SimpleDateFormat("HH:mm", Locale.getDefault())
        } else {
            SimpleDateFormat("hh:mm", Locale.getDefault())
        }
        val timeStr = timeFormat.format(Date())

        // Render dot matrix bitmap (Active color: White, Inactive color: subtle 10% translucent white)
        val activeColor = Color.WHITE
        val inactiveColor = Color.argb(26, 255, 255, 255) // 0x1AFFFFFF (approx 10% opacity)
        val dotMatrixBitmap = createDotMatrixBitmap(context, timeStr, activeColor, inactiveColor)

        // Set the rendered dot matrix bitmap on the ImageView
        views.setImageViewBitmap(R.id.widget_dot_matrix_image, dotMatrixBitmap)

        // Clicking the widget launches MainActivity
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun getSystemAccentColor(context: Context): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                context.getColor(android.R.color.system_accent1_500)
            } catch (e: Exception) {
                Color.rgb(255, 43, 43)
            }
        } else {
            Color.rgb(255, 43, 43)
        }
    }

    private fun createDotMatrixBitmap(
        context: Context,
        text: String,
        activeColor: Int,
        inactiveColor: Int
    ): Bitmap {
        val dotSize = 16f
        val dotSpacing = 4f
        val charSpacing = 24f
        val paddingX = 16f
        val paddingY = 16f

        val charWidth = 5 * dotSize + 4 * dotSpacing
        val charHeight = 7 * dotSize + 6 * dotSpacing

        val glyphSpacing = charSpacing * 2f

        val totalWidth = (text.length * charWidth + (text.length - 1) * charSpacing + glyphSpacing + charWidth + 2 * paddingX).toInt()
        val totalHeight = (charHeight + 2 * paddingY).toInt()

        val bitmap = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888)
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

        // Draw Custom Glyph
        currentX += glyphSpacing - charSpacing
        val customGlyph = loadDrawingDots(context)
        val accentColor = getSystemAccentColor(context)
        
        for (row in 0 until 7) {
            for (col in 0 until 5) {
                val isActive = customGlyph[row * 5 + col]
                paint.color = if (isActive) accentColor else inactiveColor
                
                val cx = currentX + col * (dotSize + dotSpacing) + dotSize / 2f
                val cy = paddingY + row * (dotSize + dotSpacing) + dotSize / 2f
                
                canvas.drawCircle(cx, cy, dotSize / 2f, paint)
            }
        }

        return bitmap
    }
}
