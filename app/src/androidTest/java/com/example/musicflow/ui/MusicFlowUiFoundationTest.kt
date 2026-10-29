package com.example.musicflow.ui

import android.net.Uri
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import android.graphics.Color
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.musicflow.core.media.DeviceSong
import com.example.musicflow.core.playback.PlayerUiState
import com.example.musicflow.feature.library.LibraryScreen
import com.example.musicflow.feature.library.LibraryUiState
import com.example.musicflow.feature.online.OnlineCatalogScreen
import com.example.musicflow.feature.online.OnlineCatalogUiState
import com.example.musicflow.feature.player.MiniPlayer
import com.example.musicflow.feature.player.QueueManagerScreen
import com.example.musicflow.feature.search.SearchScreen
import com.example.musicflow.feature.search.SearchUiState
import com.example.musicflow.ui.components.MusicFlowStatusKind
import com.example.musicflow.ui.components.MusicFlowStatusPanel
import com.example.musicflow.ui.theme.MusicFlowTheme
import com.example.musicflow.ui.theme.MusicFlowBlue
import com.example.musicflow.ui.theme.MusicFlowBlueDark
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MusicFlowUiFoundationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun brandPrimaryColorsHaveReadableTextContrast() {
        val light = ColorUtils.calculateContrast(
            Color.WHITE, MusicFlowBlue.toArgb()
        )
        val dark = ColorUtils.calculateContrast(
            Color.rgb(11, 18, 32), MusicFlowBlueDark.toArgb()
        )
        org.junit.Assert.assertTrue("Light primary contrast $light", light >= 4.5)
        org.junit.Assert.assertTrue("Dark primary contrast $dark", dark >= 4.5)
    }

    @Test fun errorPanelHasAccessibleRetryAction() {
        var attempts = 0
        compose.setContent {
            MusicFlowTheme {
                MusicFlowStatusPanel(
                    kind = MusicFlowStatusKind.ERROR,
                    title = "Không thể đọc nhạc",
                    description = "Lỗi kiểm thử",
                    actionLabel = "Thử lại",
                    onAction = { attempts++ }
                )
            }
        }
        compose.onNodeWithTag("musicflow-status-error").assertExists()
        compose.onNodeWithText("Không thể đọc nhạc").assertExists()
        compose.onNodeWithText("Thử lại").performClick()
        compose.runOnIdle { assertEquals(1, attempts) }
    }

    @Test fun libraryEmptyHasGuidanceAndPickerRemainsAvailable() {
        var picked = 0
        compose.setContent {
            MusicFlowTheme {
                LibraryScreen(
                    uiState = LibraryUiState.Empty,
                    hasPermission = true,
                    onBack = {},
                    onPickManual = { picked++ },
                    onRefresh = {},
                    onSongSelected = { _, _ -> },
                    onAddToPlaylist = {}
                )
            }
        }
        compose.onNodeWithTag("musicflow-status-empty").assertExists()
        compose.onNodeWithText("Thư viện chưa có nhạc").assertExists()
        // The toolbar picker is always available without any account.
        compose.onAllNodesWithText("Chọn MP3").assertCountEquals(1)
        compose.onAllNodesWithText("Chọn MP3")[0].performClick()
        compose.runOnIdle { assertEquals(1, picked) }
    }

    @Test fun libraryPermissionStateExplainsManualPicker() {
        var permissionRequests = 0
        compose.setContent {
            LibraryScreen(
                uiState = LibraryUiState.Idle,
                hasPermission = false,
                onBack = {},
                onPickManual = {},
                onRefresh = { permissionRequests++ },
                onSongSelected = { _, _ -> },
                onAddToPlaylist = {}
            )
        }
        compose.onNodeWithTag("musicflow-status-permission").assertExists()
        compose.onNodeWithText("Cấp quyền").performClick()
        compose.runOnIdle { assertEquals(1, permissionRequests) }
    }

    @Test fun searchErrorRetainsRetryAction() {
        var retries = 0
        compose.setContent {
            SearchScreen(
                state = SearchUiState(error = "Không đọc được MediaStore"),
                hasPermission = true,
                onQueryChange = {},
                onRequestPermission = {},
                onRefresh = { retries++ },
                onSongSelected = { _, _ -> },
                onAddToPlaylist = {}
            )
        }
        compose.onNodeWithTag("musicflow-status-error").assertExists()
        compose.onNodeWithText("Thử lại").performClick()
        compose.runOnIdle { assertEquals(1, retries) }
    }

    @Test fun searchClearButtonHasTalkBackLabel() {
        var queryAfterClear = "not cleared"
        val track = DeviceSong(
            id = 1L,
            uri = Uri.parse("content://media/external/audio/media/1"),
            title = "Nhạc thử",
            artist = "Nghệ sĩ",
            durationMs = 2000
        )
        compose.setContent {
            SearchScreen(
                state = SearchUiState(
                    query = "abc",
                    songs = listOf(track),
                    results = emptyList(),
                    isLoaded = true
                ),
                hasPermission = true,
                onQueryChange = { queryAfterClear = it },
                onRequestPermission = {},
                onRefresh = {},
                onSongSelected = { _, _ -> },
                onAddToPlaylist = {}
            )
        }
        compose.onNodeWithContentDescription("Xóa nội dung tìm kiếm").performClick()
        compose.runOnIdle { assertEquals("", queryAfterClear) }
        compose.onNodeWithTag("musicflow-status-empty").assertExists()
    }

    @Test fun onlineCatalogEmptyCanRefreshWithoutAccount() {
        var refreshes = 0
        compose.setContent {
            OnlineCatalogScreen(
                state = OnlineCatalogUiState(
                    savedEndpoint = "https://example.com",
                    endpointInput = "https://example.com",
                    isLoaded = true
                ),
                onBack = {},
                onEndpointChange = {},
                onSaveEndpoint = {},
                onClearEndpoint = {},
                onRefresh = { refreshes++ },
                onLoadMore = {},
                onPlayTrack = { _, _ -> }
            )
        }
        compose.onNodeWithText("Chưa có bài hát online").assertExists()
        compose.onNodeWithTag("musicflow-status-empty").assertExists()
        compose.onNodeWithText("Tải lại").performClick()
        compose.runOnIdle { assertEquals(1, refreshes) }
    }

    @Test fun onlineCatalogErrorHasRetryAction() {
        var refreshes = 0
        compose.setContent {
            OnlineCatalogScreen(
                state = OnlineCatalogUiState(
                    savedEndpoint = "https://example.com",
                    endpointInput = "https://example.com",
                    isLoaded = true,
                    error = "Không kết nối được"
                ),
                onBack = {},
                onEndpointChange = {},
                onSaveEndpoint = {},
                onClearEndpoint = {},
                onRefresh = { refreshes++ },
                onLoadMore = {},
                onPlayTrack = { _, _ -> }
            )
        }
        compose.onNodeWithTag("musicflow-status-error").assertExists()
        compose.onNodeWithText("Thử lại").performClick()
        compose.runOnIdle { assertEquals(1, refreshes) }
    }

    @Test fun queueEmptyStateIsClearAndDoesNotStopPlayback() {
        var queueActions = 0
        compose.setContent {
            QueueManagerScreen(
                state = PlayerUiState(connected = true, canEditQueue = true),
                onBack = {},
                onMove = { _, _, _ -> queueActions++; false },
                onRemove = { _, _ -> queueActions++; false },
                onClearOthers = { queueActions++; false }
            )
        }
        compose.onNodeWithTag("musicflow-status-empty").assertExists()
        compose.onNodeWithText("Hàng đợi đang trống").assertExists()
        compose.runOnIdle { assertEquals(0, queueActions) }
    }

    @Test fun miniPlayerAnnouncesTrackAndPlayState() {
        var paused = 0
        var opened = 0
        compose.setContent {
            MiniPlayer(
                state = PlayerUiState(
                    connected = true,
                    hasMedia = true,
                    isPlaying = true,
                    title = "Tên bài",
                    artist = "Nghệ sĩ"
                ),
                onOpenPlayer = { opened++ },
                onPlayPause = { paused++ }
            )
        }
        compose.onNodeWithContentDescription("Mở trình phát: Tên bài, Nghệ sĩ").performClick()
        compose.onNodeWithContentDescription("Tạm dừng nhạc").performClick()
        compose.runOnIdle {
            assertEquals(1, opened)
            assertEquals(1, paused)
        }
    }
}
