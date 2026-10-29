package com.example.climora.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey



@Entity(tableName = "city_history")
data class CityEntity(
    @PrimaryKey val name: String,
    val isFavorite: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "weather_cache")
data class WeatherCacheEntity(
    @PrimaryKey val cityName: String,
    val temp: Int,
    val condition: String,
    val icon: String,
    val humidity: Int,
    val windSpeed: Float,
    val lastUpdated: Long = System.currentTimeMillis()
)
