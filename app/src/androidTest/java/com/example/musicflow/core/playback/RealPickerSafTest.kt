package com.example.musicflow.core.playback

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.provider.DocumentsContract
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.musicflow.MainActivity
import com.example.musicflow.core.history.RoomRecentHistoryRepository
import com.example.musicflow.core.history.local.RecentHistoryDatabase
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Verifies the production OpenMultipleDocuments flow with a real DocumentsUI URI.
 * Limited to a foreground session; process-restart SAF coverage is in
 * SafPermissionRestartTest. DocumentsUI layout may vary across system images.
 */
@RunWith(AndroidJUnit4::class)
class RealPickerSafTest {

    @get:Rule val composeRule = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    private fun wavBytes(): ByteArray {
        val rate = 8000
        val length = rate * 30 * 2
        val buffer = ByteBuffer.allocate(44 + length)
            .order(ByteOrder.LITTLE_ENDIAN)
        fun write(s: String) { buffer.put(s.toByteArray(Charsets.US_ASCII)) }
        write("RIFF")
        buffer.putInt(36 + length)
        write("WAVE")
        write("fmt ")
        buffer.putInt(16)
        buffer.putShort(1.toShort())
        buffer.putShort(1.toShort())
        buffer.putInt(rate)
        buffer.putInt(rate * 2)
        buffer.putShort(2.toShort())
        buffer.putShort(16.toShort())
        write("data")
        buffer.putInt(length)
        return buffer.array()
    }

    private fun persistedRead(uri: Uri): Boolean =
        context.contentResolver.persistedUriPermissions.any {
            it.uri == uri && it.isReadPermission
        }

    private fun findNode(
        root: AccessibilityNodeInfo?,
        predicate: (String) -> Boolean
    ): AccessibilityNodeInfo? {
        if (root == null) return null
        val texts = listOfNotNull(
            root.text?.toString(),
            root.contentDescription?.toString()
        )
        if (texts.any(predicate)) return root
        for (i in 0 until root.childCount) {
            findNode(root.getChild(i), predicate)?.let { return it }
        }
        return null
    }

    private fun clickNode(node: AccessibilityNodeInfo?): Boolean {
        var current = node
        while (current != null) {
            if (current.isClickable && current.performAction(
                    AccessibilityNodeInfo.ACTION_CLICK
                )
            ) return true
            current = current.parent
        }
        return false
    }

