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
import com.example.data.model.WeatherCondition
import com.example.data.model.WeatherInfo
import com.example.data.repository.WeatherRepository
import com.example.ui.components.DotMatrix
import com.example.ui.components.WeatherDotMatrix
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class WeatherWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_UPDATE_WEATHER = "com.example.clock.UPDATE_WEATHER_WIDGET"

        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, WeatherWidgetProvider::class.java).apply {
                action = ACTION_UPDATE_WEATHER
            }
            context.sendBroadcast(intent)
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
        if (action == ACTION_UPDATE_WEATHER ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == "android.intent.action.TIME_SET" ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_USER_PRESENT ||
            action == AppWidgetManager.ACTION_APPWIDGET_UPDATE
        ) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, WeatherWidgetProvider::class.java)
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
        val intent = Intent(context, WeatherWidgetProvider::class.java).apply {
            action = ACTION_UPDATE_WEATHER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            3003,
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
        val intent = Intent(context, WeatherWidgetProvider::class.java).apply {
            action = ACTION_UPDATE_WEATHER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            3003,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Schedule periodic update every 30 minutes
        val triggerAtMillis = System.currentTimeMillis() + (30 * 60 * 1000L)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(AlarmManager.RTC, triggerAtMillis, pendingIntent)
            }
        } catch (_: Exception) {
            alarmManager.set(AlarmManager.RTC, triggerAtMillis, pendingIntent)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.weather_widget_layout)

        // Tap on widget opens MainActivity World Clock / Weather section
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_SECTION", "WORLD_CLOCK")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            3004,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_weather_root, pendingIntent)

        val weatherRepo = WeatherRepository.getInstance(context)

        // Render current cached data or default fallback immediately
        val cached = weatherRepo.weatherState.value ?: WeatherInfo(
            temperatureCelsius = 21.0,
            temperatureFahrenheit = 70.0,
            apparentTemperatureCelsius = 21.0,
            humidityPercent = 55,
            windSpeedKmh = 12.0,
            weatherCode = 0,
            isDay = true,
            condition = WeatherCondition.CLEAR_DAY,
            conditionName = "CLEAR",
            locationName = "WEATHER",
            lastUpdatedTimestamp = System.currentTimeMillis()
        )
        populateViews(context, views, cached)
        appWidgetManager.updateAppWidget(appWidgetId, views)

        // Fetch fresh weather asynchronously
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val fresh = weatherRepo.fetchCurrentWeather()
                if (fresh != null) {
                    val updatedViews = RemoteViews(context.packageName, R.layout.weather_widget_layout)
                    updatedViews.setOnClickPendingIntent(R.id.widget_weather_root, pendingIntent)
                    populateViews(context, updatedViews, fresh)
                    appWidgetManager.updateAppWidget(appWidgetId, updatedViews)
                }
            } catch (e: Exception) {
                android.util.Log.e("WeatherWidgetProvider", "Error updating weather widget", e)
            }
        }
    }

    private fun populateViews(context: Context, views: RemoteViews, weather: WeatherInfo) {
        val sharedPrefs = context.getSharedPreferences("nothing_clock_settings", Context.MODE_PRIVATE)
        val isFahrenheit = sharedPrefs.getBoolean("is_fahrenheit", false)

        val tempValue = if (isFahrenheit) weather.temperatureFahrenheit.toInt() else weather.temperatureCelsius.toInt()
        val tempUnit = if (isFahrenheit) "°F" else "°C"
        val tempString = "$tempValue$tempUnit"

        // Render dot matrix weather icon
        val iconBitmap = WeatherDotMatrix.createWeatherBitmap(
            condition = weather.condition,
            activeColor = Color.WHITE,
            inactiveColor = Color.argb(25, 255, 255, 255),
            dotSizePx = 4.5f,
            dotSpacingPx = 1.8f
        )
        views.setImageViewBitmap(R.id.widget_weather_icon, iconBitmap)

        // Render dot matrix temperature bitmap
        val tempBitmap = createDotMatrixTextBitmap(
            text = tempString,
            activeColor = Color.WHITE,
            inactiveColor = Color.argb(20, 255, 255, 255)
        )
        views.setImageViewBitmap(R.id.widget_weather_temp_matrix, tempBitmap)

        views.setTextViewText(R.id.widget_weather_condition_pill, weather.conditionName.uppercase(Locale.US))
        views.setTextViewText(R.id.widget_weather_location, weather.locationName.uppercase(Locale.US))

        val metaText = "${weather.humidityPercent}% RH • ${weather.windSpeedKmh.toInt()} KM/H"
        views.setTextViewText(R.id.widget_weather_meta, metaText)
    }

    private fun createDotMatrixTextBitmap(
        text: String,
        activeColor: Int,
        inactiveColor: Int
    ): Bitmap {
        val dotSize = 8f
        val dotSpacing = 2f
        val charSpacing = 8f
        val paddingX = 4f
        val paddingY = 4f

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
