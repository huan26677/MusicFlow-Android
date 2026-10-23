
package com.example.musicflow.core.playlist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaylistRulesTest {

    private fun playlist() = LocalPlaylist(
        id = "playlist-1",
        name = "Nhạc yêu thích",
        createdAtMs = 100L,
        updatedAtMs = 100L
    )

    private fun track(
        id: Int,
        title: String = "Bài hát $id"
    ) = PlaylistTrack(
        mediaId = "device-$id",
        audioUri = "content://media/audio/$id",
        title = title,
        artist = "Nghệ sĩ",
        addedAtMs = id.toLong()
    )

    @Test
    fun nameIsTrimmedAndNormalized() {
        assertEquals(
            "Nhạc yêu thích",
            PlaylistRules.normalizedNameOrNull(
                "  Nhạc   yêu   thích  "
            )
        )
    }

    @Test
    fun emptyNameIsRejected() {
        assertNull(
            PlaylistRules.normalizedNameOrNull("   ")
        )
    }

    @Test
    fun overlyLongNameIsRejected() {
        assertNull(
            PlaylistRules.normalizedNameOrNull(
                "A".repeat(61)
            )
        )
    }

    @Test
    fun renamePreservesIdentity() {
        val renamed = PlaylistRules.rename(
            playlist = playlist(),
            newName = "  Nhạc   thư giãn ",
            updatedAtMs = 500L
        )

        assertEquals("playlist-1", renamed?.id)
        assertEquals("Nhạc thư giãn", renamed?.name)
        assertEquals(100L, renamed?.createdAtMs)
        assertEquals(500L, renamed?.updatedAtMs)
    }

    @Test
    fun invalidRenameIsRejected() {
        assertNull(
            PlaylistRules.rename(
                playlist(),
                "  ",
                500L
            )
        )
    }

    @Test
    fun addingTracksPreservesOrder() {
        val first = PlaylistRules.addTrack(
            emptyList(),
            track(1)
        )

        val result = PlaylistRules.addTrack(
            first,
            track(2)
        )

        assertEquals(
            listOf("device-1", "device-2"),
            result.map { it.mediaId }
        )
    }

    @Test
    fun duplicateTrackIsIgnored() {
        val existing = listOf(track(1))

        val result = PlaylistRules.addTrack(
            existing,
            track(1)
        )

        assertEquals(existing, result)
    }

    @Test
    fun sameTitleDifferentIdsAreAllowed() {
        val result = PlaylistRules.addTrack(
            listOf(track(1, "Nàng Thơ")),
            track(2, "Nàng Thơ")
        )

        assertEquals(2, result.size)
    }

    @Test
    fun removingTrackDoesNotChangeOtherTracks() {
        val existing = listOf(
            track(1), track(2), track(3)
        )

        val result = PlaylistRules.removeTrack(
            existing,
            "device-2"
        )

        assertEquals(
            listOf("device-1", "device-3"),
            result.map { it.mediaId }
        )
    }

    @Test
    fun trackWithoutUriIsIgnored() {
        val result = PlaylistRules.addTrack(
            emptyList(),
            track(1).copy(audioUri = "")
        )

        assertEquals(0, result.size)
    }
}
