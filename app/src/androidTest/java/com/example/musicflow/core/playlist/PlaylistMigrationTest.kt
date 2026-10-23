
package com.example.musicflow.core.playlist

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import com.example.musicflow.core.history.local.RecentHistoryDatabase
import com.example.musicflow.core.playlist.local.PlaylistMigrations

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaylistMigrationTest {

    private val dbName = "playlist_migration_test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        RecentHistoryDatabase::class.java
    )

    @Test
    fun migration1To2PreservesRecentHistory() {

        val oldDatabase = helper.createDatabase(dbName, 1)

        oldDatabase.execSQL(
            """
            INSERT INTO recent_songs (
                mediaId,
                audioUri,
                title,
                artist,
                playedAtMs,
                position
            ) VALUES (
                'device-7',
                'content://media/audio/7',
                'Bài hát đã nghe',
                'Nghệ sĩ',
                123456,
                0
            )
            """.trimIndent()
        )

        oldDatabase.close()

        val migrated = helper.runMigrationsAndValidate(
            dbName,
            2,
            true,
            PlaylistMigrations.MIGRATION_1_2
        )

        try {
            migrated.query(
                """
                SELECT mediaId, title, playedAtMs, position
                FROM recent_songs
                WHERE mediaId = 'device-7'
                """.trimIndent()
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())

                assertEquals(
                    "device-7",
                    cursor.getString(0)
                )
                assertEquals(
                    "Bài hát đã nghe",
                    cursor.getString(1)
                )
                assertEquals(
                    123456L,
                    cursor.getLong(2)
                )
                assertEquals(
                    0,
                    cursor.getInt(3)
                )
            }

            migrated.query(
                """
                SELECT name FROM sqlite_master
                WHERE type = 'table'
                AND name = 'playlists'
                """.trimIndent()
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
            }

            migrated.query(
                """
                SELECT name FROM sqlite_master
                WHERE type = 'table'
                AND name = 'playlist_tracks'
                """.trimIndent()
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
            }

        } finally {
            migrated.close()
        }
    }
}
