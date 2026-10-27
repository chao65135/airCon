package com.aircon.app.domain.model

import kotlinx.serialization.Serializable

/** 统一的城市坐标点（WGS84）。 */
@Serializable
data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
)

/** 城市。 */
@Serializable
data class City(
    val id: String,
    val name: String,
    val admin: String = "",
    val country: String = "",
    val latitude: Double,
    val longitude: Double,
    /** IANA 时区标识，例如 "Asia/Shanghai"，用于按城市本地时区展示时间。 */
    val timezone: String = "",
    /** 人口数（用于城市搜索结果排序），未知时为 0。 */
    val population: Long = 0L,
) {
    /** 用于界面展示的完整名称，例如 “北京市 · 中国”。 */
    val displayName: String
        get() = buildString {
            append(name)
            if (admin.isNotBlank() && admin != name) append(" · ").append(admin)
            if (country.isNotBlank()) append(" · ").append(country)
        }
}

/** AQI 采用的标准。 */
@Serializable
enum class AqiStandard {
    /** 中国 HJ 633-2026（由浓度反算）。 */
    CN,

    /** 美国 EPA AQI。 */
    US,

    /** 欧洲 AQI。 */
    EU,
}

/** 污染物的实时浓度读数。 */
@Serializable
data class PollutantReading(
    val code: String,
    val name: String,
    val value: Double,
    val unit: String,
)

/** 一次 AQI 评价结果。 */
@Serializable
data class AqiIndex(
    val value: Int,
    /** 1~6 级。 */
    val level: Int,
    /** 等级名称，例如 “良”。 */
    val category: String,
    val standard: AqiStandard,
    val primaryPollutantCode: String? = null,
    val primaryPollutantName: String? = null,
    val healthAdvice: String? = null,
    /** 敏感人群建议。 */
    val sensitivePopulationAdvice: String? = null,
)

/** 逐小时 AQI 序列。 */
@Serializable
data class HourlyAqi(
    val epochMillis: Long,
    val value: Int,
)

/** 空气质量快照。 */
@Serializable
data class AirQuality(
    val index: AqiIndex,
    val pollutants: List<PollutantReading>,
    val hourly: List<HourlyAqi> = emptyList(),
    val updatedAtMillis: Long = 0L,
    /** 数据来源给出的原始说明（例如和风的 refer.sources）。 */
    val sourceNote: String? = null,
)

/** 实况天气。 */
@Serializable
data class CurrentWeather(
    val epochMillis: Long,
    val temperatureC: Double,
    val feelsLikeC: Double,
    /** 0~1。 */
    val humidity: Double,
    val precipitationMm: Double,
    val weatherCode: Int,
    val windSpeedKmh: Double,
    val windDirectionDeg: Int,
    val isDay: Boolean,
    val pressureHpa: Double? = null,
    val visibilityMeters: Double? = null,
    val uvIndex: Double? = null,
)

/** 逐小时预报。 */
@Serializable
data class HourlyForecast(
    val epochMillis: Long,
    val temperatureC: Double,
    val feelsLikeC: Double,
    val precipitationProbability: Double,
    val precipitationMm: Double,
    val weatherCode: Int,
    val isDay: Boolean,
    val humidity: Double,
    val windSpeedKmh: Double,
    val uvIndex: Double? = null,
)

/** 每日预报。 */
@Serializable
data class DailyForecast(
    /** 本地零点的毫秒时间戳（按 UTC 零点存储，仅用于排序）。 */
    val epochMillis: Long,
    /** 本地日期字符串，例如 "2024-05-31"。 */
    val date: String = "",
    val weatherCode: Int,
    val maxTempC: Double,
    val minTempC: Double,
    val precipitationProbability: Double,
    /** 本地时间字符串，例如 "06:12"。 */
    val sunriseLocal: String? = null,
    val sunsetLocal: String? = null,
)

/** 一次完整的数据快照。 */
@Serializable
data class WeatherSnapshot(
    val city: City,
    val current: CurrentWeather,
    val hourly: List<HourlyForecast>,
    val daily: List<DailyForecast>,
    val airQuality: AirQuality? = null,
    val sourceId: String,
    val sourceName: String,
    val fetchedAtMillis: Long,
)
