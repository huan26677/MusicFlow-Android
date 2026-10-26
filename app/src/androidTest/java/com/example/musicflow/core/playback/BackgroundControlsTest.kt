package com.example.musicflow.core.playback

import android.app.Notification
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import androidx.lifecycle.Lifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.musicflow.MainActivity
import com.google.common.util.concurrent.ListenableFuture
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class BackgroundControlsTest {

    private val instrumentation =
        InstrumentationRegistry.getInstrumentation()

    private val context = instrumentation.targetContext

    private data class PlaybackSnapshot(
        val mediaId: String?,
        val playing: Boolean,
        val positionMs: Long,
        val playWhenReady: Boolean
    )

    private fun snapshot(
        controller: MediaController
    ): PlaybackSnapshot {
        var result: PlaybackSnapshot? = null

        instrumentation.runOnMainSync {
            result = PlaybackSnapshot(
                mediaId = controller.currentMediaItem?.mediaId,
                playing = controller.isPlaying,
                positionMs = controller.currentPosition,
                playWhenReady = controller.playWhenReady
            )
        }

        return checkNotNull(result)
    }

    private fun awaitCondition(
        timeoutMs: Long = 15_000L,
        condition: () -> Boolean
    ): Boolean {
        val deadline = SystemClock.uptimeMillis() + timeoutMs

        while (SystemClock.uptimeMillis() < deadline) {
            if (condition()) return true
            SystemClock.sleep(100)
        }

        return condition()
    }

    private fun createWav(): File {
        val sampleRate = 8000
        val dataSize = sampleRate * 120 * 2

        val bytes = ByteBuffer.allocate(44 + dataSize)
            .order(ByteOrder.LITTLE_ENDIAN)

        fun write(value: String) {
            bytes.put(value.toByteArray(Charsets.US_ASCII))
        }

        write("RIFF")
        bytes.putInt(36 + dataSize)
        write("WAVE")
        write("fmt ")
        bytes.putInt(16)
        bytes.putShort(1.toShort())
        bytes.putShort(1.toShort())
        bytes.putInt(sampleRate)
        bytes.putInt(sampleRate * 2)
        bytes.putShort(2.toShort())
        bytes.putShort(16.toShort())
        write("data")
        bytes.putInt(dataSize)

        return File.createTempFile(
            "musicflow_bg_",
            ".wav",
            context.cacheDir
        ).also {
            it.writeBytes(bytes.array())
        }
    }
    @Test
    fun playbackSurvivesBackgroundAndSystemControls() {
        val files = listOf(createWav(), createWav())

        val items = files.mapIndexed { index, file ->
            MediaItem.Builder()
                .setMediaId("qa-bg-${index + 1}")
                .setUri(Uri.fromFile(file))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("MusicFlow BG QA ${index + 1}")
                        .setArtist("MusicFlow QA")
                        .build()
                )
                .build()
        }

        var scenario: ActivityScenario<MainActivity>? = null
        var future: ListenableFuture<MediaController>? = null
        var controller: MediaController? = null

        try {
            scenario = ActivityScenario.launch(
                MainActivity::class.java
            )

            val token = SessionToken(
                context,
                ComponentName(
                    context,
                    PlaybackService::class.java
                )
            )

            val connecting = MediaController.Builder(
                context,
                token
            ).buildAsync()

            future = connecting

            val media = connecting.get(
                20,
                TimeUnit.SECONDS
            )

            controller = media

            instrumentation.runOnMainSync {
                media.setMediaItems(items)
                media.prepare()
                media.play()
            }

            assertTrue(
                "Bài QA đầu tiên chưa phát",
                awaitCondition {
                    val state = snapshot(media)
                    state.playing &&
                        state.mediaId == "qa-bg-1"
                }
            )

            val positionBefore = snapshot(media).positionMs

            // Chuyển Activity sang trạng thái stopped.
            scenario.moveToState(Lifecycle.State.CREATED)

            SystemClock.sleep(1500)

            val background = snapshot(media)

            assertTrue(
                "Nhạc dừng khi Activity xuống nền",
                background.playing
            )

            assertEquals("qa-bg-1", background.mediaId)

            assertTrue(
                "Vị trí phát không tiếp tục tăng",
                background.positionMs > positionBefore
            )

            Log.i(
                "MusicFlowBackgroundQA",
                "PASS backgroundSurvivesActivityStop"
            )

            // Kiểm tra thông báo MediaSession của chính ứng dụng.
            val notificationManager =
                context.getSystemService(
                    Context.NOTIFICATION_SERVICE
                ) as NotificationManager

            assertTrue(
                "Không tìm thấy thông báo nhạc MusicFlow",
                awaitCondition {
                    notificationManager.activeNotifications.any {
                        notification ->
                        notification.packageName == context.packageName &&
                            notification.notification.extras
                                .getCharSequence(Notification.EXTRA_TITLE)
                                ?.toString()
                                ?.contains("MusicFlow BG QA") == true
                    }
                }
            )

            Log.i(
                "MusicFlowBackgroundQA",
                "PASS mediaNotificationVisible"
            )

            // Hủy giao diện, nhưng MediaSession vẫn còn.
            scenario.close()
            scenario = null

            assertTrue(
                "Nhạc dừng sau khi Activity bị hủy",
                awaitCondition {
                    val state = snapshot(media)
                    state.playing &&
                        state.mediaId == "qa-bg-1"
                }
            )

            Log.i(
                "MusicFlowBackgroundQA",
                "PASS survivesActivityDestroy"
            )

            // Điều khiển bằng MediaController khi không còn Activity.
            instrumentation.runOnMainSync {
                media.pause()
            }

            assertTrue(
                "MediaController không tạm dừng được",
                awaitCondition {
                    val state = snapshot(media)
                    !state.playing && !state.playWhenReady
                }
            )

            instrumentation.runOnMainSync {
                media.play()
            }

            assertTrue(
                "MediaController không phát tiếp được",
                awaitCondition {
                    snapshot(media).playing
                }
            )

            instrumentation.runOnMainSync {
                media.seekToNextMediaItem()
            }

            assertTrue(
                "MediaController không chuyển bài tiếp theo",
                awaitCondition {
                    val state = snapshot(media)
                    state.playing &&
                        state.mediaId == "qa-bg-2"
                }
            )

            instrumentation.runOnMainSync {
                media.seekToPreviousMediaItem()
            }

            assertTrue(
                "MediaController không quay về bài trước",
                awaitCondition {
                    val state = snapshot(media)
                    state.playing &&
                        state.mediaId == "qa-bg-1"
                }
            )

            Log.i(
                "MusicFlowBackgroundQA",
                "PASS controllerControlsWithoutUi"
            )

        } finally {
            controller?.let { media ->
                instrumentation.runOnMainSync {
                    media.pause()
                    media.stop()
                    media.clearMediaItems()
                }
            }

            future?.let { connecting ->
                instrumentation.runOnMainSync {
                    MediaController.releaseFuture(connecting)
                }
            }

            scenario?.close()
            files.forEach { it.delete() }
        }
    }

    private fun notificationActionsV2(): List<Notification.Action> {
        val manager = context.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        val notification = manager.activeNotifications
            .firstOrNull { posted ->
                posted.packageName == context.packageName &&
                    posted.notification.extras
                        .getCharSequence(Notification.EXTRA_TITLE)
                        ?.toString()
                        ?.contains("MusicFlow V2") == true
            }
            ?.notification

        return notification?.actions?.toList().orEmpty()
    }

    private fun sendNotificationActionV2(
        index: Int,
        minimumActions: Int = index + 1
    ) {
        assertTrue(
            "Thông báo chưa có đủ nút điều khiển",
            awaitCondition {
                notificationActionsV2().size >= minimumActions
            }
        )

        val action = notificationActionsV2()
            .getOrNull(index)

        assertNotNull("Không có nút tại vị trí $index", action)

        checkNotNull(action?.actionIntent).send()
    }

    @Test
    fun notificationButtonsControlPlaybackV2() {
        val files = List(3) { createWav() }

        val items = files.mapIndexed { index, file ->
            MediaItem.Builder()
                .setMediaId("qa-v2-actions-${index + 1}")
                .setUri(Uri.fromFile(file))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("MusicFlow V2 Track ${index + 1}")
                        .setArtist("MusicFlow QA")
                        .build()
                )
                .build()
        }

        var scenario: ActivityScenario<MainActivity>? = null
        var future: ListenableFuture<MediaController>? = null
        var controller: MediaController? = null

        try {
            scenario = ActivityScenario.launch(
                MainActivity::class.java
            )

            val token = SessionToken(
                context,
                ComponentName(context, PlaybackService::class.java)
            )

            future = MediaController.Builder(context, token)
                .buildAsync()

            val media = future.get(20, TimeUnit.SECONDS)
            controller = media

            instrumentation.runOnMainSync {
                media.setMediaItems(items, 1, 0L)
                media.prepare()
                media.play()
            }

            assertTrue(
                "Bài thứ hai chưa phát",
                awaitCondition {
                    val state = snapshot(media)
                    state.playing &&
                        state.mediaId == "qa-v2-actions-2"
                }
            )

            scenario.moveToState(Lifecycle.State.CREATED)

            // Media3 mặc định: Previous / Play-Pause / Next.
            // Test này dành cho thông báo Android 11.
            sendNotificationActionV2(
                index = 1,
                minimumActions = 3
            )

            assertTrue(
                "Nút Pause trên thông báo không hoạt động",
                awaitCondition {
                    !snapshot(media).playing
                }
            )

            sendNotificationActionV2(
                index = 1,
                minimumActions = 3
            )

            assertTrue(
                "Nút Play trên thông báo không hoạt động",
                awaitCondition {
                    snapshot(media).playing
                }
            )

            sendNotificationActionV2(
                index = 2,
                minimumActions = 3
            )

            assertTrue(
                "Nút Next trên thông báo không hoạt động",
                awaitCondition {
                    val state = snapshot(media)
                    state.playing &&
                        state.mediaId == "qa-v2-actions-3"
                }
            )

            sendNotificationActionV2(
                index = 0
            )

            assertTrue(
                "Nút Previous trên thông báo không hoạt động",
                awaitCondition {
                    val state = snapshot(media)
                    state.playing &&
                        state.mediaId == "qa-v2-actions-2"
                }
            )

            Log.i(
                "MusicFlowSystemV2QA",
                "PASS notificationButtonCommands"
            )

        } finally {
            controller?.let { media ->
                instrumentation.runOnMainSync {
                    media.pause()
                    media.stop()
                    media.clearMediaItems()
                }
            }

            future?.let {
                instrumentation.runOnMainSync {
                    MediaController.releaseFuture(it)
                }
            }

            scenario?.close()
            files.forEach { it.delete() }
        }
    }

    @Test
    fun transientAudioFocusLossV2() {
        val file = createWav()

        val audioManager = context.getSystemService(
            Context.AUDIO_SERVICE
        ) as AudioManager

        val focusListener =
            AudioManager.OnAudioFocusChangeListener { }

        val focusRequest = AudioFocusRequest.Builder(
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
        )
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(
                        AudioAttributes.CONTENT_TYPE_SPEECH
                    )
                    .build()
            )
            .setOnAudioFocusChangeListener(focusListener)
            .build()

        var focusGranted = false
        var scenario: ActivityScenario<MainActivity>? = null
        var future: ListenableFuture<MediaController>? = null
        var controller: MediaController? = null

        try {
            scenario = ActivityScenario.launch(
                MainActivity::class.java
            )

            val token = SessionToken(
                context,
                ComponentName(context, PlaybackService::class.java)
            )

            future = MediaController.Builder(context, token)
                .buildAsync()

            val media = future.get(20, TimeUnit.SECONDS)
            controller = media

            val item = MediaItem.Builder()
                .setMediaId("qa-v2-audio-focus")
                .setUri(Uri.fromFile(file))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("MusicFlow V2 Focus")
                        .build()
                )
                .build()

            instrumentation.runOnMainSync {
                media.setMediaItem(item)
                media.prepare()
                media.play()
            }

            assertTrue(
                "Playback chưa bắt đầu",
                awaitCondition {
                    snapshot(media).playing
                }
            )

            val result = audioManager.requestAudioFocus(
                focusRequest
            )

            focusGranted =
                result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED

            assertTrue(
                "Không chiếm được audio focus tạm thời",
                focusGranted
            )

            assertTrue(
                "MusicFlow không nhường audio focus",
                awaitCondition {
                    !snapshot(media).playing
                }
            )

            assertEquals(
                "Hàng đợi thay đổi khi mất audio focus",
                "qa-v2-audio-focus",
                snapshot(media).mediaId
            )

            Log.i(
                "MusicFlowSystemV2QA",
                "PASS transientAudioFocusLoss"
            )

        } finally {
            controller?.let { media ->
                instrumentation.runOnMainSync {
                    media.pause()
                    media.stop()
                    media.clearMediaItems()
                }
            }

            if (focusGranted) {
                audioManager.abandonAudioFocusRequest(focusRequest)
            }

            future?.let {
                instrumentation.runOnMainSync {
                    MediaController.releaseFuture(it)
                }
            }

            scenario?.close()
            file.delete()
        }
    }
}

