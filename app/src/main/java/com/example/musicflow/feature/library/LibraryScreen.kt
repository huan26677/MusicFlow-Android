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
import com.example.musicflow.feature.playlist.PlaylistPickerDialog
import com.example.musicflow.feature.playlist.rememberPlaylistViewModel

@Composable
fun LibraryRoute(
    onBack: () -> Unit,
    onPickManual: () -> Unit,
    onSongSelected: (List<DeviceSong>, Int) -> Unit,
    initialFavoritesOnly: Boolean = false,
    initialShowAlbums: Boolean = false,
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

    val factory = remember(repository) {
        LibraryViewModel.factory(repository)
    }

    val libraryViewModel: LibraryViewModel =
        viewModel(factory = factory)

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
        if (!hasPermission && !initialFavoritesOnly) {
            permissionLauncher.launch(permission)
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            libraryViewModel.refresh()
        }
    }

    LibraryScreen(
        uiState = uiState,
        hasPermission = hasPermission,
        onBack = onBack,
        onPickManual = onPickManual,
        initialFavoritesOnly = initialFavoritesOnly,
        initialShowAlbums = initialShowAlbums,
        onRefresh = {
            if (hasPermission) {
                libraryViewModel.refresh()
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
    initialShowAlbums: Boolean = false
) {
    var favoritesOnly by rememberSaveable(initialFavoritesOnly) {
        mutableStateOf(initialFavoritesOnly)
    }

    var selectedCategory by rememberSaveable(initialShowAlbums) {
        mutableStateOf(if (initialShowAlbums) "albums" else "songs")
    }

    val scannedSongs = (uiState as? LibraryUiState.Success)
        ?.songs.orEmpty()

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Nhạc thiết bị",
                    style = MaterialTheme.typography.headlineSmall
                )

                TextButton(onClick = onBack) {
                    Text("Quay lại")
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onRefresh) {
                    Text("Quét lại")
                }

                Button(onClick = onPickManual) {
                    Text("Chọn MP3")
                }
            }

            if (!hasPermission && (!favoritesOnly || selectedCategory != "songs")) {
                Text(
                    "MusicFlow cần quyền đọc âm thanh " +
                            "để quét thư viện thiết bị."
                )

                Button(onClick = onRefresh) {
                    Text("Cấp quyền")
                }

                Text(
                    "Nếu đã từ chối quyền nhiều lần, " +
                            "hãy kiểm tra quyền ứng dụng trong Cài đặt.",
                    style = MaterialTheme.typography.bodySmall
                )

            } else {
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

                when (val current = displayUiState) {

                    LibraryUiState.Idle -> {
                        Text("Chưa quét thư viện.")
                    }

                    LibraryUiState.Loading -> {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Đang quét nhạc...")
                    }

                    LibraryUiState.Empty -> {
                        Text(
                            "Không tìm thấy bài hát nào " +
                                    "được Android phân loại là nhạc."
                        )
                    }

                    is LibraryUiState.Error -> {
                        Text(
                            text = current.message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    is LibraryUiState.Success -> {
                        if (selectedCategory == "albums") {
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
                            val visibleSongs = if (favoritesOnly) {
                            current.songs.filter {
                                it.uri.toString() in favoriteSongUris
                            } + pickedFavorites
                        } else {
                            current.songs
                        }

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

                        if (visibleSongs.isEmpty()) {
                            Text("Chưa có bài hát yêu thích phù hợp.")
                        }

                        Text("${visibleSongs.size} bài hát")

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement =
                                Arrangement.spacedBy(4.dp)
                        ) {
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
        Text("Không có album phù hợp.")
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
