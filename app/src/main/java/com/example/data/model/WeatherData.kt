package com.example.data.model

enum class WeatherCondition {
    CLEAR_DAY,
    CLEAR_NIGHT,
    PARTLY_CLOUDY_DAY,
    PARTLY_CLOUDY_NIGHT,
    CLOUDY,
    FOGGY,
    RAINY,
    THUNDERSTORM,
    SNOWY,
    WINDY;

    companion object {
        fun fromWmoCode(code: Int, isDay: Boolean): WeatherCondition {
            return when (code) {
                0 -> if (isDay) CLEAR_DAY else CLEAR_NIGHT
                1, 2 -> if (isDay) PARTLY_CLOUDY_DAY else PARTLY_CLOUDY_NIGHT
                3 -> CLOUDY
                45, 48 -> FOGGY
                51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> RAINY
                71, 73, 75, 77, 85, 86 -> SNOWY
                95, 96, 99 -> THUNDERSTORM
                else -> if (isDay) CLEAR_DAY else CLEAR_NIGHT
            }
        }
    }
}

data class WeatherInfo(
    val temperatureCelsius: Double,
    val temperatureFahrenheit: Double,
    val apparentTemperatureCelsius: Double,
    val humidityPercent: Int,
    val windSpeedKmh: Double,
    val weatherCode: Int,
    val isDay: Boolean,
    val condition: WeatherCondition,
    val conditionName: String,
    val locationName: String,
    val lastUpdatedTimestamp: Long
)
