
package com.example.musicflow.core.history

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import com.example.musicflow.core.history.local.RecentHistoryDatabase

import kotlinx.coroutines.runBlocking

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecentHistoryDatabaseTest {

    private fun song(id: Int) = RecentSong(
        mediaId = "device-$id",
        audioUri = "content://media/audio/$id",
        title = "Song $id",
        artist = "Artist",
        playedAtMs = id.toLong()
    )

    @Test
    fun recordKeepsTwentyAndMovesReplayToFront() = runBlocking {
        val context = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        val database = Room.inMemoryDatabaseBuilder(
            context,
            RecentHistoryDatabase::class.java
        ).build()

        try {
            val repository =
                RoomRecentHistoryRepository(database)

            for (id in 1..25) {
                repository.record(song(id))
            }

            val firstResult = repository.getRecent()

            assertEquals(20, firstResult.size)
            assertEquals(
                "device-25",
                firstResult.first().mediaId
            )

            repository.record(
                song(12).copy(playedAtMs = 999L)
            )

            val replayResult = repository.getRecent()

            assertEquals(20, replayResult.size)
            assertEquals(
                "device-12",
                replayResult.first().mediaId
            )
            assertEquals(
                1,
                replayResult.count {
                    it.mediaId == "device-12"
                }
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun invalidSongIsNotStored() = runBlocking {
        val context = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        val database = Room.inMemoryDatabaseBuilder(
            context,
            RecentHistoryDatabase::class.java
        ).build()

        try {
            val repository =
                RoomRecentHistoryRepository(database)

            repository.record(
                song(1).copy(audioUri = "")
            )

            assertEquals(
                0,
                repository.getRecent().size
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun historySurvivesDatabaseReopen() = runBlocking {
        val context = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        val databaseName =
            "recent_history_test_${System.nanoTime()}.db"

        val firstDatabase = Room.databaseBuilder(
            context,
            RecentHistoryDatabase::class.java,
            databaseName
        ).build()

        try {
            RoomRecentHistoryRepository(
                firstDatabase
            ).record(song(7))
        } finally {
            firstDatabase.close()
        }

        val reopenedDatabase = Room.databaseBuilder(
            context,
            RecentHistoryDatabase::class.java,
            databaseName
        ).build()

        try {
            val repository =
                RoomRecentHistoryRepository(reopenedDatabase)

            val saved = repository.getRecent()

            assertEquals(1, saved.size)
            assertEquals(
                "device-7",
                saved.first().mediaId
            )
        } finally {
            reopenedDatabase.close()
            context.deleteDatabase(databaseName)
        }
    }
}
