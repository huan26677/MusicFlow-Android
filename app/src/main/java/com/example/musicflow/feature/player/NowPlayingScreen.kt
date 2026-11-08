package com.example.musicflow.feature.player

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.musicflow.core.favorites.LocalFavoritesStore
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import androidx.media3.common.Player
import com.example.musicflow.core.lyrics.LyricsUiState
import com.example.musicflow.core.lyrics.LyricsTimeline
import com.example.musicflow.core.playback.PlayerUiState
import androidx.compose.ui.text.font.FontWeight
import com.example.musicflow.ui.components.SongArtwork
import com.example.musicflow.ui.components.OceanFlowArtworkStage
import com.example.musicflow.ui.components.OceanFlowCard
import com.example.musicflow.core.ux.ListeningJourneyRules

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
    var advancedExpanded by rememberSaveable {
        mutableStateOf(false)
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
                .testTag("player-previous-action")
        ) { Text("Trước") }
        Button(
            enabled = state.hasMedia,
            onClick = onPlayPause,
            modifier = buttonModifier.heightIn(min = 48.dp)
                .testTag("player-play-pause-action")
                .semantics {
                    contentDescription = when {
                        state.playbackErrorMessage != null -> "Thử phát lại ${state.title}"
                        state.isPlaying -> "Tạm dừng ${state.title}"
                        else -> "Phát ${state.title}"
                    }
                }
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
                .testTag("player-next-action")
        ) {
            Text(if (state.playbackErrorMessage != null) "Bỏ qua" else "Tiếp")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.30f),
                MaterialTheme.colorScheme.background,
                MaterialTheme.colorScheme.background
            )))
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

        if (!state.hasMedia) {
            OutlinedCard(modifier = Modifier.fillMaxWidth().testTag("player-empty-guide")) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Chưa chọn bài hát", style = MaterialTheme.typography.titleMedium)
                    Text("Mở thư viện hoặc chọn tệp âm thanh để bắt đầu nghe offline. " +
                        "Bạn không cần đăng nhập.", style = MaterialTheme.typography.bodySmall)
                    Button(onClick = onOpenLibrary, modifier = Modifier.fillMaxWidth()
                        .testTag("player-empty-library")) { Text("Mở thư viện") }
                    OutlinedButton(onClick = onPickFiles, modifier = Modifier.fillMaxWidth()
                        .testTag("player-empty-files")) { Text("Chọn tệp âm thanh") }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        OceanFlowArtworkStage(artworkUri = state.artworkUri)

        Spacer(Modifier.height(28.dp))

        Text(
            text = state.title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            maxLines = if (LocalDensity.current.fontScale >= 1.75f) Int.MAX_VALUE else 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.semantics { if (state.hasMedia) heading() }
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
            enabled = state.hasMedia && favoriteUri != null,
            modifier = Modifier.heightIn(min = 48.dp)
                .semantics {
                    stateDescription = if (isFavorite) "Đã yêu thích" else "Chưa yêu thích"
                }
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
            text = ListeningJourneyRules.queuePosition(
                state.currentIndex, state.queueCount, state.hasMedia
            ),
            style = MaterialTheme.typography.bodySmall
        )

        TextButton(
            onClick = onOpenQueue,
            enabled = state.connected && state.queueCount > 0,
            modifier = Modifier.testTag("player-queue-action")
        ) {
            Text("☷ Xem và quản lý hàng đợi")
        }

        Spacer(Modifier.height(24.dp))

        Slider(
            value = displayedPosition,
            modifier = Modifier.semantics {
                contentDescription = "Vị trí phát nhạc"
                stateDescription = "Đã phát ${formatNowPlayingTime(displayedPosition.toLong())} " +
                    "trên ${formatNowPlayingTime(state.durationMs)}"
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

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val compact = maxWidth < 360.dp || LocalDensity.current.fontScale >= 1.35f
            val options: @Composable (Modifier) -> Unit = { optionModifier ->
                OutlinedButton(
                    onClick = onToggleShuffle,
                    enabled = state.hasMedia,
                    modifier = optionModifier.heightIn(min = 48.dp)
                        .testTag("player-shuffle-action")
                ) { Text(if (state.shuffleEnabled) "Trộn: Bật" else "Trộn: Tắt") }
                OutlinedButton(
                    onClick = onCycleRepeat,
                    enabled = state.hasMedia,
                    modifier = optionModifier.heightIn(min = 48.dp)
                        .testTag("player-repeat-action")
                ) { Text(repeatLabel) }
            }
            if (compact) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    options(Modifier.fillMaxWidth())
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    options(Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        OceanFlowCard {
                TextButton(
                    onClick = { advancedExpanded = !advancedExpanded },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("player-advanced-toggle")
                        .semantics {
                            stateDescription = if (advancedExpanded) "Đang mở" else "Đang thu gọn"
                        }
                ) {
                    Text(
                        if (advancedExpanded) "Thu gọn tùy chỉnh"
                        else "Tùy chỉnh phát nhạc"
                    )
                }
                Text(
                    "Tốc độ ${state.playbackSpeed}x • " +
                        if (state.sleepTimerRemainingMs > 0L) "Đang hẹn giờ tắt"
                        else "Chưa hẹn giờ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
        }

        if (advancedExpanded) {
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
                        modifier = Modifier.testTag("playback-speed-${speed}x"),
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
                    modifier = Modifier.testTag("crossfade-duration-${duration}ms"),
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

        }
        Spacer(Modifier.height(24.dp))
        HorizontalDivider()

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Lời bài hát",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() }
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
            val currentLineIndex = LyricsTimeline.activeIndex(
                lyrics.lines, state.positionMs
            )

            lyrics.lines.forEachIndexed { index, line ->
                Text(
                    text = line.text,
                    modifier = Modifier.testTag(
                        if (index == currentLineIndex) "lyrics-active-line"
                        else "lyrics-inactive-line"
                    ),
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

        if (state.hasMedia) {
            TextButton(onClick = onOpenLibrary) {
                Text("Mở thư viện nhạc")
            }
            TextButton(onClick = onPickFiles) {
                Text("Chọn tệp nhạc từ thiết bị")
            }
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
