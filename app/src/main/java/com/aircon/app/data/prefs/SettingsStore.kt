package com.aircon.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aircon.app.data.source.DataSourceIds
import com.aircon.app.domain.model.AqiStandard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * 用户设置。
 *
 * @param aqiStandard 使用的 AQI 标准
 * @param useCelsius 温度单位是否为摄氏度
 * @param primarySourceId 主数据源标识
 * @param qweatherApiHost 和风天气 API Host
 * @param qweatherApiKey 和风天气 API Key（仅供开发调试）
 * @param qweatherToken 和风天气 JWT（仅供开发调试）
 * @param refreshIntervalMinutes 后台刷新间隔
 */
data class AppSettings(
    val aqiStandard: AqiStandard = AqiStandard.CN,
    val useCelsius: Boolean = true,
    val primarySourceId: String = DataSourceIds.OPEN_METEO,
    val qweatherApiHost: String = "api.qweather.com",
    val qweatherApiKey: String = "",
    val qweatherToken: String = "",
    val refreshIntervalMinutes: Int = 60,
)

/**
 * 设置存储，使用 DataStore Preferences。
 */
class SettingsStore(private val context: Context) {

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            aqiStandard = prefs[KEY_AQI_STANDARD]
                ?.let { runCatching { AqiStandard.valueOf(it) }.getOrNull() }
                ?: AqiStandard.CN,
            useCelsius = prefs[KEY_USE_CELSIUS] ?: true,
            primarySourceId = prefs[KEY_PRIMARY_SOURCE] ?: DataSourceIds.OPEN_METEO,
            qweatherApiHost = prefs[KEY_QW_HOST] ?: "api.qweather.com",
            qweatherApiKey = prefs[KEY_QW_API_KEY] ?: "",
            qweatherToken = prefs[KEY_QW_TOKEN] ?: "",
            refreshIntervalMinutes = prefs[KEY_REFRESH_INTERVAL] ?: 60,
        )
    }

    suspend fun setAqiStandard(standard: AqiStandard) {
        context.dataStore.edit { it[KEY_AQI_STANDARD] = standard.name }
    }

    suspend fun setUseCelsius(useCelsius: Boolean) {
        context.dataStore.edit { it[KEY_USE_CELSIUS] = useCelsius }
    }

    suspend fun setPrimarySource(sourceId: String) {
        context.dataStore.edit { it[KEY_PRIMARY_SOURCE] = sourceId }
    }

    suspend fun setQWeatherCredentials(host: String, apiKey: String, token: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_QW_HOST] = host
            prefs[KEY_QW_API_KEY] = apiKey
            prefs[KEY_QW_TOKEN] = token
        }
    }

    suspend fun setRefreshIntervalMinutes(minutes: Int) {
        context.dataStore.edit { it[KEY_REFRESH_INTERVAL] = minutes }
    }

    private companion object {
        val KEY_AQI_STANDARD = stringPreferencesKey("aqi_standard")
        val KEY_USE_CELSIUS = booleanPreferencesKey("use_celsius")
        val KEY_PRIMARY_SOURCE = stringPreferencesKey("primary_source")
        val KEY_QW_HOST = stringPreferencesKey("qweather_api_host")
        val KEY_QW_API_KEY = stringPreferencesKey("qweather_api_key")
        val KEY_QW_TOKEN = stringPreferencesKey("qweather_token")
        val KEY_REFRESH_INTERVAL = intPreferencesKey("refresh_interval_minutes")
    }
}
