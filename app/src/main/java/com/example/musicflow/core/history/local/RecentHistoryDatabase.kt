
package com.example.musicflow.core.history.local

import android.content.Context

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import com.example.musicflow.core.playlist.local.PlaylistDao
import com.example.musicflow.core.playlist.local.PlaylistEntity
import com.example.musicflow.core.playlist.local.PlaylistTrackEntity
import com.example.musicflow.core.playlist.local.PlaylistMigrations

@Database(
    entities = [
        RecentSongEntity::class,
        PlaylistEntity::class,
        PlaylistTrackEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class RecentHistoryDatabase : RoomDatabase() {

    abstract fun recentSongDao(): RecentSongDao

    abstract fun playlistDao(): PlaylistDao

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
                )
                    .addMigrations(
                        PlaylistMigrations.MIGRATION_1_2
                    )
                    .build()
                    .also {
                        INSTANCE = it
                    }
            }
        }
    }
}
