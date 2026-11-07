package com.example.musicflow.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.musicflow.core.media.DeviceSong
import com.example.musicflow.core.media.PickedMusicRecord
import com.example.musicflow.feature.search.SongSearch
import com.example.musicflow.ui.components.MusicFlowStatusKind
import com.example.musicflow.ui.components.MusicFlowStatusPanel

/** Shortcuts to documents selected by the listener; not a copy of the audio. */
@Composable
fun ColumnScope.PickedMusicContent(
    songs: List<DeviceSong>,
    missing: List<PickedMusicRecord>,
    favoriteSongUris: Set<String>,
    onSongSelected: (List<DeviceSong>, Int) -> Unit,
    onAddToPlaylist: (DeviceSong) -> Unit,
    onToggleFavorite: (DeviceSong) -> Unit,
    onQueueNext: ((DeviceSong) -> Unit)?,
    onQueueEnd: ((DeviceSong) -> Unit)?,
    onForget: (String) -> Unit,
    onPickManual: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val index = remember(songs) { LibrarySongSearchIndex(songs) }
    val visibleSongs = remember(index, query) { index.filter(query) }
    val visibleMissing = remember(missing, query) {
        if (query.isBlank()) missing else missing.filter {
            SongSearch.matches(it.title, it.artist, query)
        }
    }
    Text("Nhạc đã chọn", style = MaterialTheme.typography.titleLarge)
    Text(
        "Tệp bạn chọn qua trình quản lý tệp sẽ được ghi nhớ để nghe lần sau. " +
            "MusicFlow không sao chép hay di chuyển bài hát.",
        style = MaterialTheme.typography.bodySmall
    )
    Text(
        "${songs.size} bài sẵn sàng · ${missing.size} bài cần chọn lại",
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.testTag("picked-music-count")
    )
    OutlinedTextField(
        value = query,
        onValueChange = { query = it.take(80) },
        label = { Text("Tìm trong Nhạc đã chọn") },
        placeholder = { Text("Tên bài hoặc nghệ sĩ, có thể không dấu") },
        singleLine = true,
        trailingIcon = {
            if (query.isNotEmpty()) IconButton(
                onClick = { query = "" },
                modifier = Modifier.testTag("picked-search-clear")
                    .semantics { contentDescription = "Xóa tìm kiếm nhạc đã chọn" }
            ) { Text("✕") }
        },
        modifier = Modifier.fillMaxWidth().testTag("picked-search-field")
    )
    Text(
        "Hiển thị ${visibleSongs.size + visibleMissing.size} / ${songs.size + missing.size} bài",
        style = MaterialTheme.typography.labelSmall
    )
    if (songs.isEmpty() && missing.isEmpty()) {
        MusicFlowStatusPanel(
            kind = MusicFlowStatusKind.EMPTY,
            title = "Chưa có nhạc đã chọn",
            description = "Chạm Chọn tệp nhạc để mở MP3, FLAC, WAV... " +
                "Bạn không cần cấp quyền quét cả thiết bị.",
            actionLabel = "Chọn tệp nhạc",
            onAction = onPickManual
        )
    } else {
        LazyColumn(
            modifier = Modifier.weight(1f).testTag("picked-music-list"),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (query.isNotBlank() && visibleSongs.isEmpty() && visibleMissing.isEmpty()) {
                item {
                    MusicFlowStatusPanel(
                        kind = MusicFlowStatusKind.EMPTY,
                        title = "Không tìm thấy bài phù hợp",
                        description = "Thử từ khóa ngắn hơn hoặc xóa tìm kiếm.",
                        actionLabel = "Xóa tìm kiếm",
                        onAction = { query = "" }
                    )
                }
            }
            itemsIndexed(visibleSongs, key = { _, song -> song.uri.toString() }) { index, song ->
                SongRow(
                    song = song,
                    onClick = { onSongSelected(visibleSongs, index) },
                    onAddToPlaylist = { onAddToPlaylist(song) },
                    isFavorite = song.uri.toString() in favoriteSongUris,
                    onToggleFavorite = { onToggleFavorite(song) },
                    onQueueNext = onQueueNext?.let { action -> { action(song) } },
                    onQueueEnd = onQueueEnd?.let { action -> { action(song) } }
                )
            }
            if (visibleMissing.isNotEmpty()) {
                item {
                    MusicFlowStatusPanel(
                        kind = MusicFlowStatusKind.INFO,
                        title = "Có bài không mở được",
                        description = "Tệp có thể đã được chuyển, xóa hoặc thu hồi quyền. " +
                            "Chọn lại để thêm tệp mới; bỏ lối tắt không xóa nhạc gốc.",
                        actionLabel = "Chọn lại tệp nhạc",
                        onAction = onPickManual
                    )
                }
                items(visibleMissing, key = { it.uri }) { item ->
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(item.title, style = MaterialTheme.typography.bodyLarge)
                        Text("Không truy cập được tệp này",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(
                            onClick = { onForget(item.uri) },
                            modifier = Modifier.heightIn(min = 48.dp)
                                .testTag("picked-forget-${item.uri.hashCode()}")
                        ) { Text("Bỏ lối tắt · không xóa tệp") }
                    }
                }
            }
        }
    }
}
