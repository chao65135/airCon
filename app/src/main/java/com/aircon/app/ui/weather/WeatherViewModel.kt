package com.aircon.app.ui.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aircon.app.data.prefs.SettingsStore
import com.aircon.app.data.repository.CitySearchRepository
import com.aircon.app.data.repository.WeatherRepository
import com.aircon.app.domain.model.City
import com.aircon.app.ui.theme.displayName
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * 首页 ViewModel。
 *
 * 负责：加载城市列表、切换城市、刷新数据、搜索城市、响应设置变化。
 */
class WeatherViewModel(
    private val repository: WeatherRepository,
    private val settingsStore: SettingsStore,
    private val citySearchRepository: CitySearchRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private val _cities = MutableStateFlow<List<City>>(emptyList())
    val cities: StateFlow<List<City>> = _cities.asStateFlow()

    private val _searchResults = MutableStateFlow<List<City>>(emptyList())
    val searchResults: StateFlow<List<City>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private var currentCityId: String? = null
    private var searchJob: Job? = null
    private var defaultCitySeeded = false

    init {
        observeCities()
        observeSettings()
    }

    private fun observeCities() {
        repository.observeCities()
            .onEach { list ->
                _cities.value = list
                // 首次启动且无城市时，预置一个默认城市，避免空白首页。
                // 选中逻辑交由下一次列表发射统一处理，避免重复请求。
                if (list.isEmpty() && !defaultCitySeeded) {
                    defaultCitySeeded = true
                    viewModelScope.launch { repository.addCity(DEFAULT_CITY) }
                    return@onEach
                }
                // 优先沿用当前选中城市；否则退回列表首个城市。
                val selected = list.firstOrNull { it.id == currentCityId }
                    ?: list.firstOrNull()
                if (selected != null && selected.id != currentCityId) {
                    selectCity(selected, forceRefresh = false)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeSettings() {
        settingsStore.settings
            .onEach { settings ->
                _uiState.value = _uiState.value.copy(
                    useCelsius = settings.useCelsius,
                    aqiStandardLabel = settings.aqiStandard.displayName(),
                )
            }
            .launchIn(viewModelScope)
    }

    /**
     * 选择城市。
     *
     * @param forceRefresh 忽略缓存强制刷新
     */
    fun selectCity(city: City, forceRefresh: Boolean = false) {
        currentCityId = city.id
        _uiState.value = _uiState.value.copy(
            city = city,
            isLoading = true,
            errorMessage = null,
            fallbackMessage = null,
        )
        load(city, forceRefresh)
    }

    /** 刷新当前城市。 */
    fun refresh() {
        val city = _uiState.value.city ?: return
        _uiState.value = _uiState.value.copy(isRefreshing = true, errorMessage = null)
        load(city, forceRefresh = true)
    }

    private fun load(city: City, forceRefresh: Boolean) {
        viewModelScope.launch {
            val settings = settingsStore.settings.first()
            repository.fetch(city, forceRefresh).fold(
                onSuccess = { result ->
                    val source = repository.sourceOf(result.snapshot.sourceId)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        city = result.snapshot.city,
                        snapshot = result.snapshot,
                        errorMessage = null,
                        isOffline = result.isFromCache,
                        fallbackMessage = if (result.isFallback) {
                            val reason = result.fallbackReason?.takeIf { it.isNotBlank() }
                            if (reason != null) {
                                "主数据源不可用（$reason），已降级到${result.snapshot.sourceName}"
                            } else {
                                "已降级到${result.snapshot.sourceName}"
                            }
                        } else {
                            null
                        },
                        attribution = source?.attribution,
                        attributionUrl = source?.attributionUrl,
                        aqiStandardLabel = settings.aqiStandard.displayName(),
                        useCelsius = settings.useCelsius,
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = error.message ?: "加载失败",
                    )
                },
            )
        }
    }

    /** 添加城市。 */
    fun addCity(city: City) {
        viewModelScope.launch {
            repository.addCity(city)
            selectCity(city, forceRefresh = true)
        }
    }

    /** 删除城市。 */
    fun removeCity(cityId: String) {
        viewModelScope.launch {
            repository.removeCity(cityId)
            if (currentCityId == cityId) {
                currentCityId = null
                val fallback = _cities.value.firstOrNull { it.id != cityId }
                if (fallback != null) {
                    selectCity(fallback, forceRefresh = false)
                } else {
                    // 保留温度单位与 AQI 标准标注，避免设置回读滞后导致显示回落。
                    val current = _uiState.value
                    _uiState.value = WeatherUiState(
                        aqiStandardLabel = current.aqiStandardLabel,
                        useCelsius = current.useCelsius,
                    )
                }
            }
        }
    }

    /**
     * 搜索城市。
     *
     * 输入停顿 300ms 后才发起请求，避免逐字符打字浪费免费额度。
     */
    fun searchCities(keyword: String) {
        val query = keyword.trim()
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }
        searchJob = viewModelScope.launch {
            _isSearching.value = true
            delay(SEARCH_DEBOUNCE_MILLIS)
            _searchResults.value = citySearchRepository.search(query)
            _isSearching.value = false
        }
    }

    /** 清空搜索结果。 */
    fun clearSearch() {
        searchJob?.cancel()
        _searchResults.value = emptyList()
        _isSearching.value = false
    }

    companion object {
        /** 搜索防抖时间。 */
        private const val SEARCH_DEBOUNCE_MILLIS = 300L

        /** 首启动预置城市：北京。 */
        private val DEFAULT_CITY = City(
            id = "北京|39.9042|116.4074",
            name = "北京",
            admin = "北京市",
            country = "中国",
            latitude = 39.9042,
            longitude = 116.4074,
            timezone = "Asia/Shanghai",
        )

        /**
         * 工厂。
         */
        fun factory(
            repository: WeatherRepository,
            settingsStore: SettingsStore,
            citySearchRepository: CitySearchRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(WeatherViewModel::class.java)) {
                    "Unknown ViewModel class: ${modelClass.name}"
                }
                return WeatherViewModel(repository, settingsStore, citySearchRepository) as T
            }
        }
    }
}
