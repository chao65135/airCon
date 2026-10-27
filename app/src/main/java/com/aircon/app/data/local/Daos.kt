package com.aircon.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** 城市数据访问对象。 */
@Dao
interface CityDao {

    @Query("SELECT * FROM cities ORDER BY sortOrder ASC, name ASC")
    fun observeAll(): Flow<List<CityEntity>>

    @Query("SELECT * FROM cities WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): CityEntity?

    @Query("SELECT * FROM cities WHERE isCurrentLocation = 1 LIMIT 1")
    suspend fun findCurrentLocation(): CityEntity?

    @Query("SELECT * FROM cities WHERE isCurrentLocation = 1 LIMIT 1")
    fun observeCurrentLocation(): Flow<CityEntity?>

    @Query("SELECT COUNT(*) FROM cities")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(city: CityEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cities: List<CityEntity>)

    @Query("DELETE FROM cities WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE cities SET isCurrentLocation = 0")
    suspend fun clearCurrentLocation()

    @Query("UPDATE cities SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: String, sortOrder: Int)
}

/** 快照缓存数据访问对象。 */
@Dao
interface SnapshotDao {

    @Query("SELECT * FROM snapshots WHERE cityId = :cityId LIMIT 1")
    suspend fun findByCityId(cityId: String): SnapshotEntity?

    @Query("SELECT * FROM snapshots WHERE cityId = :cityId LIMIT 1")
    fun observeByCityId(cityId: String): Flow<SnapshotEntity?>

    @Upsert
    suspend fun upsert(snapshot: SnapshotEntity)

    @Query("DELETE FROM snapshots WHERE cityId = :cityId")
    suspend fun deleteByCityId(cityId: String)

    @Query("DELETE FROM snapshots")
    suspend fun clear()
}
