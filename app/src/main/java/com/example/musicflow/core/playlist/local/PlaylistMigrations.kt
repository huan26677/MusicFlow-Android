
package com.example.musicflow.core.playlist.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object PlaylistMigrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {

        override fun migrate(db: SupportSQLiteDatabase) {

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `playlists` (
                    `id` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `createdAtMs` INTEGER NOT NULL,
                    `updatedAtMs` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `playlist_tracks` (
                    `playlistId` TEXT NOT NULL,
                    `mediaId` TEXT NOT NULL,
                    `audioUri` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `artist` TEXT NOT NULL,
                    `addedAtMs` INTEGER NOT NULL,
                    `position` INTEGER NOT NULL,
                    PRIMARY KEY(`playlistId`, `mediaId`),
                    FOREIGN KEY(`playlistId`)
                        REFERENCES `playlists`(`id`)
                        ON UPDATE NO ACTION
                        ON DELETE CASCADE
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS
                `index_playlist_tracks_playlistId`
                ON `playlist_tracks` (`playlistId`)
                """.trimIndent()
            )
        }
    }
}
