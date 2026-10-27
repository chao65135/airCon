package com.aircon.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 城市表。保存用户关注的城市与当前定位城市。
 */
@Entity(tableName = "cities")
data class CityEntity(
    @PrimaryKey val id: String,
    val name: String,
    val admin: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val timezone: String?,
    /** 是否为当前定位城市。 */
    val isCurrentLocation: Boolean = false,
    /** 排序序号，越小越靠前。 */
    val sortOrder: Int = 0,
    @ColumnInfo(defaultValue = "0") val updatedAtMillis: Long = 0L,
)

/**
 * 天气快照缓存表。
 *
 * 完整快照以 JSON 形式存储，便于离线兜底展示。
 */
@Entity(tableName = "snapshots")
data class SnapshotEntity(
    @PrimaryKey val cityId: String,
    val payload: String,
    val fetchedAtMillis: Long,
)
