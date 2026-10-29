package com.example.musicflow.feature.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.musicflow.core.playback.PlayerUiState
import com.example.musicflow.core.playback.QueueEditRules
import com.example.musicflow.ui.components.MusicFlowStatusKind
import com.example.musicflow.ui.components.MusicFlowStatusPanel

/**
 * A presentation of the MediaController timeline, never an independent queue.
 * Index-based actions carry the snapshot version so late/stale taps are rejected.
 */
@Composable
fun QueueManagerScreen(
    state: PlayerUiState,
    onBack: () -> Unit,
    onMove: (Int, Int, Long) -> Boolean,
    onRemove: (Int, Long) -> Boolean,
    onClearOthers: (Long) -> Boolean
) {
    var showClearConfirmation by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf(false) }
    val current = state.currentIndex - 1
    val canEdit = state.connected && state.canEditQueue

    fun accept(result: Boolean) {
        actionError = !result
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBack) { Text("‹ Trở về trình phát") }
        Text("Hàng đợi", style = MaterialTheme.typography.headlineMedium)
        Text(
            "${state.queueItems.size} bài hát · Bài hiện tại: ${state.currentIndex}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            "Sắp xếp bằng nút lên/xuống. Bài đang phát được giữ nguyên. " +
                "Khi bật trộn bài, hãy tắt trộn để sắp xếp thứ tự.",
            style = MaterialTheme.typography.bodySmall
        )

        if (actionError) {
            Text(
                "Hàng đợi đã thay đổi hoặc thao tác không khả dụng. Hãy thử lại.",
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            onClick = { showClearConfirmation = true },
            enabled = canEdit && QueueEditRules.canClearOthers(
                state.queueItems.size, current
            )
        ) { Text("Dọn hàng đợi (giữ bài đang phát)") }

        HorizontalDivider()
        if (!state.connected) {
            MusicFlowStatusPanel(
                kind = MusicFlowStatusKind.LOADING,
                title = "Đang kết nối với bộ phát nhạc...",
                description = "Hàng đợi sẽ hiển thị khi kết nối MediaSession sẵn sàng."
            )
        } else if (state.queueItems.isEmpty()) {
            MusicFlowStatusPanel(
                kind = MusicFlowStatusKind.EMPTY,
                title = "Hàng đợi đang trống",
                description = "Hãy chọn nhạc từ Thư viện hoặc Tìm kiếm để bắt đầu nghe."
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(state.queueItems, key = { index, _ -> index }) {
                    index, item ->
                    val isCurrent = index == current
                    val canMoveUp = canEdit && QueueEditRules.canMove(
                        state.queueItems.size, current, index, index - 1,
                        state.shuffleEnabled
                    )
                    val canMoveDown = canEdit && QueueEditRules.canMove(
                        state.queueItems.size, current, index, index + 1,
                        state.shuffleEnabled
                    )
                    val canRemove = canEdit && QueueEditRules.canRemove(
                        state.queueItems.size, current, index
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${index + 1}",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(end = 10.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                item.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = if (isCurrent) FontWeight.Bold
                                    else FontWeight.Normal,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                if (isCurrent) "Đang phát · ${item.artist}" else item.artist,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        IconButton(
                            onClick = {
                                accept(onMove(index, index - 1, state.queueVersion))
                            },
                            enabled = canMoveUp,
                            modifier = Modifier.semantics {
                                contentDescription = "Đưa ${item.title} lên"
                            }
                        ) { Text("↑") }
                        IconButton(
                            onClick = {
                                accept(onMove(index, index + 1, state.queueVersion))
                            },
                            enabled = canMoveDown,
                            modifier = Modifier.semantics {
                                contentDescription = "Đưa ${item.title} xuống"
                            }
                        ) { Text("↓") }
                        IconButton(
                            onClick = {
                                accept(onRemove(index, state.queueVersion))
                            },
                            enabled = canRemove,
                            modifier = Modifier.semantics {
                                contentDescription = "Xóa ${item.title} khỏi hàng đợi"
                            }
                        ) { Text("×") }
                    }
                    HorizontalDivider()
                }
            }
        }
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Dọn hàng đợi?") },
            text = {
                Text("Xóa tất cả các bài khác khỏi hàng đợi. " +
                    "Bài đang phát và vị trí nghe sẽ được giữ lại.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showClearConfirmation = false
                    accept(onClearOthers(state.queueVersion))
                }) { Text("Dọn hàng đợi") }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}
