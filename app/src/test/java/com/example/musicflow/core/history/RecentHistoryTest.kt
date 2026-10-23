
package com.example.musicflow.core.history

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentHistoryTest {

    private fun song(
        id: Int,
        title: String = "Song $id"
    ) = RecentSong(
        mediaId = "device-$id",
        audioUri = "content://media/audio/$id",
        title = title,
        artist = "Artist",
        playedAtMs = id.toLong()
    )

    @Test
    fun newestSongComesFirst() {
        val first = RecentHistory.add(
            emptyList(), song(1)
        )
        val result = RecentHistory.add(
            first, song(2)
        )

        assertEquals(
            listOf("device-2", "device-1"),
            result.map { it.mediaId }
        )
    }

    @Test
    fun replayMovesSongToFrontWithoutDuplicate() {
        val existing = listOf(
            song(3), song(2), song(1)
        )

        val result = RecentHistory.add(
            existing,
            song(2).copy(playedAtMs = 100L)
        )

        assertEquals(
            listOf("device-2", "device-3", "device-1"),
            result.map { it.mediaId }
        )
        assertEquals(100L, result.first().playedAtMs)
    }

    @Test
    fun historyKeepsAtMostTwentySongs() {
        val result = (1..25).fold(
            emptyList<RecentSong>()
        ) { history, id ->
            RecentHistory.add(history, song(id))
        }

        assertEquals(20, result.size)
        assertEquals("device-25", result.first().mediaId)
        assertEquals("device-6", result.last().mediaId)
    }

    @Test
    fun sameTitleDifferentIdsAreKept() {
        val first = RecentHistory.add(
            emptyList(), song(1, "Nàng Thơ")
        )
        val result = RecentHistory.add(
            first, song(2, "Nàng Thơ")
        )

        assertEquals(2, result.size)
    }

    @Test
    fun invalidSongIsIgnored() {
        val existing = listOf(song(1))

        val result = RecentHistory.add(
            existing,
            song(2).copy(audioUri = "")
        )

        assertEquals(existing, result)
        assertTrue(result.none { it.mediaId == "device-2" })
    }
}
