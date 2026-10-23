
package com.example.musicflow.core.history.local

import android.content.Context

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [RecentSongEntity::class],
    version = 1,
    exportSchema = true
)
abstract class RecentHistoryDatabase : RoomDatabase() {

    abstract fun recentSongDao(): RecentSongDao

    companion object {

        @Volatile
        private var INSTANCE: RecentHistoryDatabase? = null

        fun getInstance(
            context: Context
        ): RecentHistoryDatabase {
            return INSTANCE ?: synchronized(this) {

                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    RecentHistoryDatabase::class.java,
                    "musicflow_history.db"
                ).build().also {
                    INSTANCE = it
                }
            }
        }
    }
}
