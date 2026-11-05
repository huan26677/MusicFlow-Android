package com.example.musicflow.feature.playlist

import com.example.musicflow.core.playlist.LocalPlaylist
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistNameFilterTest {
    private val songs = listOf(
        playlist("1", "Nhạc thư giãn"),
        playlist("2", "Đêm Đà Lạt"),
        playlist("3", "Workout"),
        playlist("4", "Nhạc học bài")
    )

    @Test fun matchesVietnameseNamesWithoutDiacritics() {
        assertEquals(listOf("1", "4"), filterPlaylistNames(songs, "nhac").map { it.id })
        assertEquals(listOf("2"), filterPlaylistNames(songs, "dem da lat").map { it.id })
    }

    @Test fun ignoresCaseAndOuterWhitespace() {
        assertEquals(listOf("3"), filterPlaylistNames(songs, "  WORKOUT ").map { it.id })
    }

    @Test fun blankQueryKeepsOriginalOrderAndInputUntouched() {
        assertEquals(songs, filterPlaylistNames(songs, "  "))
        assertEquals("Nhạc thư giãn", songs.first().name)
    }

    @Test fun noMatchReturnsEmptyWithoutAlteringPlaylists() {
        assertEquals(emptyList<LocalPlaylist>(), filterPlaylistNames(songs, "không có"))
        assertEquals(4, songs.size)
    }

    private fun playlist(id: String, name: String) =
        LocalPlaylist(id, name, createdAtMs = 0L, updatedAtMs = 0L)
}
