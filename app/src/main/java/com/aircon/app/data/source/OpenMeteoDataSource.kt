package com.aircon.app.data.source

import com.aircon.app.data.remote.openmeteo.OpenMeteoAirCurrent
import com.aircon.app.data.remote.openmeteo.OpenMeteoAirHourly
import com.aircon.app.data.remote.openmeteo.OpenMeteoAirQualityResponse
import com.aircon.app.data.remote.openmeteo.OpenMeteoApi
import com.aircon.app.data.remote.openmeteo.OpenMeteoForecastResponse
import com.aircon.app.domain.aqi.ChinaAqiCalculator
import com.aircon.app.domain.model.AirQuality
import com.aircon.app.domain.model.AqiIndex
import com.aircon.app.domain.model.AqiStandard
import com.aircon.app.domain.model.City
import com.aircon.app.domain.model.CurrentWeather
import com.aircon.app.domain.model.DailyForecast
import com.aircon.app.domain.model.HourlyAqi
import com.aircon.app.domain.model.HourlyForecast
import com.aircon.app.domain.model.PollutantReading
import com.aircon.app.domain.model.WeatherSnapshot
import com.aircon.app.util.TimeUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Open-Meteo 数据源：天气 + 空气质量，免 API Key。
 *
 * 天气来自 ECMWF / DWD / NOAA / CMA 等模式；空气质量来自 CAMS
 * （欧洲 11km、全球 45km），中国区域为 45km 网格。
 *
 * 免费额度 10,000 次/天。免费层仅限非商业用途。
 */
