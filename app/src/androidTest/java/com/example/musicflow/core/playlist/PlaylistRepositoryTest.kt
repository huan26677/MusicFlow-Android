
package com.example.musicflow.core.playlist

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import com.example.musicflow.core.history.RecentSong
import com.example.musicflow.core.history.RoomRecentHistoryRepository
import com.example.musicflow.core.history.local.RecentHistoryDatabase

import kotlinx.coroutines.runBlocking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaylistRepositoryTest {

    private fun track(id: Int) = PlaylistTrack(
        mediaId = "device-$id",
        audioUri = "content://media/audio/$id",
        title = "Bài hát $id",
        artist = "Nghệ sĩ",
        addedAtMs = id.toLong()
    )

    @Test
    fun createRenameAddRemoveAndCascade() = runBlocking {
        val context = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        val database = Room.inMemoryDatabaseBuilder(
            context,
            RecentHistoryDatabase::class.java
        ).build()

        try {
            val repository = RoomPlaylistRepository(database)

            val first = repository.create(
                "  Nhạc   yêu thích  "
            )!!

            val second = repository.create("Nhạc thư giãn")!!

            assertEquals("Nhạc yêu thích", first.name)
            assertTrue(first.id != second.id)

            assertTrue(
                repository.rename(
                    first.id,
                    "  Nhạc   Việt  "
                )
            )

            assertEquals(
                "Nhạc Việt",
                repository.getPlaylist(first.id)?.name
            )

            assertTrue(repository.addTrack(first.id, track(1)))
            assertTrue(repository.addTrack(first.id, track(2)))

            // Thêm trùng trong cùng playlist phải thất bại.
            assertFalse(
                repository.addTrack(first.id, track(1))
            )

            // Cùng bài được phép thuộc playlist khác.
            assertTrue(
                repository.addTrack(second.id, track(1))
            )

            assertEquals(
                listOf("device-1", "device-2"),
                repository.getTracks(first.id)
                    .map { it.mediaId }
            )

            assertTrue(
                repository.removeTrack(
                    first.id,
                    "device-1"
                )
            )

            assertEquals(
                listOf("device-2"),
                repository.getTracks(first.id)
                    .map { it.mediaId }
            )

            // Xóa playlist đầu tiên.
            assertTrue(repository.deletePlaylist(first.id))

            // Các bài liên kết bị xóa theo.
            assertTrue(
                repository.getTracks(first.id).isEmpty()
            )

            // Playlist khác không bị ảnh hưởng.
            assertEquals(
                1,
                repository.getTracks(second.id).size
            )

        } finally {
            database.close()
        }
    }

    @Test
    fun playlistChangesDoNotDeleteRecentHistory() = runBlocking {
        val context = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        val database = Room.inMemoryDatabaseBuilder(
            context,
            RecentHistoryDatabase::class.java
        ).build()

        try {
            val history =
                RoomRecentHistoryRepository(database)

            val playlists =
                RoomPlaylistRepository(database)

            history.record(
                RecentSong(
                    mediaId = "device-1",
                    audioUri = "content://media/audio/1",
                    title = "Bài hát 1",
                    artist = "Nghệ sĩ",
                    playedAtMs = 100L
                )
            )

            val playlist = playlists.create(
                "Danh sách thử nghiệm"
            )!!

            playlists.addTrack(playlist.id, track(1))
            playlists.deletePlaylist(playlist.id)

            val recent = history.getRecent()

            assertEquals(1, recent.size)
            assertEquals(
                "device-1",
                recent.first().mediaId
            )

        } finally {
            database.close()
        }
    }
}
