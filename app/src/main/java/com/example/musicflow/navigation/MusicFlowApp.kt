package com.example.musicflow.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.musicflow.core.playback.PlayerUiState
import com.example.musicflow.core.media.DeviceSong
import com.example.musicflow.feature.library.LibraryRoute
import com.example.musicflow.feature.player.MiniPlayer
import com.example.musicflow.feature.player.NowPlayingScreen
import com.example.musicflow.feature.search.SearchRoute
import kotlinx.serialization.Serializable

@Serializable
data object HomeTab : NavKey

@Serializable
data object SearchTab : NavKey

@Serializable
data object LibraryTab : NavKey

@Serializable
data object AccountTab : NavKey

@Serializable
data object NowPlaying : NavKey

@Composable
fun MusicFlowApp(
    state: PlayerUiState,
    onPickFiles: () -> Unit,
    onPlayDeviceSongs: (List<DeviceSong>, Int) -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit

) {
    val backStack = rememberNavBackStack(HomeTab)

    val currentRoute = backStack.last()
    val isNowPlaying = currentRoute == NowPlaying

    fun switchTab(tab: NavKey) {
        if (backStack.size != 1 || backStack.first() != tab) {
            backStack.clear()
            backStack.add(tab)
        }
    }

    fun openPlayer() {
        if (
            state.hasMedia &&
            backStack.lastOrNull() != NowPlaying
        ) {
            backStack.add(NowPlaying)
        }
    }

    Scaffold(
        bottomBar = {
            if (!isNowPlaying) {
                Column {
                    if (state.hasMedia) {
                        MiniPlayer(
                            state = state,
                            onOpenPlayer = {
                                openPlayer()
                            },
                            onPlayPause = onPlayPause
                        )
                    }

                    NavigationBar {
                        val tabs = listOf(
                            HomeTab,
                            SearchTab,
                            LibraryTab,
                            AccountTab
                        )

                        tabs.forEach { tab ->
                            val label = when (tab) {
                                HomeTab -> "Home"
                                SearchTab -> "Search"
                                LibraryTab -> "Library"
                                else -> "Account"
                            }

                            val symbol = when (tab) {
                                HomeTab -> "⌂"
                                SearchTab -> "⌕"
                                LibraryTab -> "♫"
                                else -> "⚙"
                            }

                            NavigationBarItem(
                                selected = currentRoute == tab,
                                onClick = { switchTab(tab) },
                                icon = { Text(symbol) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->

        NavDisplay(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),

            backStack = backStack,

            onBack = {
                if (backStack.size > 1) {
                    backStack.removeAt(backStack.lastIndex)
                }
            },

            entryProvider = entryProvider {

                entry<HomeTab> {
                    PlaceholderScreen(
                        title = "MusicFlow",
                        description =
                            "Trang chủ nhạc online sẽ được " +
                                    "kết nối với Supabase.",
                        actionLabel = "Mở nhạc thiết bị",
                        onAction = {
                            switchTab(LibraryTab)
                        }
                    )
                }

                entry<SearchTab> {
                    SearchRoute(
                        onSongSelected = { songs, index ->
                            if (songs.isNotEmpty()) {
                                onPlayDeviceSongs(songs, index)

                                // Mở Now Playing như khi chọn bài từ Library.
                                if (backStack.lastOrNull() != NowPlaying) {
                                    backStack.add(NowPlaying)
                                }
                            }
                        }
                    )
                }

                entry<LibraryTab> {
                    LibraryRoute(
                        onBack = {
                            switchTab(HomeTab)
                        },
                        onPickManual = onPickFiles,
                        onSongSelected = { songs, index ->
                            if (songs.isNotEmpty()) {
                                onPlayDeviceSongs(songs, index)

                                // Mở Now Playing dựa trên hành động chọn bài,
                                // không chờ StateFlow cập nhật hasMedia.
                                if (backStack.lastOrNull() != NowPlaying) {
                                    backStack.add(NowPlaying)
                                }
                            }
                        }
                    )
                }

                entry<AccountTab> {
                    PlaceholderScreen(
                        title = "Tài khoản",
                        description =
                            "Đăng nhập, đăng ký và cài đặt " +
                                    "giao diện sẽ được thêm sau."
                    )
                }

                entry<NowPlaying> {
                    NowPlayingScreen(
                        state = state,

                        onBack = {
                            if (backStack.size > 1) {
                                backStack.removeAt(
                                    backStack.lastIndex
                                )
                            }
                        },

                        onPickFiles = onPickFiles,

                        onOpenLibrary = {
                            switchTab(LibraryTab)
                        },

                        onPlayPause = onPlayPause,
                        onPrevious = onPrevious,
                        onNext = onNext,
                        onSeek = onSeek,
                        onToggleShuffle = onToggleShuffle,
                        onCycleRepeat = onCycleRepeat
                    )
                }
            }
        )
    }
}

@Composable
private fun PlaceholderScreen(
    title: String,
    description: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(12.dp))

        Text(description)

        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))

            Button(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}
