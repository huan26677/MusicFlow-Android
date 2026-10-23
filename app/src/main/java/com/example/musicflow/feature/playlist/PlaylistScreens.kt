
package com.example.musicflow.feature.playlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

import com.example.musicflow.core.history.local.RecentHistoryDatabase
import com.example.musicflow.core.playlist.LocalPlaylist
import com.example.musicflow.core.playlist.PlaylistRules
import com.example.musicflow.core.playlist.RoomPlaylistRepository

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(onClick = onBack) {
                Text("‹ Trở về")
            }

            Text(
                "Danh sách phát",
                style = MaterialTheme.typography.headlineSmall
            )
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
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }

            is PlaylistsUiState.Error -> {
                Text(
                    current.message,
                    color = MaterialTheme.colorScheme.error
                )
            }

            is PlaylistsUiState.Ready -> {
                if (current.playlists.isEmpty()) {
                    Text(
                        "Chưa có playlist nào. " +
                                "Hãy tạo danh sách phát đầu tiên."
                    )
                } else {
                    Text(
                        "${current.playlists.size} playlist",
                        style = MaterialTheme.typography.bodySmall
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement =
                            Arrangement.spacedBy(4.dp)
                    ) {
                        items(
                            items = current.playlists,
                            key = { it.id }
                        ) { playlist ->
                            var expanded by remember {
                                mutableStateOf(false)
                            }

                            ListItem(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onOpenPlaylist(playlist.id)
                                    },
                                headlineContent = {
                                    Text(
                                        playlist.name,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                supportingContent = {
                                    Text("Chạm để xem bài hát")
                                },
                                trailingContent = {
                                    Box {
                                        TextButton(
                                            onClick = {
                                                expanded = true
                                            }
                                        ) {
                                            Text("Tùy chọn")
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

    val editing = renameTarget
    if (showCreateDialog || editing != null) {
        val validName =
            PlaylistRules.normalizedNameOrNull(nameInput) != null

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
                        isError = !validName,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        "Tên phải có từ 1 đến " +
                                "${PlaylistRules.MAX_NAME_LENGTH} ký tự.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = validName,
                    onClick = {
                        if (editing == null) {
                            vm.create(nameInput)
                        } else {
                            vm.rename(editing.id, nameInput)
                        }

                        showCreateDialog = false
                        renameTarget = null
                    }
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
    onBack: () -> Unit
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("‹ Danh sách phát")
        }

        when (val current = playlistsState) {

            PlaylistsUiState.Loading -> {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }

            is PlaylistsUiState.Error -> {
                Text(
                    current.message,
                    color = MaterialTheme.colorScheme.error
                )
            }

            is PlaylistsUiState.Ready -> {
                val playlist = current.playlists
                    .firstOrNull { it.id == playlistId }

                if (playlist == null) {
                    Text("Playlist không còn tồn tại.")
                } else {
                    Text(
                        text = playlist.name,
                        style = MaterialTheme.typography.headlineSmall
                    )

                    when (val tracks = tracksState) {

                        PlaylistTracksUiState.Loading -> {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        is PlaylistTracksUiState.Error -> {
                            Text(
                                tracks.message,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        is PlaylistTracksUiState.Ready -> {
                            if (tracks.tracks.isEmpty()) {
                                Text(
                                    "Playlist chưa có bài hát. " +
                                            "Bạn sẽ có thể thêm bài từ " +
                                            "Library và Search ở bước tiếp theo."
                                )
                            } else {
                                Text(
                                    "${tracks.tracks.size} bài hát",
                                    style =
                                        MaterialTheme.typography.bodySmall
                                )

                                LazyColumn(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    items(
                                        items = tracks.tracks,
                                        key = { it.mediaId }
                                    ) { track ->
                                        ListItem(
                                            headlineContent = {
                                                Text(
                                                    track.title,
                                                    maxLines = 1,
                                                    overflow =
                                                        TextOverflow.Ellipsis
                                                )
                                            },
                                            supportingContent = {
                                                Text(
                                                    track.artist,
                                                    maxLines = 1,
                                                    overflow =
                                                        TextOverflow.Ellipsis
                                                )
                                            }
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
}
