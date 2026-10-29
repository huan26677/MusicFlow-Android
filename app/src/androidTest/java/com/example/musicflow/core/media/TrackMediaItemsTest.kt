package com.example.musicflow.core.media

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.musicflow.core.online.OnlineTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackMediaItemsTest {
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
            OnlineTrack("42", "Online", "Artist", "Album", 1000L, url)
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
            OnlineTrack("same", "Playable 1", "Artist", null, 0L, "https://example.com/a.mp3"),
            OnlineTrack("same", "Playable 2", "Artist", null, 0L, "https://example.com/b.mp3")
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
}
