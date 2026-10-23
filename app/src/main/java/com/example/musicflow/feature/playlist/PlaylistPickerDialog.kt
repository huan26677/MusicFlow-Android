
package com.example.musicflow.feature.playlist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.example.musicflow.core.media.DeviceSong

@Composable
fun PlaylistPickerDialog(
    song: DeviceSong,
    viewModel: PlaylistViewModel,
    onDismiss: () -> Unit
) {
    val playlistsState by viewModel.playlistsState
        .collectAsStateWithLifecycle()

    val addState by viewModel.addTrackState
        .collectAsStateWithLifecycle()

    // Mỗi lần mở hộp thoại, xóa thông báo cũ.
    LaunchedEffect(song.id) {
        viewModel.resetAddTrackState()
    }

    val isSaving = addState == AddTrackUiState.Saving

    AlertDialog(
        onDismissRequest = {
            if (!isSaving) onDismiss()
        },
        title = {
            Text("Thêm vào playlist")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider()

                when (val current = playlistsState) {

                    PlaylistsUiState.Loading -> {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Đang tải playlist...")
                    }

                    is PlaylistsUiState.Error -> {
                        Text(
                            text = current.message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    is PlaylistsUiState.Ready -> {
                        if (current.playlists.isEmpty()) {
                            Text(
                                "Bạn chưa có playlist. " +
                                        "Hãy tạo playlist từ Home " +
                                        "trước khi thêm bài."
                            )
                        } else {
                            Text(
                                "Chọn playlist:",
                                style = MaterialTheme.typography.labelLarge
                            )

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 260.dp)
                            ) {
                                items(
                                    items = current.playlists,
                                    key = { it.id }
                                ) { playlist ->
                                    TextButton(
                                        enabled = !isSaving,
                                        onClick = {
                                            viewModel.addDeviceSong(
                                                playlistId = playlist.id,
                                                playlistName = playlist.name,
                                                song = song
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = playlist.name,
                                            modifier = Modifier.fillMaxWidth(),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                when (val result = addState) {

                    AddTrackUiState.Idle -> Unit

                    AddTrackUiState.Saving -> {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Đang thêm bài hát...")
                    }

                    is AddTrackUiState.Success -> {
                        Text(
                            text = "Đã thêm vào \"${result.playlistName}\".",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    is AddTrackUiState.Error -> {
                        Text(
                            text = result.message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSaving,
                onClick = onDismiss
            ) {
                Text("Đóng")
            }
        }
    )
}