    private fun clickNamedAction(
        timeoutMs: Long,
        matcher: (String) -> Boolean
    ): Boolean {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            val node = findNode(
                instrumentation.uiAutomation.rootInActiveWindow,
                matcher
            )
            if (clickNode(node)) return true
            SystemClock.sleep(200)
        }
        return false
    }

    private fun selectRealDocument(title: String): Boolean {
        val deadline = SystemClock.uptimeMillis() + 25_000L
        var visitedRecent = false
        while (SystemClock.uptimeMillis() < deadline) {
            val root = instrumentation.uiAutomation.rootInActiveWindow
            val file = findNode(root) {
                it.contains(title, ignoreCase = true)
            }
            if (clickNode(file)) {
                // Single-item ACTION_OPEN_DOCUMENT may return directly.
                // On multi-select layouts an Open/Select confirmation is shown.
                clickNamedAction(2_000L) { label ->
                    label.trim().equals("Open", true) ||
                        label.trim().equals("Select", true) ||
                        label.trim().equals("Mở", true) ||
                        label.trim().equals("Chọn", true)
                }
                return true
            }
            if (!visitedRecent &&
                SystemClock.uptimeMillis() + 22_000L < deadline
            ) {
                visitedRecent = true
                clickNode(findNode(root) {
                    it.trim().equals("Recent", true) ||
                        it.trim().equals("Gần đây", true)
                })
            }
            SystemClock.sleep(200)
        }
        return false
    }

    @Test
    fun realOpenMultipleDocumentsGrantsPersistedReadAndPlays() {
        val title = "musicflow_real_picker_${System.nanoTime()}.wav"
        val created = AtomicReference<Uri?>(null)
        val createError = AtomicReference<String?>(null)
        val finishedCreate = CountDownLatch(1)

        var scenario: ActivityScenario<MainActivity>? = null
        var future: ListenableFuture<MediaController>? = null
        var controller: MediaController? = null

        try {
            val launched = ActivityScenario.launch(MainActivity::class.java)
            scenario = launched
            launched.onActivity { activity ->
                val launcher = activity.activityResultRegistry.register(
                    "musicflow-real-picker-source-v1",
                    ActivityResultContracts.CreateDocument("audio/wav")
                ) { uri ->
                    try {
                        val doc = checkNotNull(uri) {
                            "No URI returned from ACTION_CREATE_DOCUMENT"
                        }
                        activity.contentResolver.takePersistableUriPermission(
                            doc,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                        activity.contentResolver.openOutputStream(doc, "w").use {
                            checkNotNull(it).write(wavBytes())
                        }
                        // The real MusicFlow picker must acquire its own READ grant.
                        activity.contentResolver.releasePersistableUriPermission(
                            doc, Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                        created.set(doc)
                    } catch (e: Exception) {
                        createError.set(e.toString())
                    } finally {
                        finishedCreate.countDown()
                    }
                }
                launcher.launch(title)
            }

            assertTrue(
                "DocumentsUI Save/Lưu button not found",
                clickNamedAction(20_000L) { label ->
                    label.trim().equals("Save", true) ||
                        label.trim().equals("Lưu", true)
                }
            )
            assertTrue(
                "Source document was not created",
                finishedCreate.await(25, TimeUnit.SECONDS)
            )
            assertNull("CreateDocument failed: ${createError.get()}", createError.get())

            val uri = checkNotNull(created.get())
            assertFalse(
                "READ permission already persisted before real picker",
                persistedRead(uri)
            )
            Log.i("MusicFlowRealPickerQA", "PASS sourcePreparedWithoutPersistedRead")

            val token = SessionToken(
                context,
                ComponentName(context, PlaybackService::class.java)
            )
            future = MediaController.Builder(context, token).buildAsync()
            val media = future.get(20, TimeUnit.SECONDS)
            controller = media

            composeRule.onNodeWithText("Mở thư viện thiết bị")
                .performClick()
            composeRule.onNodeWithText("Chọn MP3").performClick()

            assertTrue(
                "Document was not selectable in real OpenMultipleDocuments UI",
                selectRealDocument(title)
            )

            val deadline = SystemClock.uptimeMillis() + 25_000L
            var playsExpected = false
            while (!playsExpected && SystemClock.uptimeMillis() < deadline) {
                instrumentation.runOnMainSync {
                    playsExpected = media.isPlaying &&
                        media.currentMediaItem?.mediaId == uri.toString()
                }
                if (!playsExpected) SystemClock.sleep(150)
            }
            assertTrue(
                "Production picker did not play selected SAF document",
                playsExpected
            )
            Log.i("MusicFlowRealPickerQA", "PASS realPickerPlayback")

            assertTrue(
                "Production callback did not persist READ grant",
                persistedRead(uri)
            )
            Log.i("MusicFlowRealPickerQA", "PASS realPickerPersistedRead")
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
            created.get()?.let { uri ->
                runBlocking {
                    runCatching {
                        RoomRecentHistoryRepository(
                            RecentHistoryDatabase.getInstance(context)
                        ).remove(uri.toString())
                    }.onFailure { Log.w("MusicFlowRealPickerQA", "History cleanup", it) }
                }
                runCatching {
                    if (!DocumentsContract.deleteDocument(context.contentResolver, uri)) {
                        Log.w("MusicFlowRealPickerQA", "Document cleanup returned false")
                    }
                }.onFailure { Log.w("MusicFlowRealPickerQA", "Document cleanup", it) }
                runCatching {
                    context.contentResolver.releasePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }
            }
        }
    }
}
