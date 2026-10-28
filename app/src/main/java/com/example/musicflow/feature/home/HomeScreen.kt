
package com.example.musicflow.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

import com.example.musicflow.core.history.RecentSong
import com.example.musicflow.core.history.RoomRecentHistoryRepository
import com.example.musicflow.core.history.local.RecentHistoryDatabase
import com.example.musicflow.core.playback.PlayerUiState
import com.example.musicflow.core.favorites.LocalFavoritesStore

@Composable
fun HomeRoute(
    state: PlayerUiState,
    onOpenLibrary: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenPlaylists: () -> Unit,
    onOpenPlayer: () -> Unit,
    onRecentSongSelected: (List<RecentSong>, Int) -> Unit,
    onOpenFavoriteSongs: () -> Unit,
    onOpenFavoriteAlbums: () -> Unit,
    onOpenOnlineCatalog: () -> Unit = {}
) {
    val context = LocalContext.current

    val favoritesStore = remember(context) {
        LocalFavoritesStore.getInstance(context)
    }

    val favoriteSongUris by favoritesStore.songUris
        .collectAsStateWithLifecycle()

    val favoriteAlbumIds by favoritesStore.albumIds
        .collectAsStateWithLifecycle()

    val repository = remember(context) {
        RoomRecentHistoryRepository(
            RecentHistoryDatabase.getInstance(
                context.applicationContext
            )
        )
    }

    val factory = remember(repository) {
        HomeViewModel.factory(repository)
    }

    val homeViewModel: HomeViewModel =
        viewModel(factory = factory)

    val historyState by homeViewModel.historyState
        .collectAsStateWithLifecycle()

    val actionError by homeViewModel.actionError
        .collectAsStateWithLifecycle()

    HomeScreen(
        state = state,
        historyState = historyState,
        onOpenLibrary = onOpenLibrary,
        onOpenSearch = onOpenSearch,
        onOpenPlaylists = onOpenPlaylists,
        onOpenPlayer = onOpenPlayer,
        onRecentSongSelected = onRecentSongSelected,
        favoriteSongCount = favoriteSongUris.size,
        favoriteAlbumCount = favoriteAlbumIds.size,
        onOpenFavoriteSongs = onOpenFavoriteSongs,
        onOpenFavoriteAlbums = onOpenFavoriteAlbums,
        onOpenOnlineCatalog = onOpenOnlineCatalog,
        actionError = actionError,
        onDismissActionError = homeViewModel::dismissActionError,
        onRemoveRecentSong = homeViewModel::removeRecentSong,
        onClearHistory = homeViewModel::clearRecentHistory
    )
}

@Composable
fun HomeScreen(
    state: PlayerUiState,
    historyState: HomeHistoryUiState,
    onOpenLibrary: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenPlaylists: () -> Unit,
    onOpenPlayer: () -> Unit,
    onRecentSongSelected: (List<RecentSong>, Int) -> Unit,
    favoriteSongCount: Int = 0,
    favoriteAlbumCount: Int = 0,
    onOpenFavoriteSongs: () -> Unit = {},
    onOpenFavoriteAlbums: () -> Unit = {},
    actionError: String?,
    onDismissActionError: () -> Unit,
    onRemoveRecentSong: (String) -> Unit,
    onClearHistory: () -> Unit,
    onOpenOnlineCatalog: () -> Unit = {}
) {
    var showClearDialog by rememberSaveable {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "MusicFlow",
            style = MaterialTheme.typography.headlineLarge
        )

        Text(
            text = "Khám phá và thưởng thức nhạc của bạn.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Khám phá nhạc",
                    style = MaterialTheme.typography.titleLarge
                )

                Button(
                    onClick = onOpenLibrary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Mở thư viện thiết bị")
                }

                OutlinedButton(
                    onClick = onOpenSearch,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tìm kiếm bài hát")
                }

                OutlinedButton(
                    onClick = onOpenPlaylists,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Danh sách phát của tôi")
                }

                OutlinedButton(
                    onClick = onOpenOnlineCatalog,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Khám phá nhạc online")
                }

                HorizontalDivider()

                Text(
                    text = "Yêu thích của tôi",
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedButton(
                    onClick = onOpenFavoriteSongs,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("♥ Bài hát yêu thích ($favoriteSongCount)")
                }

                OutlinedButton(
                    onClick = onOpenFavoriteAlbums,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("♥ Album yêu thích ($favoriteAlbumCount)")
                }
            }
        }

        if (state.hasMedia) {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Bài hát hiện tại",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = state.title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = state.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = state.status,
                        style = MaterialTheme.typography.bodySmall
                    )

                    TextButton(onClick = onOpenPlayer) {
                        Text("Mở Now Playing")
                    }
                }
            }
        } else {
            Text(
                text = "Chưa có bài hát nào đang được chọn.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Nghe gần đây",
                style = MaterialTheme.typography.titleLarge
            )

            if (historyState is HomeHistoryUiState.Success) {
                TextButton(
                    onClick = { showClearDialog = true }
                ) {
                    Text("Xóa tất cả")
                }
            }
        }

        if (actionError != null) {
            Text(
                text = actionError,
                color = MaterialTheme.colorScheme.error
            )

            TextButton(onClick = onDismissActionError) {
                Text("Đóng thông báo")
            }
        }

        when (val history = historyState) {

            HomeHistoryUiState.Loading -> {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Đang tải lịch sử nghe nhạc...")
            }

            HomeHistoryUiState.Empty -> {
                Text(
                    text = "Chưa có bài hát nào trong lịch sử. " +
                            "Hãy mở Library hoặc Search để nghe nhạc.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            is HomeHistoryUiState.Error -> {
                Text(
                    text = history.message,
                    color = MaterialTheme.colorScheme.error
                )
            }

            is HomeHistoryUiState.Success -> {
                Text(
                    text = "${history.songs.size} bài hát gần đây",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        history.songs.forEachIndexed { index, song ->
                            ListItem(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("history-song-${song.mediaId}")
                                    .clickable {
                                        onRecentSongSelected(
                                            history.songs,
                                            index
                                        )
                                    },
                                leadingContent = {
                                    Text("${index + 1}")
                                },
                                headlineContent = {
                                    Text(
                                        text = song.title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                supportingContent = {
                                    Text(
                                        text = song.artist,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                trailingContent = {
                                    TextButton(
                                        onClick = {
                                            onRemoveRecentSong(song.mediaId)
                                        }
                                    ) {
                                        Text("Xóa")
                                    }
                                }
                            )

                            if (index < history.songs.lastIndex) {
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = {
                showClearDialog = false
            },
            title = {
                Text("Xóa lịch sử nghe nhạc?")
            },
            text = {
                Text(
                    "Toàn bộ danh sách Nghe gần đây sẽ bị xóa. " +
                        "Các file nhạc trên thiết bị không bị ảnh hưởng."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        onClearHistory()
                    }
                ) {
                    Text("Xóa lịch sử")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                    }
                ) {
                    Text("Hủy")
                }
            }
        )
    }
}
