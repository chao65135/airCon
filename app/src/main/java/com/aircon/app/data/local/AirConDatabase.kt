package com.aircon.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * 本地数据库，保存城市列表与天气快照缓存。
 */
@Database(
    entities = [CityEntity::class, SnapshotEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AirConDatabase : RoomDatabase() {

    abstract fun cityDao(): CityDao

    abstract fun snapshotDao(): SnapshotDao

    companion object {
        private const val DB_NAME = "aircon.db"

        @Volatile
        private var instance: AirConDatabase? = null

        fun getInstance(context: Context): AirConDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AirConDatabase::class.java,
                    DB_NAME,
                ).fallbackToDestructiveMigration(dropAllTables = true).build().also {
                    instance = it
                }
            }
    }
}
