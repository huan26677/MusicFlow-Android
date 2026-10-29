package com.example.musicflow.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicflow.core.playback.PlayerUiState
import com.example.musicflow.ui.components.SongArtwork

@Composable
fun MiniPlayer(
    state: PlayerUiState,
    onOpenPlayer: () -> Unit,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (state.durationMs > 0L) {
        (
                state.positionMs.toFloat() /
                        state.durationMs.toFloat()
                ).coerceIn(0f, 1f)
    } else {
        0f
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenPlayer)
                    .semantics {
                        contentDescription = "Mở trình phát: ${state.title}, ${state.artist}"
                    }
                    .padding(
                        horizontal = 12.dp,
                        vertical = 8.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SongArtwork(
                    uri = state.artworkUri,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                )

                Spacer(Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = state.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = state.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(4.dp))

                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.semantics {
                        contentDescription = if (state.isPlaying) {
                            "Tạm dừng nhạc"
                        } else {
                            "Phát nhạc"
                        }
                    }
                ) {
                    Text(
                        text = if (state.isPlaying) "Ⅱ" else "▶",
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Chỉ hiển thị tiến trình khi đã biết
            // thời lượng bài hát.
            if (state.durationMs > 0L) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurface
                                .copy(alpha = 0.12f)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .background(
                                MaterialTheme.colorScheme.primary
                            )
                    )
                }
            }
        }
    }
}
