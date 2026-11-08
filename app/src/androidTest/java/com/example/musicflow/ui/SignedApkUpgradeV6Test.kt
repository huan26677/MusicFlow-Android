package com.example.musicflow.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.provider.DocumentsContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.musicflow.MainActivity
import com.example.musicflow.core.favorites.LocalFavoritesStore
import com.example.musicflow.core.history.local.RecentHistoryDatabase
import com.example.musicflow.core.media.DeviceSong
import com.example.musicflow.core.media.LocalPickedMusicStore
import com.example.musicflow.core.media.PickedMusicRecord
import com.example.musicflow.core.playlist.PlaylistTrack
import com.example.musicflow.core.playlist.RoomPlaylistRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * TWO separate instrumentation processes, with an actual adb install -r in between.
 * Run ONLY on a disposable QA emulator: never clear or edit a personal device.
 */
@RunWith(AndroidJUnit4::class)
class SignedApkUpgradeV6Test {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val prefs by lazy {
        context.getSharedPreferences("musicflow_upgrade_v6_qa", Context.MODE_PRIVATE)
    }
    private val resolver get() = context.contentResolver
    private val playlistRepository get() =
        RoomPlaylistRepository(RecentHistoryDatabase.getInstance(context))
    private val device by lazy { UiDevice.getInstance(instrumentation) }

    private fun smallWav(): ByteArray {
        val samples = 8000
        val data = ByteBuffer.allocate(44 + samples * 2).order(ByteOrder.LITTLE_ENDIAN)
        fun text(value: String) { data.put(value.toByteArray(Charsets.US_ASCII)) }
        text("RIFF"); data.putInt(36 + samples * 2)
        text("WAVE"); text("fmt "); data.putInt(16)
        data.putShort(1.toShort()); data.putShort(1.toShort())
        data.putInt(8000); data.putInt(16000)
        data.putShort(2.toShort()); data.putShort(16.toShort())
        text("data"); data.putInt(samples * 2)
        return data.array()
    }

