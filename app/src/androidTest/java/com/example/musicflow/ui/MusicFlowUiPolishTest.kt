package com.example.musicflow.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.musicflow.core.lyrics.LyricsUiState
import com.example.musicflow.core.playback.PlayerUiState
import com.example.musicflow.feature.account.AccountScreen
import com.example.musicflow.feature.account.AccountUiState
import com.example.musicflow.feature.account.AccountAuthMode
import com.example.musicflow.feature.library.LibraryScreen
import com.example.musicflow.feature.library.LibraryUiState
import com.example.musicflow.feature.player.NowPlayingScreen
import com.example.musicflow.ui.theme.MusicFlowTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MusicFlowUiPolishTest {
    @get:Rule val compose = createComposeRule()

    @Test fun playerTransportRemainsClickableAtLargeFontOnNarrowScreen() {
        var previous = 0
        var next = 0
        var paused = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, 1.8f)
            ) {
                MusicFlowTheme {
                    Box(Modifier.width(310.dp).height(640.dp)) {
                        NowPlayingScreen(
                            state = PlayerUiState(
                                connected = true,
                                hasMedia = true,
                                isPlaying = true,
                                title = "Tên bài hát thử với tên rất dài",
                                artist = "Nghệ sĩ thử",
                                durationMs = 200_000,
                                positionMs = 20_000,
                                canPrevious = true,
                                canNext = true
                            ),
                            onBack = {},
                            onPickFiles = {},
                            onOpenLibrary = {},
                            onPlayPause = { paused++ },
                            onPrevious = { previous++ },
                            onNext = { next++ },
                            onSeek = {},
                            onToggleShuffle = {},
                            onCycleRepeat = {},
                            onSetPlaybackSpeed = {},
                            onSetSleepTimerMinutes = {},
                            onSetEqualizerEnabled = {},
                            onSetEqualizerBand = { _, _ -> },
                            onSetCrossfadeDuration = {},
                            lyrics = LyricsUiState(),
                            onPickLyrics = {}
                        )
                    }
                }
            }
        }
        compose.onNodeWithContentDescription("Vị trí phát nhạc").assertExists()
        // Main playback action must precede secondary queue/seek controls in
        // the scrolling layout. Scroll the lower control into view first:
        // boundsInRoot of an offscreen node can be clipped to an empty rect.
        compose.onNodeWithContentDescription("Vị trí phát nhạc")
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("player-play-pause-action").assertIsDisplayed()
        assertTrue(
            compose.onNodeWithTag("player-play-pause-action")
                .fetchSemanticsNode().boundsInRoot.top <
                compose.onNodeWithContentDescription("Vị trí phát nhạc")
                    .fetchSemanticsNode().boundsInRoot.top
        )
        compose.onNodeWithText("Trước").performScrollTo().performClick()
        compose.onNodeWithText("Tạm dừng").performScrollTo().performClick()
        compose.onNodeWithText("Tiếp").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(1, previous)
            assertEquals(1, paused)
            assertEquals(1, next)
        }
    }

    @Test fun guestThemeSelectorRemainsReachableAtLargeFont() {
        var selected = ""
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, 1.8f)
            ) {
                MusicFlowTheme {
                    Box(Modifier.width(310.dp).height(650.dp)) {
                        accountScreen(
                            state = AccountUiState(
                                loading = false,
                                configured = false
                            ),
                            onThemeModeChange = { selected = it }
                        )
                    }
                }
            }
        }
        compose.onNodeWithText("Tài khoản MusicFlow").assertExists()
        compose.onNodeWithTag("account-demo-tools-toggle")
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("account-theme-dark").assertIsDisplayed()
        assertTrue(
            compose.onNodeWithTag("account-theme-dark")
                .fetchSemanticsNode().boundsInRoot.top <
                compose.onNodeWithTag("account-demo-tools-toggle")
                    .fetchSemanticsNode().boundsInRoot.top
        )
        compose.onNodeWithTag("account-theme-dark").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("dark", selected) }
    }

    @Test fun libraryExplainsHorizontallyScrollableCategoriesAtLargeFont() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, 1.8f)
            ) {
                MusicFlowTheme {
                    Box(Modifier.width(310.dp).height(640.dp)) {
                        LibraryScreen(
                            uiState = LibraryUiState.Idle,
                            hasPermission = false,
                            onBack = {},
                            onPickManual = {},
                            onRefresh = {},
                            onSongSelected = { _, _ -> },
                            onAddToPlaylist = {}
                        )
                    }
                }
            }
        }
        compose.onNodeWithTag("library-categories-scroll-hint")
            .assertExists()
        compose.onNodeWithTag("library-category-genres")
            .performScrollTo().performClick()
        compose.onNodeWithTag("library-category-genres").assertExists()
    }

    @Test fun authButtonsStayReachableAtLargeFontWithoutCallingAuth() {
        var signIn = 0
        var signUp = 0
        compose.setContent {
            var mode by remember { mutableStateOf(AccountAuthMode.SIGN_IN) }
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, 1.8f)
            ) {
                MusicFlowTheme {
                    Box(Modifier.width(310.dp).height(650.dp)) {
                        accountScreen(
                            state = AccountUiState(
                                loading = false,
                                configured = true,
                                googleAvailable = false,
                                authMode = mode
                            ),
                            onThemeModeChange = {},
                            onSignIn = { signIn++ },
                            onSignUp = { signUp++ },
                            onAuthModeChange = { mode = it }
                        )
                    }
                }
            }
        }
        compose.onNodeWithTag("account-submit").performScrollTo().performClick()
        compose.onNodeWithTag("account-signup-tab").performScrollTo().performClick()
        compose.onNodeWithTag("account-submit").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(1, signIn)
            assertEquals(1, signUp)
        }
    }
}

@androidx.compose.runtime.Composable
private fun accountScreen(
    state: AccountUiState,
    onThemeModeChange: (String) -> Unit,
    onSignIn: () -> Unit = {},
    onSignUp: () -> Unit = {},
    onAuthModeChange: (AccountAuthMode) -> Unit = {}
) {
    AccountScreen(
        state = state,
        themeMode = "system",
        onThemeModeChange = onThemeModeChange,
        onEmailChange = {},
        onPasswordChange = {},
        onDisplayNameChange = {},
        onSignIn = onSignIn,
        onSignUp = onSignUp,
        onAuthModeChange = onAuthModeChange,
        onGoogleSignIn = {},
        onSaveProfile = {},
        onSignOut = {},
        onSyncLibrary = {},
        onSyncHistory = {},
        onRelink = { _, _ -> },
        onDeleteCloudPlaylist = {},
        onDeleteCloudFavorite = {}
    )
}
