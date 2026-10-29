# Climora (Climio) - Aplicación Meteorológica Nativa para Android

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=android&logoColor=white)
![Room persistence](https://img.shields.io/badge/Room%20DB-003B57?style=for-the-badge&logo=sqlite&logoColor=white)

Climora es una aplicación móvil nativa para la plataforma Android desarrollada con el lenguaje Kotlin y Jetpack Compose. La aplicación ofrece información meteorológica detallada en tiempo real mediante una interfaz basada en el sistema de diseño de iOS Weather, incorporando soporte offline completo, geolocalización GPS y fondos dinámicos adaptativos.

---

## 1. Características Principales

* **Información Meteorológica en Tiempo Real**: Consulta de temperatura actual, sensación térmica, condición climática, velocidad del viento y nivel de humedad utilizando servicios REST.
* **Pronóstico Extendido por Horas**: Visualización interactiva en carrusel horizontal con las condiciones meteorológicas estimadas para las próximas 24 horas.
* **Pronóstico de 5 Días**: Resumen proyectado con temperaturas mínimas, máximas y barras de rango térmico proporcional por día.
* **Geolocalización GPS**: Detección de la ubicación del dispositivo mediante el proveedor FusedLocationProviderClient de Google Play Services.
* **Fondos Dinámicos y Adaptativos**: Sistema de interfaz reactivo que altera los colores de fondo y gradientes en función de la condición climática actual y el horario (día/noche).
* **Persistencia y Caché Offline Real**: Implementación de Room Persistence Library con dos tablas relacionales para garantizar la visualización de datos sin conexión a red.
* **Gestión de Favoritos e Historial**: Registro persistente de búsquedas recientes y selección de ciudades favoritas con sincronización de estado en tiempo real.
* **Búsqueda Filtrada de Ciudades**: Interfaz de búsqueda con listado de sugerencias populares y filtrado reactivo a medida que el usuario escribe.
* **Sistema de Diseño y Tipografía**: Implementación de Material Design 3 con la tipografía Lexend e icono adaptativo vectorial corporativo.

---

## 2. Arquitectura y Patrones de Diseño

El desarrollo del proyecto sigue la arquitectura recomendada por la guía oficial de Android y los principios de Clean Architecture:

```
                      +-------------------------+
                      |   UI (Jetpack Compose)  |
                      +-------------------------+
                                   |
                                   v
                      +-------------------------+
                      |    WeatherViewModel     |
                      |   (StateFlow / UiState) |
                      +-------------------------+
                                   |
                     +-------------+-------------+
                     |                           |
                     v                           v
        +--------------------------+   +-------------------+
        | Room DB (Local Cache)    |   | Retrofit API      |
        | - CityEntity             |   | - OpenWeatherMap  |
        | - WeatherCacheEntity     |   +-------------------+
        +--------------------------+
```

### Patrones de diseño implementados:
* **MVVM (Model - View - ViewModel)**: Desacoplamiento estricto entre la capa de presentación y la capa de lógica de negocio.
* **Unidirectional Data Flow (UDF)**: Flujo de datos unidireccional y reactivo gestionado mediante StateFlow y Kotlin Flow.
* **Concurrencia Asíncrona**: Utilización de Corrutinas de Kotlin para operaciones de red y lectura/escritura de base de datos sin bloquear el hilo principal (UI Thread).
* **Procesamiento de Anotaciones**: Kotlin Symbol Processing (KSP) para la generación optimizada del código fuente de Room.

---

## 3. Especificaciones Técnicas y Dependencias

| Componente | Tecnología / Librería | Versión |
| :--- | :--- | :--- |
| Lenguaje | Kotlin | 2.0.21 |
| Framework de UI | Jetpack Compose (BOM) | 2024.12.01 |
| Guía de Estilos | Material Design 3 | Latest |
| Navegación | Navigation Compose | 2.8.5 |
| Base de Datos | Room Persistence Library | 2.6.1 |
| Cliente HTTP | Retrofit 2 + OkHttp 3 | 2.11.0 / 4.12.0 |
| Serialización | Kotlinx Serialization Json | 1.7.3 |
| Carga de Imágenes | Coil Compose | 2.7.0 |
| Ubicación | Google Play Services Location | 21.3.0 |
| Tipografía | Compose Text Google Fonts (Lexend) | Latest |

---

## 4. Estructura del Código Fuente

```
app/src/main/java/com/example/climora/
├── data/
│   ├── local/                  # Capa de almacenamiento local
│   │   ├── AppDatabase.kt      # Configuración de base de datos Room (Versión 2)
│   │   ├── CityDao.kt          # Objeto de acceso a datos (DAO SQL)
│   │   └── Entities.kt         # Definición de tablas CityEntity y WeatherCacheEntity
│   ├── model/                  # Modelos de datos y DTOs
│   │   └── WeatherModels.kt    # Estructuras de respuesta JSON
│   └── remote/                 # Capa de comunicaciones de red
│       └── WeatherApi.kt       # Declaración de endpoints REST
├── ui/
│   └── theme/                  # Sistema de diseño de interfaz
│       ├── Color.kt            # Paleta de colores en modo oscuro
│       ├── Theme.kt            # Configuración del tema Material 3
│       └── Type.kt             # Integración tipográfica Lexend
├── viewmodel/
│   └── WeatherViewModel.kt     # Gestión de estado (WeatherUiState) y lógica de negocio
└── MainActivity.kt             # Punto de entrada, NavHost y pantallas del sistema
```

---

## 5. Esquema de Base de Datos (Room)

La base de datos SQLite denominada `climio_database` cuenta con dos entidades relacionales:

1. **`city_history`**: Mantiene el registro de ciudades consultadas, marca de tiempo (`timestamp`) y el estado booleano de favorito (`isFavorite`).
2. **`weather_cache`**: Almacena el último estado de temperatura, descripción climática, código de icono, nivel de humedad y velocidad del viento por ciudad para habilitar la lectura en modo offline.

---

## 6. Instrucciones de Instalación y Despliegue

### Requisitos del Entorno:
* Android Studio Ladybug (2024.2.1) o superior.
* Java Development Kit (JDK) 11 o superior.
* Dispositivo físico o virtual (AVD) con Android 7.0 (API Nivel 24) o superior.

### Pasos de Configuración:
1. Clonar el repositorio del proyecto:
   ```bash
   git clone https://github.com/usuario/climora.git
   ```
2. Abrir el proyecto en Android Studio y ejecutar la sincronización de Gradle (*Sync Project with Gradle Files*).
3. Verificar la asignación de la clave de API de OpenWeatherMap en la variable `apiKey` dentro de `WeatherViewModel.kt`:
   ```kotlin
   private val apiKey = "TU_CLAVE_API"
   ```
4. Seleccionar el módulo `app` y ejecutar en el dispositivo de destino mediante **Run (Shift + F10)**.

---

## 7. Módulos y Pantallas

* **Inicio**: Vista principal con la temperatura actual, descripción meteorológica, velocidad del viento, humedad, pronóstico extendido por horas y pronóstico a 5 días con indicadores de rango térmico.
* **Buscar**: Módulo de búsqueda de ciudades con caja de texto reactiva, filtrado automático y listado de ciudades populares sugeridas.
* **Mis Favoritos**: Listado persistente de las ciudades marcadas como favoritas, incluyendo la temperatura e icono guardado en caché.
* **Historial**: Registro ordenado cronológicamente de las búsquedas recientes con opciones para eliminar registros o alternar el estado de favorito.

---

## 8. Licencia y Créditos

Este proyecto fue desarrollado como Proyecto Final de Desarrollo de Aplicaciones Móviles. Todos los derechos reservados.
