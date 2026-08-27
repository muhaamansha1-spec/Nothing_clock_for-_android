package com.example.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.util.Log
import com.example.data.model.WeatherCondition
import com.example.data.model.WeatherInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.TimeZone

class WeatherRepository private constructor(private val context: Context) {

    companion object {
        private const val TAG = "WeatherRepository"
        private const val PREFS_NAME = "nothing_weather_cache"
        private const val KEY_TEMP_C = "cached_temp_c"
        private const val KEY_TEMP_F = "cached_temp_f"
        private const val KEY_APPARENT_C = "cached_apparent_c"
        private const val KEY_HUMIDITY = "cached_humidity"
        private const val KEY_WIND = "cached_wind"
        private const val KEY_CODE = "cached_code"
        private const val KEY_IS_DAY = "cached_is_day"
        private const val KEY_LOCATION = "cached_location"
        private const val KEY_TIMESTAMP = "cached_timestamp"

        @Volatile
        private var INSTANCE: WeatherRepository? = null

        fun getInstance(context: Context): WeatherRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = WeatherRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    private val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _weatherState = MutableStateFlow<WeatherInfo?>(loadCachedWeather())
    val weatherState: StateFlow<WeatherInfo?> = _weatherState.asStateFlow()

    private fun loadCachedWeather(): WeatherInfo? {
        val timestamp = sharedPrefs.getLong(KEY_TIMESTAMP, 0L)
        if (timestamp == 0L) return null

        val tempC = sharedPrefs.getFloat(KEY_TEMP_C, 20f).toDouble()
        val tempF = sharedPrefs.getFloat(KEY_TEMP_F, 68f).toDouble()
        val apparentC = sharedPrefs.getFloat(KEY_APPARENT_C, 20f).toDouble()
        val humidity = sharedPrefs.getInt(KEY_HUMIDITY, 50)
        val wind = sharedPrefs.getFloat(KEY_WIND, 10f).toDouble()
        val code = sharedPrefs.getInt(KEY_CODE, 0)
        val isDay = sharedPrefs.getBoolean(KEY_IS_DAY, true)
        val location = sharedPrefs.getString(KEY_LOCATION, "LONDON") ?: "LONDON"

        val condition = WeatherCondition.fromWmoCode(code, isDay)
        val conditionName = getConditionDisplayName(code)

        return WeatherInfo(
            temperatureCelsius = tempC,
            temperatureFahrenheit = tempF,
            apparentTemperatureCelsius = apparentC,
            humidityPercent = humidity,
            windSpeedKmh = wind,
            weatherCode = code,
            isDay = isDay,
            condition = condition,
            conditionName = conditionName,
            locationName = location,
            lastUpdatedTimestamp = timestamp
        )
    }

    private fun saveCachedWeather(weather: WeatherInfo) {
        sharedPrefs.edit()
            .putFloat(KEY_TEMP_C, weather.temperatureCelsius.toFloat())
            .putFloat(KEY_TEMP_F, weather.temperatureFahrenheit.toFloat())
            .putFloat(KEY_APPARENT_C, weather.apparentTemperatureCelsius.toFloat())
            .putInt(KEY_HUMIDITY, weather.humidityPercent)
            .putFloat(KEY_WIND, weather.windSpeedKmh.toFloat())
            .putInt(KEY_CODE, weather.weatherCode)
            .putBoolean(KEY_IS_DAY, weather.isDay)
            .putString(KEY_LOCATION, weather.locationName)
            .putLong(KEY_TIMESTAMP, weather.lastUpdatedTimestamp)
            .apply()
    }

    /**
     * Fetches current weather from Open-Meteo open-source weather API.
     */
    suspend fun fetchCurrentWeather(): WeatherInfo? = withContext(Dispatchers.IO) {
        try {
            val (lat, lon, cityName) = resolveLocationCoordinates()

            val urlString = "https://api.open-meteo.com/v1/forecast?" +
                    "latitude=$lat&longitude=$lon" +
                    "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code,wind_speed_10m" +
                    "&temperature_unit=celsius"

            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "NothingClock-Android/1.0")
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.use { it.readText() }
                connection.disconnect()

                val json = JSONObject(response)
                val current = json.getJSONObject("current")

                val tempC = current.getDouble("temperature_2m")
                val tempF = (tempC * 9.0 / 5.0) + 32.0
                val apparentC = current.optDouble("apparent_temperature", tempC)
                val humidity = current.optInt("relative_humidity_2m", 50)
                val wind = current.optDouble("wind_speed_10m", 0.0)
                val code = current.getInt("weather_code")
                val isDay = current.getInt("is_day") == 1

                val condition = WeatherCondition.fromWmoCode(code, isDay)
                val conditionName = getConditionDisplayName(code)

                val info = WeatherInfo(
                    temperatureCelsius = tempC,
                    temperatureFahrenheit = tempF,
                    apparentTemperatureCelsius = apparentC,
                    humidityPercent = humidity,
                    windSpeedKmh = wind,
                    weatherCode = code,
                    isDay = isDay,
                    condition = condition,
                    conditionName = conditionName,
                    locationName = cityName.uppercase(Locale.US),
                    lastUpdatedTimestamp = System.currentTimeMillis()
                )

                saveCachedWeather(info)
                _weatherState.value = info
                return@withContext info
            } else {
                Log.w(TAG, "Open-Meteo returned status code: ${connection.responseCode}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching weather data from Open-Meteo", e)
        }
        return@withContext _weatherState.value
    }

