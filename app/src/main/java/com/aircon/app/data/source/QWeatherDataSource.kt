package com.aircon.app.data.source

import com.aircon.app.data.remote.qweather.QWeatherApi
import com.aircon.app.data.remote.qweather.QWeatherAirQualityResponse
import com.aircon.app.data.remote.qweather.QWeatherIndex
import com.aircon.app.domain.model.AirQuality
import com.aircon.app.domain.model.AqiIndex
import com.aircon.app.domain.model.AqiStandard
import com.aircon.app.domain.model.City
import com.aircon.app.domain.model.CurrentWeather
import com.aircon.app.domain.model.DailyForecast
import com.aircon.app.domain.model.HourlyForecast
import com.aircon.app.domain.model.PollutantReading
import com.aircon.app.domain.model.WeatherSnapshot
import com.aircon.app.domain.weather.WeatherCodeMapper
import com.aircon.app.util.TimeUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * 和风天气数据源：国内落地首选，支持中国 AQI（HJ 633）。
 *
 * 认证通过 OkHttp 拦截器注入（`X-QW-Api-Key` 或 `Authorization: Bearer`）。
 *
 * ⚠️ 免费额度为每月 5 万次请求，按量计费。API Key 必须放在服务端，
 * 客户端直连仅适用于开发调试。
 */
class QWeatherDataSource(
    private val api: QWeatherApi,
    private val isConfiguredProvider: () -> Boolean,
    private val clock: () -> Long = System::currentTimeMillis,
) : WeatherDataSource {

    override val id: String = DataSourceIds.QWEATHER
    override val displayName: String = "和风天气"
    override val isConfigured: Boolean get() = isConfiguredProvider()
    override val attribution: String = "天气服务由和风天气驱动 · QWeather"
    override val attributionUrl: String = "https://www.qweather.com/"

    override suspend fun fetch(city: City, aqiStandard: AqiStandard): WeatherSnapshot {
        val now = clock()
        val (current, hourly, daily, air) = coroutineScope {
            val currentDeferred = async { api.currentWeather(city.latitude, city.longitude) }
            val hourlyDeferred = async { api.hourlyWeather(city.latitude, city.longitude) }
            val dailyDeferred = async { api.dailyWeather(city.latitude, city.longitude) }
            val airDeferred = async { api.currentAirQuality(city.latitude, city.longitude) }
            Quad(
                currentDeferred.await(),
                hourlyDeferred.await(),
                dailyDeferred.await(),
                airDeferred.await(),
            )
        }

        val currentWeather = CurrentWeather(
            epochMillis = TimeUtils.toEpochMillis(current.now.time),
            temperatureC = current.now.temperature.value,
            feelsLikeC = current.now.feelsLike.value,
            humidity = current.now.humidity,
            precipitationMm = current.now.precipitation.amount.value,
            // 和风的 condition.code 是自有编码（如 104），需经中文描述映射到 WMO 编码。
            weatherCode = WeatherCodeMapper.fromChineseText(current.now.condition.text),
            windSpeedKmh = current.now.wind.speed.value,
            windDirectionDeg = current.now.wind.direction.degree,
            isDay = true,
            pressureHpa = current.now.pressure.value.takeIf { it > 0.0 },
            visibilityMeters = current.now.visibility.value.takeIf { it > 0.0 },
        )

        val hourlyForecasts = hourly.hours.map { item ->
            HourlyForecast(
                epochMillis = TimeUtils.toEpochMillis(item.forecastTime),
                temperatureC = item.temperature.value,
                feelsLikeC = item.feelsLike.value,
                precipitationProbability = item.precipitation.probability,
                precipitationMm = item.precipitation.amount.value,
                weatherCode = WeatherCodeMapper.fromChineseText(item.condition.text),
                isDay = true,
                humidity = item.humidity,
                windSpeedKmh = item.wind.speed.value,
                uvIndex = item.uvIndex,
            )
        }.filter { it.epochMillis >= now - 3_600_000L }

        val dailyForecasts = daily.daily.map { item ->
            DailyForecast(
                epochMillis = TimeUtils.toEpochMillis("${item.fxDate}T00:00"),
                date = item.fxDate,
                // 和风的 condition.code 是自有编码（如 104），需经中文描述映射到 WMO 编码。
                weatherCode = WeatherCodeMapper.fromChineseText(item.condition.text),
                maxTempC = item.tempMax.value,
                minTempC = item.tempMin.value,
                precipitationProbability = 0.0,
                sunriseLocal = item.sunrise.takeLast(5).ifBlank { null },
                sunsetLocal = item.sunset.takeLast(5).ifBlank { null },
            )
        }

        return WeatherSnapshot(
            city = city,
            current = currentWeather,
            hourly = hourlyForecasts,
            daily = dailyForecasts,
            airQuality = buildAirQuality(air, aqiStandard, now),
            sourceId = id,
            sourceName = displayName,
            fetchedAtMillis = now,
        )
    }

    /**
     * 构建空气质量。
     *
     * 和风直接提供中国 AQI（`cn-mee`），优先使用接口原始值，
     * 保证与官方口径一致；`aqiDisplay` 为展示值，不做四舍五入处理。
     */
    private fun buildAirQuality(
        response: QWeatherAirQualityResponse,
        aqiStandard: AqiStandard,
        now: Long,
    ): AirQuality? {
        val pollutants = response.pollutants.map { item ->
            PollutantReading(
                code = item.code,
                name = item.name,
                value = item.concentration.value,
                unit = item.concentration.unit.ifBlank { "μg/m³" },
            )
        }

        val index = selectIndex(response.indexes, aqiStandard)?.let { item ->
            AqiIndex(
                value = item.aqi.toInt(),
                level = item.level.toIntOrNull() ?: 0,
                category = item.category,
                standard = aqiStandard,
                primaryPollutantCode = item.primaryPollutant.code.ifBlank { null },
                primaryPollutantName = item.primaryPollutant.name.ifBlank { null },
                healthAdvice = item.health?.advice?.generalPopulation?.ifBlank { null },
                sensitivePopulationAdvice =
                    item.health?.advice?.sensitivePopulation?.ifBlank { null },
            )
        } ?: return null

        val note = buildString {
            append("和风天气空气质量")
            if (response.refer.sources.isNotEmpty()) {
                append(" · 数据来源：")
                append(response.refer.sources.joinToString("、"))
            }
        }

        return AirQuality(
            index = index,
            pollutants = pollutants,
            hourly = emptyList(),
            updatedAtMillis = now,
            sourceNote = note,
        )
    }

    /**
     * 按标准挑选 AQI 指数条目。
     *
     * 和风的 `code` 取值包括 `cn-mee`（中国）、`us-epa`（美国）、`qaqi`、`gb-defra` 等。
     */
    private fun selectIndex(
        indexes: List<QWeatherIndex>,
        aqiStandard: AqiStandard,
    ): QWeatherIndex? {
        if (indexes.isEmpty()) return null
        val preferred = when (aqiStandard) {
            AqiStandard.CN -> listOf("cn-mee", "cn-aqi")
            AqiStandard.US -> listOf("us-epa")
            AqiStandard.EU -> listOf("eu")
        }
        preferred.forEach { code ->
            indexes.firstOrNull { it.code.equals(code, ignoreCase = true) }?.let { return it }
        }
        return indexes.firstOrNull()
    }
}

/** 简单的四元组容器。 */
private data class Quad<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
)
