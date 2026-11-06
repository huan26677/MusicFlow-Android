
package com.example.musicflow.feature.playlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

import com.example.musicflow.core.history.local.RecentHistoryDatabase
import com.example.musicflow.core.playlist.LocalPlaylist
import com.example.musicflow.core.playlist.PlaylistRules
import com.example.musicflow.core.playlist.PlaylistTrack
import com.example.musicflow.core.playlist.RoomPlaylistRepository
import com.example.musicflow.ui.components.MusicFlowStatusKind
import com.example.musicflow.ui.components.MusicFlowStatusPanel

@Composable
internal fun rememberPlaylistViewModel(): PlaylistViewModel {
    val context = LocalContext.current.applicationContext

    val repository = remember(context) {
        RoomPlaylistRepository(
            RecentHistoryDatabase.getInstance(context)
        )
    }

    val factory = remember(repository) {
        PlaylistViewModel.factory(repository)
    }

    return viewModel(factory = factory)
}

@Composable
fun PlaylistListRoute(
    onBack: () -> Unit,
    onOpenPlaylist: (String) -> Unit
) {
    val vm = rememberPlaylistViewModel()

    val state by vm.playlistsState
        .collectAsStateWithLifecycle()

    val actionError by vm.actionError
        .collectAsStateWithLifecycle()

    var showCreateDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var renameTarget by remember {
        mutableStateOf<LocalPlaylist?>(null)
    }

    var deleteTarget by remember {
        mutableStateOf<LocalPlaylist?>(null)
    }

    var nameInput by rememberSaveable {
        mutableStateOf("")
    }
    var searchText by rememberSaveable {
        mutableStateOf("")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val compact = maxWidth < 360.dp || LocalDensity.current.fontScale >= 1.35f
            if (compact) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onBack) { Text("‹ Trở về") }
                    Text("Danh sách phát", style = MaterialTheme.typography.headlineSmall)
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(onClick = onBack) { Text("‹ Trở về") }
                    Text("Danh sách phát", style = MaterialTheme.typography.headlineSmall)
                }
            }
        }

        Button(
            onClick = {
                nameInput = ""
                showCreateDialog = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Tạo playlist mới")
        }

        if (actionError != null) {
            Text(
                text = actionError!!,
                color = MaterialTheme.colorScheme.error
            )

            TextButton(onClick = vm::dismissError) {
                Text("Đóng thông báo")
            }
        }

        when (val current = state) {

            PlaylistsUiState.Loading -> {
                MusicFlowStatusPanel(
                    kind = MusicFlowStatusKind.LOADING,
                    title = "Đang tải danh sách phát...",
                    description = "Playlist của bạn được lưu ngay trên thiết bị."
                )
            }

            is PlaylistsUiState.Error -> {
                MusicFlowStatusPanel(
                    kind = MusicFlowStatusKind.ERROR,
                    title = "Không thể tải playlist",
                    description = current.message
                )
            }

            is PlaylistsUiState.Ready -> {
                if (current.playlists.isEmpty()) {
                    MusicFlowStatusPanel(
                        kind = MusicFlowStatusKind.EMPTY,
                        title = "Chưa có playlist nào",
                        description = "Hãy dùng nút Tạo playlist mới ở trên để " +
                            "bắt đầu lưu nhạc yêu thích. Không cần đăng nhập."
                    )
                } else {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        label = { Text("Tìm playlist") },
                        placeholder = { Text("Nhập tên danh sách phát") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        modifier = Modifier.fillMaxWidth().testTag("playlist-search")
                    )
                    val visiblePlaylists = remember(current.playlists, searchText) {
                        filterPlaylistNames(current.playlists, searchText)
                    }
                    Text(
                        if (searchText.isBlank()) {
                            "${current.playlists.size} playlist"
                        } else {
                            "Tìm thấy ${visiblePlaylists.size}/${current.playlists.size} playlist"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (visiblePlaylists.isEmpty()) {
                        MusicFlowStatusPanel(
                            kind = MusicFlowStatusKind.EMPTY,
                            title = "Không tìm thấy playlist",
                            description = "Thử tên khác hoặc xóa từ khóa tìm kiếm.",
                            actionLabel = "Xóa tìm kiếm",
                            onAction = { searchText = "" }
                        )
                    } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement =
                            Arrangement.spacedBy(4.dp)
                    ) {
                        items(
                            items = visiblePlaylists,
                            key = { it.id }
                        ) { playlist ->
                            var expanded by remember {
                                mutableStateOf(false)
                            }

                            ListItem(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("playlist-${playlist.id}")
                                    .clickable {
                                        onOpenPlaylist(playlist.id)
                                    },
                                headlineContent = {
                                    Text(
                                        playlist.name,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                supportingContent = {
                                    Text("Chạm để xem bài hát")
                                },
                                trailingContent = {
                                    Box {
                                        IconButton(
                                            onClick = { expanded = true },
                                            modifier = Modifier
                                                .testTag("playlist-options-${playlist.id}")
                                                .semantics {
                                                    contentDescription = "Tùy chọn ${playlist.name}"
                                                }
                                        ) {
                                            Icon(Icons.Filled.MoreVert, contentDescription = null)
                                        }

                                        DropdownMenu(
                                            expanded = expanded,
                                            onDismissRequest = {
                                                expanded = false
                                            }
                                        ) {
                                            DropdownMenuItem(
                                                text = {
                                                    Text("Đổi tên")
                                                },
                                                onClick = {
                                                    expanded = false
                                                    nameInput = playlist.name
                                                    renameTarget = playlist
                                                }
                                            )

                                            DropdownMenuItem(
                                                text = {
                                                    Text("Xóa playlist")
                                                },
                                                onClick = {
                                                    expanded = false
                                                    deleteTarget = playlist
                                                }
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                    }
                }
            }
        }
    }

    val editing = renameTarget
    if (showCreateDialog || editing != null) {
        val normalized = PlaylistRules.normalizedNameOrNull(nameInput)
        val validName = normalized != null &&
            (editing == null || normalized != editing.name.trim())

        fun saveName() {
            if (!validName) return
            if (editing == null) vm.create(nameInput)
            else vm.rename(editing.id, nameInput)
            showCreateDialog = false
            renameTarget = null
        }

        AlertDialog(
            onDismissRequest = {
                showCreateDialog = false
                renameTarget = null
            },
            title = {
                Text(
                    if (editing == null) {
                        "Tạo playlist"
                    } else {
                        "Đổi tên playlist"
                    }
                )
            },
            text = {
                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = {
                            nameInput = it
                        },
                        label = { Text("Tên playlist") },
                        singleLine = true,
                        isError = normalized == null,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { saveName() }),
                        modifier = Modifier.fillMaxWidth().testTag("playlist-name-input")
                    )

                    Text(
                        "Tên phải có từ 1 đến " +
                                "${PlaylistRules.MAX_NAME_LENGTH} ký tự.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (editing != null && normalized == editing.name.trim()) {
                        Text("Tên chưa thay đổi.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = validName,
                    onClick = { saveName() },
                    modifier = Modifier.testTag("playlist-name-save")
                ) {
                    Text(
                        if (editing == null) "Tạo"
                        else "Lưu"
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCreateDialog = false
                        renameTarget = null
                    }
                ) {
                    Text("Hủy")
                }
            }
        )
    }

    val deleting = deleteTarget
    if (deleting != null) {
        AlertDialog(
            onDismissRequest = {
                deleteTarget = null
            },
            title = {
                Text("Xóa playlist?")
            },
            text = {
                Text(
                    "Xóa \"${deleting.name}\"? " +
                            "Các file nhạc và lịch sử nghe " +
                            "sẽ không bị ảnh hưởng."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.delete(deleting.id)
                        deleteTarget = null
                    }
                ) {
                    Text("Xóa")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        deleteTarget = null
                    }
                ) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
fun PlaylistDetailRoute(
    playlistId: String,
    onBack: () -> Unit,
    onPlayTracks: (List<PlaylistTrack>, Int) -> Unit,
    onFindSongs: () -> Unit = {}
) {
    val vm = rememberPlaylistViewModel()

    val playlistsState by vm.playlistsState
        .collectAsStateWithLifecycle()

    // Ghi nhớ Flow theo playlistId để không tạo
    // subscription mới mỗi lần recomposition.
    val trackFlow = remember(vm, playlistId) {
        vm.tracksState(playlistId)
    }

    val tracksState by trackFlow.collectAsStateWithLifecycle(
        initialValue = PlaylistTracksUiState.Loading
    )

    val actionError by vm.actionError
        .collectAsStateWithLifecycle()

    var removeTarget by remember {
        mutableStateOf<PlaylistTrack?>(null)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("‹ Danh sách phát")
        }

        if (actionError != null) {
            Text(
                text = actionError!!,
                color = MaterialTheme.colorScheme.error
            )

            TextButton(onClick = vm::dismissError) {
                Text("Đóng thông báo")
            }
        }

        when (val current = playlistsState) {

            PlaylistsUiState.Loading -> {
                MusicFlowStatusPanel(
                    kind = MusicFlowStatusKind.LOADING,
                    title = "Đang tải playlist...",
                    description = "Đang đọc danh sách phát trên thiết bị."
                )
            }

            is PlaylistsUiState.Error -> {
                MusicFlowStatusPanel(
                    kind = MusicFlowStatusKind.ERROR,
                    title = "Không thể mở playlist",
                    description = current.message
                )
            }

            is PlaylistsUiState.Ready -> {
                val playlist = current.playlists
                    .firstOrNull { it.id == playlistId }

                if (playlist == null) {
                    MusicFlowStatusPanel(
                        kind = MusicFlowStatusKind.EMPTY,
                        title = "Playlist không còn tồn tại",
                        description = "Danh sách phát này có thể đã bị xóa.",
                        actionLabel = "Về danh sách phát",
                        onAction = onBack
                    )
                } else {
                    Text(
                        text = playlist.name,
                        style = MaterialTheme.typography.headlineSmall
                    )

                    when (val tracks = tracksState) {

                        PlaylistTracksUiState.Loading -> {
                            MusicFlowStatusPanel(
                                kind = MusicFlowStatusKind.LOADING,
                                title = "Đang tải bài hát...",
                                description = "Đang đọc các bài trong playlist."
                            )
                        }

                        is PlaylistTracksUiState.Error -> {
                            MusicFlowStatusPanel(
                                kind = MusicFlowStatusKind.ERROR,
                                title = "Không thể tải bài hát",
                                description = tracks.message
                            )
                        }

                        is PlaylistTracksUiState.Ready -> {
                            if (tracks.tracks.isEmpty()) {
                                PlaylistEmptyState(onFindSongs = onFindSongs)
                            } else {
                                Text(
                                    "${tracks.tracks.size} bài hát",
                                    style =
                                        MaterialTheme.typography.bodySmall
                                )

                                OutlinedButton(
                                    onClick = onFindSongs,
                                    modifier = Modifier.fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("playlist-detail-add-songs")
                                ) { Text("Thêm bài hát vào playlist") }

                                Button(
                                    onClick = {
                                        onPlayTracks(
                                            tracks.tracks,
                                            0
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("▶ Phát toàn bộ")
                                }

                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("playlist-detail-tracks"),
                                    verticalArrangement =
                                        Arrangement.spacedBy(4.dp)
                                ) {
                                    itemsIndexed(
                                        items = tracks.tracks,
                                        key = { _, track -> track.mediaId }
                                    ) { index, track ->

                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("playlist-track-${track.mediaId}")
                                        ) {
                                            // Keep a full-width title; actions live in one
                                            // menu to prevent overflow at large font scales.
                                            ListItem(
                                                modifier = Modifier.fillMaxWidth().clickable {
                                                    onPlayTracks(tracks.tracks, index)
                                                },
                                                headlineContent = {
                                                    Text(
                                                        text = track.title,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                },
                                                supportingContent = {
                                                    Text(
                                                        text = track.artist,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                PlaylistTrackOptions(
                                                    mediaId = track.mediaId,
                                                    title = track.title,
                                                    canMoveUp = index > 0,
                                                    canMoveDown = index < tracks.tracks.lastIndex,
                                                    onMoveUp = {
                                                        vm.moveTrack(
                                                            playlistId = playlistId,
                                                            mediaId = track.mediaId,
                                                            offset = -1
                                                        )
                                                    },
                                                    onMoveDown = {
                                                        vm.moveTrack(
                                                            playlistId = playlistId,
                                                            mediaId = track.mediaId,
                                                            offset = 1
                                                        )
                                                    },
                                                    onRequestRemove = { removeTarget = track }
                                                )
                                            }
                                            HorizontalDivider()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    removeTarget?.let { track ->

        AlertDialog(
            onDismissRequest = {
                removeTarget = null
            },
            title = {
                Text("Xóa bài khỏi playlist?")
            },
            text = {
                Text(
                    "Xóa \"${track.title}\" khỏi danh sách phát? " +
                        "File nhạc trên thiết bị và lịch sử nghe " +
                        "sẽ không bị ảnh hưởng."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.removeTrack(
                            playlistId = playlistId,
                            mediaId = track.mediaId
                        )

                        removeTarget = null
                    }
                ) {
                    Text("Xóa khỏi playlist")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        removeTarget = null
                    }
                ) {
                    Text("Hủy")
                }
            }
        )
    }
}

/** A clear next action without creating a fake song or opening an extra dialog. */
@Composable
internal fun PlaylistEmptyState(onFindSongs: () -> Unit) {
    MusicFlowStatusPanel(
        kind = MusicFlowStatusKind.EMPTY,
        title = "Playlist chưa có bài hát",
        description = "Thêm ngay bài hát từ thư viện hoặc chọn tệp âm thanh riêng. " +
            "Không cần chọn lại playlist, không cần đăng nhập và không di chuyển tệp.",
        actionLabel = "Tìm bài hát để thêm",
        onAction = onFindSongs
    )
}
