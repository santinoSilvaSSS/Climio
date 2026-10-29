package com.example.climora.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.climora.data.local.AppDatabase
import com.example.climora.data.local.CityEntity
import com.example.climora.data.local.WeatherCacheEntity
import com.example.climora.data.model.SearchLocationItem
import com.example.climora.data.model.WeatherApiData
import com.example.climora.data.remote.WeatherApi
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit

sealed interface WeatherUiState {
    object Idle : WeatherUiState
    object Loading : WeatherUiState
    data class Success(val weatherData: WeatherApiData) : WeatherUiState
    data class Error(val message: String) : WeatherUiState
}

class WeatherViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val cityDao = db.cityDao()
    private val locationClient = LocationServices.getFusedLocationProviderClient(application)

    // WeatherAPI.com Key Activa
    private val apiKey = "9f5e5b5a7be34381955124851260810"

    private val json = Json { ignoreUnknownKeys = true }
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.weatherapi.com/v1/")
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .client(OkHttpClient())
        .build()

    private val api = retrofit.create(WeatherApi::class.java)

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Idle)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private val _weatherDataState = MutableStateFlow<WeatherApiData?>(null)
    val weatherDataState: StateFlow<WeatherApiData?> = _weatherDataState.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchLocationItem>>(emptyList())
    val searchResults: StateFlow<List<SearchLocationItem>> = _searchResults.asStateFlow()

    private var searchJob: Job? = null

    val cityHistory = cityDao.getAllCities()
    val favoriteCities = cityDao.getFavoriteCities()
    val weatherCacheFlow = cityDao.getAllWeatherCache()

    init {
        viewModelScope.launch {
            val history = cityDao.getAllCities().firstOrNull()
            val lastCity = history?.firstOrNull()?.name ?: "Resistencia"
            searchCity(lastCity)
        }
    }

    fun searchCitySuggestions(query: String) {
        searchJob?.cancel()
        if (query.isBlank() || query.length < 2) {
            _searchResults.value = emptyList()
            return
        }

        searchJob = viewModelScope.launch {
            delay(300) // Debounce para no saturar la red
            try {
                val results = api.searchLocations(apiKey, query)
                _searchResults.value = results
            } catch (e: Exception) {
                _searchResults.value = emptyList()
            }
        }
    }

    fun searchCity(cityName: String) {
        if (cityName.isBlank()) return

        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading
            try {
                val data = api.getWeatherForecast(apiKey, cityName, days = 5)

                _weatherDataState.value = data
                _uiState.value = WeatherUiState.Success(data)

                val cityNameResult = data.location.name.ifBlank { cityName }
                cityDao.insertCity(CityEntity(name = cityNameResult))
                cityDao.insertWeatherCache(
                    WeatherCacheEntity(
                        cityName = cityNameResult,
                        temp = data.current.tempC.toInt(),
                        condition = data.current.condition.text,
                        icon = data.current.condition.icon,
                        humidity = data.current.humidity,
                        windSpeed = data.current.windKph
                    )
                )
            } catch (e: HttpException) {
                val cached = cityDao.getWeatherCache(cityName)
                if (cached != null) {
                    val msg = "Modo Offline: Datos guardados de ${cached.cityName}"
                    _uiState.value = WeatherUiState.Error(msg)
                } else {
                    val msg = when (e.code()) {
                        400, 404 -> "Ciudad no encontrada."
                        401, 403 -> "API Key no válida en WeatherAPI."
                        else -> "Error en servidor (${e.code()})"
                    }
                    _uiState.value = WeatherUiState.Error(msg)
                }
            } catch (e: Exception) {
                _uiState.value = WeatherUiState.Error("Error de red. Revisa tu conexión a internet.")
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun fetchLocationWeather() {
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading
            locationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        viewModelScope.launch {
                            try {
                                val query = "${location.latitude},${location.longitude}"
                                val data = api.getWeatherForecast(apiKey, query, days = 5)

                                _weatherDataState.value = data
                                _uiState.value = WeatherUiState.Success(data)

                                val cityNameResult = data.location.name.ifBlank { "Mi Ubicación" }
                                cityDao.insertCity(CityEntity(name = cityNameResult))
                                cityDao.insertWeatherCache(
                                    WeatherCacheEntity(
                                        cityName = cityNameResult,
                                        temp = data.current.tempC.toInt(),
                                        condition = data.current.condition.text,
                                        icon = data.current.condition.icon,
                                        humidity = data.current.humidity,
                                        windSpeed = data.current.windKph
                                    )
                                )
                            } catch (e: Exception) {
                                _uiState.value = WeatherUiState.Error("Error al consultar clima por GPS.")
                            }
                        }
                    } else {
                        locationClient.lastLocation.addOnSuccessListener { lastLoc ->
                            if (lastLoc != null) {
                                viewModelScope.launch {
                                    try {
                                        val query = "${lastLoc.latitude},${lastLoc.longitude}"
                                        val data = api.getWeatherForecast(apiKey, query, days = 5)

                                        _weatherDataState.value = data
                                        _uiState.value = WeatherUiState.Success(data)

                                        val cityNameResult = data.location.name.ifBlank { "Mi Ubicación" }
                                        cityDao.insertCity(CityEntity(name = cityNameResult))
                                    } catch (e: Exception) {
                                        _uiState.value = WeatherUiState.Error("Error al consultar clima por GPS.")
                                    }
                                }
                            } else {
                                _uiState.value = WeatherUiState.Error("Por favor, activa el GPS e inténtalo de nuevo.")
                            }
                        }
                    }
                }.addOnFailureListener {
                    _uiState.value = WeatherUiState.Error("Por favor, activa el GPS e inténtalo de nuevo.")
                }
        }
    }

    fun toggleFavorite(cityName: String, isFavorite: Boolean) {
        viewModelScope.launch {
            cityDao.updateFavorite(cityName, isFavorite)
        }
    }

    fun deleteCity(cityName: String) {
        viewModelScope.launch {
            cityDao.deleteCity(CityEntity(name = cityName))
        }
    }
}