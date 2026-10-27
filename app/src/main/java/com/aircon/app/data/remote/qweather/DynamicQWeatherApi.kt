package com.aircon.app.data.remote.qweather

import com.aircon.app.data.prefs.AppSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 动态解析的和风天气 API。
 *
 * 和风天气的 API Host、API Key、JWT 都允许用户在设置页修改，
 * 因此不能在容器初始化时固化 Retrofit 实例。
 *
 * 这里按「当前设置」惰性地构建并缓存 Retrofit 实例，设置变化时自动重建。
 */
class DynamicQWeatherApi(
    private val settingsProvider: suspend () -> AppSettings,
    private val apiFactory: (AppSettings) -> QWeatherApi,
) : QWeatherApi {

    private val mutex = Mutex()

    private var cachedKey: String? = null
    private var cachedApi: QWeatherApi? = null

    /**
     * 解析出可用的 API 实例。
     *
     * @throws IllegalStateException 未配置任何凭证
     */
    private suspend fun resolve(): QWeatherApi {
        val settings = settingsProvider()
        if (settings.qweatherApiKey.isBlank() && settings.qweatherToken.isBlank()) {
            throw IllegalStateException("未配置和风天气 API Key 或 JWT")
        }
        val key = buildString {
            append(settings.qweatherApiHost)
            append('|')
            append(settings.qweatherApiKey)
            append('|')
            append(settings.qweatherToken)
        }
        mutex.withLock {
            val existing = cachedApi
            if (existing != null && cachedKey == key) return existing
            val created = apiFactory(settings)
            cachedApi = created
            cachedKey = key
            return created
        }
    }

    override suspend fun currentWeather(
        latitude: Double,
        longitude: Double,
    ): QWeatherCurrentResponse = resolve().currentWeather(latitude, longitude)

    override suspend fun hourlyWeather(
        latitude: Double,
        longitude: Double,
        hours: Int,
        localTime: Boolean,
    ): QWeatherHourlyResponse = resolve().hourlyWeather(latitude, longitude, hours, localTime)

    override suspend fun dailyWeather(
        latitude: Double,
        longitude: Double,
        days: Int,
        localTime: Boolean,
    ): QWeatherDailyResponse = resolve().dailyWeather(latitude, longitude, days, localTime)

    override suspend fun currentAirQuality(
        latitude: Double,
        longitude: Double,
    ): QWeatherAirQualityResponse = resolve().currentAirQuality(latitude, longitude)

    override suspend fun hourlyAirQuality(
        latitude: Double,
        longitude: Double,
    ): QWeatherAirHourlyResponse = resolve().hourlyAirQuality(latitude, longitude)
}
