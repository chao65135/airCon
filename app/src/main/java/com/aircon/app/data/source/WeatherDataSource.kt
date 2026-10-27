package com.aircon.app.data.source

import com.aircon.app.domain.model.City
import com.aircon.app.domain.model.WeatherSnapshot

/**
 * 天气数据源抽象。
 *
 * 通过 [com.aircon.app.data.repository.WeatherRepository] 实现多源调度：
 * 主源失败时自动降级到备用源。
 */
interface WeatherDataSource {

    /** 数据源标识，用于持久化与展示。 */
    val id: String

    /** 数据源展示名称。 */
    val displayName: String

    /** 是否已配置（和风需要 API Key / Token）。 */
    val isConfigured: Boolean

    /** 版权与署名文本，必须在 App 中展示。 */
    val attribution: String

    /** 署名链接。 */
    val attributionUrl: String

    /**
     * 拉取指定城市的完整快照。
     *
     * @param city 目标城市
     * @param aqiStandard 期望的 AQI 标准；数据源不支持时自行降级并在结果中体现
     */
    suspend fun fetch(city: City, aqiStandard: com.aircon.app.domain.model.AqiStandard): WeatherSnapshot
}

/** 数据源标识常量。 */
object DataSourceIds {
    const val OPEN_METEO = "open-meteo"
    const val QWEATHER = "qweather"
}
