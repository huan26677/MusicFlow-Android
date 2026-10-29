package com.example.musicflow.core.playback

import android.Manifest
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.musicflow.core.history.RoomRecentHistoryRepository
import com.example.musicflow.core.history.local.RecentHistoryDatabase
import com.example.musicflow.core.media.DeviceMusicScanner
import com.example.musicflow.core.media.TrackMediaItems
import com.example.musicflow.core.playlist.PlaylistMediaItems
import com.example.musicflow.core.playlist.PlaylistTrack
import com.example.musicflow.core.playlist.RoomPlaylistRepository
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit

/**
 * Offline acceptance: MediaStore -> scanner -> Room playlist -> MediaSession ->
 * history -> queue snapshot/restore. No Supabase or online rights involved.
 */
@RunWith(AndroidJUnit4::class)
class OfflineAcceptanceFlowTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val resolver = context.contentResolver

    private fun wav(): ByteArray {
        val samples = 8000 * 30
        val bytes = ByteBuffer.allocate(44 + samples * 2)
            .order(ByteOrder.LITTLE_ENDIAN)
        fun mark(s: String) { bytes.put(s.toByteArray(Charsets.US_ASCII)) }
        mark("RIFF")
        bytes.putInt(36 + samples * 2)
        mark("WAVE")
        mark("fmt ")
        bytes.putInt(16)
        bytes.putShort(1.toShort())
        bytes.putShort(1.toShort())
        bytes.putInt(8000)
        bytes.putInt(16000)
        bytes.putShort(2.toShort())
        bytes.putShort(16.toShort())
        mark("data")
        bytes.putInt(samples * 2)
        return bytes.array()
    }

    private fun insertFile(title: String): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, "$title.wav")
            put(MediaStore.Audio.Media.TITLE, title)
            put(MediaStore.Audio.Media.ARTIST, "MusicFlow Offline QA")
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
            put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/MusicFlowQA/")
            put(MediaStore.Audio.Media.IS_MUSIC, 1)
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        val uri = checkNotNull(resolver.insert(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values
        ))
        try {
            checkNotNull(resolver.openOutputStream(uri, "w")).use {
                it.write(wav())
            }
            resolver.update(uri, ContentValues().apply {
                put(MediaStore.Audio.Media.IS_PENDING, 0)
            }, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
        return uri
    }

    private fun grantReadPermission() {
        val permission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(
                context, permission
            ) == PackageManager.PERMISSION_GRANTED) return
        ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand(
                "pm grant ${context.packageName} $permission"
            )
        ).use { it.readBytes() }
        assertEquals(PackageManager.PERMISSION_GRANTED,
            ContextCompat.checkSelfPermission(context, permission))
    }

    private suspend fun awaitCondition(
        label: String,
        condition: suspend () -> Boolean
    ) {
        withTimeout(20_000L) {
            while (!condition()) delay(100L)
        }
        Log.i("MusicFlowOfflineE2E", "PASS $label")
    }

    @Test
    fun mediaStorePlaylistServiceHistoryAndQueueRestoreOffline() = runBlocking<Unit> {
        grantReadPermission()
        val stamp = System.nanoTime()
        val names = listOf("MusicFlowOfflineQA_${stamp}_A", "MusicFlowOfflineQA_${stamp}_B")
        val created = mutableListOf<Uri>()
        val database = RecentHistoryDatabase.getInstance(context)
        val playlistRepo = RoomPlaylistRepository(database)
        val historyRepo = RoomRecentHistoryRepository(database)
        val prefs = context.getSharedPreferences(
            "musicflow_playback_queue_v1", Context.MODE_PRIVATE
        )
        val oldSnapshot = prefs.getString("snapshot", null)
        var playlistId: String? = null
        var future: ListenableFuture<MediaController>? = null
        var controller: MediaController? = null
        var restoredPlayer: ExoPlayer? = null
        val newIds = mutableListOf<String>()

        try {
            names.forEach { created += insertFile(it) }
            val songs = withTimeout(20_000L) {
                var found = emptyList<com.example.musicflow.core.media.DeviceSong>()
                while (found.size != 2) {
                    found = DeviceMusicScanner.scan(context)
                        .filter { it.uri in created }
                    if (found.size != 2) delay(200)
                }
                found.sortedBy { names.indexOf(it.title) }
            }
            assertEquals(names, songs.map { it.title })
            assertEquals(created, songs.map { it.uri })
            Log.i("MusicFlowOfflineE2E", "PASS mediaStoreScanner")

            val playlist = checkNotNull(playlistRepo.create("Offline E2E $stamp"))
            playlistId = playlist.id
            songs.forEachIndexed { index, song ->
                val item = TrackMediaItems.fromDevice(song)
                assertTrue(item.localConfiguration?.uri?.scheme == "content")
                assertTrue(playlistRepo.addTrack(playlist.id, PlaylistTrack(
                    mediaId = item.mediaId,
                    audioUri = song.uri.toString(),
                    title = song.title,
                    artist = song.artist,
                    addedAtMs = index.toLong()
                )))
                newIds += item.mediaId
            }
            val storedTracks = playlistRepo.getTracks(playlist.id)
            assertEquals(newIds, storedTracks.map { it.mediaId })
            Log.i("MusicFlowOfflineE2E", "PASS localPlaylistPersisted")

            future = MediaController.Builder(
                context,
                SessionToken(context, ComponentName(
                    context, PlaybackService::class.java
                ))
            ).buildAsync()
            val media = checkNotNull(future).get(20, TimeUnit.SECONDS)
            controller = media
            instrumentation.runOnMainSync {
                media.shuffleModeEnabled = false
                media.repeatMode = Player.REPEAT_MODE_OFF
                media.setMediaItems(PlaylistMediaItems.fromTracks(storedTracks), 0, 0L)
                media.prepare()
                media.play()
            }
            awaitCondition("offlineMediaSessionPlaying") {
                var playing = false
                instrumentation.runOnMainSync {
                    playing = media.isPlaying &&
                        media.currentMediaItem?.mediaId == newIds[0]
                }
                playing
            }
            awaitCondition("offlineHistoryFirst") {
                historyRepo.getRecent().any { it.mediaId == newIds[0] }
            }

            instrumentation.runOnMainSync { media.seekToNextMediaItem() }
            awaitCondition("offlineNextTrack") {
                var playing = false
                instrumentation.runOnMainSync {
                    playing = media.isPlaying &&
                        media.currentMediaItem?.mediaId == newIds[1]
                }
                playing
            }
            awaitCondition("offlineHistoryOrder") {
                historyRepo.getRecent().filter { it.mediaId in newIds }
                    .map { it.mediaId } == newIds.reversed()
            }

            instrumentation.runOnMainSync {
                media.pause()
                media.seekTo(4500L)
            }
            awaitCondition("offlineQueueSnapshot") {
                runCatching {
                    val json = JSONObject(checkNotNull(
                        prefs.getString("snapshot", null)
                    ))
                    val items = json.getJSONArray("items")
                    items.length() == 2 &&
                        (0..1).map { items.getJSONObject(it).getString("mediaId") } ==
                            newIds &&
                        json.getInt("index") == 1 &&
                        json.getLong("positionMs") >= 4500L
                }.getOrDefault(false)
            }

            instrumentation.runOnMainSync {
                val replay = ExoPlayer.Builder(context).build()
                restoredPlayer = replay
                assertTrue(PlaybackQueueStore(context).restoreInto(replay))
                assertEquals(newIds, (0 until replay.mediaItemCount).map {
                    replay.getMediaItemAt(it).mediaId
                })
                assertEquals(1, replay.currentMediaItemIndex)
                assertTrue(replay.currentPosition >= 4500L)
                assertFalse(replay.playWhenReady)
                assertEquals(created[1], replay.currentMediaItem?.localConfiguration?.uri)
            }
            Log.i("MusicFlowOfflineE2E", "PASS offlineQueueRestoredPaused")
            assertEquals(newIds, playlistRepo.getTracks(playlist.id).map { it.mediaId })
            Log.i("MusicFlowOfflineE2E", "PASS offlinePlaylistUnchanged")
        } finally {
            instrumentation.runOnMainSync {
                restoredPlayer?.release()
                controller?.let { media ->
                    media.pause()
                    media.stop()
                    media.clearMediaItems()
                }
                future?.let(MediaController::releaseFuture)
            }
            for (id in newIds) runCatching { historyRepo.remove(id) }
            playlistId?.let { runCatching { playlistRepo.deletePlaylist(it) } }
            for (uri in created) runCatching { resolver.delete(uri, null, null) }
            prefs.edit().apply {
                if (oldSnapshot == null) remove("snapshot")
                else putString("snapshot", oldSnapshot)
            }.commit()
        }
    }
}
