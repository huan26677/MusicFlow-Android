package com.example.musicflow.core.playlist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure validation never needs an Android database or a network connection. */
class PlaylistBackupRulesTest {
    private val track = PlaylistTrack(
        mediaId = "device-42",
        audioUri = "content://media/external/audio/media/42",
        title = "Nhạc Việt",
        artist = "Nghệ sĩ QA",
        addedAtMs = 1L
    )

    @Test fun acceptsLocalMetadataAndEmptyPlaylists() {
        assertEquals(1, PlaylistLocalBackup.validate(
            listOf(PlaylistBackupEntry("Đường về", listOf(track)))
        ).size)
        assertTrue(PlaylistLocalBackup.validate(
            listOf(PlaylistBackupEntry("Trống", emptyList()))
        ).first().tracks.isEmpty())
    }

    @Test fun refusesRemoteStreamingLinksAndDuplicateTracks() {
        assertThrows(IllegalArgumentException::class.java) {
            PlaylistLocalBackup.validate(listOf(PlaylistBackupEntry("Remote", listOf(
                track.copy(audioUri = "https://example.invalid/song.mp3")
            ))))
        }
        assertThrows(IllegalArgumentException::class.java) {
            PlaylistLocalBackup.validate(listOf(PlaylistBackupEntry("Trùng", listOf(track, track))))
        }
    }

    @Test fun refusesOversizedOrMalformedPlaylistMetadata() {
        assertThrows(IllegalArgumentException::class.java) {
            PlaylistLocalBackup.validate(listOf(PlaylistBackupEntry("", emptyList())))
        }
        assertThrows(IllegalArgumentException::class.java) {
            PlaylistLocalBackup.validate((1..101).map {
                PlaylistBackupEntry("Danh sách $it", emptyList())
            })
        }
        assertThrows(IllegalArgumentException::class.java) {
            PlaylistLocalBackup.validate(listOf(PlaylistBackupEntry("Bài", listOf(
                track.copy(title = "a".repeat(181))
            ))))
        }
    }
}
