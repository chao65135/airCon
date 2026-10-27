package com.aircon.app.data.remote.qweather

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 和风天气通用元数据。 */
@Serializable
data class QWeatherMetadata(
    val tag: String = "",
    val attributions: List<String> = emptyList(),
)

/** 数值 + 单位。 */
@Serializable
data class QWeatherValue(
    val value: Double = 0.0,
    val unit: String = "",
)

/** 天气现象。 */
@Serializable
data class QWeatherCondition(
    val text: String = "",
    val code: String = "",
)

/** 风向。 */
@Serializable
data class QWeatherWindDirection(
    val degree: Int = 0,
    val compass: String = "",
)

/** 风。 */
@Serializable
data class QWeatherWind(
    val direction: QWeatherWindDirection = QWeatherWindDirection(),
    val speed: QWeatherValue = QWeatherValue(),
    val scale: Int = 0,
)

/** 降水。 */
@Serializable
data class QWeatherPrecipitation(
    val amount: QWeatherValue = QWeatherValue(),
    val intensity: QWeatherValue = QWeatherValue(),
    val probability: Double = 0.0,
    val type: String = "none",
)

/** 实时天气。 */
@Serializable
data class QWeatherCurrent(
    val time: String = "",
    val condition: QWeatherCondition = QWeatherCondition(),
    val temperature: QWeatherValue = QWeatherValue(),
    val feelsLike: QWeatherValue = QWeatherValue(),
    val humidity: Double = 0.0,
    val wind: QWeatherWind = QWeatherWind(),
    val precipitation: QWeatherPrecipitation = QWeatherPrecipitation(),
    val pressure: QWeatherValue = QWeatherValue(),
    val visibility: QWeatherValue = QWeatherValue(),
    val cloudCover: Double = 0.0,
)

@Serializable
data class QWeatherCurrentResponse(
    val metadata: QWeatherMetadata = QWeatherMetadata(),
    val now: QWeatherCurrent = QWeatherCurrent(),
)

/** 逐小时天气条目。 */
@Serializable
data class QWeatherHour(
    val forecastTime: String = "",
    val condition: QWeatherCondition = QWeatherCondition(),
    val temperature: QWeatherValue = QWeatherValue(),
    val feelsLike: QWeatherValue = QWeatherValue(),
    val humidity: Double = 0.0,
    val wind: QWeatherWind = QWeatherWind(),
    val precipitation: QWeatherPrecipitation = QWeatherPrecipitation(),
    val pressure: QWeatherValue = QWeatherValue(),
    val visibility: QWeatherValue = QWeatherValue(),
    val cloudCover: Double = 0.0,
    val uvIndex: Double = 0.0,
)

@Serializable
data class QWeatherHourlyResponse(
    val metadata: QWeatherMetadata = QWeatherMetadata(),
    val hours: List<QWeatherHour> = emptyList(),
)

/** 每日天气条目。 */
@Serializable
data class QWeatherDay(
    val fxDate: String = "",
    val sunrise: String = "",
    val sunset: String = "",
    val condition: QWeatherCondition = QWeatherCondition(),
    val tempMax: QWeatherValue = QWeatherValue(),
    val tempMin: QWeatherValue = QWeatherValue(),
    val humidity: Double = 0.0,
    val wind: QWeatherWind = QWeatherWind(),
    @SerialName("precip") val precipitation: QWeatherValue = QWeatherValue(),
    val pressure: QWeatherValue = QWeatherValue(),
    val visibility: QWeatherValue = QWeatherValue(),
    val uvIndex: Double = 0.0,
)

@Serializable
data class QWeatherDailyResponse(
    val metadata: QWeatherMetadata = QWeatherMetadata(),
    val daily: List<QWeatherDay> = emptyList(),
)

/** 首要污染物。 */
@Serializable
data class QWeatherPrimaryPollutant(
    val code: String = "",
    val name: String = "",
    val fullName: String = "",
)

/** AQI 颜色。 */
@Serializable
data class QWeatherColor(
    val red: Int = 0,
    val green: Int = 0,
    val blue: Int = 0,
    val alpha: Double = 1.0,
)

/** 健康建议。 */
@Serializable
data class QWeatherHealthAdvice(
    val generalPopulation: String = "",
    val sensitivePopulation: String = "",
)

@Serializable
data class QWeatherHealth(
    val effect: String? = null,
    val advice: QWeatherHealthAdvice = QWeatherHealthAdvice(),
)

/** AQI 指数条目。 */
@Serializable
data class QWeatherIndex(
    val code: String = "",
    val name: String = "",
    val aqi: Double = 0.0,
    val aqiDisplay: String = "",
    val level: String = "",
    val category: String = "",
    val color: QWeatherColor = QWeatherColor(),
    val primaryPollutant: QWeatherPrimaryPollutant = QWeatherPrimaryPollutant(),
    val health: QWeatherHealth? = null,
)

/** 污染物浓度。 */
@Serializable
data class QWeatherPollutant(
    val code: String = "",
    val name: String = "",
    val concentration: QWeatherValue = QWeatherValue(),
)

/** 空气质量元数据（refer）。 */
@Serializable
data class QWeatherRefer(
    val sources: List<String> = emptyList(),
)

@Serializable
data class QWeatherAirQualityResponse(
    val metadata: QWeatherMetadata = QWeatherMetadata(),
    val indexes: List<QWeatherIndex> = emptyList(),
    val pollutants: List<QWeatherPollutant> = emptyList(),
    val refer: QWeatherRefer = QWeatherRefer(),
)

@Serializable
data class QWeatherAirHour(
    val forecastTime: String = "",
    val indexes: List<QWeatherIndex> = emptyList(),
    val pollutants: List<QWeatherPollutant> = emptyList(),
)

@Serializable
data class QWeatherAirHourlyResponse(
    val metadata: QWeatherMetadata = QWeatherMetadata(),
    val hours: List<QWeatherAirHour> = emptyList(),
)
