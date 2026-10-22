
package com.example.musicflow

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken

import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private var controllerFuture:
            ListenableFuture<MediaController>? = null

    private var controller by mutableStateOf<MediaController?>(null)

    private var isPlaying by mutableStateOf(false)
    private var hasMedia by mutableStateOf(false)

    private var songTitle by mutableStateOf("Chưa chọn bài hát")
    private var status by mutableStateOf("Đang kết nối bộ phát...")

    private var positionMs by mutableStateOf(0L)
    private var durationMs by mutableStateOf(0L)

    private var queueCount by mutableStateOf(0)
    private var currentIndex by mutableStateOf(0)

    private var canNext by mutableStateOf(false)
    private var canPrevious by mutableStateOf(false)

    private var pendingUris: List<Uri> = emptyList()

    private val playerListener = object : Player.Listener {

        override fun onEvents(
            player: Player,
            events: Player.Events
        ) {
            refreshPlayerState()
        }

        override fun onPlayerError(error: PlaybackException) {
            status = "Lỗi phát nhạc: ${error.errorCodeName}"
        }
    }

    private val pickAudio = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->

        if (uris.isNotEmpty()) {

            val selectedUris = uris.distinct()

            selectedUris.forEach { uri ->
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) {
                    // Vẫn thử sử dụng URI trong phiên hiện tại.
                }
            }

            pendingUris = selectedUris

            playSelectedFiles()
        }
    }

    private fun getFileName(uri: Uri): String {

        return try {
            var fileName: String? = null

            contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->

                if (cursor.moveToFirst()) {
                    fileName = cursor.getString(0)
                }
            }

            fileName ?: "Bài hát không tên"

        } catch (_: Exception) {
            "Bài hát không tên"
        }
    }

    private fun playSelectedFiles() {

        val currentController = controller ?: return

        if (pendingUris.isEmpty()) return

        val mediaItems = pendingUris.map { uri ->

            val name = getFileName(uri)

            MediaItem.Builder()
                .setMediaId(uri.toString())
                .setUri(uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(name)
                        .build()
                )
                .build()
        }

        pendingUris = emptyList()

        currentController.setMediaItems(mediaItems)
        currentController.prepare()
        currentController.play()

        refreshPlayerState()
    }

    private fun refreshPlayerState() {

        val player = controller ?: return

        isPlaying = player.isPlaying
        hasMedia = player.mediaItemCount > 0

        queueCount = player.mediaItemCount

        currentIndex = if (hasMedia) {
            player.currentMediaItemIndex + 1
        } else {
            0
        }

        songTitle = player.currentMediaItem
            ?.mediaMetadata
            ?.title
            ?.toString()
            ?: "Chưa chọn bài hát"

        canNext = player.hasNextMediaItem()
        canPrevious = player.hasPreviousMediaItem()

        val newDuration = player.duration

        durationMs = if (
            newDuration > 0 &&
            newDuration != C.TIME_UNSET
        ) {
            newDuration
        } else {
            0L
        }

        positionMs = player.currentPosition.coerceAtLeast(0L)

        status = if (player.playerError != null) {
            "Lỗi phát nhạc: ${player.playerError?.errorCodeName}"
        } else {
            when (player.playbackState) {
                Player.STATE_IDLE -> "Bộ phát sẵn sàng"
                Player.STATE_BUFFERING -> "Đang tải bài hát..."
                Player.STATE_READY ->
                    if (player.isPlaying) "Đang phát"
                    else "Đã tạm dừng"
                Player.STATE_ENDED -> "Đã phát hết hàng đợi"
                else -> "Đang xử lý..."
            }
        }
    }

    private fun formatTime(timeMs: Long): String {

        val totalSeconds = timeMs.coerceAtLeast(0L) / 1000

        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60

        return "$minutes:${seconds.toString().padStart(2, '0')}"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            var isDragging by remember {
                mutableStateOf(false)
            }

            var sliderValue by remember {
                mutableFloatStateOf(0f)
            }

            var showDeviceMusic by remember {
                mutableStateOf(false)
            }

            LaunchedEffect(controller) {

                while (true) {

                    controller?.let { player ->

                        if (!isDragging) {
                            positionMs =
                                player.currentPosition.coerceAtLeast(0L)
                        }

                        val newDuration = player.duration

                        durationMs =
                            if (newDuration > 0 &&
                                newDuration != C.TIME_UNSET
                            ) {
                                newDuration
                            } else {
                                0L
                            }
                    }

                    delay(500)
                }
            }

            val sliderMaximum =
                durationMs.coerceAtLeast(1L).toFloat()

            val displayedPosition = if (isDragging) {
                sliderValue
            } else {
                positionMs.toFloat()
            }.coerceIn(0f, sliderMaximum)

            MaterialTheme {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {

                    Text(
                        text = "MusicFlow",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(status)

                    Spacer(Modifier.height(24.dp))

                    Text(
                        text = songTitle,
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "Bài $currentIndex / $queueCount"
                    )

                    Spacer(Modifier.height(28.dp))

                    Button(
                        enabled = controller != null,
                        onClick = {
                            pickAudio.launch(arrayOf("audio/*"))
                        }
                    ) {
                        Text("Chọn nhiều file MP3")
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        enabled = controller != null,
                        onClick = {
                            showDeviceMusic = true
                        }
                    ) {
                        Text("Quét và xem nhạc thiết bị")
                    }

                    Spacer(Modifier.height(24.dp))

                    Slider(
                        value = displayedPosition,
                        onValueChange = { value ->
                            isDragging = true
                            sliderValue = value
                        },
                        onValueChangeFinished = {

                            controller?.seekTo(
                                sliderValue.toLong()
                            )

                            isDragging = false
                        },
                        valueRange = 0f..sliderMaximum,
                        enabled = hasMedia && durationMs > 0
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceBetween
                    ) {
                        Text(
                            formatTime(displayedPosition.toLong())
                        )

                        Text(
                            if (durationMs > 0)
                                formatTime(durationMs)
                            else "--:--"
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceEvenly,
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Button(
                            enabled = canPrevious,
                            onClick = {
                                controller?.seekToPreviousMediaItem()
                            }
                        ) {
                            Text("Trước")
                        }

                        Button(
                            enabled = hasMedia,
                            onClick = {

                                controller?.let { player ->

                                    if (player.isPlaying) {
                                        player.pause()
                                    } else {

                                        if (
                                            player.playbackState ==
                                            Player.STATE_ENDED
                                        ) {
                                            player.seekToDefaultPosition()
                                        }

                                        player.play()
                                    }
                                }
                            }
                        ) {
                            Text(
                                if (isPlaying) "Tạm dừng"
                                else "Phát"
                            )
                        }

                        Button(
                            enabled = canNext,
                            onClick = {
                                controller?.seekToNextMediaItem()
                            }
                        ) {
                            Text("Tiếp")
                        }
                    }
                }

                if (showDeviceMusic) {

                    DeviceMusicDialog(

                        onDismiss = {
                            showDeviceMusic = false
                        },

                        onSelect = { songs, selectedIndex ->

                            controller?.let { player ->

                                val mediaItems = songs.map { song ->

                                    MediaItem.Builder()
                                        .setMediaId("device-${song.id}")
                                        .setUri(song.uri)
                                        .setMediaMetadata(
                                            MediaMetadata.Builder()
                                                .setTitle(song.title)
                                                .setArtist(song.artist)
                                                .build()
                                        )
                                        .build()
                                }

                                player.setMediaItems(
                                    mediaItems,
                                    selectedIndex,
                                    0L
                                )

                                player.prepare()
                                player.play()

                                refreshPlayerState()
                            }

                            showDeviceMusic = false
                        }
                    )
                }

            }
        }
    }

    override fun onStart() {
        super.onStart()

        val token = SessionToken(
            this,
            ComponentName(this, PlaybackService::class.java)
        )

        val future = MediaController.Builder(this, token)
            .buildAsync()

        controllerFuture = future

        future.addListener(
            {
                try {
                    val newController = future.get()

                    if (controllerFuture === future) {

                        controller = newController

                        newController.addListener(playerListener)

                        refreshPlayerState()

                        playSelectedFiles()
                    }

                } catch (_: Exception) {

                    if (controllerFuture === future) {
                        status = "Không thể kết nối bộ phát"
                    }
                }
            },
            ContextCompat.getMainExecutor(this)
        )
    }

    override fun onStop() {

        controller?.removeListener(playerListener)
        controller = null

        controllerFuture?.let { future ->
            MediaController.releaseFuture(future)
        }

        controllerFuture = null

        super.onStop()
    }
}
