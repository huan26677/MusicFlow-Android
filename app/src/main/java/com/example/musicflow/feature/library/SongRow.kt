
package com.example.musicflow.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import com.example.musicflow.core.media.DeviceSong
import com.example.musicflow.ui.components.SongArtwork

@Composable
fun SongRow(
    song: DeviceSong,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onAddToPlaylist: (() -> Unit)? = null,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    onQueueNext: (() -> Unit)? = null,
    onQueueEnd: (() -> Unit)? = null
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
    val compact = maxWidth < 360.dp || LocalDensity.current.fontScale >= 1.35f
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .heightIn(min = 72.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Vùng chính vẫn dùng để phát bài hát.
        TextButton(
            onClick = onClick,
            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                .semantics {
                    contentDescription = "Phát ${song.title}, ${song.artist}"
                },
            contentPadding = PaddingValues(
                horizontal = 8.dp,
                vertical = 8.dp
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SongArtwork(
                    uri = song.uri,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(10.dp))
                )

                Spacer(Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = if (compact) 2 else 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (!compact) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = formatSongDuration(song.durationMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (compact && (onToggleFavorite != null || onAddToPlaylist != null ||
                    onQueueNext != null || onQueueEnd != null)) {
            var optionsOpen by remember { mutableStateOf(false) }
            Box {
                IconButton(
                    onClick = { optionsOpen = true },
                    modifier = Modifier.testTag("song-compact-actions")
                        .semantics {
                            contentDescription = "Tùy chọn cho ${song.title}"
                            stateDescription = if (isFavorite) "Đã yêu thích" else "Chưa yêu thích"
                        }
                ) { Text("⋮", style = MaterialTheme.typography.titleLarge) }
                DropdownMenu(
                    expanded = optionsOpen,
                    onDismissRequest = { optionsOpen = false }
                ) {
                    if (onToggleFavorite != null) DropdownMenuItem(
                        text = { Text(if (isFavorite) "Bỏ yêu thích" else "Yêu thích") },
                        onClick = { optionsOpen = false; onToggleFavorite() }
                    )
                    if (onAddToPlaylist != null) DropdownMenuItem(
                        text = { Text("Thêm vào playlist") },
                        onClick = { optionsOpen = false; onAddToPlaylist() }
                    )
                    if (onQueueNext != null) DropdownMenuItem(
                        text = { Text("Phát tiếp") },
                        onClick = { optionsOpen = false; onQueueNext() }
                    )
                    if (onQueueEnd != null) DropdownMenuItem(
                        text = { Text("Thêm vào hàng đợi") },
                        onClick = { optionsOpen = false; onQueueEnd() }
                    )
                }
            }
        }
        if (!compact) {
        if (onToggleFavorite != null) {
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.semantics {
                    contentDescription =
                        if (isFavorite) {
                            "Bỏ yêu thích ${song.title}"
                        } else {
                            "Yêu thích ${song.title}"
                        }
                    stateDescription = if (isFavorite) "Đã yêu thích" else "Chưa yêu thích"
                }
            ) {
                Text(
                    text = if (isFavorite) "♥" else "♡",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (isFavorite) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }

        // Nút riêng, không nằm bên trong TextButton phát nhạc.
        if (onAddToPlaylist != null) {
            IconButton(
                onClick = onAddToPlaylist,
                modifier = Modifier.semantics {
                    contentDescription =
                        "Thêm ${song.title} vào playlist"
                }
            ) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }

        if (onQueueNext != null || onQueueEnd != null) {
            var queueMenuOpen by remember { mutableStateOf(false) }
            Box {
                IconButton(
                    onClick = { queueMenuOpen = true },
                    modifier = Modifier.semantics {
                        contentDescription = "Hàng đợi cho ${song.title}"
                    }
                ) { Text("⋮", style = MaterialTheme.typography.titleLarge) }
                DropdownMenu(
                    expanded = queueMenuOpen,
                    onDismissRequest = { queueMenuOpen = false }
                ) {
                    if (onQueueNext != null) {
                        DropdownMenuItem(
                            text = { Text("Phát tiếp") },
                            onClick = {
                                queueMenuOpen = false
                                onQueueNext()
                            }
                        )
                    }
                    if (onQueueEnd != null) {
                        DropdownMenuItem(
                            text = { Text("Thêm vào hàng đợi") },
                            onClick = {
                                queueMenuOpen = false
                                onQueueEnd()
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

private fun formatSongDuration(durationMs: Long): String {
    if (durationMs <= 0L) {
        return "--:--"
    }

    val totalSeconds = durationMs / 1000L
    val totalMinutes = totalSeconds / 60L

    val seconds = (totalSeconds % 60L)
        .toString()
        .padStart(2, '0')

    return if (totalMinutes >= 60L) {
        val hours = totalMinutes / 60L
        val minutes = (totalMinutes % 60L)
            .toString()
            .padStart(2, '0')

        "$hours:$minutes:$seconds"
    } else {
        "$totalMinutes:$seconds"
    }
}
