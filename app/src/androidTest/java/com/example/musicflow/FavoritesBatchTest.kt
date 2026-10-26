package com.example.musicflow

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.musicflow.core.favorites.FavoriteSongDetails
import com.example.musicflow.core.favorites.LocalFavoritesStore
import com.example.musicflow.core.media.DeviceSong
import com.example.musicflow.core.playback.PlayerUiState
import com.example.musicflow.feature.home.HomeHistoryUiState
import com.example.musicflow.feature.home.HomeScreen
import com.example.musicflow.feature.library.LibraryScreen
import com.example.musicflow.feature.library.LibraryUiState
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FavoritesBatchTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context
        get() = InstrumentationRegistry
            .getInstrumentation().targetContext

    @Test
    fun favoritesPersistSongAndAlbum() {
        val store = LocalFavoritesStore.getInstance(context)
        val uri = Uri.parse(
            "content://com.example.musicflow.qa/${System.nanoTime()}"
        )
        val albumId = System.currentTimeMillis()

        try {
            store.toggleSong(uri, "QA Favorite", "QA Artist")
            store.toggleAlbum(albumId)

            assertTrue(uri.toString() in store.songUris.value)
            assertTrue(albumId.toString() in store.albumIds.value)

            val prefs = context.getSharedPreferences(
                "musicflow_favorites", Context.MODE_PRIVATE
            )

            assertTrue(
                uri.toString() in prefs.getStringSet(
                    "song_uris", emptySet()
                ).orEmpty()
            )

            val json = JSONObject(
                prefs.getString("song_details", "{}") ?: "{}"
            )
            assertEquals(
                "QA Favorite",
                json.getJSONObject(uri.toString()).getString("title")
            )

            Log.i(
                "MusicFlowFavoritesQA",
                "PASS favoritesPersistence"
            )
        } finally {
            if (uri.toString() in store.songUris.value) {
                store.toggleSong(uri)
            }
            if (albumId.toString() in store.albumIds.value) {
                store.toggleAlbum(albumId)
            }
        }
    }
    @Test
    fun favoriteLibraryAndAlbum() {
        val song = DeviceSong(
            id = 900001L,
            uri = Uri.parse(
                "content://media/external/audio/media/900001"
            ),
            title = "QA Batch Song",
            artist = "QA Artist",
            durationMs = 30_000L,
            albumId = 400001L,
            album = "QA Batch Album"
        )

        val favoriteSongs = mutableStateOf(
            setOf(song.uri.toString())
        )

        var selectedUri: Uri? = null

        composeRule.setContent {
            LibraryScreen(
                uiState = LibraryUiState.Success(listOf(song)),
                hasPermission = true,
                onBack = {},
                onPickManual = {},
                onRefresh = {},
                onSongSelected = { songs, index ->
                    selectedUri = songs[index].uri
                },
                onAddToPlaylist = {},
                favoriteSongUris = favoriteSongs.value,
                onToggleFavorite = {
                    favoriteSongs.value =
                        favoriteSongs.value - it.uri.toString()
                },
                favoriteAlbumIds = setOf("400001"),
                onToggleAlbum = {},
                initialFavoritesOnly = true
            )
        }

        composeRule.onNodeWithText("QA Batch Song")
            .assertExists()

        composeRule.onNodeWithContentDescription(
            "Bỏ yêu thích QA Batch Song"
        ).performClick()

        composeRule.onNodeWithText("QA Batch Song")
            .assertDoesNotExist()

        composeRule.onNodeWithText("Album")
            .performClick()

        composeRule.onNodeWithText("QA Batch Album")
            .performClick()

        composeRule.onNodeWithText("Phát toàn bộ album")
            .performClick()

        composeRule.runOnIdle {
            assertEquals(song.uri, selectedUri)
        }

        Log.i(
            "MusicFlowFavoritesQA",
            "PASS favoriteLibraryAndAlbum"
        )
    }

    @Test
    fun safFavoriteWithoutMediaStorePermission() {
        val rawUri =
            "content://com.android.providers.downloads.documents/" +
                "document/qa_favorite"

        var selected: DeviceSong? = null

        composeRule.setContent {
            LibraryScreen(
                uiState = LibraryUiState.Idle,
                hasPermission = false,
                onBack = {},
                onPickManual = {},
                onRefresh = {},
                onSongSelected = { songs, index ->
                    selected = songs[index]
                },
                onAddToPlaylist = {},
                favoriteSongUris = setOf(rawUri),
                favoriteSongDetails = mapOf(
                    rawUri to FavoriteSongDetails(
                        "QA SAF Song",
                        "QA SAF Artist"
                    )
                ),
                initialFavoritesOnly = true
            )
        }

        composeRule.onNodeWithText("QA SAF Song")
            .performClick()

        composeRule.runOnIdle {
            assertEquals(rawUri, selected?.uri.toString())
            assertEquals(rawUri, selected?.mediaId)
        }

        Log.i(
            "MusicFlowFavoritesQA",
            "PASS safFavoriteWithoutMediaStorePermission"
        )
    }

    @Test
    fun homeFavoritesShortcuts() {
        var songsOpened = false
        var albumsOpened = false

        composeRule.setContent {
            HomeScreen(
                state = PlayerUiState(),
                historyState = HomeHistoryUiState.Empty,
                onOpenLibrary = {},
                onOpenSearch = {},
                onOpenPlaylists = {},
                onOpenPlayer = {},
                onRecentSongSelected = { _, _ -> },
                favoriteSongCount = 3,
                favoriteAlbumCount = 2,
                onOpenFavoriteSongs = { songsOpened = true },
                onOpenFavoriteAlbums = { albumsOpened = true },
                actionError = null,
                onDismissActionError = {},
                onRemoveRecentSong = {},
                onClearHistory = {}
            )
        }

        composeRule.onNodeWithText("♥ Bài hát yêu thích (3)")
            .performScrollTo()
            .performClick()

        composeRule.onNodeWithText("♥ Album yêu thích (2)")
            .performScrollTo()
            .performClick()

        composeRule.runOnIdle {
            assertTrue(songsOpened)
            assertTrue(albumsOpened)
        }

        Log.i(
            "MusicFlowFavoritesQA",
            "PASS homeFavoritesShortcuts"
        )
    }
}

