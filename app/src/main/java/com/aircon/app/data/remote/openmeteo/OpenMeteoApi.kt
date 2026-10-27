package com.aircon.app.data.remote.openmeteo

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Open-Meteo 免费接口（无需 API Key）。
 *
 * - 天气预报：https://api.open-meteo.com/v1/forecast
 * - 空气质量：https://air-quality-api.open-meteo.com/v1/air-quality
 *
 * 免费额度：10,000 次/天、5,000 次/小时、600 次/分钟。
 * 免费层仅限非商业用途，商用需购买订阅（见 https://open-meteo.com/en/pricing）。
 */
interface OpenMeteoApi {

    @GET("v1/forecast")
    suspend fun forecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = CURRENT_FIELDS,
        @Query("hourly") hourly: String = HOURLY_FIELDS,
        @Query("daily") daily: String = DAILY_FIELDS,
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 7,
    ): OpenMeteoForecastResponse

    @GET("v1/air-quality")
    suspend fun airQuality(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = AIR_CURRENT_FIELDS,
        @Query("hourly") hourly: String = AIR_HOURLY_FIELDS,
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 5,
        @Query("past_days") pastDays: Int = 1,
    ): OpenMeteoAirQualityResponse

    @GET("v1/geocoding")
    suspend fun geocoding(
        @Query("name") name: String,
        @Query("count") count: Int = 20,
        @Query("language") language: String = "zh",
        @Query("format") format: String = "json",
    ): OpenMeteoGeocodingResponse

    companion object {
        const val BASE_URL = "https://api.open-meteo.com/"
        const val AIR_BASE_URL = "https://air-quality-api.open-meteo.com/"
        const val GEOCODING_BASE_URL = "https://geocoding-api.open-meteo.com/"

        private const val CURRENT_FIELDS =
            "temperature_2m,relative_humidity_2m,apparent_temperature,is_day," +
                "precipitation,weather_code,wind_speed_10m,wind_direction_10m," +
                "surface_pressure,cloud_cover"

        private const val HOURLY_FIELDS =
            "temperature_2m,apparent_temperature,relative_humidity_2m," +
                "precipitation_probability,precipitation,weather_code,is_day," +
                "wind_speed_10m,visibility,uv_index"

        private const val DAILY_FIELDS =
            "weather_code,temperature_2m_max,temperature_2m_min," +
                "precipitation_probability_max,sunrise,sunset,uv_index_max"

        private const val AIR_CURRENT_FIELDS =
            "pm10,pm2_5,carbon_monoxide,nitrogen_dioxide,sulphur_dioxide,ozone," +
                "us_aqi,european_aqi"

        private const val AIR_HOURLY_FIELDS =
            "pm10,pm2_5,carbon_monoxide,nitrogen_dioxide,sulphur_dioxide,ozone," +
                "us_aqi,european_aqi"
    }
}
