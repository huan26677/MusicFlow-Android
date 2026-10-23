
package com.example.musicflow.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import com.example.musicflow.core.playback.PlayerUiState

@Composable
fun HomeScreen(
    state: PlayerUiState,
    onOpenLibrary: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenPlayer: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "MusicFlow",
            style = MaterialTheme.typography.headlineLarge
        )

        Text(
            text = "Khám phá và thưởng thức nhạc của bạn.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Khám phá nhạc",
                    style = MaterialTheme.typography.titleLarge
                )

                Button(
                    onClick = onOpenLibrary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Mở thư viện thiết bị")
                }

                OutlinedButton(
                    onClick = onOpenSearch,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tìm kiếm bài hát")
                }
            }
        }

        if (state.hasMedia) {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Bài hát hiện tại",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = state.title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = state.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = state.status,
                        style = MaterialTheme.typography.bodySmall
                    )

                    TextButton(
                        onClick = onOpenPlayer
                    ) {
                        Text("Mở Now Playing")
                    }
                }
            }
        } else {
            Text(
                text = "Chưa có bài hát nào đang được chọn.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