    private fun saveActualDocument(): Uri {
        val created = AtomicReference<Uri?>()
        val error = AtomicReference<Throwable?>()
        val latch = CountDownLatch(1)
        val name = "musicflow_upgrade_qa_${System.nanoTime()}.wav"
        instrumentation.runOnMainSync {
            val activity = compose.activity
            val launcher = activity.activityResultRegistry.register(
                "musicflow-upgrade-qa-document",
                ActivityResultContracts.CreateDocument("audio/wav")
            ) { uri ->
                try {
                    val doc = checkNotNull(uri) { "DocumentsUI did not return a URI" }
                    activity.contentResolver.takePersistableUriPermission(
                        doc, Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    checkNotNull(activity.contentResolver.openOutputStream(doc, "w")).use {
                        it.write(smallWav())
                    }
                    created.set(doc)
                } catch (failure: Throwable) {
                    error.set(failure)
                } finally {
                    latch.countDown()
                }
            }
            launcher.launch(name)
        }

        var clicked = false
        val deadline = SystemClock.uptimeMillis() + 40_000L
        while (SystemClock.uptimeMillis() < deadline) {
            val save = device.findObject(By.res("android", "button1"))
                ?: device.findObject(By.text("SAVE"))
                ?: device.findObject(By.text("Save"))
                ?: device.findObject(By.text("Lưu"))
            if (save != null && save.isEnabled) {
                clicked = runCatching { save.click(); true }.getOrDefault(false)
                if (clicked) break
            }
            SystemClock.sleep(250)
        }
        assertTrue("Could not click real DocumentsUI Save", clicked)
        assertTrue("CreateDocument callback timed out", latch.await(35, TimeUnit.SECONDS))
        if (error.get() != null) throw AssertionError("CreateDocument failed", error.get())
        return checkNotNull(created.get())
    }

    @Test fun seedSignedV5PlaylistFavoriteAndRealSafGrant() {
        assertTrue(
            "QA seed already exists; refusing to overwrite it",
            !prefs.contains("playlist_id")
        )
        val document = saveActualDocument()
        val uri = document.toString()
        val title = "Nhạc nâng cấp v6 QA"
        val picked = LocalPickedMusicStore.getInstance(context)
        picked.remember(listOf(PickedMusicRecord(
            uri = uri, title = title, artist = "QA nội bộ",
            addedSeconds = System.currentTimeMillis() / 1000L,
            durationMs = 1_000L
        )))
        assertTrue(
            "DocumentsUI READ must persist before the app upgrade",
            resolver.persistedUriPermissions.any { it.uri == document && it.isReadPermission }
        )
        assertTrue(runBlocking { picked.inspect().available.any { it.uri == document } })
        val playlist = runBlocking {
            checkNotNull(playlistRepository.create("MusicFlow nâng cấp QA"))
        }
        assertTrue(runBlocking {
            playlistRepository.addTrack(playlist.id, PlaylistTrack(
                mediaId = uri, audioUri = uri, title = title, artist = "QA nội bộ",
                addedAtMs = System.currentTimeMillis()
            ))
        })
        assertTrue(LocalFavoritesStore.getInstance(context).addSongIfAbsent(DeviceSong(
            id = -42L, uri = document, title = title,
            artist = "QA nội bộ", durationMs = 1_000L
        )))
        assertTrue("QA marker commit failed", prefs.edit()
            .putString("playlist_id", playlist.id)
            .putString("document_uri", uri)
            .putString("song_title", title)
            .commit())
        assertEquals(1, runBlocking { playlistRepository.getTracks(playlist.id).size })
    }

    @Test fun verifySignedV6PreservesAllDataAndCleanUpTestOwnedOnly() {
        val playlistId = checkNotNull(prefs.getString("playlist_id", null)) {
            "No QA seed; seed signed v5 before installing signed v6"
        }
        val uriText = checkNotNull(prefs.getString("document_uri", null))
        val title = checkNotNull(prefs.getString("song_title", null))
        val uri = Uri.parse(uriText)
        try {
            assertNotNull(
                "Room playlist lost across signed APK replacement",
                runBlocking { playlistRepository.getPlaylist(playlistId) }
            )
            val tracks = runBlocking { playlistRepository.getTracks(playlistId) }
            assertEquals(listOf(uriText), tracks.map { it.audioUri })
            assertEquals(title, tracks.single().title)
            assertTrue(
                "Favorites lost across APK replacement",
                uriText in LocalFavoritesStore.getInstance(context).songUris.value
            )
            assertTrue(
                "SAF persisted READ permission lost across APK replacement",
                resolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission }
            )
            assertTrue(
                "Picked catalog lost across APK replacement",
                LocalPickedMusicStore.getInstance(context).records.value.any { it.uri == uriText }
            )
            assertTrue(
                "Picked SAF file is not readable after installing signed v6",
                runBlocking {
                    LocalPickedMusicStore.getInstance(context).inspect().available.any {
                        it.uri == uri
                    }
                }
            )
            val signature = ByteArray(4)
            val bytes = checkNotNull(resolver.openInputStream(uri)).use { it.read(signature) }
            assertEquals(4, bytes)
            assertEquals("RIFF", String(signature, Charsets.US_ASCII))
        } finally {
            // Only resources named by this test's own persisted marker.
            runBlocking { playlistRepository.deletePlaylist(playlistId) }
            LocalPickedMusicStore.getInstance(context).forget(uriText)
            val favorites = LocalFavoritesStore.getInstance(context)
            if (uriText in favorites.songUris.value) favorites.toggleSong(uri)
            runCatching { DocumentsContract.deleteDocument(resolver, uri) }
            runCatching { resolver.releasePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            ) }
            assertTrue(prefs.edit().clear().commit())
        }
    }
}
