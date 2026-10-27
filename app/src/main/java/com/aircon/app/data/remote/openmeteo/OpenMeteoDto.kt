package com.aircon.app.data.remote.openmeteo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Open-Meteo 天气预报接口响应。 */
@Serializable
data class OpenMeteoForecastResponse(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timezone: String = "",
    @SerialName("utc_offset_seconds") val utcOffsetSeconds: Int = 0,
    val current: OpenMeteoCurrent = OpenMeteoCurrent(),
    val hourly: OpenMeteoHourly = OpenMeteoHourly(),
    val daily: OpenMeteoDaily = OpenMeteoDaily(),
)

@Serializable
data class OpenMeteoCurrent(
    val time: String = "",
    @SerialName("temperature_2m") val temperature2m: Double = 0.0,
    @SerialName("relative_humidity_2m") val relativeHumidity2m: Double = 0.0,
    @SerialName("apparent_temperature") val apparentTemperature: Double = 0.0,
    @SerialName("is_day") val isDay: Int = 1,
    val precipitation: Double = 0.0,
    @SerialName("weather_code") val weatherCode: Int = 0,
    @SerialName("wind_speed_10m") val windSpeed10m: Double = 0.0,
    @SerialName("wind_direction_10m") val windDirection10m: Double = 0.0,
    @SerialName("surface_pressure") val surfacePressure: Double = 0.0,
    @SerialName("cloud_cover") val cloudCover: Double = 0.0,
)

@Serializable
data class OpenMeteoHourly(
    val time: List<String> = emptyList(),
    @SerialName("temperature_2m") val temperature2m: List<Double?> = emptyList(),
    @SerialName("apparent_temperature") val apparentTemperature: List<Double?> = emptyList(),
    @SerialName("relative_humidity_2m") val relativeHumidity2m: List<Double?> = emptyList(),
    @SerialName("precipitation_probability") val precipitationProbability: List<Double?> = emptyList(),
    val precipitation: List<Double?> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int?> = emptyList(),
    @SerialName("is_day") val isDay: List<Int?> = emptyList(),
    @SerialName("wind_speed_10m") val windSpeed10m: List<Double?> = emptyList(),
    @SerialName("visibility") val visibility: List<Double?> = emptyList(),
    @SerialName("uv_index") val uvIndex: List<Double?> = emptyList(),
)

@Serializable
data class OpenMeteoDaily(
    val time: List<String> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int?> = emptyList(),
    @SerialName("temperature_2m_max") val temperature2mMax: List<Double?> = emptyList(),
    @SerialName("temperature_2m_min") val temperature2mMin: List<Double?> = emptyList(),
    @SerialName("precipitation_probability_max") val precipitationProbabilityMax: List<Double?> = emptyList(),
    val sunrise: List<String?> = emptyList(),
    val sunset: List<String?> = emptyList(),
    @SerialName("uv_index_max") val uvIndexMax: List<Double?> = emptyList(),
)

/** Open-Meteo 空气质量接口响应。 */
@Serializable
data class OpenMeteoAirQualityResponse(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timezone: String = "",
    @SerialName("utc_offset_seconds") val utcOffsetSeconds: Int = 0,
    val current: OpenMeteoAirCurrent = OpenMeteoAirCurrent(),
    val hourly: OpenMeteoAirHourly = OpenMeteoAirHourly(),
)

@Serializable
data class OpenMeteoAirCurrent(
    val time: String = "",
    @SerialName("pm10") val pm10: Double? = null,
    @SerialName("pm2_5") val pm25: Double? = null,
    @SerialName("carbon_monoxide") val carbonMonoxide: Double? = null,
    @SerialName("nitrogen_dioxide") val nitrogenDioxide: Double? = null,
    @SerialName("sulphur_dioxide") val sulphurDioxide: Double? = null,
    val ozone: Double? = null,
    @SerialName("us_aqi") val usAqi: Double? = null,
    @SerialName("european_aqi") val europeanAqi: Double? = null,
)

/** Open-Meteo 地理编码接口响应（用于城市搜索）。 */
@Serializable
data class OpenMeteoGeocodingResponse(
    val results: List<OpenMeteoGeocodingResult> = emptyList(),
)

@Serializable
data class OpenMeteoGeocodingResult(
    val id: Long = 0L,
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val country: String = "",
    val admin1: String = "",
    val admin2: String = "",
    val timezone: String = "",
    val population: Long = 0L,
    val country_code: String = "",
    val feature_code: String = "",
)

@Serializable
data class OpenMeteoAirHourly(
    val time: List<String> = emptyList(),
    @SerialName("pm10") val pm10: List<Double?> = emptyList(),
    @SerialName("pm2_5") val pm25: List<Double?> = emptyList(),
    @SerialName("carbon_monoxide") val carbonMonoxide: List<Double?> = emptyList(),
    @SerialName("nitrogen_dioxide") val nitrogenDioxide: List<Double?> = emptyList(),
    @SerialName("sulphur_dioxide") val sulphurDioxide: List<Double?> = emptyList(),
    val ozone: List<Double?> = emptyList(),
    @SerialName("us_aqi") val usAqi: List<Double?> = emptyList(),
    @SerialName("european_aqi") val europeanAqi: List<Double?> = emptyList(),
)
