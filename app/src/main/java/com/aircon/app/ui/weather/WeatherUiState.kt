package com.aircon.app.ui.weather

import com.aircon.app.domain.model.AirQuality
import com.aircon.app.domain.model.City
import com.aircon.app.domain.model.WeatherSnapshot

/**
 * 首页界面状态。
 */
data class WeatherUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val city: City? = null,
    val snapshot: WeatherSnapshot? = null,
    val errorMessage: String? = null,
    /** 是否展示的是离线缓存数据。 */
    val isOffline: Boolean = false,
    /** 数据源降级提示。 */
    val fallbackMessage: String? = null,
    /** 当前生效的数据源署名。 */
    val attribution: String? = null,
    val attributionUrl: String? = null,
    /** AQI 标准，用于界面标注。 */
    val aqiStandardLabel: String = "",
    val useCelsius: Boolean = true,
) {
    val airQuality: AirQuality? get() = snapshot?.airQuality
    val hasData: Boolean get() = snapshot != null
}
