package com.example.climora

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.climora.data.local.CityEntity
import com.example.climora.ui.theme.*
import com.example.climora.viewmodel.WeatherUiState
import com.example.climora.viewmodel.WeatherViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClimoraTheme {
                MainScreen()
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Inicio : Screen("inicio", "Inicio", Icons.Default.Home)
    object Buscar : Screen("buscar", "Buscar", Icons.Default.Search)
    object Favoritos : Screen("favoritos", "Favoritos", Icons.Default.FavoriteBorder)
    object Historial : Screen("historial", "Historial", Icons.Default.History)
}

@Composable
fun MainScreen(viewModel: WeatherViewModel = viewModel()) {
    var currentRoute by rememberSaveable { mutableStateOf(Screen.Inicio.route) }
    val items = listOf(Screen.Inicio, Screen.Buscar, Screen.Favoritos, Screen.Historial)

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = SolidBlueBackground,
                contentColor = MutedText,
                tonalElevation = 0.dp
            ) {
                items.forEach { screen ->
                    val selected = currentRoute == screen.route
                    NavigationBarItem(
                        icon = {
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (selected) NavSelected else Color.Transparent,
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .padding(horizontal = 20.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    screen.icon,
                                    contentDescription = null,
                                    tint = if (selected) Color.White else MutedText
                                )
                            }
                        },
                        label = {
                            Text(
                                screen.title,
                                color = if (selected) NavTextActive else MutedText,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        selected = selected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                currentRoute = screen.route
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentRoute) {
                Screen.Inicio.route -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateToSearch = { currentRoute = Screen.Buscar.route }
                )
                Screen.Buscar.route -> SearchScreen(
                    viewModel = viewModel,
                    onNavigateToHome = { currentRoute = Screen.Inicio.route }
                )
                Screen.Favoritos.route -> FavoritesScreen(
                    viewModel = viewModel,
                    onNavigateToHome = { currentRoute = Screen.Inicio.route }
                )
                Screen.Historial.route -> HistoryScreen(
                    viewModel = viewModel,
                    onNavigateToHome = { currentRoute = Screen.Inicio.route }
                )
            }
        }
    }
}

@Composable
fun WeatherConditionImage(
    conditionText: String = "",
    conditionCode: Int = 1000,
    pop: Int = 0,
    iconSize: Dp = 38.dp,
    modifier: Modifier = Modifier
) {
    val cond = conditionText.lowercase()

    val isThunder = conditionCode in listOf(1087, 1273, 1276, 1279, 1282) || cond.contains("tormenta") || cond.contains("thunder")
    val isRainyCondition = conditionCode in listOf(1063, 1180, 1183, 1186, 1189, 1192, 1195, 1240, 1243) || cond.contains("lluvia") || cond.contains("llovizna") || cond.contains("chubasco")
    val isPartlyCloudy = conditionCode == 1003 || cond.contains("parcialmente") || cond.contains("seminublado") || cond.contains("algo nublado")
    val isFullSunny = conditionCode == 1000 || cond.contains("soleado") || cond.contains("despejado") || cond.contains("clear") || (cond.contains("sol") && !cond.contains("parcialmente"))

    val drawableRes = when {
        isThunder -> R.drawable.ic_weather_thunder
        isRainyCondition -> {
            if (pop > 50) R.drawable.ic_rain_cloud_heavy // >50%: 3 gotas diagonales e irregulares
            else R.drawable.ic_rain_cloud_light // <=50%: 1 gota diagonal e irregular en el extremo
        }
        isFullSunny -> R.drawable.ic_weather_sunny_full // SOL RADIANTE COMPLETO CON RAYOS
        isPartlyCloudy -> R.drawable.ic_weather_sunny // SOL DE FONDO CON NUBE AL FRENTE
        else -> R.drawable.ic_weather_cloudy // NUBE GRIS LISA
    }

    Image(
        painter = painterResource(id = drawableRes),
        contentDescription = conditionText,
        modifier = modifier.size(iconSize)
    )
}

