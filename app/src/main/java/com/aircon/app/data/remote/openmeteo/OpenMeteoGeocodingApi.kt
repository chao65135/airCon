package com.aircon.app.data.remote.openmeteo

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Open-Meteo 地理编码接口（免费、免 Key），用于城市搜索。
 *
 * 支持中文城市名，例如 `name=北京`。
 */
interface OpenMeteoGeocodingApi {

    @GET("v1/search")
    suspend fun search(
        @Query("name") name: String,
        @Query("count") count: Int = 20,
        @Query("language") language: String = "zh",
        @Query("format") format: String = "json",
    ): OpenMeteoGeocodingResponse

    companion object {
        const val BASE_URL = "https://geocoding-api.open-meteo.com/"
    }
}
