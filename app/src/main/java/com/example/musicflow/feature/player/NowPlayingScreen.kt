package com.example.musicflow.feature.player

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.musicflow.core.favorites.LocalFavoritesStore
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import androidx.media3.common.Player
import com.example.musicflow.core.playback.PlayerUiState
import com.example.musicflow.ui.components.SongArtwork

@Composable
fun NowPlayingScreen(
    state: PlayerUiState,
    onBack: () -> Unit,
    onPickFiles: () -> Unit,
    onOpenLibrary: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit
) {
    val context = LocalContext.current

    val favoritesStore = remember(context) {
        LocalFavoritesStore.getInstance(context)
    }

    val favoriteSongUris by favoritesStore.songUris
        .collectAsStateWithLifecycle()

    val favoriteUri = state.audioUri?.takeIf {
        it.scheme == "content"
    }

    val isFavorite = favoriteUri != null &&
        favoriteUri.toString() in favoriteSongUris

    var isDragging by remember {
        mutableStateOf(false)
    }

    var sliderValue by remember {
        mutableFloatStateOf(0f)
    }

    LaunchedEffect(state.currentIndex) {
        isDragging = false
    }

    val maximum = state.durationMs
        .coerceAtLeast(1L)
        .toFloat()

    val displayedPosition = (
            if (isDragging) sliderValue
            else state.positionMs.toFloat()
            ).coerceIn(0f, maximum)

    val repeatLabel = when (state.repeatMode) {
        Player.REPEAT_MODE_ALL -> "Lặp: Tất cả"
        Player.REPEAT_MODE_ONE -> "Lặp: 1 bài"
        else -> "Lặp: Tắt"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onBack) {
                Text("‹ Trở về")
            }

            Text(
                text = "ĐANG PHÁT",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(24.dp))

        SongArtwork(
            uri = state.artworkUri,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(28.dp))
        )

        Spacer(Modifier.height(28.dp))

        Text(
            text = state.title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = state.artist,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = {
                favoriteUri?.let { uri ->
                    favoritesStore.toggleSong(
                    uri = uri,
                    title = state.title,
                    artist = state.artist
                )
                }
            },
            enabled = state.hasMedia && favoriteUri != null
        ) {
            Text(
                if (isFavorite) {
                    "♥ Đã yêu thích"
                } else {
                    "♡ Yêu thích"
                }
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Bài ${state.currentIndex} / " +
                    "${state.queueCount}",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(Modifier.height(24.dp))

        Slider(
            value = displayedPosition,
            onValueChange = {
                isDragging = true
                sliderValue = it
            },
            onValueChangeFinished = {
                onSeek(sliderValue.toLong())
                isDragging = false
            },
            valueRange = 0f..maximum,
            enabled = state.hasMedia &&
                    state.durationMs > 0L
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {
            Text(
                formatNowPlayingTime(
                    displayedPosition.toLong()
                )
            )

            Text(
                if (state.durationMs > 0L) {
                    formatNowPlayingTime(state.durationMs)
                } else {
                    "--:--"
                }
            )
        }

        Spacer(Modifier.height(22.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                enabled = state.canPrevious,
                onClick = onPrevious
            ) {
                Text("Trước")
            }

            Button(
                enabled = state.hasMedia,
                onClick = onPlayPause
            ) {
                Text(
                    when {
                        state.playbackErrorMessage != null -> "Thử lại"
                        state.isPlaying -> "Tạm dừng"
                        else -> "Phát"
                    }
                )
            }

            OutlinedButton(
                enabled = state.canNext,
                onClick = onNext
            ) {
                Text(
                    if (state.playbackErrorMessage != null) {
                        "Bỏ qua"
                    } else {
                        "Tiếp"
                    }
                )
            }
        }

        Spacer(Modifier.height(22.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onToggleShuffle,
                enabled = state.hasMedia,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    horizontal = 8.dp
                )
            ) {
                Text(
                    if (state.shuffleEnabled) {
                        "Trộn: Bật"
                    } else {
                        "Trộn: Tắt"
                    }
                )
            }

            OutlinedButton(
                onClick = onCycleRepeat,
                enabled = state.hasMedia,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    horizontal = 8.dp
                )
            ) {
                Text(repeatLabel)
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = state.status,
            style = MaterialTheme.typography.bodySmall,
            color = if (state.playbackErrorMessage != null) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )

        if (state.playbackErrorMessage != null) {
            Text(
                text = "Bạn có thể thử lại hoặc quay về " +
                    "Library / danh sách phát để chọn bài khác. " +
                    "Nếu file đã bị xóa, hãy xóa mục đó khỏi playlist.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(24.dp))

        TextButton(onClick = onOpenLibrary) {
            Text("Mở thư viện nhạc")
        }

        TextButton(onClick = onPickFiles) {
            Text("Chọn MP3 từ thiết bị")
        }
    }
}

private fun formatNowPlayingTime(
    timeMs: Long
): String {
    val seconds = timeMs.coerceAtLeast(0L) / 1000
    val minutes = seconds / 60
    val remainder = seconds % 60

    return "$minutes:${remainder.toString().padStart(2, '0')}"
}
