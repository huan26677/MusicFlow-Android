package com.example.musicflow.core.playlist

import android.util.Log

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import com.example.musicflow.core.history.RecentSong
import com.example.musicflow.core.history.RoomRecentHistoryRepository
import com.example.musicflow.core.history.local.RecentHistoryDatabase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaylistReorderDatabaseTest {

    private val context =
        InstrumentationRegistry.getInstrumentation()
            .targetContext

    private fun track(id: Int) = PlaylistTrack(
        mediaId = "device-$id",
        audioUri = "content://media/audio/$id",
        title = "Bài $id",
        artist = "Test",
        addedAtMs = id.toLong()
    )

    private fun ids(items: List<PlaylistTrack>) =
        items.map { it.mediaId }

    @Test
    fun reorderPersistsAfterDatabaseReopen() = runBlocking<Unit> {
        val dbName =
            "playlist_reorder_${System.nanoTime()}.db"

        var database: RecentHistoryDatabase? = null

        try {
            database = Room.databaseBuilder(
                context,
                RecentHistoryDatabase::class.java,
                dbName
            ).build()

            val repo = RoomPlaylistRepository(
                checkNotNull(database)
            )

            val playlist = repo.create("Thử thứ tự")!!

            for (id in 1..4) {
                assertTrue(repo.addTrack(playlist.id, track(id)))
            }

            assertTrue(
                repo.removeTrack(playlist.id, "device-2")
            )

            assertTrue(
                repo.moveTrack(
                    playlist.id, "device-4", -1
                )
            )

            assertTrue(
                repo.moveTrack(
                    playlist.id, "device-1", 1
                )
            )

            val expected = listOf(
                "device-4", "device-1", "device-3"
            )

            assertEquals(
                expected,
                ids(repo.getTracks(playlist.id))
            )

            checkNotNull(database).close()
            database = null

            database = Room.databaseBuilder(
                context,
                RecentHistoryDatabase::class.java,
                dbName
            ).build()

            val reopened = RoomPlaylistRepository(
                checkNotNull(database)
            )

            assertEquals(
                expected,
                ids(reopened.getTracks(playlist.id))
            )

            assertEquals(
                expected,
                ids(
                    reopened.observeTracks(playlist.id)
                        .first()
                )
            )

            assertTrue(
                reopened.addTrack(playlist.id, track(5))
            )

            assertEquals(
                expected + "device-5",
                ids(reopened.getTracks(playlist.id))
            )

            Log.i(
                "MusicFlowReorderTest",
                "PASS reorderPersistsAfterDatabaseReopen"
            )
        } finally {
            database?.close()
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun reorderDoesNotAffectOtherPlaylistOrHistory() =
        runBlocking<Unit> {
            val database = Room.inMemoryDatabaseBuilder(
                context,
                RecentHistoryDatabase::class.java
            ).build()

            try {
                val playlists = RoomPlaylistRepository(database)
                val history =
                    RoomRecentHistoryRepository(database)

                val first = playlists.create("Playlist A")!!
                val second = playlists.create("Playlist B")!!

                for (id in 1..3) {
                    assertTrue(
                        playlists.addTrack(first.id, track(id))
                    )
                }

                assertTrue(
                    playlists.addTrack(second.id, track(1))
                )

                assertTrue(
                    playlists.addTrack(second.id, track(2))
                )

                history.record(
                    RecentSong(
                        mediaId = "device-1",
                        audioUri = "content://media/audio/1",
                        title = "Bài 1",
                        artist = "Test",
                        playedAtMs = 100L
                    )
                )

                assertTrue(
                    playlists.moveTrack(
                        first.id, "device-2", 1
                    )
                )

                assertFalse(
                    playlists.moveTrack(
                        first.id, "device-1", -1
                    )
                )

                assertFalse(
                    playlists.moveTrack(
                        first.id, "missing", 1
                    )
                )

                assertFalse(
                    playlists.moveTrack(
                        first.id, "device-1", 0
                    )
                )

                assertEquals(
                    listOf(
                        "device-1", "device-3", "device-2"
                    ),
                    ids(playlists.getTracks(first.id))
                )

                assertEquals(
                    listOf("device-1", "device-2"),
                    ids(playlists.getTracks(second.id))
                )

                assertEquals(
                    "device-1",
                    history.getRecent().first().mediaId
                )

                Log.i(
                    "MusicFlowReorderTest",
                    "PASS reorderDoesNotAffectOtherPlaylistOrHistory"
                )
            } finally {
                database.close()
            }
        }
}

