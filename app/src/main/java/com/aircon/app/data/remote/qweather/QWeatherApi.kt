package com.aircon.app.data.remote.qweather

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 和风天气（QWeather）开发服务。
 *
 * 端点为标准 v1 路径，API Host 由用户在设置中配置（默认为 [DEFAULT_API_HOST]）。
 *
 * 认证方式：推荐 JWT（Ed25519）。本项目同时支持：
 * - `X-QW-Api-Key` 请求头（API Key）
 * - `Authorization: Bearer <token>` 请求头
 *
 * ⚠️ 私钥 / API Key 绝对不能打进 APK，生产环境请通过自建后端代理转发。
 */
interface QWeatherApi {

    @GET("weather/v1/now/{latitude}/{longitude}")
    suspend fun currentWeather(
        @Path("latitude") latitude: Double,
        @Path("longitude") longitude: Double,
    ): QWeatherCurrentResponse

    @GET("weather/v1/hourly/{latitude}/{longitude}")
    suspend fun hourlyWeather(
        @Path("latitude") latitude: Double,
        @Path("longitude") longitude: Double,
        @Query("hours") hours: Int = 24,
        @Query("localTime") localTime: Boolean = true,
    ): QWeatherHourlyResponse

    @GET("weather/v1/daily/{latitude}/{longitude}")
    suspend fun dailyWeather(
        @Path("latitude") latitude: Double,
        @Path("longitude") longitude: Double,
        @Query("days") days: Int = 7,
        @Query("localTime") localTime: Boolean = true,
    ): QWeatherDailyResponse

    @GET("airquality/v1/current/{latitude}/{longitude}")
    suspend fun currentAirQuality(
        @Path("latitude") latitude: Double,
        @Path("longitude") longitude: Double,
    ): QWeatherAirQualityResponse

    @GET("airquality/v1/hourly/{latitude}/{longitude}")
    suspend fun hourlyAirQuality(
        @Path("latitude") latitude: Double,
        @Path("longitude") longitude: Double,
    ): QWeatherAirHourlyResponse

    companion object {
        const val DEFAULT_API_HOST = "api.qweather.com"
    }
}
