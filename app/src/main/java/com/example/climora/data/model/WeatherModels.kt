package com.example.climora.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeatherApiData(
    val location: LocationData = LocationData(),
    val current: CurrentData = CurrentData(),
    val forecast: ForecastData = ForecastData()
)

@Serializable
data class LocationData(
    val name: String = "",
    val region: String = "",
    val country: String = ""
)

@Serializable
data class CurrentData(
    @SerialName("temp_c") val tempC: Float = 0f,
    @SerialName("feelslike_c") val feelsLikeC: Float = 0f,
    val humidity: Int = 0,
    @SerialName("wind_kph") val windKph: Float = 0f,
    @SerialName("wind_dir") val windDir: String = "N",
    val uv: Float = 0f,
    @SerialName("vis_km") val visKm: Float = 0f,
    @SerialName("pressure_mb") val pressureMb: Float = 0f,
    val condition: ConditionData = ConditionData()
)

@Serializable
data class ConditionData(
    val text: String = "",
    val icon: String = "",
    val code: Int = 1000
)

@Serializable
data class ForecastData(
    val forecastday: List<ForecastDayItem> = emptyList()
)

@Serializable
data class ForecastDayItem(
    val date: String = "",
    val day: DayData = DayData(),
    val hour: List<HourData> = emptyList()
)

@Serializable
data class DayData(
    @SerialName("maxtemp_c") val maxTempC: Float = 0f,
    @SerialName("mintemp_c") val minTempC: Float = 0f,
    @SerialName("daily_chance_of_rain") val dailyChanceOfRain: Int = 0,
    @SerialName("daily_will_it_rain") val dailyWillItRain: Int = 0,
    val condition: ConditionData = ConditionData()
)

@Serializable
data class HourData(
    val time: String = "",
    @SerialName("temp_c") val tempC: Float = 0f,
    @SerialName("chance_of_rain") val chanceOfRain: Int = 0,
    @SerialName("will_it_rain") val willItRain: Int = 0,
    val condition: ConditionData = ConditionData()
)

@Serializable
data class SearchLocationItem(
    val id: Int = 0,
    val name: String = "",
    val region: String = "",
    val country: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0
)
