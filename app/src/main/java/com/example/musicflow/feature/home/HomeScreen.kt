
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
import com.example.musicflow.core.ux.ListeningJourneyRules
import com.example.musicflow.ui.components.MusicFlowStatusKind
import com.example.musicflow.ui.components.MusicFlowStatusPanel
import com.example.musicflow.ui.components.OceanFlowCard
import com.example.musicflow.ui.components.OceanFlowHeader
import com.example.musicflow.ui.components.OceanFlowSectionTitle
import com.example.musicflow.ui.components.SongArtwork
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip

@Composable
fun HomeRoute(
    state: PlayerUiState,
    onOpenLibrary: () -> Unit,
    onOpenSmartLibrary: () -> Unit = onOpenLibrary,
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

    val guidePreferences = remember(context) {
        context.getSharedPreferences("musicflow_home_guide_v3", android.content.Context.MODE_PRIVATE)
    }
    var showFirstUseGuide by rememberSaveable {
        mutableStateOf(!guidePreferences.getBoolean("dismissed", false))
    }

    HomeScreen(
        state = state,
        historyState = historyState,
        onOpenLibrary = onOpenLibrary,
        onOpenSmartLibrary = onOpenSmartLibrary,
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
        onClearHistory = homeViewModel::clearRecentHistory,
        showFirstUseGuide = showFirstUseGuide,
        onDismissFirstUseGuide = {
            guidePreferences.edit().putBoolean("dismissed", true).apply()
            showFirstUseGuide = false
        }
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
    onOpenOnlineCatalog: () -> Unit = {},
    showFirstUseGuide: Boolean = false,
    onDismissFirstUseGuide: () -> Unit = {},
    onOpenSmartLibrary: () -> Unit = onOpenLibrary
) {
    var showClearDialog by rememberSaveable {
        mutableStateOf(false)
    }
    var recentExpanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OceanFlowHeader(
            eyebrow = "Ocean Flow · Âm nhạc của bạn",
            title = "MusicFlow",
            subtitle = "Một giai điệu, một cảm xúc. Chọn nhạc yêu thích và nghe theo cách của bạn."
        )

        if (state.hasMedia) {
            OceanFlowCard {
                Text("TIẾP TỤC THƯỞNG THỨC",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary)
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SongArtwork(
                        uri = state.artworkUri,
                        modifier = Modifier.size(72.dp).clip(RoundedCornerShape(16.dp))
                    )
                    Column(modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(state.title, style = MaterialTheme.typography.titleLarge,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(state.artist, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Button(onClick = onOpenPlayer,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag("home-continue-listening")) {
                    Text("Mở trình phát nhạc")
                }
            }
        }

        OceanFlowCard {
                OceanFlowSectionTitle("Bắt đầu nghe")
                Text("Thư viện riêng luôn sẵn sàng, kể cả khi không có Internet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)

                Button(
                    onClick = onOpenLibrary,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag("home-library-action")
                ) {
                    Text("Mở thư viện thiết bị")
                }

                OutlinedButton(
                    onClick = onOpenSearch,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag("home-search-action")
                ) {
                    Text("Tìm kiếm bài hát")
                }

                OutlinedButton(
                    onClick = onOpenPlaylists,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag("home-playlist-action")
                ) {
                    Text("Danh sách phát của tôi")
                }

                OutlinedButton(
                    onClick = onOpenSmartLibrary,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag("home-smart-library-action")
                ) { Text("Playlist thông minh · Tự cập nhật") }
        }

        if (showFirstUseGuide) {
            OceanFlowCard(
                modifier = Modifier.testTag("home-first-use-guide")
            ) {
                    Text(
                        text = "Mới dùng MusicFlow?",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "1. Mở Thư viện để quét nhạc hoặc chọn tệp âm thanh.\n" +
                            "2. Chạm vào một bài hát để phát.\n" +
                            "3. Dùng trình phát thu nhỏ để tạm dừng hoặc mở màn hình phát.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    TextButton(
                        onClick = onDismissFirstUseGuide,
                        modifier = Modifier.testTag("home-guide-dismiss")
                    ) {
                        Text("Đã hiểu")
                    }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OceanFlowSectionTitle("Nghe gần đây")

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
                MusicFlowStatusPanel(
                    kind = MusicFlowStatusKind.LOADING,
                    title = "Đang tải lịch sử nghe nhạc...",
                    description = "Danh sách các bài đã nghe sẽ xuất hiện tại đây."
                )
            }

            HomeHistoryUiState.Empty -> {
                MusicFlowStatusPanel(
                    kind = MusicFlowStatusKind.EMPTY,
                    title = "Chưa có bài hát nào trong lịch sử",
                    description = "Nghe nhạc từ Thư viện hoặc Tìm kiếm để bắt đầu " +
                        "tạo danh sách gần đây. Không cần đăng nhập tài khoản.",
                    actionLabel = "Mở thư viện",
                    onAction = onOpenLibrary
                )
            }

            is HomeHistoryUiState.Error -> {
                MusicFlowStatusPanel(
                    kind = MusicFlowStatusKind.ERROR,
                    title = "Không thể tải lịch sử nghe",
                    description = history.message
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
                        val visibleCount = ListeningJourneyRules.recentVisibleCount(
                            history.songs.size, recentExpanded
                        )
                        history.songs.take(visibleCount).forEachIndexed { index, song ->
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

                            if (index < visibleCount - 1) {
                                HorizontalDivider()
                            }
                        }
                        if (history.songs.size > ListeningJourneyRules.RECENT_PREVIEW_LIMIT) {
                            TextButton(
                                onClick = { recentExpanded = !recentExpanded },
                                modifier = Modifier.fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("home-recent-toggle")
                            ) {
                                Text(if (recentExpanded) "Thu gọn lịch sử"
                                    else "Xem tất cả ${history.songs.size} bài đã nghe")
                            }
                        }
                    }
                }
            }
        }

        OceanFlowCard {
                OceanFlowSectionTitle("Khám phá thêm")
                OutlinedButton(
                    onClick = onOpenFavoriteSongs,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("♥ Bài hát yêu thích ($favoriteSongCount)") }
                OutlinedButton(
                    onClick = onOpenFavoriteAlbums,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("♥ Album yêu thích ($favoriteAlbumCount)") }
                OutlinedButton(
                    onClick = onOpenOnlineCatalog,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Khám phá nhạc online") }
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