fun formatDayLabel(dateStr: String, index: Int): String {
    if (index == 0) return "Hoy"
    val daysOfWeek = listOf("Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb")
    return try {
        val dateParts = dateStr.split("-")
        if (dateParts.size == 3) {
            val year = dateParts[0].toInt()
            val month = dateParts[1].toInt()
            val day = dateParts[2].toInt()
            val calendar = Calendar.getInstance()
            calendar.set(year, month - 1, day)
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
            daysOfWeek[dayOfWeek]
        } else "Día ${index + 1}"
    } catch (e: Exception) {
        "Día ${index + 1}"
    }
}

fun getUvDescription(uv: Float): String {
    return when {
        uv <= 2f -> "Bajo"
        uv <= 5f -> "Moderado"
        uv <= 7f -> "Alto (protección)"
        uv <= 10f -> "Muy Alto"
        else -> "Extremo"
    }
}

@Composable
fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    value: String,
    description: String
) {
    Card(
        modifier = modifier.height(125.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MutedText, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    title,
                    color = MutedText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }

            Text(
                value,
                color = TextWhite,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                description,
                color = TextWhite.copy(alpha = 0.8f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
fun HomeScreen(viewModel: WeatherViewModel, onNavigateToSearch: () -> Unit) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val history by viewModel.cityHistory.collectAsState(initial = emptyList())

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    var showForecastDialog by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.fetchLocationWeather()
        } else {
            Toast.makeText(context, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
        }
    }

    val handleGpsClick = {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                           locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else if (!isGpsEnabled) {
            Toast.makeText(context, "Por favor, activa el GPS de tu celular", Toast.LENGTH_LONG).show()
            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        } else {
            viewModel.fetchLocationWeather()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SolidBlueBackground)
            .padding(horizontal = 20.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Top Navigation Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Climio", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            Row {
                IconButton(onClick = { handleGpsClick() }) {
                    Icon(Icons.Default.MyLocation, contentDescription = "GPS", tint = AccentBlue)
                }
                IconButton(onClick = {
                    val currentName = (uiState as? WeatherUiState.Success)?.weatherData?.location?.name ?: ""
                    val isFav = history.find { it.name.equals(currentName, ignoreCase = true) }?.isFavorite == true
                    if (currentName.isNotBlank()) viewModel.toggleFavorite(currentName, !isFav)
                }) {
                    val currentName = (uiState as? WeatherUiState.Success)?.weatherData?.location?.name ?: ""
                    val isFav = history.find { it.name.equals(currentName, ignoreCase = true) }?.isFavorite == true
                    Icon(
                        if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorito",
                        tint = if (isFav) Color.Red else TextWhite
                    )
                }
                IconButton(onClick = {
                    val currentName = (uiState as? WeatherUiState.Success)?.weatherData?.location?.name ?: "Resistencia"
                    viewModel.searchCity(currentName)
                }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualizar", tint = TextWhite)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (val state = uiState) {
            is WeatherUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AccentBlue)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Consultando clima en tiempo real...", color = MutedText, fontSize = 16.sp)
                    }
                }
            }
            is WeatherUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFEF5350), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(state.message, color = Color(0xFFEF5350), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            is WeatherUiState.Success -> {
                val data = state.weatherData
                val current = data.current
                val todayForecast = data.forecast.forecastday.firstOrNull()?.day

                val maxToday = todayForecast?.maxTempC?.toInt() ?: current.tempC.toInt()
                val minToday = todayForecast?.minTempC?.toInt() ?: current.tempC.toInt()

                val allHours = data.forecast.forecastday.flatMap { it.hour }
                
                val currentHourFormatted = remember {
                    SimpleDateFormat("yyyy-MM-dd HH:00", Locale.getDefault()).format(Date())
                }
                val next24Hours = remember(data) {
                    val filtered = allHours.filter { it.time >= currentHourFormatted }
                    if (filtered.isNotEmpty()) filtered.take(24) else allHours.take(24)
                }

                val upcomingRainHour = next24Hours.find { it.chanceOfRain >= 50 || it.willItRain == 1 }

                if (showForecastDialog) {
                    AlertDialog(
                        onDismissRequest = { showForecastDialog = false },
                        containerColor = SolidBlueBackground,
                        title = {
                            Text(
                                "Detalles - ${data.location.name}",
                                color = TextWhite,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("• Sensación térmica: ${current.feelsLikeC.toInt()}°C", color = TextWhite, fontSize = 15.sp)
                                Text("• Máxima de hoy: ${maxToday}°C", color = TextWhite, fontSize = 15.sp)
                                Text("• Mínima de hoy: ${minToday}°C", color = TextWhite, fontSize = 15.sp)
                                Text("• Humedad relativa: ${current.humidity}%", color = TextWhite, fontSize = 15.sp)
                                Text("• Velocidad del viento: ${current.windKph} km/h", color = TextWhite, fontSize = 15.sp)
                                if (todayForecast != null) {
                                    Text("• Probabilidad de Lluvia Hoy: ${todayForecast.dailyChanceOfRain}%", color = AccentBlue, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                                Text("• Condición: ${current.condition.text}", color = TextWhite, fontSize = 15.sp)
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = { showForecastDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                            ) {
                                Text("Entendido", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }

                // HERO SECTION
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NearMe, contentDescription = null, tint = TextWhite, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(data.location.name, fontSize = 28.sp, color = TextWhite, fontWeight = FontWeight.Normal)
                    }

                    WeatherConditionImage(
                        conditionText = current.condition.text,
                        conditionCode = current.condition.code,
                        pop = todayForecast?.dailyChanceOfRain ?: 0,
                        iconSize = 90.dp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Text(
                        "${current.tempC.toInt()}°",
                        fontSize = 84.sp,
                        fontWeight = FontWeight.ExtraLight,
                        color = TextWhite
                    )

                    Text(
                        current.condition.text,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextWhite.copy(alpha = 0.9f)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        "Máx: ${maxToday}°   Mín: ${minToday}°",
                        fontSize = 15.sp,
                        color = TextWhite.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                }

                // AVISO DE PRECIPITACIÓN / LLUVIA PRÓXIMA SI CORRESPONDE
                if (upcomingRainHour != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A5F)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_rain_cloud_light),
                                contentDescription = null,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Aviso de Lluvia en Tiempo Real",
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                val timeLabel = upcomingRainHour.time.substringAfter(" ")
                                Text(
                                    "Se prevé ${upcomingRainHour.condition.text.lowercase()} con ${upcomingRainHour.chanceOfRain}% de probabilidad a las $timeLabel hs.",
                                    color = MutedText,
                                    fontSize = 13.sp,
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // PRONÓSTICO HORA POR HORA (24 HORAS CONSECUTIVAS)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Pronóstico hora por hora (próximas 24h). Viento de ${current.windKph.toInt()} km/h y humedad del ${current.humidity}%.",
                            color = TextWhite.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = Color.White.copy(alpha = 0.15f)
                        )

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            items(next24Hours) { hourItem ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    val timeLabel = hourItem.time.substringAfter(" ")
                                    Text(timeLabel, color = TextWhite.copy(alpha = 0.8f), fontSize = 13.sp, fontWeight = FontWeight.Medium)

                                    WeatherConditionImage(
                                        conditionText = hourItem.condition.text,
                                        conditionCode = hourItem.condition.code,
                                        pop = hourItem.chanceOfRain,
                                        iconSize = 38.dp
                                    )

                                    if (hourItem.chanceOfRain > 0) {
                                        Text("${hourItem.chanceOfRain}%", color = Color(0xFF50C4CC), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    } else {
                                        Spacer(modifier = Modifier.height(14.dp))
                                    }

                                    Text("${hourItem.tempC.toInt()}°", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }
                        }
                    }
                }

                // TARJETAS DE MÉTRICAS METEOROLÓGICAS (ÍNDICE UV, VIENTO, VISIBILIDAD, PRESIÓN)
                Spacer(modifier = Modifier.height(16.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            modifier = Modifier.weight(1f),
                            title = "ÍNDICE UV",
                            icon = Icons.Default.WbSunny,
                            value = "${current.uv.toInt()}",
                            description = getUvDescription(current.uv)
                        )

                        MetricCard(
                            modifier = Modifier.weight(1f),
                            title = "VIENTO",
                            icon = Icons.Default.Air,
                            value = "${current.windKph.toInt()} km/h",
                            description = "Dirección: ${current.windDir}"
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            modifier = Modifier.weight(1f),
                            title = "VISIBILIDAD",
                            icon = Icons.Default.Visibility,
                            value = "${current.visKm.toInt()} km",
                            description = if (current.visKm >= 8f) "Excelente claridad" else "Visibilidad reducida"
                        )

                        MetricCard(
                            modifier = Modifier.weight(1f),
                            title = "PRESIÓN",
                            icon = Icons.Default.Compress,
                            value = "${current.pressureMb.toInt()} hPa",
                            description = if (current.pressureMb >= 1013f) "Presión normal" else "Baja presión"
                        )
                    }
                }

                // PRONÓSTICO DE 5 DÍAS (LISTA COMPLETA VERTICAL RESTRUCTURADA Y CENTRADA)
                if (data.forecast.forecastday.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DateRange, contentDescription = null, tint = MutedText, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "PRONÓSTICO DE 5 DÍAS",
                                    color = MutedText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = Color.White.copy(alpha = 0.15f)
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                data.forecast.forecastday.take(5).forEachIndexed { idx, dayItem ->
                                    val dayName = formatDayLabel(dayItem.date, idx)
                                    val dayChance = dayItem.day.dailyChanceOfRain

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            dayName,
                                            color = TextWhite,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.width(48.dp)
                                        )

                                        // SLOT FIJO 1: ICONO DEL CLIMA DINÁMICO
                                        Box(
                                            modifier = Modifier.width(36.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            WeatherConditionImage(
                                                conditionText = dayItem.day.condition.text,
                                                conditionCode = dayItem.day.condition.code,
                                                pop = dayChance,
                                                iconSize = 32.dp
                                            )
                                        }

                                        // SLOT FIJO 2: PORCENTAJE DE LLUVIA REAL
                                        Box(
                                            modifier = Modifier.width(44.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            if (dayChance > 0) {
                                                Text(
                                                    "${dayChance}%",
                                                    color = Color(0xFF50C4CC),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        // SLOT FIJO 3: RANGO DE TEMPERATURA
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            Text(
                                                "${dayItem.day.minTempC.toInt()}°",
                                                color = MutedText,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.width(28.dp),
                                                textAlign = TextAlign.End
                                            )

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Box(
                                                modifier = Modifier
                                                    .width(70.dp)
                                                    .height(5.dp)
                                                    .background(
                                                        Brush.horizontalGradient(
                                                            listOf(AccentBlue, Color(0xFFFFB74D))
                                                        ),
                                                        shape = RoundedCornerShape(3.dp)
                                                    )
                                            )

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Text(
                                                "${dayItem.day.maxTempC.toInt()}°",
                                                color = TextWhite,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.width(28.dp),
                                                textAlign = TextAlign.End
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Busca una ciudad o presiona el GPS", color = MutedText, fontSize = 16.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        ActionButton(
            icon = Icons.Default.WbSunny,
            text = "Ver Pronóstico Detallado",
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                coroutineScope.launch {
                    scrollState.animateScrollTo(scrollState.maxValue)
                }
                showForecastDialog = true
            }
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun ActionButton(icon: ImageVector, text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
        shape = RoundedCornerShape(28.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color.Black)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, color = Color.Black, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SearchScreen(viewModel: WeatherViewModel, onNavigateToHome: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val suggestions by viewModel.searchResults.collectAsState()

    val popularCities = listOf(
        "Resistencia", "Buenos Aires", "Madrid", "Ciudad de México", "Bogotá",
        "Santiago", "Lima", "Miami", "Barcelona", "Montevideo", "Caracas", "Roma", "París"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SolidBlueBackground)
            .padding(20.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text("Buscar Ciudad", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextWhite)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                viewModel.searchCitySuggestions(it)
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Escribe una ciudad del mundo...", color = MutedText) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MutedText) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = {
                        query = ""
                        viewModel.searchCitySuggestions("")
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = MutedText)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentBlue,
                unfocusedBorderColor = MutedText,
                cursorColor = AccentBlue,
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (query.isNotBlank() && suggestions.isNotEmpty()) {
            Text("Sugerencias globales en tiempo real", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextWhite)
            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(suggestions) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.searchCity("${item.lat},${item.lon}")
                                onNavigateToHome()
                            },
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(item.name, color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text("${item.region}, ${item.country}", color = MutedText, fontSize = 13.sp)
                            }
                            Icon(Icons.Default.NorthWest, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        } else {
            Text("Ciudades sugeridas", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextWhite)
            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(popularCities) { city ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.searchCity(city)
                                onNavigateToHome()
                            },
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 14.dp, vertical = 14.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(city, color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Icon(Icons.Default.NorthWest, contentDescription = null, tint = MutedText, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FavoritesScreen(viewModel: WeatherViewModel, onNavigateToHome: () -> Unit) {
    val favorites by viewModel.favoriteCities.collectAsState(initial = emptyList())
    CityListScreen("Mis Favoritos", favorites, Icons.Default.FavoriteBorder, "No tienes ciudades favoritas.", viewModel, onNavigateToHome = onNavigateToHome)
}

@Composable
fun HistoryScreen(viewModel: WeatherViewModel, onNavigateToHome: () -> Unit) {
    val history by viewModel.cityHistory.collectAsState(initial = emptyList())
    CityListScreen("Historial", history, Icons.Default.History, "Tu historial de búsqueda está vacío", viewModel, onNavigateToHome = onNavigateToHome)
}

@Composable
fun CityListScreen(
    title: String,
    cities: List<CityEntity>,
    icon: ImageVector,
    emptyMsg: String,
    viewModel: WeatherViewModel,
    onNavigateToHome: () -> Unit
) {
    val cacheList by viewModel.weatherCacheFlow.collectAsState(initial = emptyList())
    val cacheMap = remember(cacheList) {
        cacheList.associateBy { it.cityName.lowercase() }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .background(SolidBlueBackground)
        .padding(20.dp)) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextWhite)
        Spacer(modifier = Modifier.height(16.dp))
        if (cities.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(icon, contentDescription = null, tint = MutedText, modifier = Modifier.size(80.dp))
                Spacer(modifier = Modifier.height(24.dp))
                Text(emptyMsg, color = MutedText, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(cities) { city ->
                    val cachedWeather = cacheMap[city.name.lowercase()]

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.searchCity(city.name)
                                onNavigateToHome()
                            },
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(city.name, color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                                if (cachedWeather != null) {
                                    Text(
                                        cachedWeather.condition.replaceFirstChar { it.uppercase() },
                                        color = MutedText,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (cachedWeather != null) {
                                    Text(
                                        "${cachedWeather.temp}°",
                                        color = TextWhite,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))

                                    WeatherConditionImage(
                                        conditionText = cachedWeather.condition,
                                        iconSize = 36.dp
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                IconButton(onClick = { viewModel.toggleFavorite(city.name, !city.isFavorite) }) {
                                    Icon(
                                        if (city.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = null,
                                        tint = if (city.isFavorite) Color.Red else MutedText
                                    )
                                }
                                IconButton(onClick = { viewModel.deleteCity(city.name) }) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = MutedText)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