    @SuppressLint("MissingPermission")
    private fun resolveLocationCoordinates(): Triple<Double, Double, String> {
        // Try device LocationManager first if permission is present
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (locationManager != null) {
                val hasFine = context.checkCallingOrSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED
                val hasCoarse = context.checkCallingOrSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED

                if (hasFine || hasCoarse) {
                    val lastKnownGps: Location? = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    val lastKnownNet: Location? = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    val loc = lastKnownGps ?: lastKnownNet

                    if (loc != null) {
                        var cityName = "LOCAL"
                        try {
                            if (Geocoder.isPresent()) {
                                val geocoder = Geocoder(context, Locale.getDefault())
                                val addresses = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                                if (!addresses.isNullOrEmpty()) {
                                    val city = addresses[0].locality ?: addresses[0].subAdminArea ?: addresses[0].countryName
                                    if (!city.isNullOrBlank()) {
                                        cityName = city
                                    }
                                }
                            }
                        } catch (_: Exception) {}
                        return Triple(loc.latitude, loc.longitude, cityName)
                    }
                }
            }
        } catch (_: Exception) {}

        // Fallback: TimeZone based coordinates
        val tzId = TimeZone.getDefault().id
        return getFallbackCoordinatesForTimezone(tzId)
    }

    private fun getFallbackCoordinatesForTimezone(tzId: String): Triple<Double, Double, String> {
        val idUpper = tzId.uppercase(Locale.US)
        return when {
            idUpper.contains("LONDON") -> Triple(51.5074, -0.1278, "LONDON")
            idUpper.contains("NEW_YORK") -> Triple(40.7128, -74.0060, "NEW YORK")
            idUpper.contains("LOS_ANGELES") -> Triple(34.0522, -118.2437, "LOS ANGELES")
            idUpper.contains("CHICAGO") -> Triple(41.8781, -87.6298, "CHICAGO")
            idUpper.contains("TOKYO") -> Triple(35.6762, 139.6503, "TOKYO")
            idUpper.contains("PARIS") -> Triple(48.8566, 2.3522, "PARIS")
            idUpper.contains("BERLIN") -> Triple(52.5200, 13.4050, "BERLIN")
            idUpper.contains("SYDNEY") -> Triple(-33.8688, 151.2093, "SYDNEY")
            idUpper.contains("SINGAPORE") -> Triple(1.3521, 103.8198, "SINGAPORE")
            idUpper.contains("DUBAI") -> Triple(25.2048, 55.2708, "DUBAI")
            idUpper.contains("TORONTO") -> Triple(43.6532, -79.3832, "TORONTO")
            idUpper.contains("SEOUL") -> Triple(37.5665, 126.9780, "SEOUL")
            idUpper.contains("HONG_KONG") -> Triple(22.3193, 114.1694, "HONG KONG")
            idUpper.contains("MUMBAI") || idUpper.contains("KOLKATA") -> Triple(19.0760, 72.8777, "MUMBAI")
            idUpper.contains("SAO_PAULO") -> Triple(-23.5505, -46.6333, "SÃO PAULO")
            else -> {
                val cleanName = tzId.substringAfterLast("/").replace("_", " ")
                Triple(51.5074, -0.1278, if (cleanName.isNotBlank()) cleanName else "LONDON")
            }
        }
    }

    private fun getConditionDisplayName(code: Int): String {
        return when (code) {
            0 -> "CLEAR"
            1, 2 -> "PARTLY CLOUDY"
            3 -> "OVERCAST"
            45, 48 -> "FOG"
            51, 53, 55 -> "DRIZZLE"
            56, 57 -> "FREEZING DRIZZLE"
            61, 63 -> "RAIN"
            65 -> "HEAVY RAIN"
            66, 67 -> "FREEZING RAIN"
            71, 73 -> "SNOW"
            75, 77 -> "HEAVY SNOW"
            80, 81, 82 -> "SHOWERS"
            85, 86 -> "SNOW SHOWERS"
            95 -> "THUNDERSTORM"
            96, 99 -> "THUNDERSTORM / HAIL"
            else -> "CLEAR"
        }
    }
}
