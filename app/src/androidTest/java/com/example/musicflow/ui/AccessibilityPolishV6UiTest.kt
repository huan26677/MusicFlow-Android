package com.example.musicflow.ui

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.musicflow.core.lyrics.LyricsUiState
import com.example.musicflow.core.media.DeviceSong
import com.example.musicflow.core.playback.PlayerUiState
import com.example.musicflow.feature.account.AccountAuthMode
import com.example.musicflow.feature.account.AccountScreen
import com.example.musicflow.feature.account.AccountUiState
import com.example.musicflow.feature.home.HomeHistoryUiState
import com.example.musicflow.feature.home.HomeScreen
import com.example.musicflow.feature.library.SongRow
import com.example.musicflow.feature.player.NowPlayingScreen
import com.example.musicflow.ui.components.MusicFlowStatusKind
import com.example.musicflow.ui.components.MusicFlowStatusPanel
import com.example.musicflow.ui.components.OceanFlowHeader
import com.example.musicflow.ui.components.OceanFlowSectionTitle
import com.example.musicflow.ui.theme.MusicFlowTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** TalkBack semantic contracts + practical controls at 200% font, not a manual TalkBack audit. */
@RunWith(AndroidJUnit4::class)
class AccessibilityPolishV6UiTest {
    @get:Rule val compose = createComposeRule()
    private val isHeading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)

    @Test fun headingsStayNavigableAndFullTextAt200PercentInLightAndDark() {
        compose.setContent {
            val density = LocalDensity.current
            var dark by remember { mutableStateOf(false) }
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2.0f)) {
                MusicFlowTheme(darkTheme = dark) {
                    Column {
                        OceanFlowHeader(
                            "Ocean Flow", "Thư viện của bạn",
                            "Nghe nhạc offline ngay cả khi không có tài khoản"
                        )
                        OceanFlowSectionTitle("Bắt đầu nghe")
                        androidx.compose.material3.Button(onClick = { dark = !dark }) {
                            androidx.compose.material3.Text("Đổi giao diện")
                        }
                    }
                }
            }
        }
        compose.onNodeWithText("Thư viện của bạn").assert(isHeading)
        compose.onNodeWithText("Bắt đầu nghe").assert(isHeading)
        compose.onNodeWithText("Nghe nhạc offline ngay cả khi không có tài khoản")
            .assertExists()
        compose.onNodeWithText("Đổi giao diện").performClick()
        compose.onNodeWithText("Thư viện của bạn").assert(isHeading)
    }

    @Test fun statusPanelAnnouncesHeadingWithNoPermissionOrLoginRequired() {
        compose.setContent {
            MusicFlowTheme {
                MusicFlowStatusPanel(
                    MusicFlowStatusKind.EMPTY,
                    "Chưa có nhạc đã chọn", "Chọn tệp SAF để nghe ngay.",
                    actionLabel = "Chọn tệp", onAction = {}
                )
            }
        }
        compose.onNodeWithText("Chưa có nhạc đã chọn").assert(isHeading)
        compose.onNodeWithText("Chọn tệp").assertExists()
    }

    @Test fun songRowPlaybackIsNamedAndFavoriteStateExposedAt200Percent() {
        val song = DeviceSong(
            id = 10L,
            uri = Uri.parse("content://media/external/audio/media/10"),
            title = "Đường về xa", artist = "Nghệ sĩ QA", durationMs = 150_000L
        )
        var plays = 0
        var favorites = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2.0f)) {
                MusicFlowTheme {
                    Box(Modifier.width(300.dp).height(200.dp)) {
                        SongRow(
                            song = song, onClick = { plays++ },
                            onToggleFavorite = { favorites++ }
                        )
                    }
                }
            }
        }
        compose.onNodeWithContentDescription("Phát Đường về xa, Nghệ sĩ QA")
            .performClick()
        compose.onNodeWithContentDescription("Tùy chọn cho Đường về xa").performClick()
        compose.onNodeWithText("Yêu thích").performClick()
        compose.runOnIdle {
            assertEquals(1, plays)
            assertEquals(1, favorites)
        }
    }

    @Test fun playerTransportAndSeekSemanticsRemainReachableAt200Percent() {
        var paused = 0
        var next = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2.0f)) {
                MusicFlowTheme {
                    Box(Modifier.width(320.dp).height(690.dp)) {
                        NowPlayingScreen(
                            state = PlayerUiState(
                                connected = true, hasMedia = true, isPlaying = true,
                                title = "Đường xa qua núi",
                                artist = "Ca sĩ QA", positionMs = 31_000L,
                                durationMs = 120_000L, canNext = true
                            ),
                            onBack = {}, onPickFiles = {}, onOpenLibrary = {},
                            onPlayPause = { paused++ }, onPrevious = {}, onNext = { next++ },
                            onSeek = {}, onToggleShuffle = {}, onCycleRepeat = {},
                            onSetPlaybackSpeed = {}, onSetSleepTimerMinutes = {},
                            onSetEqualizerEnabled = {}, onSetEqualizerBand = { _, _ -> },
                            onSetCrossfadeDuration = {}, lyrics = LyricsUiState(),
                            onPickLyrics = {}
                        )
                    }
                }
            }
        }
        compose.onNodeWithText("Đường xa qua núi").assert(isHeading)
        compose.onNodeWithContentDescription("Vị trí phát nhạc").assertExists()
        compose.onNodeWithContentDescription("Tạm dừng Đường xa qua núi")
            .performScrollTo().performClick()
        compose.onNodeWithTag("player-next-action").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(1, paused)
            assertEquals(1, next)
        }
    }

    @Test fun accountModesRemainAccessibleAsSeparateRowsAt200Percent() {
        var signUps = 0
        var demoChanges = 0
        compose.setContent {
            var mode by remember { mutableStateOf(AccountAuthMode.SIGN_IN) }
            var demo by remember { mutableStateOf(false) }
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2.0f)) {
                MusicFlowTheme {
                    Box(Modifier.width(320.dp).height(720.dp)) {
                        AccountScreen(
                            state = AccountUiState(
                                loading = false, configured = true,
                                authMode = mode, demoMode = demo,
                                googleAvailable = false
                            ),
                            themeMode = "system",
                            onThemeModeChange = {}, onEmailChange = {},
                            onPasswordChange = {}, onDisplayNameChange = {},
                            onSignIn = {}, onSignUp = { signUps++ },
                            onGoogleSignIn = {}, onSaveProfile = {}, onSignOut = {},
                            onSyncLibrary = {}, onSyncHistory = {},
                            onRelink = { _, _ -> }, onDeleteCloudPlaylist = {},
                            onDeleteCloudFavorite = {},
                            onAuthModeChange = { mode = it },
                            onDemoModeChange = { demo = it; demoChanges++ }
                        )
                    }
                }
            }
        }
        compose.onNodeWithTag("account-auth-demo-mode").performScrollTo().performClick()
        compose.onNodeWithTag("account-signup-tab").performScrollTo().performClick()
        compose.onNodeWithTag("account-submit").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(1, demoChanges)
            assertEquals(1, signUps)
        }
    }

    @Test fun homeActionsAreAvailableAt200PercentWithoutAuth() {
        var library = 0
        var search = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2.0f)) {
                MusicFlowTheme {
                    Box(Modifier.width(320.dp).height(720.dp)) {
                        HomeScreen(
                            state = PlayerUiState(), historyState = HomeHistoryUiState.Empty,
                            onOpenLibrary = { library++ }, onOpenSearch = { search++ },
                            onOpenPlaylists = {}, onOpenPlayer = {},
                            onRecentSongSelected = { _, _ -> },
                            actionError = null, onDismissActionError = {},
                            onRemoveRecentSong = {}, onClearHistory = {}
                        )
                    }
                }
            }
        }
        compose.onNodeWithTag("home-library-action").performScrollTo().performClick()
        compose.onNodeWithTag("home-search-action").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(1, library)
            assertEquals(1, search)
        }
    }
}
