
package com.example.musicflow.feature.search

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

import com.example.musicflow.core.media.DeviceSong
import com.example.musicflow.core.media.MediaStoreLocalMusicRepository
import com.example.musicflow.core.media.LocalPickedMusicStore
import com.example.musicflow.feature.library.SongRow
import com.example.musicflow.feature.library.MusicCollectionsContent
import com.example.musicflow.feature.playlist.PlaylistPickerDialog
import com.example.musicflow.core.favorites.LocalFavoritesStore
import com.example.musicflow.feature.playlist.rememberPlaylistViewModel
import com.example.musicflow.ui.components.MusicFlowStatusKind
import com.example.musicflow.ui.components.MusicFlowStatusPanel
import com.example.musicflow.ui.components.OceanFlowHeader
import com.example.musicflow.core.ux.ListeningJourneyRules
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

@Composable
fun SearchRoute(
    initialQuery: String,
    onQueryRemembered: (String) -> Unit,
    onSongSelected: (List<DeviceSong>, Int) -> Unit,
    onQueueNext: ((DeviceSong) -> Unit)? = null,
    onQueueEnd: ((DeviceSong) -> Unit)? = null,
    onOpenLibrary: (() -> Unit)? = null
) {
    val context = LocalContext.current

    val favoritesStore = remember(context) {
        LocalFavoritesStore.getInstance(context)
    }

    val favoriteSongUris by favoritesStore.songUris
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

    val pickedStore = remember(context) {
        LocalPickedMusicStore.getInstance(context.applicationContext)
    }
    val savedPicked by pickedStore.records.collectAsStateWithLifecycle()
    // Inspect with persisted grants on IO; do not expose unverified shortcuts.
    val repository = remember(context, permission) {
        OfflineSearchRepository(
            mediaStore = MediaStoreLocalMusicRepository(context.applicationContext),
            mayScanDevice = {
                ContextCompat.checkSelfPermission(context, permission) ==
                    PackageManager.PERMISSION_GRANTED
            },
            readablePickedSongs = { pickedStore.inspect().available }
        )
    }
    val factory = remember(repository) {
        SearchViewModel.factory(repository = repository, initialQuery = initialQuery)
    }
    val searchViewModel: SearchViewModel = viewModel(factory = factory)
    val playlistViewModel = rememberPlaylistViewModel()
    var selectedPlaylistSong by remember { mutableStateOf<DeviceSong?>(null) }
    val state by searchViewModel.uiState.collectAsStateWithLifecycle()
    var refreshOnResume by remember { mutableIntStateOf(0) }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            hasPermission = granted
        }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        refreshOnResume++
        hasPermission = ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    LaunchedEffect(hasPermission, savedPicked, refreshOnResume) {
        // On permission revoke, clear MediaStore results by reloading from
        // allowed sources, not by clearing the entire offline catalog.
        searchViewModel.refresh()
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
            searchViewModel.refresh()
        },
        onSongSelected = onSongSelected,
        onQueueNext = onQueueNext,
        onQueueEnd = onQueueEnd,
        onOpenLibrary = onOpenLibrary,
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
    onOpenLibrary: (() -> Unit)? = null,
    favoriteSongUris: Set<String> = emptySet(),
    onToggleFavorite: (DeviceSong) -> Unit = {}
) {
    var selectedCategory by rememberSaveable {
        mutableStateOf("songs")
    }
    // The empty/loading/permission states need a full-page scroll at 180% font.
    // Keep result layouts unbounded only by their own LazyColumn/collection list.
    val scrollWholePage = !state.isLoaded || state.songs.isEmpty() ||
        state.query.isBlank() || state.isLoading || state.error != null ||
        (!state.isFiltering && state.results.isEmpty() &&
            state.albumResults.isEmpty() && state.artistResults.isEmpty() &&
            state.genreResults.isEmpty())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(if (scrollWholePage) Modifier.verticalScroll(rememberScrollState())
                else Modifier)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OceanFlowHeader(
            eyebrow = "Khám phá · Ocean Flow",
            title = "Tìm kiếm",
            subtitle = "Tìm cả nhạc trong máy và tệp đã chọn — nhập không dấu cũng được."
        )
        if (hasPermission && (!state.isLoaded || state.songs.isNotEmpty())) {
            TextButton(
                onClick = onRefresh,
                enabled = !state.isLoading,
                modifier = Modifier.heightIn(min = 48.dp).testTag("search-refresh")
            ) { Text("Quét lại") }
        }

        if (!hasPermission) {
            Surface(
                modifier = Modifier.fillMaxWidth()
                    .testTag("search-saf-without-permission"),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Đang tìm trong nhạc đã chọn",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        "Không cần cấp quyền quét máy để tìm tệp đã chọn. " +
                            "Chỉ cấp quyền nếu bạn muốn tìm toàn bộ nhạc thiết bị.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedButton(
                        onClick = onRequestPermission,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .testTag("search-request-device-scan")
                    ) {
                        Text("Cho phép quét nhạc trên thiết bị")
                    }
                    if (onOpenLibrary != null) {
                        TextButton(
                            onClick = onOpenLibrary,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .testTag("search-open-library")
                        ) { Text("Mở Thư viện để chọn tệp nhạc") }
                    }
                }
            }
        }

        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth().testTag("search-query"),
            label = { Text("Tìm bài hát hoặc nghệ sĩ") },
            placeholder = { Text("Ví dụ: duong ve nha") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier.testTag("search-clear-query").semantics {
                            contentDescription = "Xóa nội dung tìm kiếm"
                        }
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = null)
                    }
                }
            }
        )

        if (state.query.isBlank()) {
            Text(
                "Tìm theo bài hát, nghệ sĩ, album hoặc thể loại — có thể nhập không dấu.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (state.isLoaded && state.query.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple("songs", "Bài hát", state.results.size),
                    Triple("albums", "Album", state.albumResults.size),
                    Triple("artists", "Nghệ sĩ", state.artistResults.size),
                    Triple("genres", "Thể loại", state.genreResults.size)
                ).forEach { (category, label, count) ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = {
                            Text(if (state.isFiltering) label else "$label ($count)")
                        },
                        modifier = Modifier.heightIn(min = 48.dp)
                            .testTag("search-category-$category")
                    )
                }
            }
            Text(
                "Vuốt ngang để xem đủ nhóm kết quả: Bài hát, Album, Nghệ sĩ và Thể loại.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("search-category-scroll-hint")
            )
        }

        if (state.isLoaded && !state.isLoading && !state.isFiltering &&
            state.error == null && state.query.isNotBlank()
        ) {
            val alternative = ListeningJourneyRules.suggestedSearchCategory(
                selectedCategory, state.results.size, state.albumResults.size,
                state.artistResults.size, state.genreResults.size
            )
            if (alternative != null) {
                OutlinedButton(
                    onClick = { selectedCategory = alternative },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag("search-related-suggestion")
                ) {
                    Text("Có kết quả ở ${ListeningJourneyRules.searchCategoryLabel(alternative)} — xem ngay")
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
                    actionLabel = if (hasPermission) null else "Tìm lại nhạc đã chọn",
                    onAction = if (hasPermission) null else onRefresh
                )
            }

            state.songs.isEmpty() -> {
                MusicFlowStatusPanel(
                    kind = MusicFlowStatusKind.EMPTY,
                    title = if (hasPermission) "Không tìm thấy bài hát nào trên thiết bị"
                        else "Chưa có nhạc đã chọn",
                    description = if (hasPermission) {
                        "Không tìm thấy nhạc trên máy hoặc tệp đã chọn còn đọc được. " +
                            "Bạn có thể chọn tệp nhạc thủ công trong Thư viện."
                    } else {
                        "Hãy chọn một tệp nhạc trong Thư viện, sau đó quay lại để tìm kiếm " +
                            "mà không cần cấp quyền quét toàn bộ thiết bị."
                    },
                    actionLabel = if (hasPermission) "Quét lại" else null,
                    onAction = if (hasPermission) onRefresh else null
                )
                if (hasPermission && onOpenLibrary != null) {
                    OutlinedButton(
                        onClick = onOpenLibrary,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .testTag("search-open-library")
                    ) { Text("Mở Thư viện để chọn tệp nhạc") }
                }
            }

            state.query.isBlank() -> {
                MusicFlowStatusPanel(
                    kind = MusicFlowStatusKind.INFO,
                    title = "Bạn muốn nghe gì?",
                    description = "Nhập từ khóa để tìm trong nhạc thiết bị và nhạc đã chọn. " +
                        "Không cần đăng nhập để tìm hoặc phát nhạc offline."
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
                    description = if (
                        state.albumResults.isNotEmpty() ||
                        state.artistResults.isNotEmpty() ||
                        state.genreResults.isNotEmpty()
                    ) {
                        "Không có bài hát khớp trực tiếp. Bạn có kết quả ở mục Album, " +
                            "Nghệ sĩ hoặc Thể loại phía trên."
                    } else {
                        "Thử từ khóa ngắn hơn, tìm không dấu hoặc nhập tên nghệ sĩ."
                    },
                    actionLabel = "Xóa tìm kiếm",
                    onAction = { onQueryChange("") }
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
                        key = { _, song -> song.uri.toString() }
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
