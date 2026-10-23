
package com.example.musicflow

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import androidx.media3.common.util.UnstableApi
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay

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
                        Surface(
                            color = MaterialTheme.colorScheme
                                .surfaceContainerHigh
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { openPlayer() }
                                    .padding(
                                        horizontal = 16.dp,
                                        vertical = 8.dp
                                    ),
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {
                                SongArtwork(
                                    uri = state.artworkUri,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )

                                Spacer(Modifier.width(12.dp))

                                Text(
                                    text = state.title,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                TextButton(onClick = onPlayPause) {
                                    Text(
                                        if (state.isPlaying) {
                                            "Tạm dừng"
                                        } else {
                                            "Phát"
                                        }
                                    )
                                }
                            }
                        }
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
                    PlaceholderScreen(
                        title = "Tìm kiếm",
                        description =
                            "Bộ lọc nhạc online và nhạc " +
                                    "thiết bị sẽ được thêm sau."
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
