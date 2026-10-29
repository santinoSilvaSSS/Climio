package com.example.climora.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CityDao {
    @Query("SELECT * FROM city_history ORDER BY timestamp DESC")
    fun getAllCities(): Flow<List<CityEntity>>

    @Query("SELECT * FROM city_history WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteCities(): Flow<List<CityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCity(city: CityEntity)

    @Query("UPDATE city_history SET isFavorite = :isFavorite WHERE name = :cityName")
    suspend fun updateFavorite(cityName: String, isFavorite: Boolean)

    @Delete
    suspend fun deleteCity(city: CityEntity)

    // Weather Cache
    @Query("SELECT * FROM weather_cache WHERE LOWER(cityName) = LOWER(:cityName) LIMIT 1")
    suspend fun getWeatherCache(cityName: String): WeatherCacheEntity?

    @Query("SELECT * FROM weather_cache")
    fun getAllWeatherCache(): Flow<List<WeatherCacheEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeatherCache(cache: WeatherCacheEntity)
}
