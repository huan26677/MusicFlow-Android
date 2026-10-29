package com.example.musicflow.feature.player

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.musicflow.core.favorites.LocalFavoritesStore
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import androidx.media3.common.Player
import com.example.musicflow.core.lyrics.LyricsUiState
import com.example.musicflow.core.playback.PlayerUiState
import androidx.compose.ui.text.font.FontWeight
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
    onCycleRepeat: () -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    onSetSleepTimerMinutes: (Int) -> Unit,
    onSetEqualizerEnabled: (Boolean) -> Unit,
    onSetEqualizerBand: (Int, Int) -> Unit,
    onSetCrossfadeDuration: (Int) -> Unit,
    lyrics: LyricsUiState,
    onPickLyrics: () -> Unit,
    onOpenQueue: () -> Unit = {}
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

    // The same callbacks work at normal and large font scales.
    // Avoid three squeezed buttons on narrow screens.
    val transport: @Composable (Modifier) -> Unit = { buttonModifier ->
        OutlinedButton(
            enabled = state.canPrevious,
            onClick = onPrevious,
            modifier = buttonModifier.heightIn(min = 48.dp)
        ) { Text("Trước") }
        Button(
            enabled = state.hasMedia,
            onClick = onPlayPause,
            modifier = buttonModifier.heightIn(min = 48.dp)
        ) {
            Text(when {
                state.playbackErrorMessage != null -> "Thử lại"
                state.isPlaying -> "Tạm dừng"
                else -> "Phát"
            })
        }
        OutlinedButton(
            enabled = state.canNext,
            onClick = onNext,
            modifier = buttonModifier.heightIn(min = 48.dp)
        ) {
            Text(if (state.playbackErrorMessage != null) "Bỏ qua" else "Tiếp")
        }
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
                .widthIn(max = 300.dp)
                .aspectRatio(1f)
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

        TextButton(
            onClick = onOpenQueue,
            enabled = state.connected
        ) {
            Text("☷ Xem và quản lý hàng đợi")
        }

        Spacer(Modifier.height(24.dp))

        Slider(
            value = displayedPosition,
            modifier = Modifier.semantics {
                contentDescription = "Vị trí phát nhạc"
            },
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

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val compact = maxWidth < 360.dp ||
                LocalDensity.current.fontScale >= 1.35f
            if (compact) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) { transport(Modifier.fillMaxWidth()) }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) { transport(Modifier.weight(1f)) }
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
            text = "Tốc độ phát",
            style = MaterialTheme.typography.titleMedium
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(0.75f, 1f, 1.25f, 1.5f, 2f)
                .forEach { speed ->
                    FilterChip(
                        selected = kotlin.math.abs(
                            state.playbackSpeed - speed
                        ) < 0.01f,
                        onClick = { onSetPlaybackSpeed(speed) },
                        enabled = state.hasMedia,
                        label = { Text("${speed}x") }
                    )
                }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Hẹn giờ tắt nhạc",
            style = MaterialTheme.typography.titleMedium
        )

        val remainingMs = state.sleepTimerRemainingMs

        Text(
            text = if (remainingMs > 0L) {
                val minutesLeft =
                    (remainingMs + 59_999L) / 60_000L

                "Còn khoảng $minutesLeft phút"
            } else {
                "Chưa hẹn giờ"
            },
            style = MaterialTheme.typography.bodyMedium
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(15, 30, 45, 60).forEach { minutes ->
                OutlinedButton(
                    onClick = {
                        onSetSleepTimerMinutes(minutes)
                    },
                    enabled = state.connected && state.hasMedia
                ) {
                    Text("$minutes phút")
                }
            }
        }

        if (remainingMs > 0L) {
            TextButton(
                onClick = {
                    onSetSleepTimerMinutes(0)
                }
            ) {
                Text("Hủy hẹn giờ")
            }
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))
        Text("Chuyển bài", style = MaterialTheme.typography.titleLarge)
        Text("Gapless tự động khi tệp hỗ trợ. Crossfade thử nghiệm, mặc định tắt.",
            style = MaterialTheme.typography.bodySmall)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(0, 1000, 2000, 3000).forEach { duration ->
                FilterChip(
                    selected = state.crossfadeDurationMs == duration,
                    onClick = { onSetCrossfadeDuration(duration) },
                    label = { Text(if (duration == 0) "Tắt" else "${duration / 1000}s") }
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Equalizer", style = MaterialTheme.typography.titleLarge)
        val eq = state.equalizer
        if (!eq.supported) {
            Text("Thiết bị chưa cung cấp bộ cân bằng âm thanh.",
                style = MaterialTheme.typography.bodyMedium)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Bật Equalizer", modifier = Modifier.weight(1f))
                Switch(checked = eq.enabled, onCheckedChange = onSetEqualizerEnabled)
            }
            eq.bands.forEach { band ->
                var dragged by remember(band.index, eq.enabled) {
                    mutableFloatStateOf(band.levelMb.toFloat())
                }
                LaunchedEffect(band.levelMb) {
                    dragged = band.levelMb.toFloat()
                }
                val frequency = if (band.frequencyHz >= 1000) {
                    "${band.frequencyHz / 1000f} kHz"
                } else "${band.frequencyHz} Hz"
                Text("$frequency  (${(dragged / 100f).toInt()} dB)")
                Slider(
                    value = dragged.coerceIn(
                        eq.minLevelMb.toFloat(), eq.maxLevelMb.toFloat()
                    ),
                    modifier = Modifier.semantics {
                        contentDescription = "Equalizer $frequency"
                    },
                    onValueChange = { dragged = it },
                    onValueChangeFinished = {
                        onSetEqualizerBand(band.index, dragged.toInt())
                    },
                    valueRange = eq.minLevelMb.toFloat()..eq.maxLevelMb.toFloat(),
                    enabled = eq.enabled && eq.minLevelMb < eq.maxLevelMb
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Lời bài hát",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(Modifier.height(8.dp))

        if (lyrics.error != null) {
            Text(
                text = lyrics.error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        } else if (lyrics.lines.isEmpty()) {
            Text(
                text = "Chưa có lời bài hát. " +
                    "Hãy nhập file .lrc.",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            val currentLineIndex = lyrics.lines.indexOfLast {
                it.timeMs <= state.positionMs
            }

            lyrics.lines.forEachIndexed { index, line ->
                Text(
                    text = line.text,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (index == currentLineIndex) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Normal
                    },
                    color = if (index == currentLineIndex) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = onPickLyrics,
            enabled = state.hasMedia
        ) {
            Text("Nhập file .lrc")
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
