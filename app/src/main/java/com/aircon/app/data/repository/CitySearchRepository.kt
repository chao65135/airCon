package com.aircon.app.data.repository

import com.aircon.app.data.remote.openmeteo.OpenMeteoGeocodingApi
import com.aircon.app.data.remote.openmeteo.OpenMeteoGeocodingResult
import com.aircon.app.domain.model.City
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 城市搜索仓库。
 *
 * 使用 Open-Meteo 免费地理编码接口，支持中文城市名。
 */
class CitySearchRepository(
    private val geocodingApi: OpenMeteoGeocodingApi,
) {

    /**
     * 按名称搜索城市。
     *
     * @param keyword 关键词，例如 “北京”
     * @return 匹配的城市列表，按人口从大到小排序
     */
    suspend fun search(keyword: String): List<City> {
        if (keyword.isBlank()) return emptyList()
        return withContext(Dispatchers.IO) {
            runCatching { geocodingApi.search(keyword.trim()) }
                .getOrDefault(OpenMeteoGeocodingResponseEmpty)
                .results
                .map { it.toCity() }
                .sortedByDescending { it.population }
        }
    }
}

/** 空的地理编码响应，用于异常兜底。 */
private val OpenMeteoGeocodingResponseEmpty =
    com.aircon.app.data.remote.openmeteo.OpenMeteoGeocodingResponse()

/**
 * 地理编码结果转领域模型。
 *
 * ID 由经纬度与名称组合而成，保证同一城市在不同搜索词下保持一致。
 */
fun OpenMeteoGeocodingResult.toCity(): City {
    val lat = "%.4f".format(java.util.Locale.US, latitude)
    val lon = "%.4f".format(java.util.Locale.US, longitude)
    return City(
        id = "$name|$lat|$lon",
        name = name,
        admin = listOf(admin1, admin2).filter { it.isNotBlank() }.distinct().joinToString(" · "),
        country = country,
        latitude = latitude,
        longitude = longitude,
        timezone = timezone,
    )
}
