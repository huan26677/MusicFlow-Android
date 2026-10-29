package com.example.musicflow.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MediaSourceContractTest {
    @Test fun deviceLegacyMediaIdIsStable() {
        val ref = MediaSourceContract.device(123L, null)
        assertEquals("device-123", ref.mediaId)
        assertEquals(MediaSourceKind.MEDIA_STORE, ref.kind)
    }

    @Test fun explicitDeviceMediaIdIsNotOverwritten() {
        assertEquals("custom-123", MediaSourceContract.device(123, "custom-123").mediaId)
    }

    @Test fun pickerIdIsTheExistingUriString() {
        val raw = "content://com.android.providers.downloads.documents/document/27%3A003"
        val ref = MediaSourceContract.picked(raw)
        assertEquals(raw, ref.mediaId)
        assertEquals(MediaSourceKind.SAF_DOCUMENT, ref.kind)
    }

    @Test fun syntheticFavoriteUsingRawContentIdClassifiesAsSaf() {
        assertEquals(
            MediaSourceKind.SAF_DOCUMENT,
            MediaSourceContract.device(-1, "content://test/sound/1").kind
        )
    }

    @Test fun explicitFileUriClassifiesAsPickedAudio() {
        val ref = MediaSourceContract.device(-2, "file:///tmp/old-qa-track.wav")
        assertEquals(MediaSourceKind.SAF_DOCUMENT, ref.kind)
        assertEquals("file:///tmp/old-qa-track.wav", ref.mediaId)
    }

    @Test fun pickerCompatibilityAcceptsLegacyFileUris() {
        assertEquals(
            "file:///sdcard/song.mp3",
            MediaSourceContract.picked("file:///sdcard/song.mp3").mediaId
        )
    }

    @Test fun pickerCannotBecomeArbitraryRemoteUrl() {
        assertThrows(IllegalArgumentException::class.java) {
            MediaSourceContract.picked("https://evil.example.com/file.mp3")
        }
    }

    @Test fun onlineIdUsesExistingPrefix() {
        val ref = MediaSourceContract.online("uuid:42")
        assertEquals("online:uuid:42", ref.mediaId)
        assertEquals(MediaSourceKind.LICENSED_ONLINE, ref.kind)
    }

    @Test fun onlineIdCannotBeEmpty() {
        assertThrows(IllegalArgumentException::class.java) {
            MediaSourceContract.online(" ")
        }
    }

    @Test fun storedLegacyIdIsPreserved() {
        val ref = MediaSourceContract.stored("qa-service-custom-ID")
        assertEquals("qa-service-custom-ID", ref.mediaId)
        assertEquals(MediaSourceKind.STORED_LEGACY, ref.kind)
    }

    @Test fun storedKnownIdsArePreserved() {
        listOf(
            "device-55" to MediaSourceKind.MEDIA_STORE,
            "content://provider/a%2Fb" to MediaSourceKind.SAF_DOCUMENT,
            "file:///cache/x.wav" to MediaSourceKind.SAF_DOCUMENT,
            "online:song42" to MediaSourceKind.LICENSED_ONLINE,
            "device-not-a-number" to MediaSourceKind.STORED_LEGACY
        ).forEach { (id, kind) ->
            assertEquals(id, MediaSourceContract.stored(id).mediaId)
            assertEquals(kind, MediaSourceContract.stored(id).kind)
        }
    }
}
