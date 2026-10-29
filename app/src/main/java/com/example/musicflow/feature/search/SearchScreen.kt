
package com.example.musicflow.feature.search

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

import com.example.musicflow.core.media.DeviceSong
import com.example.musicflow.core.media.MediaStoreLocalMusicRepository
import com.example.musicflow.feature.library.SongRow
import com.example.musicflow.feature.library.MusicCollectionsContent
import com.example.musicflow.feature.playlist.PlaylistPickerDialog
import com.example.musicflow.core.favorites.LocalFavoritesStore
import com.example.musicflow.feature.playlist.rememberPlaylistViewModel
import com.example.musicflow.ui.components.MusicFlowStatusKind
import com.example.musicflow.ui.components.MusicFlowStatusPanel
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

@Composable
fun SearchRoute(
    initialQuery: String,
    onQueryRemembered: (String) -> Unit,
    onSongSelected: (List<DeviceSong>, Int) -> Unit,
    onQueueNext: ((DeviceSong) -> Unit)? = null,
    onQueueEnd: ((DeviceSong) -> Unit)? = null
) {
    val context = LocalContext.current

    val favoritesStore = remember(context) {
        LocalFavoritesStore.getInstance(context)
    }

    val favoriteSongUris by favoritesStore.songUris
        .collectAsStateWithLifecycle()

    val repository = remember(context) {
        MediaStoreLocalMusicRepository(
            context.applicationContext
        )
    }

    val factory = remember(repository) {
        SearchViewModel.factory(
            repository = repository,
            initialQuery = initialQuery
        )
    }

    val searchViewModel: SearchViewModel =
        viewModel(factory = factory)

    val playlistViewModel = rememberPlaylistViewModel()

    var selectedPlaylistSong by remember {
        mutableStateOf<DeviceSong?>(null)
    }

    val state by searchViewModel.uiState
        .collectAsStateWithLifecycle()

    val permission = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasPermission by remember(context, permission) {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            hasPermission = granted
        }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasPermission = ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            searchViewModel.refresh()
        } else {
            searchViewModel.clearSongs()
        }
    }

    SearchScreen(
        state = state,
        hasPermission = hasPermission,
        onQueryChange = { query ->
            searchViewModel.updateQuery(query)
            onQueryRemembered(query)
        },
        onRequestPermission = {
            permissionLauncher.launch(permission)
        },
        onRefresh = {
            if (hasPermission) {
                searchViewModel.refresh()
            } else {
                permissionLauncher.launch(permission)
            }
        },
        onSongSelected = onSongSelected,
        onQueueNext = onQueueNext,
        onQueueEnd = onQueueEnd,
        onAddToPlaylist = { song ->
            selectedPlaylistSong = song
        },
        favoriteSongUris = favoriteSongUris,
        onToggleFavorite = { song ->
            favoritesStore.toggleSong(
                uri = song.uri,
                title = song.title,
                artist = song.artist
            )
        }
    )

    selectedPlaylistSong?.let { song ->
        PlaylistPickerDialog(
            song = song,
            viewModel = playlistViewModel,
            onDismiss = {
                selectedPlaylistSong = null
            }
        )
    }
}

