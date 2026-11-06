package com.example.musicflow.ui

import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import com.example.musicflow.core.history.local.RecentHistoryDatabase
import com.example.musicflow.core.playlist.RoomPlaylistRepository
import com.example.musicflow.feature.playlist.PlaylistQuickAddRoute
import com.example.musicflow.ui.theme.MusicFlowTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Real Android DocumentsUI round trip on an isolated QA emulator.
 * Unlike the fake-RPC Compose tests, this verifies that OpenDocument returns an
 * actual content URI to PlaylistQuickAddRoute, that the app persists its read
 * permission, and that Room retains the selected track. Only test-owned media
 * and playlist rows are created/deleted. No sign-in, cloud, or external files.
 */
@RunWith(AndroidJUnit4::class)
class PlaylistQuickAddRealSafUiTest {
    @get:Rule val compose = createComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val device by lazy { UiDevice.getInstance(instrumentation) }

    private fun tinyWav(): ByteArray {
        val sampleRate = 8000
        val pcmBytes = sampleRate / 4 * 2
        val data = ByteBuffer.allocate(44 + pcmBytes).order(ByteOrder.LITTLE_ENDIAN)
        fun ascii(value: String) { data.put(value.toByteArray(Charsets.US_ASCII)) }
        ascii("RIFF"); data.putInt(36 + pcmBytes); ascii("WAVE")
        ascii("fmt "); data.putInt(16); data.putShort(1); data.putShort(1)
        data.putInt(sampleRate); data.putInt(sampleRate * 2)
        data.putShort(2); data.putShort(16)
        ascii("data"); data.putInt(pcmBytes)
        return data.array()
    }

    private fun makeQaAudio(filename: String): Uri {
        val resolver = context.contentResolver
        // Downloads is exposed directly by DocumentsUI, unlike the Music
        // directory which needs several folder-navigation gestures.
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "audio/wav")
            if (Build.VERSION.SDK_INT >= 29) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        val uri = checkNotNull(
            resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        )
        try {
            checkNotNull(resolver.openOutputStream(uri)).use { it.write(tinyWav()) }
            if (Build.VERSION.SDK_INT >= 29) {
                resolver.update(uri, ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }, null, null)
            }
            return uri
        } catch (error: Exception) {
            resolver.delete(uri, null, null)
            throw error
        }
    }

    private fun selectFromRealDocumentsUi(filename: String): Boolean {
        val deadline = SystemClock.uptimeMillis() + 45_000L
        var openedDownloads = false

        fun clickDocumentRow(selector: androidx.test.uiautomator.BySelector): Boolean {
            val child = device.findObject(selector) ?: return false
            var row = child
            repeat(6) {
                if (!row.isClickable && row.parent != null) row = row.parent
            }
            return runCatching {
                // DocumentsUI grid cards may not expose clickable semantics;
                // a real tap on their visible center still activates the row.
                if (row.isClickable) row.click()
                else {
                    val rect = row.visibleBounds
                    if (rect.isEmpty) return@runCatching false
                    device.click(rect.centerX(), rect.centerY())
                }
                true
            }.getOrDefault(false)
        }

        while (SystemClock.uptimeMillis() < deadline) {
            if (clickDocumentRow(By.text(filename)) ||
                clickDocumentRow(By.desc(filename))) {
                    Log.i("MusicFlowQuickAddSafQA", "DOCUMENT_SELECTED=$filename")
                    return true
            }

            // Android 11 OpenDocument defaults to Recent ("No items"). The
            // Downloads root is visible in the Show roots drawer and contains
            // the test-owned MediaStore.Downloads WAV file.
            if (!openedDownloads) {
                if (clickDocumentRow(By.text("Downloads"))) {
                    openedDownloads = true
                    SystemClock.sleep(500)
                    continue
                }
                val drawer = device.findObject(By.desc("Show roots"))
                    ?: device.findObject(By.desc("Show navigation drawer"))
                if (drawer != null) {
                    drawer.click()
                    SystemClock.sleep(500)
                    continue
                }
            }
            SystemClock.sleep(350)
        }
        Log.e("MusicFlowQuickAddSafQA", "DOCUMENT_NOT_FOUND=$filename")
        return false
    }

    @Test fun realOpenDocumentAddsPersistedReadableTrackToSelectedPlaylist() {
        val filename = "musicflow_quickadd_saf_${System.nanoTime()}.wav"
        val repository = RoomPlaylistRepository(RecentHistoryDatabase.getInstance(context))
        val playlist = checkNotNull(runBlocking {
            repository.create("SAF Quick Add QA ${System.nanoTime()}")
        })
        var sourceUri: Uri? = null
        var persisted: Uri? = null
        try {
            sourceUri = makeQaAudio(filename)
            compose.setContent {
                MusicFlowTheme {
                    PlaylistQuickAddRoute(playlist.id, onDone = {})
                }
            }
            compose.waitUntil(15_000) {
                compose.onAllNodesWithTag("quick-add-saf-file")
                    .fetchSemanticsNodes().isNotEmpty()
            }
            assertFalse(runBlocking { repository.getTracks(playlist.id) }.isNotEmpty())
            compose.onNodeWithTag("quick-add-saf-file").performClick()
            assertTrue("System DocumentsUI did not offer test WAV file",
                selectFromRealDocumentsUi(filename))

            compose.waitUntil(25_000) {
                runBlocking { repository.getTracks(playlist.id).size == 1 }
            }
            compose.onNodeWithTag("quick-add-success").assertExists()
            val track = runBlocking { repository.getTracks(playlist.id) }.single()
            val actualUri = Uri.parse(track.audioUri)
            persisted = actualUri
            assertEquals(actualUri.toString(), track.mediaId)
            assertEquals(filename, track.title)
            assertEquals("content", actualUri.scheme)
            assertTrue("Read permission not persisted by the Quick Add callback",
                context.contentResolver.persistedUriPermissions.any {
                    it.uri == actualUri && it.isReadPermission
                })
            context.contentResolver.openFileDescriptor(actualUri, "r")?.use { descriptor ->
                assertTrue("Picked audio document is empty", descriptor.statSize > 44)
            } ?: error("Picked audio document is unreadable")
            assertNotNull(sourceUri)
            Log.i("MusicFlowQuickAddSafQA", "PASS_DOCUMENTS_UI_AND_ROOM")
        } finally {
            runBlocking { repository.deletePlaylist(playlist.id) }
            persisted?.let { uri ->
                runCatching {
                    context.contentResolver.releasePersistableUriPermission(
                        uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            }
            sourceUri?.let { uri ->
                runCatching { context.contentResolver.delete(uri, null, null) }
            }
        }
    }
}
