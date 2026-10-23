package com.example.musicflow.core.playlist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaylistReorderRulesTest {

    private fun track(id: Int) = PlaylistTrack(
        mediaId = "device-$id",
        audioUri = "content://media/audio/$id",
        title = "Bài $id",
        artist = "Test",
        addedAtMs = id.toLong()
    )

    private val songs = listOf(
        track(1), track(2), track(3)
    )

    private fun ids(items: List<PlaylistTrack>) =
        items.map { it.mediaId }

    @Test
    fun moveUp() {
        val result = PlaylistRules.moveTrack(
            songs, "device-2", -1
        )!!

        assertEquals(
            listOf("device-2", "device-1", "device-3"),
            ids(result)
        )
    }

    @Test
    fun moveDown() {
        val result = PlaylistRules.moveTrack(
            songs, "device-2", 1
        )!!

        assertEquals(
            listOf("device-1", "device-3", "device-2"),
            ids(result)
        )
    }

    @Test
    fun boundariesAreRejected() {
        assertNull(
            PlaylistRules.moveTrack(songs, "device-1", -1)
        )

        assertNull(
            PlaylistRules.moveTrack(songs, "device-3", 1)
        )
    }

    @Test
    fun invalidMovesAreRejected() {
        assertNull(
            PlaylistRules.moveTrack(songs, "missing", 1)
        )

        assertNull(
            PlaylistRules.moveTrack(songs, "device-2", 0)
        )
    }

    @Test
    fun sourceListIsNotModified() {
        PlaylistRules.moveTrack(songs, "device-2", -1)

        assertEquals(
            listOf("device-1", "device-2", "device-3"),
            ids(songs)
        )
    }
}
