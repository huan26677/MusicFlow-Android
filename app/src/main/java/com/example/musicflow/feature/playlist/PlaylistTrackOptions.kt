package com.example.musicflow.feature.playlist

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * All track actions use a single discoverable 48dp+ control.
 * Deleting always delegates to the existing confirmation dialog in PlaylistDetailRoute.
 */
@Composable
internal fun PlaylistTrackOptions(
    mediaId: String,
    title: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRequestRemove: () -> Unit,
    onRelink: () -> Unit = {}
) {
    var expanded by remember(mediaId) { mutableStateOf(false) }
    Box {
        TextButton(
            onClick = { expanded = true },
            modifier = Modifier
                .testTag("playlist-track-options-$mediaId")
                .semantics { contentDescription = "Tùy chọn bài $title" }
        ) {
            Text("Tùy chọn bài hát")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Đưa lên một vị trí") },
                enabled = canMoveUp,
                onClick = {
                    expanded = false
                    onMoveUp()
                },
                modifier = Modifier
                    .testTag("playlist-track-move-up")
                    .semantics { contentDescription = "Đưa $title lên" }
            )
            DropdownMenuItem(
                text = { Text("Đưa xuống một vị trí") },
                enabled = canMoveDown,
                onClick = {
                    expanded = false
                    onMoveDown()
                },
                modifier = Modifier
                    .testTag("playlist-track-move-down")
                    .semantics { contentDescription = "Đưa $title xuống" }
            )
            DropdownMenuItem(
                text = { Text("Chọn lại tệp nhạc") },
                onClick = {
                    expanded = false
                    onRelink()
                },
                modifier = Modifier.testTag("playlist-track-relink")
            )
            DropdownMenuItem(
                text = { Text("Xóa khỏi playlist") },
                onClick = {
                    expanded = false
                    onRequestRemove()
                },
                modifier = Modifier.testTag("playlist-track-request-delete")
            )
        }
    }
}
