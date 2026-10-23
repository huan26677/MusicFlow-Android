package com.example.musicflow.feature.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.musicflow.core.media.DeviceSong
import com.example.musicflow.ui.components.SongArtwork

@Composable
fun SongRow(
    song: DeviceSong,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
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
                    maxLines = 1,
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

            Spacer(Modifier.width(8.dp))

            Text(
                text = formatSongDuration(song.durationMs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
