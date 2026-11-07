package com.example.musicflow.feature.library

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.musicflow.core.favorites.LocalFavoritesStore
import com.example.musicflow.core.favorites.FavoriteSongDetails
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.musicflow.core.media.DeviceSong
import com.example.musicflow.core.media.MediaStoreLocalMusicRepository
import com.example.musicflow.core.media.LocalPickedMusicStore
import com.example.musicflow.core.media.PickedMusicInspection
import com.example.musicflow.core.media.PickedMusicRecord
import com.example.musicflow.core.history.RecentSong
import com.example.musicflow.core.history.RoomRecentHistoryRepository
import com.example.musicflow.core.history.local.RecentHistoryDatabase
import com.example.musicflow.feature.playlist.PlaylistPickerDialog
import com.example.musicflow.feature.playlist.rememberPlaylistViewModel
import com.example.musicflow.ui.components.MusicFlowStatusKind
import com.example.musicflow.ui.components.MusicFlowStatusPanel
import com.example.musicflow.ui.components.OceanFlowHeader

@Composable
fun LibraryRoute(
    onBack: () -> Unit,
    onPickManual: () -> Unit,
    onSongSelected: (List<DeviceSong>, Int) -> Unit,
    initialFavoritesOnly: Boolean = false,
    initialShowAlbums: Boolean = false,
    initialShowSmart: Boolean = false,
    onQueueNext: ((DeviceSong) -> Unit)? = null,
    onQueueEnd: ((DeviceSong) -> Unit)? = null
) {
    val context = LocalContext.current

    val favoritesStore = remember(context) {
        LocalFavoritesStore.getInstance(context)
    }

    val favoriteSongUris by favoritesStore.songUris
        .collectAsStateWithLifecycle()

    val favoriteSongDetails by favoritesStore.songDetails
        .collectAsStateWithLifecycle()

    val favoriteAlbumIds by favoritesStore.albumIds
        .collectAsStateWithLifecycle()

    val repository = remember {
        MediaStoreLocalMusicRepository(context.applicationContext)
    }
    val historyRepository = remember(context) {
        RoomRecentHistoryRepository(RecentHistoryDatabase.getInstance(context.applicationContext))
    }
    val recentSongs by remember(historyRepository) { historyRepository.observeRecent() }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val pickedStore = remember(context) { LocalPickedMusicStore.getInstance(context) }
    val savedPicked by pickedStore.records.collectAsStateWithLifecycle()
    var pickedInspection by remember { mutableStateOf(PickedMusicInspection()) }
    var pickedRefresh by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { pickedRefresh++ }
    LaunchedEffect(savedPicked, pickedRefresh) {
        pickedInspection = pickedStore.inspect()
    }

    val factory = remember(repository) {
        LibraryViewModel.factory(repository)
    }

    val libraryViewModel: LibraryViewModel =
        viewModel(factory = factory)
    var genresEnabled by rememberSaveable { mutableStateOf(false) }

    val playlistViewModel = rememberPlaylistViewModel()

    var selectedPlaylistSong by remember {
        mutableStateOf<DeviceSong?>(null)
    }

    val uiState by libraryViewModel.uiState
        .collectAsStateWithLifecycle()

    val permission = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasPermission by remember {
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
        val granted = ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED

        hasPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission && !initialFavoritesOnly && !initialShowSmart) {
            permissionLauncher.launch(permission)
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            libraryViewModel.refresh(includeGenres = genresEnabled)
        }
    }

    LibraryScreen(
        uiState = uiState,
        hasPermission = hasPermission,
        onBack = onBack,
        onPickManual = onPickManual,
        initialFavoritesOnly = initialFavoritesOnly,
        initialShowAlbums = initialShowAlbums,
        initialShowSmart = initialShowSmart,
        onRefresh = {
            if (hasPermission) {
                libraryViewModel.refresh(includeGenres = genresEnabled)
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
        favoriteSongDetails = favoriteSongDetails,
        recentSongs = recentSongs,
        pickedSongs = pickedInspection.available,
        unavailablePicked = pickedInspection.unavailable,
        onGenresSelected = {
            if (hasPermission && !genresEnabled) {
                genresEnabled = true
                libraryViewModel.refresh(includeGenres = true)
            }
        },
        onForgetPicked = pickedStore::forget,
        onToggleFavorite = { song ->
            favoritesStore.toggleSong(
                uri = song.uri,
                title = song.title,
                artist = song.artist
            )
        },
        favoriteAlbumIds = favoriteAlbumIds,
        onToggleAlbum = { albumId ->
            favoritesStore.toggleAlbum(albumId)
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
fun LibraryScreen(
    uiState: LibraryUiState,
    hasPermission: Boolean,
    onBack: () -> Unit,
    onPickManual: () -> Unit,
    onRefresh: () -> Unit,
    onSongSelected: (List<DeviceSong>, Int) -> Unit,
    onAddToPlaylist: (DeviceSong) -> Unit,
    onQueueNext: ((DeviceSong) -> Unit)? = null,
    onQueueEnd: ((DeviceSong) -> Unit)? = null,
    favoriteSongUris: Set<String> = emptySet(),
    favoriteSongDetails: Map<String, FavoriteSongDetails> = emptyMap(),
    onToggleFavorite: (DeviceSong) -> Unit = {},
    favoriteAlbumIds: Set<String> = emptySet(),
    onToggleAlbum: (Long) -> Unit = {},
    initialFavoritesOnly: Boolean = false,
    initialShowAlbums: Boolean = false,
    recentSongs: List<RecentSong> = emptyList(),
    initialShowSmart: Boolean = false,
    pickedSongs: List<DeviceSong> = emptyList(),
    unavailablePicked: List<PickedMusicRecord> = emptyList(),
    onForgetPicked: (String) -> Unit = {},
    onGenresSelected: () -> Unit = {}
) {
    var favoritesOnly by rememberSaveable(initialFavoritesOnly) {
        mutableStateOf(initialFavoritesOnly)
    }

    var selectedCategory by rememberSaveable(initialShowAlbums, initialShowSmart) {
        mutableStateOf(when {
            initialShowSmart -> "smart"
            initialShowAlbums -> "albums"
            else -> "songs"
        })
    }
    var songSort by rememberSaveable {
        mutableStateOf(LibrarySongSort.TITLE)
    }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var songSearchQuery by rememberSaveable { mutableStateOf("") }

    // A saved category can be restored after process restart; load metadata
    // even when the user did not tap Thể loại again.
    LaunchedEffect(selectedCategory, hasPermission) {
        if (selectedCategory == "genres" && hasPermission) onGenresSelected()
    }

    val scannedSongs = (uiState as? LibraryUiState.Success)
        ?.songs.orEmpty()
    val smartSongs = remember(scannedSongs, pickedSongs) {
        (scannedSongs + pickedSongs).distinctBy { it.uri.toString() }
    }

    val pickedFavorites = remember(
        favoriteSongUris,
        favoriteSongDetails,
        scannedSongs
    ) {
        val scannedUris = scannedSongs
            .map { it.uri.toString() }
            .toSet()

        favoriteSongUris.mapNotNull { rawUri ->
            val uri = Uri.parse(rawUri)

            if (
                uri.scheme != "content" ||
                uri.authority == MediaStore.AUTHORITY ||
                rawUri in scannedUris
            ) {
                null
            } else {
                val details = favoriteSongDetails[rawUri]

                DeviceSong(
                    id = -1L - (
                        rawUri.hashCode().toLong() and 0xffffffffL
                    ),
                    uri = uri,
                    title = details?.title
                        ?: uri.lastPathSegment
                        ?: "File âm thanh đã chọn",
                    artist = details?.artist
                        ?: "Không rõ nghệ sĩ",
                    durationMs = 0L,
                    mediaId = rawUri
                )
            }
        }.sortedBy { it.title.lowercase() }
    }

    val displayUiState = if (
        favoritesOnly &&
        selectedCategory == "songs" &&
        uiState !is LibraryUiState.Loading &&
        uiState !is LibraryUiState.Error
    ) {
        LibraryUiState.Success(scannedSongs)
    } else {
        uiState
    }

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OceanFlowHeader(
                eyebrow = "Thư viện riêng · Offline",
                title = "Nhạc thiết bị",
                subtitle = "Mọi giai điệu từ thiết bị, theo cách của bạn."
            )
            TextButton(onClick = onBack,
                modifier = Modifier.heightIn(min = 48.dp)) { Text("Quay lại") }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onRefresh,
                    modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Quét lại")
                }

                Button(onClick = onPickManual,
                    modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Chọn tệp nhạc")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "songs" to "Bài hát", "picked" to "Nhạc đã chọn",
                    "smart" to "Thông minh", "albums" to "Album",
                    "artists" to "Nghệ sĩ", "genres" to "Thể loại"
                ).forEach { (category, label) ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(label) },
                        modifier = Modifier.testTag("library-category-$category")
                    )
                }
            }

            if (selectedCategory == "picked") {
                PickedMusicContent(
                    songs = pickedSongs,
                    missing = unavailablePicked,
                    favoriteSongUris = favoriteSongUris,
                    onSongSelected = onSongSelected,
                    onAddToPlaylist = onAddToPlaylist,
                    onToggleFavorite = onToggleFavorite,
                    onQueueNext = onQueueNext,
                    onQueueEnd = onQueueEnd,
                    onForget = onForgetPicked,
                    onPickManual = onPickManual
                )
            } else if (selectedCategory == "smart" && !hasPermission) {
                Text(
                    "Bạn vẫn có thể nghe nhạc đã chọn mà không cần cấp quyền quét thiết bị.",
                    style = MaterialTheme.typography.bodySmall
                )
                SmartLibraryContent(
                    songs = pickedSongs, favoriteSongUris = favoriteSongUris,
                    recentSongUris = recentSongs.map { it.audioUri },
                    onSongSelected = onSongSelected, onAddToPlaylist = onAddToPlaylist,
                    onToggleFavorite = onToggleFavorite,
                    onQueueNext = onQueueNext, onQueueEnd = onQueueEnd,
                    onBrowseSongs = { selectedCategory = "picked" },
                    browseActionLabel = "Xem nhạc đã chọn"
                )
            } else if (!hasPermission && (!favoritesOnly || selectedCategory != "songs")) {
                MusicFlowStatusPanel(
                    kind = MusicFlowStatusKind.PERMISSION,
                    title = "Chỉ cần cấp quyền nếu muốn quét cả máy",
                    description = "Cấp quyền Âm thanh để hiển thị toàn bộ nhạc Android. " +
                        "Hoặc dùng Nhạc đã chọn để nghe file riêng mà không cần cấp quyền. " +
                        "Nếu đã từ chối nhiều lần, mở Cài đặt ứng dụng để cho phép.",
                    actionLabel = "Cấp quyền đọc âm thanh",
                    onAction = onRefresh
                )
                OutlinedButton(
                    onClick = { selectedCategory = "picked" },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag("library-permission-open-picked")
                ) { Text("Tiếp tục với nhạc đã chọn") }

            } else {
                when (val current = displayUiState) {

                    LibraryUiState.Idle -> {
                        MusicFlowStatusPanel(
                            kind = MusicFlowStatusKind.INFO,
                            title = "Chưa quét thư viện",
                            description = "Quét nhạc trên thiết bị để xem bài hát, album và nghệ sĩ.",
                            actionLabel = "Quét nhạc",
                            onAction = onRefresh
                        )
                    }

                    LibraryUiState.Loading -> {
                        MusicFlowStatusPanel(
                            kind = MusicFlowStatusKind.LOADING,
                            title = "Đang quét nhạc...",
                            description = "MusicFlow đang đọc danh sách bài hát trên thiết bị."
                        )
                    }

                    LibraryUiState.Empty -> {
                        if (selectedCategory == "smart") {
                            SmartLibraryContent(
                                songs = pickedSongs,
                                favoriteSongUris = favoriteSongUris,
                                recentSongUris = recentSongs.map { it.audioUri },
                                onSongSelected = onSongSelected,
                                onAddToPlaylist = onAddToPlaylist,
                                onToggleFavorite = onToggleFavorite,
                                onQueueNext = onQueueNext,
                                onQueueEnd = onQueueEnd,
                                onBrowseSongs = { selectedCategory = "songs" }
                            )
                        } else {
                            MusicFlowStatusPanel(
                                kind = MusicFlowStatusKind.EMPTY,
                                title = "Thư viện chưa có nhạc",
                                description = "Không tìm thấy bài hát nào được Android phân loại " +
                                    "là nhạc. Hãy chọn tệp nhạc thủ công (MP3, FLAC, WAV...)."
                            )
                        }
                    }

                    is LibraryUiState.Error -> {
                        MusicFlowStatusPanel(
                            kind = MusicFlowStatusKind.ERROR,
                            title = "Không thể quét thư viện",
                            description = current.message,
                            actionLabel = "Thử lại",
                            onAction = onRefresh
                        )
                        if (selectedCategory == "smart" && pickedSongs.isNotEmpty()) {
                            SmartLibraryContent(
                                songs = pickedSongs,
                                favoriteSongUris = favoriteSongUris,
                                recentSongUris = recentSongs.map { it.audioUri },
                                onSongSelected = onSongSelected,
                                onAddToPlaylist = onAddToPlaylist,
                                onToggleFavorite = onToggleFavorite,
                                onQueueNext = onQueueNext,
                                onQueueEnd = onQueueEnd,
                                onBrowseSongs = { selectedCategory = "picked" },
                                browseActionLabel = "Xem nhạc đã chọn"
                            )
                        }
                    }

                    is LibraryUiState.Success -> {
                        if (selectedCategory == "smart") {
                            SmartLibraryContent(
                                songs = smartSongs,
                                favoriteSongUris = favoriteSongUris,
                                recentSongUris = recentSongs.map { it.audioUri },
                                onSongSelected = onSongSelected,
                                onAddToPlaylist = onAddToPlaylist,
                                onToggleFavorite = onToggleFavorite,
                                onQueueNext = onQueueNext,
                                onQueueEnd = onQueueEnd,
                                onBrowseSongs = { selectedCategory = "songs" }
                            )
                        } else if (selectedCategory == "albums") {
                            AlbumFavoritesContent(
                                songs = current.songs,
                                favoriteAlbumIds = favoriteAlbumIds,
                                favoriteSongUris = favoriteSongUris,
                                onToggleAlbum = onToggleAlbum,
                                onToggleFavorite = onToggleFavorite,
                                onSongSelected = onSongSelected,
                                onAddToPlaylist = onAddToPlaylist,
                                onQueueNext = onQueueNext,
                                onQueueEnd = onQueueEnd,
                                initialFavoritesOnly = initialFavoritesOnly
                            )
                        } else if (selectedCategory == "artists") {
                            val artistGroups = remember(current.songs) {
                                MusicCollections.artists(current.songs)
                            }
                            MusicCollectionsContent(
                                title = "nghệ sĩ",
                                collections = artistGroups,
                                favoriteSongUris = favoriteSongUris,
                                onToggleFavorite = onToggleFavorite,
                                onAddToPlaylist = onAddToPlaylist,
                                onSongSelected = onSongSelected,
                                onQueueNext = onQueueNext,
                                onQueueEnd = onQueueEnd
                            )
                        } else if (selectedCategory == "genres") {
                            val genreGroups = remember(current.songs) {
                                MusicCollections.genres(current.songs)
                            }
                            MusicCollectionsContent(
                                title = "thể loại",
                                collections = genreGroups,
                                favoriteSongUris = favoriteSongUris,
                                onToggleFavorite = onToggleFavorite,
                                onAddToPlaylist = onAddToPlaylist,
                                onSongSelected = onSongSelected,
                                onQueueNext = onQueueNext,
                                onQueueEnd = onQueueEnd
                            )
                        } else {
                            val unsortedSongs = remember(current.songs, favoritesOnly,
                                favoriteSongUris, pickedFavorites) {
                                if (favoritesOnly) current.songs.filter {
                                    it.uri.toString() in favoriteSongUris
                                } + pickedFavorites else current.songs
                            }
                        val sortedSongs = remember(unsortedSongs, songSort) {
                            sortLibrarySongs(unsortedSongs, songSort)
                        }
                        val searchIndex = remember(sortedSongs) {
                            LibrarySongSearchIndex(sortedSongs)
                        }
                        val visibleSongs = remember(searchIndex, songSearchQuery) {
                            searchIndex.filter(songSearchQuery)
                        }

                        // One virtualized scrolling surface. On large text sizes the
                        // filters and sort controls scroll instead of squeezing song rows.
                        LazyColumn(
                            modifier = Modifier.weight(1f).testTag("library-songs-list"),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                        item {
                        FilterChip(
                            selected = favoritesOnly,
                            onClick = { favoritesOnly = !favoritesOnly },
                            label = {
                                Text(
                                    if (favoritesOnly) "Đang xem yêu thích"
                                    else "Chỉ hiện yêu thích"
                                )
                            }
                        )
                        }

                        item {
                        OutlinedTextField(
                            value = songSearchQuery,
                            onValueChange = { songSearchQuery = it.take(80) },
                            label = { Text("Tìm trong Bài hát") },
                            placeholder = { Text("Tên bài hoặc nghệ sĩ, có thể không dấu") },
                            singleLine = true,
                            trailingIcon = {
                                if (songSearchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { songSearchQuery = "" },
                                        modifier = Modifier.testTag("library-search-clear")
                                            .semantics {
                                                contentDescription = "Xóa tìm kiếm bài hát"
                                            }
                                    ) { Text("✕") }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("library-search-field")
                        )
                        }

                        item {
                        Text(
                            "Hiển thị ${visibleSongs.size} / ${sortedSongs.size} bài",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("library-search-count")
                        )
                        }

                        if (visibleSongs.isEmpty()) {
                            item {
                            MusicFlowStatusPanel(
                                kind = MusicFlowStatusKind.EMPTY,
                                title = if (songSearchQuery.isNotBlank())
                                    "Không tìm thấy bài phù hợp"
                                    else if (favoritesOnly) "Chưa có bài hát yêu thích"
                                    else "Chưa có bài hát trong thư viện",
                                description = if (songSearchQuery.isNotBlank()) {
                                    "Thử tên bài hoặc nghệ sĩ ngắn hơn, hoặc xóa từ khóa tìm kiếm."
                                } else if (favoritesOnly) {
                                    "Chạm vào hình trái tim trên bài hát để lưu vào mục yêu thích."
                                } else {
                                    "Hãy quét lại thư viện hoặc chọn tệp âm thanh từ thiết bị."
                                },
                                actionLabel = when {
                                    songSearchQuery.isNotBlank() -> "Xóa tìm kiếm"
                                    favoritesOnly -> null
                                    else -> "Quét lại"
                                },
                                onAction = if (songSearchQuery.isNotBlank())
                                    ({ songSearchQuery = "" })
                                    else if (favoritesOnly) null else onRefresh
                            )
                            }
                        }

                        item {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${visibleSongs.size} bài hát")
                            Box {
                                OutlinedButton(
                                    onClick = { sortMenuExpanded = true },
                                    modifier = Modifier.testTag("library-sort-button")
                                ) {
                                    Text("Sắp xếp: ${songSort.label}")
                                }
                                DropdownMenu(
                                    expanded = sortMenuExpanded,
                                    onDismissRequest = { sortMenuExpanded = false }
                                ) {
                                    LibrarySongSort.entries.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option.label) },
                                            onClick = {
                                                songSort = option
                                                sortMenuExpanded = false
                                            },
                                            modifier = Modifier.testTag(
                                                "library-sort-${option.name.lowercase()}"
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        }

                            itemsIndexed(
                                items = visibleSongs,
                                key = { _, song -> song.uri.toString() }
                            ) { index, song ->

                                SongRow(
                                    song = song,
                                    onClick = {
                                        onSongSelected(
                                            visibleSongs,
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
        }
    }
}

@Composable
private fun ColumnScope.AlbumFavoritesContent(
    songs: List<DeviceSong>,
    favoriteAlbumIds: Set<String>,
    favoriteSongUris: Set<String>,
    onToggleAlbum: (Long) -> Unit,
    onToggleFavorite: (DeviceSong) -> Unit,
    onSongSelected: (List<DeviceSong>, Int) -> Unit,
    onAddToPlaylist: (DeviceSong) -> Unit,
    initialFavoritesOnly: Boolean = false,
    onQueueNext: ((DeviceSong) -> Unit)? = null,
    onQueueEnd: ((DeviceSong) -> Unit)? = null
) {
    var favoritesOnly by rememberSaveable(initialFavoritesOnly) {
        mutableStateOf(initialFavoritesOnly)
    }

    var expandedAlbumId by rememberSaveable {
        mutableStateOf<Long?>(null)
    }

    val groups = songs
        .filter { it.albumId != null }
        .groupBy { it.albumId!! }
        .toList()
        .sortedBy { it.second.first().album.lowercase() }

    val albums = if (favoritesOnly) {
        groups.filter {
            it.first.toString() in favoriteAlbumIds
        }
    } else {
        groups
    }

    FilterChip(
        selected = favoritesOnly,
        onClick = { favoritesOnly = !favoritesOnly },
        label = {
            Text(
                if (favoritesOnly) "Album yêu thích"
                else "Chỉ hiện album yêu thích"
            )
        }
    )

    Text("${albums.size} album")

    if (albums.isEmpty()) {
        MusicFlowStatusPanel(
            kind = MusicFlowStatusKind.EMPTY,
            title = if (favoritesOnly) "Chưa có album yêu thích"
                else "Không có album phù hợp",
            description = if (favoritesOnly) {
                "Chọn trái tim trên album để lưu vào danh sách yêu thích."
            } else {
                "Hãy quét lại thư viện nếu bạn vừa thêm nhạc."
            }
        )
    }

    LazyColumn(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(albums, key = { it.first }) { (albumId, tracks) ->
            val isFavorite =
                albumId.toString() in favoriteAlbumIds

            Column {
                ListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            expandedAlbumId =
                                if (expandedAlbumId == albumId) {
                                    null
                                } else {
                                    albumId
                                }
                        },
                    headlineContent = {
                        Text(tracks.first().album)
                    },
                    supportingContent = {
                        Text(
                            "${tracks.first().artist} · " +
                                "${tracks.size} bài hát"
                        )
                    },
                    trailingContent = {
                        IconButton(
                            onClick = { onToggleAlbum(albumId) },
                            modifier = Modifier.semantics {
                                contentDescription =
                                    if (isFavorite) {
                                        "Bỏ yêu thích album"
                                    } else {
                                        "Yêu thích album"
                                    }
                            }
                        ) {
                            Text(
                                if (isFavorite) "♥" else "♡",
                                color = if (isFavorite) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme
                                        .onSurfaceVariant
                                }
                            )
                        }
                    }
                )

                if (expandedAlbumId == albumId) {
                    TextButton(
                        onClick = {
                            onSongSelected(tracks, 0)
                        }
                    ) {
                        Text("Phát toàn bộ album")
                    }

                    tracks.forEachIndexed { index, song ->
                        SongRow(
                            song = song,
                            onClick = {
                                onSongSelected(tracks, index)
                            },
                            onAddToPlaylist = {
                                onAddToPlaylist(song)
                            },
                            isFavorite = song.uri.toString()
                                in favoriteSongUris,
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
