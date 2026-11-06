package com.example.musicflow.core.playlist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistQuickAddRulesTest {
    @Test fun scannedMusicUsesSameKeyAsStoredPlaylistTrack() {
        assertEquals("device-42", PlaylistQuickAddRules.mediaId(null, 42))
        assertEquals("device-42", PlaylistQuickAddRules.mediaId("  ", 42))
        assertFalse(PlaylistQuickAddRules.canAdd(null, 42, setOf("device-42"), false))
    }

    @Test fun safFileUsesItsPersistedContentUriToPreventDuplicates() {
        val uri = "content://com.android.providers.media.documents/document/audio%3A42"
        assertEquals(uri, PlaylistQuickAddRules.mediaId(uri, -1))
        assertFalse(PlaylistQuickAddRules.canAdd(uri, -1, setOf(uri), false))
        assertTrue(PlaylistQuickAddRules.canAdd(uri, -1, emptySet(), false))
    }

    @Test fun addingIsLockedWhileAnotherWriteIsPending() {
        assertFalse(PlaylistQuickAddRules.canAdd("musicflow-one", 1, emptySet(), true))
        assertTrue(PlaylistQuickAddRules.canAdd("musicflow-one", 1, emptySet(), false))
    }

    @Test fun searchIsAccentInsensitiveAndBlankShowsEverything() {
        assertTrue(PlaylistQuickAddRules.match("Đường về nhà", "Đen Vâu", "duong nha"))
        assertTrue(PlaylistQuickAddRules.match("Bài hát", "Nghệ sĩ", "  "))
        assertFalse(PlaylistQuickAddRules.match("Đường về nhà", "Đen Vâu", "mưa xuân"))
    }
}
