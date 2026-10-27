package com.aircon.app.di

import android.content.Context
import com.aircon.app.data.local.AirConDatabase
import com.aircon.app.data.prefs.AppSettings
import com.aircon.app.data.prefs.SettingsStore
import com.aircon.app.data.remote.openmeteo.OpenMeteoApi
import com.aircon.app.data.remote.openmeteo.OpenMeteoGeocodingApi
import com.aircon.app.data.remote.qweather.DynamicQWeatherApi
import com.aircon.app.data.remote.qweather.QWeatherApi
import com.aircon.app.data.repository.CitySearchRepository
import com.aircon.app.data.repository.WeatherRepository
import com.aircon.app.data.source.OpenMeteoDataSource
import com.aircon.app.data.source.QWeatherDataSource
import com.aircon.app.data.source.WeatherDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.TimeUnit

/**
 * 轻量依赖容器（手动 DI）。
 *
 * 保持依赖数量最小，避免引入 Hilt 带来的构建复杂度；
 * 后续如有需要可平滑迁移到 Hilt。
 */
class AppContainer(private val context: Context) {

    /** 应用级 JSON 序列化器。 */
    val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    private val database: AirConDatabase by lazy { AirConDatabase.getInstance(context) }

    val settingsStore: SettingsStore by lazy { SettingsStore(context) }

    /** 应用级协程作用域，用于长时间观察设置变化。 */
    private val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * 设置的内存快照。
     *
     * [WeatherDataSource.isConfigured] 是同步属性，而 DataStore 读取是挂起的，
     * 因此这里常驻观察设置变化并缓存最新值，供同步判定使用。
     */
    @Volatile
    private var cachedSettings: AppSettings = AppSettings()

    init {
        appScope.launch {
            settingsStore.settings.collect { cachedSettings = it }
        }
    }

    private val loggingInterceptor: HttpLoggingInterceptor by lazy {
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    private val openMeteoRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(OpenMeteoApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    private val openMeteoAirQualityRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(OpenMeteoApi.AIR_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    private val openMeteoApi: OpenMeteoApi by lazy {
        openMeteoRetrofit.create(OpenMeteoApi::class.java)
    }

    private val openMeteoAirQualityApi: OpenMeteoApi by lazy {
        openMeteoAirQualityRetrofit.create(OpenMeteoApi::class.java)
    }

    private val openMeteoGeocodingRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(OpenMeteoGeocodingApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    /** 城市搜索仓库（Open-Meteo 地理编码，免费免 Key）。 */
    val citySearchRepository: CitySearchRepository by lazy {
        CitySearchRepository(
            geocodingApi = openMeteoGeocodingRetrofit.create(OpenMeteoGeocodingApi::class.java),
        )
    }

    /**
     * 和风天气的 Retrofit 实例，API Host 与凭证来自用户设置，因此需要动态重建。
     */
    private fun qWeatherRetrofit(host: String, apiKey: String, token: String): Retrofit {
        val authInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder().apply {
                if (token.isNotBlank()) {
                    header("Authorization", "Bearer $token")
                } else if (apiKey.isNotBlank()) {
                    header("X-QW-Api-Key", apiKey)
                }
            }.build()
            chain.proceed(request)
        }
        val client = okHttpClient.newBuilder()
            .addInterceptor(authInterceptor)
            .build()
        return Retrofit.Builder()
            .baseUrl("https://$host/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    /**
     * 和风天气数据源。
     *
     * API Host 与凭证可在运行时变更，因此通过 [DynamicQWeatherApi] 按当前设置解析。
     */
    val qWeatherDataSource: QWeatherDataSource by lazy {
        QWeatherDataSource(
            api = DynamicQWeatherApi(
                settingsProvider = { settingsStore.settings.first() },
                apiFactory = { settings ->
                    qWeatherRetrofit(
                        host = settings.qweatherApiHost,
                        apiKey = settings.qweatherApiKey,
                        token = settings.qweatherToken,
                    ).create(QWeatherApi::class.java)
                },
            ),
            isConfiguredProvider = {
                cachedSettings.qweatherApiKey.isNotBlank() ||
                    cachedSettings.qweatherToken.isNotBlank()
            },
        )
    }

    /** 所有数据源，顺序决定降级优先级。 */
    val dataSources: List<WeatherDataSource> by lazy {
        listOf(openMeteoDataSource, qWeatherDataSource)
    }

    val openMeteoDataSource: OpenMeteoDataSource by lazy {
        OpenMeteoDataSource(
            forecastApi = openMeteoApi,
            airQualityApi = openMeteoAirQualityApi,
        )
    }

    val weatherRepository: WeatherRepository by lazy {
        WeatherRepository(
            dataSources = dataSources,
            settingsStore = settingsStore,
            cityDao = database.cityDao(),
            snapshotDao = database.snapshotDao(),
            json = json,
        )
    }
}