@Composable
fun SearchScreen(
    state: SearchUiState,
    hasPermission: Boolean,
    onQueryChange: (String) -> Unit,
    onRequestPermission: () -> Unit,
    onRefresh: () -> Unit,
    onSongSelected: (List<DeviceSong>, Int) -> Unit,
    onAddToPlaylist: (DeviceSong) -> Unit,
    onQueueNext: ((DeviceSong) -> Unit)? = null,
    onQueueEnd: ((DeviceSong) -> Unit)? = null,
    favoriteSongUris: Set<String> = emptySet(),
    onToggleFavorite: (DeviceSong) -> Unit = {}
) {
    var selectedCategory by rememberSaveable {
        mutableStateOf("songs")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Tìm kiếm",
                style = MaterialTheme.typography.headlineSmall
            )

            if (hasPermission) {
                TextButton(
                    onClick = onRefresh,
                    enabled = !state.isLoading
                ) {
                    Text("Quét lại")
                }
            }
        }

        if (!hasPermission) {
            MusicFlowStatusPanel(
                kind = MusicFlowStatusKind.PERMISSION,
                title = "Cần quyền đọc nhạc",
                description = "Để tìm kiếm nhạc trên thiết bị, hãy cấp quyền đọc âm thanh. " +
                    "Nếu đã từ chối nhiều lần, hãy mở quyền ứng dụng trong Cài đặt.",
                actionLabel = "Cấp quyền",
                onAction = onRequestPermission
            )

            return@Column
        }

        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Bài hát, album, nghệ sĩ hoặc thể loại") },
            placeholder = { Text("Ví dụ: duong ve nha") },
            singleLine = true,
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier.semantics {
                            contentDescription = "Xóa nội dung tìm kiếm"
                        }
                    ) {
                        Text("×")
                    }
                }
            }
        )

        if (state.isLoaded && state.query.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "songs" to "Bài hát",
                    "albums" to "Album",
                    "artists" to "Nghệ sĩ",
                    "genres" to "Thể loại"
                ).forEach { (category, label) ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(label) }
                    )
                }
            }
        }

        when {
            state.isLoading -> {
                MusicFlowStatusPanel(
                    kind = MusicFlowStatusKind.LOADING,
                    title = "Đang quét nhạc trên thiết bị...",
                    description = "Đang chuẩn bị thư viện để tìm kiếm."
                )
            }

            state.error != null -> {
                MusicFlowStatusPanel(
                    kind = MusicFlowStatusKind.ERROR,
                    title = "Không thể tìm kiếm",
                    description = state.error,
                    actionLabel = "Thử lại",
                    onAction = onRefresh
                )
            }

            !state.isLoaded -> {
                MusicFlowStatusPanel(
                    kind = MusicFlowStatusKind.INFO,
                    title = "Chưa tải thư viện nhạc",
                    description = "Quét nhạc để bắt đầu tìm kiếm.",
                    actionLabel = "Quét lại",
                    onAction = onRefresh
                )
            }

            state.songs.isEmpty() -> {
                MusicFlowStatusPanel(
                    kind = MusicFlowStatusKind.EMPTY,
                    title = "Không tìm thấy bài hát nào trên thiết bị",
                    description = "Hãy thêm tệp nhạc hoặc kiểm tra quyền đọc âm thanh.",
                    actionLabel = "Quét lại",
                    onAction = onRefresh
                )
            }

            state.query.isBlank() -> {
                MusicFlowStatusPanel(
                    kind = MusicFlowStatusKind.INFO,
                    title = "Bạn muốn nghe gì?",
                    description = "Nhập tên bài hát, album, nghệ sĩ hoặc thể loại để tìm kiếm."
                )
            }

            state.isFiltering -> {
                MusicFlowStatusPanel(
                    kind = MusicFlowStatusKind.LOADING,
                    title = "Đang tìm bài hát...",
                    description = "Đang lọc kết quả theo nội dung bạn nhập."
                )
            }

            selectedCategory == "albums" -> {
                MusicCollectionsContent(
                    title = "album phù hợp",
                    collections = state.albumResults,
                    favoriteSongUris = favoriteSongUris,
                    onToggleFavorite = onToggleFavorite,
                    onAddToPlaylist = onAddToPlaylist,
                    onSongSelected = onSongSelected,
                    onQueueNext = onQueueNext,
                    onQueueEnd = onQueueEnd
                )
            }

            selectedCategory == "artists" -> {
                MusicCollectionsContent(
                    title = "nghệ sĩ phù hợp",
                    collections = state.artistResults,
                    favoriteSongUris = favoriteSongUris,
                    onToggleFavorite = onToggleFavorite,
                    onAddToPlaylist = onAddToPlaylist,
                    onSongSelected = onSongSelected,
                    onQueueNext = onQueueNext,
                    onQueueEnd = onQueueEnd
                )
            }

            selectedCategory == "genres" -> {
                MusicCollectionsContent(
                    title = "thể loại phù hợp",
                    collections = state.genreResults,
                    favoriteSongUris = favoriteSongUris,
                    onToggleFavorite = onToggleFavorite,
                    onAddToPlaylist = onAddToPlaylist,
                    onSongSelected = onSongSelected,
                    onQueueNext = onQueueNext,
                    onQueueEnd = onQueueEnd
                )
            }

            state.results.isEmpty() -> {
                MusicFlowStatusPanel(
                    kind = MusicFlowStatusKind.EMPTY,
                    title = "Không tìm thấy bài hát phù hợp",
                    description = "Thử từ khóa ngắn hơn hoặc tìm bằng tên nghệ sĩ."
                )
            }

            else -> {
                Text(
                    text = "${state.results.size} kết quả",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(
                        items = state.results,
                        key = { _, song -> song.id }
                    ) { index, song ->
                        SongRow(
                            song = song,
                            onClick = {
                                onSongSelected(
                                    state.results,
                                    index
                                )
                            },
                            onAddToPlaylist = {
                                onAddToPlaylist(song)
                            },
                            isFavorite = song.uri.toString() in favoriteSongUris,
                            onToggleFavorite = {
                                onToggleFavorite(song)
                            },
                            onQueueNext = onQueueNext?.let { action -> { action(song) } },
                            onQueueEnd = onQueueEnd?.let { action -> { action(song) } }
                        )
                    }
                }
            }
        }
    }
}