class OpenMeteoDataSource(
    private val forecastApi: OpenMeteoApi,
    private val airQualityApi: OpenMeteoApi,
    private val clock: () -> Long = System::currentTimeMillis,
) : WeatherDataSource {

    override val id: String = DataSourceIds.OPEN_METEO
    override val displayName: String = "Open-Meteo"
    override val isConfigured: Boolean = true
    override val attribution: String = "天气数据由 Open-Meteo.com 提供，空气质量数据来自 CAMS"
    override val attributionUrl: String = "https://open-meteo.com/"

    private companion object {
        /** 逐小时预报 / AQI 窗口长度（小时）。 */
        const val HOURLY_WINDOW = 24
    }

    override suspend fun fetch(city: City, aqiStandard: AqiStandard): WeatherSnapshot {
        val (forecast, airQuality) = coroutineScope {
            val forecastDeferred = async { forecastApi.forecast(city.latitude, city.longitude) }
            val airDeferred = async { airQualityApi.airQuality(city.latitude, city.longitude) }
            forecastDeferred.await() to airDeferred.await()
        }
        return buildSnapshot(city, forecast, airQuality, aqiStandard)
    }

    private fun buildSnapshot(
        city: City,
        forecast: OpenMeteoForecastResponse,
        airQuality: OpenMeteoAirQualityResponse,
        aqiStandard: AqiStandard,
    ): WeatherSnapshot {
        val now = clock()
        val current = forecast.current
        val currentMillis = TimeUtils.toEpochMillis(current.time, forecast.utcOffsetSeconds)

        val currentWeather = CurrentWeather(
            epochMillis = currentMillis,
            temperatureC = current.temperature2m,
            feelsLikeC = current.apparentTemperature,
            humidity = current.relativeHumidity2m / 100.0,
            precipitationMm = current.precipitation,
            weatherCode = current.weatherCode,
            windSpeedKmh = current.windSpeed10m,
            windDirectionDeg = current.windDirection10m.toInt(),
            isDay = current.isDay == 1,
            pressureHpa = current.surfacePressure.takeIf { it > 0.0 },
        )

        val hourly = forecast.hourly.time.mapIndexedNotNull { index, time ->
            val temp = forecast.hourly.temperature2m.getOrNull(index) ?: return@mapIndexedNotNull null
            val epoch = TimeUtils.toEpochMillis(time, forecast.utcOffsetSeconds)
            HourlyForecast(
                epochMillis = epoch,
                temperatureC = temp,
                feelsLikeC = forecast.hourly.apparentTemperature.getOrNull(index) ?: temp,
                precipitationProbability =
                    (forecast.hourly.precipitationProbability.getOrNull(index) ?: 0.0) / 100.0,
                precipitationMm = forecast.hourly.precipitation.getOrNull(index) ?: 0.0,
                weatherCode = forecast.hourly.weatherCode.getOrNull(index) ?: 0,
                isDay = (forecast.hourly.isDay.getOrNull(index) ?: 1) == 1,
                humidity = (forecast.hourly.relativeHumidity2m.getOrNull(index) ?: 0.0) / 100.0,
                windSpeedKmh = forecast.hourly.windSpeed10m.getOrNull(index) ?: 0.0,
                uvIndex = forecast.hourly.uvIndex.getOrNull(index),
            )
        }.filter { it.epochMillis >= now - 3_600_000L }

        val daily = forecast.daily.time.mapIndexedNotNull { index, time ->
            val max = forecast.daily.temperature2mMax.getOrNull(index) ?: return@mapIndexedNotNull null
            DailyForecast(
                epochMillis = TimeUtils.toEpochMillis("${time}T00:00", forecast.utcOffsetSeconds),
                date = time,
                weatherCode = forecast.daily.weatherCode.getOrNull(index) ?: 0,
                maxTempC = max,
                minTempC = forecast.daily.temperature2mMin.getOrNull(index) ?: max,
                precipitationProbability =
                    (forecast.daily.precipitationProbabilityMax.getOrNull(index) ?: 0.0) / 100.0,
                sunriseLocal = forecast.daily.sunrise.getOrNull(index)?.takeLast(5),
                sunsetLocal = forecast.daily.sunset.getOrNull(index)?.takeLast(5),
            )
        }

        return WeatherSnapshot(
            city = city,
            current = currentWeather,
            hourly = hourly,
            daily = daily,
            airQuality = buildAirQuality(airQuality, aqiStandard, now),
            sourceId = id,
            sourceName = displayName,
            fetchedAtMillis = now,
        )
    }

    /**
     * 构建空气质量。
     *
     * - 中国 AQI：使用 HJ 633—2026 由浓度反算
     * - 美国 / 欧洲 AQI：直接使用接口返回的 `us_aqi` / `european_aqi`
     */
    private fun buildAirQuality(
        response: OpenMeteoAirQualityResponse,
        aqiStandard: AqiStandard,
        now: Long,
    ): AirQuality? {
        val current = response.current
        val hourly = response.hourly

        val pm25Series = hourly.pm25
        val pm10Series = hourly.pm10
        val no2Series = hourly.nitrogenDioxide
        val so2Series = hourly.sulphurDioxide
        val coSeries = hourly.carbonMonoxide
        val o3Series = hourly.ozone

        val currentIndex = hourly.time.indexOfFirst { it >= current.time }
            .takeIf { it >= 0 } ?: 0

        // 中国 AQI 需要 24h / 8h 平均浓度，实时报口径。
        val pm25_24h = pm25Series.average24hEndingAt(currentIndex)
        val pm10_24h = pm10Series.average24hEndingAt(currentIndex)
        val o3_8h = o3Series.average8hEndingAt(currentIndex)
        val no2_1h = no2Series.getOrNull(currentIndex)
        val so2_1h = so2Series.getOrNull(currentIndex)
        // Open-Meteo 的 CO 单位为 μg/m³，HJ 633 使用 mg/m³。
        val co_1h = coSeries.getOrNull(currentIndex)?.let { it / 1000.0 }

        val pollutants = buildPollutantList(current)
        val note = "CAMS ENSEMBLE · 中国区域为 45km 网格模型数据"

        val index: AqiIndex? = when (aqiStandard) {
            AqiStandard.CN -> ChinaAqiCalculator.buildAirQuality(
                concentrations = ChinaAqiCalculator.Concentrations(
                    pm25_24h = pm25_24h,
                    pm10_24h = pm10_24h,
                    no2_1h = no2_1h,
                    so2_1h = so2_1h,
                    co_1h = co_1h,
                    o3_8h = o3_8h,
                ),
                pollutants = pollutants,
                updatedAtMillis = now,
                sourceNote = note,
            )?.index

            AqiStandard.US -> current.usAqi?.toInt()?.let { value ->
                AqiIndex(
                    value = value,
                    level = usLevel(value),
                    category = usCategory(value),
                    standard = AqiStandard.US,
                    primaryPollutantName = pollutants.maxByOrNull { it.value }?.name,
                )
            }

            AqiStandard.EU -> current.europeanAqi?.toInt()?.let { value ->
                AqiIndex(
                    value = value,
                    level = euLevel(value),
                    category = euCategory(value),
                    standard = AqiStandard.EU,
                    primaryPollutantName = pollutants.maxByOrNull { it.value }?.name,
                )
            }
        }

        if (index == null) return null

        return AirQuality(
            index = index,
            pollutants = pollutants,
            hourly = buildHourlyAqi(hourly, response.utcOffsetSeconds, aqiStandard, currentIndex,
                pm25Series, pm10Series, no2Series, so2Series, coSeries, o3Series),
            updatedAtMillis = now,
            sourceNote = note,
        )
    }

    /**
     * 逐小时 AQI：只保留从 [fromIndex]（当前小时）起的 24 小时，与首页逐小时预报窗口保持一致。
     */
    private fun buildHourlyAqi(
        hourly: OpenMeteoAirHourly,
        utcOffsetSeconds: Int,
        aqiStandard: AqiStandard,
        fromIndex: Int,
        pm25Series: List<Double?>,
        pm10Series: List<Double?>,
        no2Series: List<Double?>,
        so2Series: List<Double?>,
        coSeries: List<Double?>,
        o3Series: List<Double?>,
    ): List<HourlyAqi> {
        val start = fromIndex.coerceAtLeast(0)
        val indices = start until minOf(start + HOURLY_WINDOW, hourly.time.size)
        if (indices.isEmpty()) return emptyList()

        return when (aqiStandard) {
            AqiStandard.CN -> {
                val timestamps = indices.map { TimeUtils.toEpochMillis(hourly.time[it], utcOffsetSeconds) }
                val concentrations = indices.map { index ->
                    ChinaAqiCalculator.Concentrations(
                        pm25_24h = pm25Series.average24hEndingAt(index),
                        pm10_24h = pm10Series.average24hEndingAt(index),
                        no2_1h = no2Series.getOrNull(index),
                        so2_1h = so2Series.getOrNull(index),
                        co_1h = coSeries.getOrNull(index)?.let { it / 1000.0 },
                        o3_8h = o3Series.average8hEndingAt(index),
                    )
                }
                ChinaAqiCalculator.buildHourlyAqi(timestamps, concentrations)
            }

            AqiStandard.US -> indices.mapNotNull { index ->
                val value = hourly.usAqi.getOrNull(index) ?: return@mapNotNull null
                HourlyAqi(
                    epochMillis = TimeUtils.toEpochMillis(hourly.time[index], utcOffsetSeconds),
                    value = value.toInt(),
                )
            }

            AqiStandard.EU -> indices.mapNotNull { index ->
                val value = hourly.europeanAqi.getOrNull(index) ?: return@mapNotNull null
                HourlyAqi(
                    epochMillis = TimeUtils.toEpochMillis(hourly.time[index], utcOffsetSeconds),
                    value = value.toInt(),
                )
            }
        }
    }

    private fun buildPollutantList(current: OpenMeteoAirCurrent): List<PollutantReading> =
        buildList {
            current.pm25?.let { add(PollutantReading("pm25", "PM2.5", it, "μg/m³")) }
            current.pm10?.let { add(PollutantReading("pm10", "PM10", it, "μg/m³")) }
            current.ozone?.let { add(PollutantReading("o3", "O₃", it, "μg/m³")) }
            current.nitrogenDioxide?.let { add(PollutantReading("no2", "NO₂", it, "μg/m³")) }
            current.sulphurDioxide?.let { add(PollutantReading("so2", "SO₂", it, "μg/m³")) }
            current.carbonMonoxide?.let { add(PollutantReading("co", "CO", it, "μg/m³")) }
        }

    private fun usLevel(value: Int): Int = when (value) {
        in 0..50 -> 1
        in 51..100 -> 2
        in 101..150 -> 3
        in 151..200 -> 4
        in 201..300 -> 5
        else -> 6
    }

    private fun usCategory(value: Int): String = when (value) {
        in 0..50 -> "优"
        in 51..100 -> "良"
        in 101..150 -> "对敏感人群不健康"
        in 151..200 -> "不健康"
        in 201..300 -> "非常不健康"
        else -> "危险"
    }

    private fun euLevel(value: Int): Int = when (value) {
        in 0..20 -> 1
        in 21..40 -> 2
        in 41..60 -> 3
        in 61..80 -> 4
        in 81..100 -> 5
        else -> 6
    }

    private fun euCategory(value: Int): String = when (value) {
        in 0..20 -> "优"
        in 21..40 -> "良"
        in 41..60 -> "中等"
        in 61..80 -> "较差"
        in 81..100 -> "差"
        else -> "极差"
    }
}

/**
 * 计算以 [endIndex] 结尾的 24 小时平均浓度（不足 24 个点则按实际数量平均）。
 */
internal fun List<Double?>.average24hEndingAt(endIndex: Int): Double? {
    if (isEmpty()) return null
    val from = (endIndex - 23).coerceAtLeast(0)
    val slice = subList(from, (endIndex + 1).coerceAtMost(size))
    val values = slice.filterNotNull()
    return if (values.isEmpty()) null else values.average()
}

/**
 * 计算以 [endIndex] 结尾的 8 小时滑动平均浓度。
 */
internal fun List<Double?>.average8hEndingAt(endIndex: Int): Double? {
    if (isEmpty()) return null
    val from = (endIndex - 7).coerceAtLeast(0)
    val slice = subList(from, (endIndex + 1).coerceAtMost(size))
    val values = slice.filterNotNull()
    return if (values.isEmpty()) null else values.average()
}
