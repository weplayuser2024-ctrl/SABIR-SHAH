package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserProfileEntity::class,
        FriendEntity::class,
        ChatMessageEntity::class,
        MomentEntity::class,
        GiftRecordEntity::class,
        RoomEntity::class,
        ShopInventoryEntity::class,
        PrivacySettingsEntity::class,
        DailyTaskEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class ZyroDatabase : RoomDatabase() {
    abstract fun zyroDao(): ZyroDao

    companion object {
        @Volatile
        private var INSTANCE: ZyroDatabase? = null

        fun getInstance(context: Context): ZyroDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ZyroDatabase::class.java,
                    "zyro_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
