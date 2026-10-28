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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.musicflow.core.history.RecentSong
import com.example.musicflow.core.playback.PlayerUiState
import com.example.musicflow.core.lyrics.LyricsUiState
import com.example.musicflow.core.media.DeviceSong
import com.example.musicflow.core.playlist.PlaylistTrack

import com.example.musicflow.feature.home.HomeRoute
import com.example.musicflow.feature.account.AccountRoute
import com.example.musicflow.feature.library.LibraryRoute
import com.example.musicflow.feature.online.OnlineCatalogRoute
import com.example.musicflow.core.online.OnlineTrack
import com.example.musicflow.feature.playlist.PlaylistListRoute
import com.example.musicflow.feature.playlist.PlaylistDetailRoute
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
data class FavoritesRoute(
    val showAlbums: Boolean
) : NavKey

@Serializable
data object AccountTab : NavKey

@Serializable
data object NowPlaying : NavKey

@Serializable
data object PlaylistsRoute : NavKey

@Serializable
data object OnlineCatalogPage : NavKey

@Serializable
data class PlaylistDetail(
    val playlistId: String
) : NavKey

@Composable
fun MusicFlowApp(
    state: PlayerUiState,
    onPickFiles: () -> Unit,
    onPlayDeviceSongs: (List<DeviceSong>, Int) -> Unit,
    onPlayRecentSongs: (List<RecentSong>, Int) -> Unit,
    onPlayPlaylistTracks: (List<PlaylistTrack>, Int) -> Unit,
    onPlayOnlineTracks: (List<OnlineTrack>, Int) -> Unit,

    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    onSetSleepTimerMinutes: (Int) -> Unit,
    onSetEqualizerEnabled: (Boolean) -> Unit,
    onSetEqualizerBand: (Int, Int) -> Unit,
    onSetCrossfadeDuration: (Int) -> Unit,
    lyrics: LyricsUiState,
    onPickLyrics: () -> Unit,
    themeMode: String,
    onThemeModeChange: (String) -> Unit

) {
    var savedSearchQuery by rememberSaveable {
        mutableStateOf("")
    }

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
                                selected = currentRoute == tab ||
                                    (tab == LibraryTab && currentRoute is FavoritesRoute),
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

            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),

            onBack = {
                if (backStack.size > 1) {
                    backStack.removeAt(backStack.lastIndex)
                }
            },

            entryProvider = entryProvider {

                entry<HomeTab> {
                    HomeRoute(
                        state = state,

                        onOpenLibrary = {
                            switchTab(LibraryTab)
                        },

                        onOpenFavoriteSongs = {
                            backStack.add(FavoritesRoute(showAlbums = false))
                        },

                        onOpenFavoriteAlbums = {
                            backStack.add(FavoritesRoute(showAlbums = true))
                        },

                        onOpenSearch = {
                            switchTab(SearchTab)
                        },

                        onOpenOnlineCatalog = {
                            if (backStack.lastOrNull() != OnlineCatalogPage) {
                                backStack.add(OnlineCatalogPage)
                            }
                        },

                        onOpenPlaylists = {
                            if (backStack.lastOrNull() != PlaylistsRoute) {
                                backStack.add(PlaylistsRoute)
                            }
                        },

                        onOpenPlayer = {
                            openPlayer()
                        },

                        onRecentSongSelected = { songs, index ->
                            if (index in songs.indices) {
                                onPlayRecentSongs(songs, index)

                                // Mở Now Playing ngay sau khi chọn bài.
                                if (backStack.lastOrNull() != NowPlaying) {
                                    backStack.add(NowPlaying)
                                }
                            }
                        }
                    )
                }

                entry<SearchTab> {
                    SearchRoute(
                        initialQuery = savedSearchQuery,
                        onQueryRemembered = { query ->
                            savedSearchQuery = query
                        },
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

                entry<FavoritesRoute> { route ->
                    LibraryRoute(
                        initialFavoritesOnly = true,
                        initialShowAlbums = route.showAlbums,

                        onBack = {
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.lastIndex)
                            } else {
                                switchTab(HomeTab)
                            }
                        },

                        onPickManual = onPickFiles,

                        onSongSelected = { songs, index ->
                            if (index in songs.indices) {
                                onPlayDeviceSongs(songs, index)

                                if (backStack.lastOrNull() != NowPlaying) {
                                    backStack.add(NowPlaying)
                                }
                            }
                        }
                    )
                }

                entry<AccountTab> {
                    AccountRoute(
                        themeMode = themeMode,
                        onThemeModeChange = onThemeModeChange
                    )
                }

                entry<OnlineCatalogPage> {
                    OnlineCatalogRoute(
                        onBack = {
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.lastIndex)
                            } else {
                                switchTab(HomeTab)
                            }
                        },
                        onPlayTrack = { tracks, index ->
                            onPlayOnlineTracks(tracks, index)
                            if (backStack.lastOrNull() != NowPlaying) {
                                backStack.add(NowPlaying)
                            }
                        }
                    )
                }

                entry<PlaylistsRoute> {
                    PlaylistListRoute(
                        onBack = {
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        },
                        onOpenPlaylist = { playlistId ->
                            backStack.add(
                                PlaylistDetail(playlistId)
                            )
                        }
                    )
                }

                entry<PlaylistDetail> { route ->
                    PlaylistDetailRoute(
                        playlistId = route.playlistId,

                        onBack = {
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        },

                        onPlayTracks = { tracks, index ->
                            if (index in tracks.indices) {
                                onPlayPlaylistTracks(tracks, index)

                                if (backStack.lastOrNull() != NowPlaying) {
                                    backStack.add(NowPlaying)
                                }
                            }
                        }
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
                        onCycleRepeat = onCycleRepeat,
                        onSetPlaybackSpeed = onSetPlaybackSpeed,
                        onSetSleepTimerMinutes = onSetSleepTimerMinutes,
                        onSetEqualizerEnabled = onSetEqualizerEnabled,
                        onSetEqualizerBand = onSetEqualizerBand,
                        onSetCrossfadeDuration = onSetCrossfadeDuration,
                        lyrics = lyrics,
                        onPickLyrics = onPickLyrics
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
