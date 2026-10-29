package com.example.musicflow.core.media

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.musicflow.core.online.OnlineTrack
import com.example.musicflow.core.online.OnlineTrackRights
import com.example.musicflow.core.playlist.PlaylistMediaItems
import com.example.musicflow.core.playlist.PlaylistTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackMediaItemsTest {
    private val rights = OnlineTrackRights(
        licenseCode = "CC-BY-4.0",
        licenseUrl = "https://creativecommons.org/licenses/by/4.0/",
        attribution = "MusicFlow QA artist — track",
        sourceUrl = "https://example.com/source",
        rightsConfirmed = true,
        streamingAllowed = true,
        verifiedAt = "2026-10-29T00:00:00Z"
    )
    @Test fun deviceMediaItemRetainsLegacyIdAndMetadata() {
        val uri = Uri.parse("content://media/external/audio/media/123")
        val item = TrackMediaItems.fromDevice(
            DeviceSong(123, uri, "Title", "Artist", 1200L)
        )
        assertEquals("device-123", item.mediaId)
        assertEquals(uri, item.localConfiguration?.uri)
        assertEquals("Title", item.mediaMetadata.title.toString())
        assertEquals("Artist", item.mediaMetadata.artist.toString())
    }

    @Test fun pickerAndStoredItemsRetainExactUriAndId() {
        val uri = Uri.parse("content://com.android.providers.downloads.documents/document/123%3A003")
        val picked = TrackMediaItems.fromPicked(uri, "Chosen", null)
        assertEquals(uri.toString(), picked.mediaId)
        assertEquals(uri, picked.localConfiguration?.uri)
        val restored = TrackMediaItems.fromStored(
            "my-legacy-id", uri.toString(), "Chosen", "Old Artist"
        )
        assertEquals("my-legacy-id", restored.mediaId)
        assertEquals(uri, restored.localConfiguration?.uri)
        assertEquals("Old Artist", restored.mediaMetadata.artist.toString())
    }

    @Test fun onlineUsesSameIdAndPreservesSignedQuery() {
        val url = "https://example.com/song.mp3?sig=abc&expires=123"
        val item = TrackMediaItems.fromOnlineOrNull(
            OnlineTrack("42", "Online", "Artist", "Album", 1000L, url, rights)
        )
        assertNotNull(item)
        assertEquals("online:42", item?.mediaId)
        assertEquals(url, item?.localConfiguration?.uri.toString())
        assertEquals("Album", item?.mediaMetadata?.albumTitle.toString())
    }

    @Test fun onlineRejectsNonHttpsAndCredentialOrFragmentUrls() {
        val invalid = listOf(
            "http://example.com/file.mp3",
            "file:///storage/music.mp3",
            "https://user:pass@example.com/file.mp3",
            "https://example.com/file.mp3#fragment",
            "https://"
        )
        invalid.forEach { raw ->
            assertFalse(raw, TrackMediaItems.isSafeHttpsMediaUrl(raw))
            assertNull(TrackMediaItems.fromOnlineOrNull(
                OnlineTrack("42", "Online", "Artist", null, 1000L, raw)
            ))
        }
    }

    @Test fun onlineSelectionIsStableWithDuplicateIdsAndUnplayableTracks() {
        val tracks = listOf(
            OnlineTrack("same", "Unplayable", "Artist", null, 0L, null),
            OnlineTrack("same", "Playable 1", "Artist", null, 0L, "https://example.com/a.mp3", rights),
            OnlineTrack("same", "Playable 2", "Artist", null, 0L, "https://example.com/b.mp3", rights)
        )
        val selected = TrackMediaItems.onlineQueue(tracks, 2)
        assertNotNull(selected)
        assertEquals(1, selected?.startIndex)
        assertEquals(2, selected?.items?.size)
        assertEquals("Playable 2", selected?.items?.get(1)?.mediaMetadata?.title.toString())
        assertNull(TrackMediaItems.onlineQueue(tracks, 0))
        assertNull(TrackMediaItems.onlineQueue(tracks, 9))
        assertTrue(TrackMediaItems.onlineQueue(tracks, 1)!!.items.isNotEmpty())
    }

    @Test fun unverifiedOnlineIsRejectedEvenWithHttpsUrl() {
        val url = "https://example.com/track.mp3"
        assertNull(TrackMediaItems.fromOnlineOrNull(
            OnlineTrack("1", "Unreviewed", "Artist", null, 100L, url)))
        assertNull(TrackMediaItems.fromOnlineOrNull(
            OnlineTrack("2", "Disallowed", "Artist", null, 100L, url,
                rights.copy(streamingAllowed = false))))
    }

    @Test fun oldStoredOnlineUrlsCannotBypassRights() {
        assertNull(TrackMediaItems.fromStoredOrNull(
            "online:old-id", "https://example.com/old.mp3", "Legacy", "Artist"))
        assertNull(TrackMediaItems.fromStoredOrNull(
            "custom-id", "https://example.com/old.mp3", "Legacy", "Artist"))
        assertTrue(TrackMediaItems.isSafeStoredLocal(
            "online:old-id", "content://media/external/audio/media/1"))
    }

    @Test fun personalDownloadedOfflineFilesPlayEvenWithLegacyOnlineIds() {
        listOf(
            "content://media/external/audio/media/99",
            "content://com.android.providers.downloads.documents/document/33",
            "file:///storage/emulated/0/Music/my-download.mp3",
            "/storage/emulated/0/Download/my-download.mp3"
        ).forEach { localUri ->
            assertTrue(
                "Offline URI should not require online rights: $localUri",
                TrackMediaItems.isSafeStoredLocal("online:previous-track", localUri)
            )
            val restored = TrackMediaItems.fromStoredOrNull(
                "online:previous-track",
                localUri,
                "Bài nhạc tải về",
                "Nghệ sĩ"
            )
            assertNotNull(restored)
            assertEquals("online:previous-track", restored?.mediaId)
            assertEquals(localUri, restored?.localConfiguration?.uri.toString())
        }
        assertFalse(TrackMediaItems.isSafeStoredLocal(
            "offline-custom", "https://example.com/song.mp3"
        ))
        assertFalse(TrackMediaItems.isSafeStoredLocal(
            "offline-custom", "http://example.com/song.mp3"
        ))
    }

    @Test fun playlistSelectionKeepsLocalIndicesAndSkipsOldOnlineRecords() {
        val tracks = listOf(
            PlaylistTrack("online:old", "https://example.com/old.mp3", "Remote", "Artist", 0),
            PlaylistTrack("device-12", "content://media/external/audio/media/12",
                "Local A", "Artist", 1),
            PlaylistTrack("device-13", "content://media/external/audio/media/13",
                "Local B", "Artist", 2)
        )
        assertNull(PlaylistMediaItems.playableSelection(tracks, 0))
        val playable = PlaylistMediaItems.playableSelection(tracks, 2)
        assertNotNull(playable)
        assertEquals(1, playable?.second)
        assertEquals(listOf("device-12", "device-13"),
            playable?.first?.map { it.mediaId })
    }
}
