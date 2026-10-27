package com.aircon.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aircon.app.data.prefs.AppSettings
import com.aircon.app.data.prefs.SettingsStore
import com.aircon.app.domain.model.AqiStandard
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 设置页 ViewModel。
 *
 * 所有写入操作都直接落到 [SettingsStore]，界面通过 [settings] 流回读。
 */
class SettingsViewModel(
    private val settingsStore: SettingsStore,
) : ViewModel() {

    /** 当前设置；首帧尚未读取完成时为 null。 */
    val settings: StateFlow<AppSettings?> = settingsStore.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    fun setUseCelsius(useCelsius: Boolean) {
        viewModelScope.launch { settingsStore.setUseCelsius(useCelsius) }
    }

    fun setAqiStandard(standard: AqiStandard) {
        viewModelScope.launch { settingsStore.setAqiStandard(standard) }
    }

    fun setPrimarySource(sourceId: String) {
        viewModelScope.launch { settingsStore.setPrimarySource(sourceId) }
    }

    fun setRefreshIntervalMinutes(minutes: Int) {
        viewModelScope.launch { settingsStore.setRefreshIntervalMinutes(minutes) }
    }

    /** 保存和风天气凭证。 */
    fun saveQWeather(host: String, apiKey: String, token: String) {
        viewModelScope.launch {
            settingsStore.setQWeatherCredentials(host.trim(), apiKey.trim(), token.trim())
        }
    }

    companion object {
        /** 工厂。 */
        fun factory(settingsStore: SettingsStore): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                        "Unknown ViewModel class: ${modelClass.name}"
                    }
                    return SettingsViewModel(settingsStore) as T
                }
            }
    }
}
