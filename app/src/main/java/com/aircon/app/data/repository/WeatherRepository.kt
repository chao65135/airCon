package com.aircon.app.data.repository

import com.aircon.app.data.local.CityDao
import com.aircon.app.data.local.CityEntity
import com.aircon.app.data.local.SnapshotDao
import com.aircon.app.data.local.SnapshotEntity
import com.aircon.app.data.prefs.SettingsStore
import com.aircon.app.data.source.WeatherDataSource
import com.aircon.app.domain.model.City
import com.aircon.app.domain.model.WeatherSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

/**
 * 天气数据仓库。
 *
 * 职责：
 * 1. **多源调度**：主数据源失败时自动降级到备用源，并把实际使用的源告知界面。
 * 2. **缓存**：按「城市 + 时间窗口」缓存，避免重复消耗免费额度。
 *    - 缓存有效期 30 分钟
 *    - 失败时回退到本地快照（离线兜底）
 *
 * 免费额度的关键点：缓存窗口应当大于「用户刷新频率」，
 * 避免多次刷新把每日 10,000 次的额度打光。
 */
class WeatherRepository(
    private val dataSources: List<WeatherDataSource>,
    private val settingsStore: SettingsStore,
    private val cityDao: CityDao,
    private val snapshotDao: SnapshotDao,
    private val json: Json,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    /** 拉取结果，包含实际使用的数据源信息。 */
    data class FetchResult(
        val snapshot: WeatherSnapshot,
        /** 是否发生了降级。 */
        val isFallback: Boolean,
        /** 降级原因。 */
        val fallbackReason: String? = null,
        /** 是否来自本地缓存。 */
        val isFromCache: Boolean = false,
    )

    /**
     * 拉取指定城市的天气与空气质量。
     *
     * @param city 目标城市
     * @param forceRefresh 忽略内存缓存强制刷新
     */
    suspend fun fetch(city: City, forceRefresh: Boolean = false): Result<FetchResult> {
        val settings = settingsStore.settings.first()
        val primary = dataSources.firstOrNull { it.id == settings.primarySourceId }
            ?: dataSources.firstOrNull()
            ?: return Result.failure(IllegalStateException("没有可用的数据源"))

        if (!forceRefresh) {
            memoryCache[city.id]?.let { cached ->
                if (clock() - cached.fetchedAtMillis < CACHE_TTL_MILLIS) {
                    return Result.success(
                        FetchResult(cached.snapshot, isFallback = false, isFromCache = true),
                    )
                }
            }
        }

        val fallbacks = dataSources.filter { it.id != primary.id && it.isConfigured }
        val candidates = listOf(primary) + fallbacks

        var lastError: Throwable? = null
        for ((index, source) in candidates.withIndex()) {
            try {
                val snapshot = source.fetch(city, settings.aqiStandard)
                memoryCache[city.id] = CacheEntry(snapshot, clock())
                persistSnapshot(snapshot)
                return Result.success(
                    FetchResult(
                        snapshot = snapshot,
                        isFallback = index > 0,
                        fallbackReason = if (index > 0) {
                            lastError?.message ?: "主数据源不可用"
                        } else {
                            null
                        },
                    ),
                )
            } catch (e: Exception) {
                lastError = e
            }
        }

        // 全部失败：回退到本地缓存快照
        val cached = readCachedSnapshot(city.id)
        if (cached != null) {
            return Result.success(
                FetchResult(
                    snapshot = cached,
                    isFallback = true,
                    fallbackReason = lastError?.message ?: "网络异常",
                    isFromCache = true,
                ),
            )
        }
        return Result.failure(lastError ?: IllegalStateException("数据获取失败"))
    }

    /**
     * 按数据源标识查找数据源，用于获取署名信息。
     *
     * @param sourceId 数据源标识
     * @return 匹配的数据源，未找到时返回 null
     */
    fun sourceOf(sourceId: String): WeatherDataSource? =
        dataSources.firstOrNull { it.id == sourceId }

    /** 读取本地缓存的快照。 */
    suspend fun readCachedSnapshot(cityId: String): WeatherSnapshot? {
        val entity = snapshotDao.findByCityId(cityId) ?: return null
        return runCatching { json.decodeFromString<WeatherSnapshot>(entity.payload) }.getOrNull()
    }

    /** 观察本地缓存快照。 */
    fun observeCachedSnapshot(cityId: String): Flow<WeatherSnapshot?> =
        snapshotDao.observeByCityId(cityId).map { entity ->
            entity?.let {
                runCatching { json.decodeFromString<WeatherSnapshot>(it.payload) }.getOrNull()
            }
        }

    private suspend fun persistSnapshot(snapshot: WeatherSnapshot) {
        runCatching {
            snapshotDao.upsert(
                SnapshotEntity(
                    cityId = snapshot.city.id,
                    payload = json.encodeToString(WeatherSnapshot.serializer(), snapshot),
                    fetchedAtMillis = snapshot.fetchedAtMillis,
                ),
            )
        }
    }

    // ---------- 城市管理 ----------

    fun observeCities(): Flow<List<City>> =
        cityDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun cities(): List<City> = cityDao.observeAll().first().map { it.toDomain() }

    suspend fun addCity(city: City) {
        val order = cityDao.count()
        cityDao.upsert(
            CityEntity(
                id = city.id,
                name = city.name,
                admin = city.admin,
                country = city.country,
                latitude = city.latitude,
                longitude = city.longitude,
                timezone = city.timezone,
                isCurrentLocation = false,
                sortOrder = order,
                updatedAtMillis = clock(),
            ),
        )
    }

    suspend fun removeCity(cityId: String) {
        cityDao.deleteById(cityId)
        snapshotDao.deleteByCityId(cityId)
    }

    suspend fun setCurrentLocation(city: City) {
        cityDao.clearCurrentLocation()
        cityDao.upsert(
            CityEntity(
                id = city.id,
                name = city.name,
                admin = city.admin,
                country = city.country,
                latitude = city.latitude,
                longitude = city.longitude,
                timezone = city.timezone,
                isCurrentLocation = true,
                sortOrder = -1,
                updatedAtMillis = clock(),
            ),
        )
    }

    suspend fun currentLocationCity(): City? = cityDao.findCurrentLocation()?.toDomain()

    private data class CacheEntry(
        val snapshot: WeatherSnapshot,
        val fetchedAtMillis: Long,
    )

    companion object {
        /** 内存缓存有效期：30 分钟。 */
        const val CACHE_TTL_MILLIS = 30 * 60 * 1000L

        private val memoryCache = mutableMapOf<String, CacheEntry>()
    }
}

/** 数据库实体转领域模型。 */
fun CityEntity.toDomain(): City = City(
    id = id,
    name = name,
    admin = admin,
    country = country,
    latitude = latitude,
    longitude = longitude,
)
