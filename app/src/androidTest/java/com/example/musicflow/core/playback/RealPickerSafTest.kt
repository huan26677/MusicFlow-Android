package com.example.musicflow.core.playback

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.provider.DocumentsContract
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import android.view.InputDevice
import android.view.MotionEvent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken

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

    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

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

    private fun logPickerUi(stage: String) {
        val root =
            instrumentation.uiAutomation.rootInActiveWindow

        Log.i(
            "MusicFlowRealPickerQA",
            "$stage ROOT_PACKAGE=${root?.packageName}"
        )

        var recorded = 0

        fun visit(node: AccessibilityNodeInfo?, depth: Int) {
            if (node == null || recorded >= 70) return

            val text = node.text?.toString().orEmpty()
                .take(100)
            val description =
                node.contentDescription?.toString().orEmpty()
                    .take(100)

            val actionLabels = setOf(
                "open", "select", "má»Ÿ", "chá»n",
                "recent", "gáº§n Ä‘Ã¢y"
            )

            val isQaFile =
                text.startsWith("musicflow_real_picker_")
            val isActionLabel =
                text.lowercase() in actionLabels ||
                text.equals("Chọn MP3", ignoreCase = true)
            val isImportantView =
                (node.viewIdResourceName ?: "").let { id ->
                    id.contains(
                        "option_menu",
                        ignoreCase = true
                    ) ||
                    id.contains(
                        "button",
                        ignoreCase = true
                    )
                }

            if (
                (text.isNotBlank() || description.isNotBlank()) &&
                (isQaFile || isActionLabel || isImportantView)
            ) {

                Log.i(
                    "MusicFlowRealPickerQA",
                    "$stage UI depth=$depth " +
                        "text=$text desc=$description " +
                        "id=${node.viewIdResourceName} " +
                        "class=${node.className} " +
                        "clickable=${node.isClickable} " +
                        "selected=${node.isSelected}"
                )
                recorded++
            }

            for (i in 0 until node.childCount) {
                visit(node.getChild(i), depth + 1)
            }
        }

        visit(root, 0)
    }

    private fun findDocumentTitleNode(
        node: AccessibilityNodeInfo?,
        title: String
    ): AccessibilityNodeInfo? {
        if (node == null) return null

        if (
            node.viewIdResourceName == "android:id/title" &&
            node.text?.toString() == title
        ) {
            return node
        }

        for (i in 0 until node.childCount) {
            findDocumentTitleNode(
                node.getChild(i),
                title
            )?.let { return it }
        }

        return null
    }

    private fun tapDocumentTitle(
        node: AccessibilityNodeInfo
    ): Boolean {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        if (bounds.isEmpty || !node.isVisibleToUser) {
            Log.e(
                "MusicFlowRealPickerQA",
                "DOCUMENT_TITLE_NOT_VISIBLE"
            )
            return false
        }

        val x = bounds.exactCenterX()
        val y = bounds.exactCenterY()
        val downTime = SystemClock.uptimeMillis()

        val down = MotionEvent.obtain(
            downTime,
            downTime,
            MotionEvent.ACTION_DOWN,
            x, y, 0
        )

        val up = MotionEvent.obtain(
            downTime,
            downTime + 100L,
            MotionEvent.ACTION_UP,
            x, y, 0
        )

        down.source = InputDevice.SOURCE_TOUCHSCREEN
        up.source = InputDevice.SOURCE_TOUCHSCREEN

        return try {
            val downOk =
                instrumentation.uiAutomation
                    .injectInputEvent(down, true)

            SystemClock.sleep(100)

            val upOk =
                instrumentation.uiAutomation
                    .injectInputEvent(up, true)

            Log.i(
                "MusicFlowRealPickerQA",
                "TITLE_TAP down=$downOk up=$upOk"
            )

            downOk && upOk
        } finally {
            down.recycle()
            up.recycle()
        }
    }

    private fun selectRealDocument(title: String): Boolean {
        val deadline = SystemClock.uptimeMillis() + 90_000L
        var documentVisible = false

        Log.i("MusicFlowRealPickerQA", "MANUAL_SELECTION_REQUIRED file=$title")

        while (SystemClock.uptimeMillis() < deadline) {
            val root = instrumentation.uiAutomation.rootInActiveWindow
            val currentPackage = root?.packageName?.toString()

            if (currentPackage == "com.google.android.documentsui") {
                if (!documentVisible && findDocumentTitleNode(root, title) != null) {
                    documentVisible = true
                    Log.i(
                        "MusicFlowRealPickerQA",
                        "DOCUMENT_VISIBLE_READY_FOR_SELECTION"
                    )
                }
            }

            if (currentPackage == context.packageName) {
                Log.i(
                    "MusicFlowRealPickerQA",
                    "PICKER_RETURNED_TO_APP observed=$documentVisible"
                )
                return true
            }

            if (currentPackage != null &&
                currentPackage != "com.google.android.documentsui"
            ) {
                Log.e(
                    "MusicFlowRealPickerQA",
                    "UNEXPECTED_PICKER_PACKAGE=$currentPackage"
                )
                return false
            }

            SystemClock.sleep(250)
        }

        logPickerUi("MANUAL_SELECTION_TIMEOUT")

        Log.e(
            "MusicFlowRealPickerQA",
            "MANUAL_SELECTION_TIMEOUT package=" +
                instrumentation.uiAutomation.rootInActiveWindow?.packageName
        )
        return false
    }

    @Test
    fun realOpenMultipleDocumentsGrantsPersistedReadAndPlays() {
        val title = "musicflow_real_picker_${System.nanoTime()}.wav"
        val created = AtomicReference<Uri?>(null)
        val createError = AtomicReference<String?>(null)
        val finishedCreate = CountDownLatch(1)


        var future: ListenableFuture<MediaController>? = null
        var controller: MediaController? = null

        try {
            val activity = composeRule.activity
            instrumentation.runOnMainSync {
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

            instrumentation.runOnMainSync {
    activity.openAudioPickerForQa()
}

Log.i(
    "MusicFlowRealPickerQA",
    "PASS productionPickerLaunched"
)

            
            
            

            assertTrue(
                "Document was not selectable in real OpenMultipleDocuments UI",
                selectRealDocument(title)
            )

            val deadline =
                SystemClock.uptimeMillis() + 25_000L

            var playsExpected = false
            var actualUri: Uri? = null
            var actualMediaId: String? = null
            var actualTitle: String? = null
            var actualState = -1
            var actualError: String? = null

            while (
                !playsExpected &&
                SystemClock.uptimeMillis() < deadline
            ) {
                instrumentation.runOnMainSync {
                    val item = media.currentMediaItem

                    actualUri =
                        item?.localConfiguration?.uri
                    actualMediaId = item?.mediaId
                    actualTitle =
                        item?.mediaMetadata?.title?.toString()
                    actualState = media.playbackState
                    actualError =
                        media.playerError?.errorCodeName

                    playsExpected =
                        media.isPlaying &&
                        actualTitle == title &&
                        actualUri?.scheme == "content"
                }

                if (!playsExpected) {
                    SystemClock.sleep(150)
                }
            }

            Log.i(
                "MusicFlowRealPickerQA",
                "PICKER_RESULT " +
                    "playing=$playsExpected " +
                    "mediaId=$actualMediaId " +
                    "title=$actualTitle " +
                    "state=$actualState " +
                    "error=$actualError"
            )

            assertTrue(
                "File SAF chưa phát đúng. " +
                    "mediaId=$actualMediaId " +
                    "title=$actualTitle " +
                    "state=$actualState " +
                    "error=$actualError",
                playsExpected
            )

            val selectedUri = checkNotNull(actualUri)

            val header = ByteArray(4)
            val bytesRead =
                context.contentResolver
                    .openInputStream(selectedUri)
                    .use { input ->
                        checkNotNull(input).read(header)
                    }

            assertEquals(
                "Không đọc đủ WAV header",
                4,
                bytesRead
            )

            assertEquals(
                "File đã chọn không phải WAV mong đợi",
                "RIFF",
                String(header, Charsets.US_ASCII)
            )

            Log.i(
                "MusicFlowRealPickerQA",
                "PASS realPickerPlayback"
            )

            assertTrue(
                "Production picker chưa lưu READ permission",
                persistedRead(selectedUri)
            )

            Log.i(
                "MusicFlowRealPickerQA",
                "PASS realPickerPersistedRead"
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
    private fun createMultiQaWav(
        activity: MainActivity,
        title: String,
        key: String
    ): Uri {
        val result = AtomicReference<Uri?>(null)
        val error = AtomicReference<String?>(null)
        val finished = CountDownLatch(1)

        instrumentation.runOnMainSync {
            val launcher = activity.activityResultRegistry.register(
                key,
                ActivityResultContracts.CreateDocument("audio/wav")
            ) { uri ->
                try {
                    val doc = checkNotNull(uri)
                    activity.contentResolver.takePersistableUriPermission(
                        doc,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    activity.contentResolver.openOutputStream(doc, "w").use {
                        checkNotNull(it).write(wavBytes())
                    }
                    activity.contentResolver.releasePersistableUriPermission(
                        doc,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    result.set(doc)
                } catch (e: Exception) {
                    error.set(e.toString())
                } finally {
                    finished.countDown()
                }
            }
            launcher.launch(title)
        }

        assertTrue("Save not found", clickNamedAction(20_000L) {
            it.equals("Save", true) || it.equals("Lưu", true)
        })
        assertTrue("Document creation timeout", finished.await(25, TimeUnit.SECONDS))
        assertNull(error.get())

        return checkNotNull(result.get())
    }


    @Test
    fun realOpenMultipleDocumentsSelectsTwoFilesAndNavigates() {
        val id = System.nanoTime()
        val titles = listOf(
            "musicflow_real_picker_multi_${id}_1.wav",
            "musicflow_real_picker_multi_${id}_2.wav"
        )
        val created = mutableListOf<Uri>()
        var future: ListenableFuture<MediaController>? = null
        var controller: MediaController? = null

        try {
            val activity = composeRule.activity

            titles.forEachIndexed { index, title ->
                created += createMultiQaWav(
                    activity,
                    title,
                    "musicflow-multi-$id-$index"
                )
            }

            assertTrue(created.all { !persistedRead(it) })
            Log.i("MusicFlowRealPickerQA", "PASS multiSourcesPrepared")

            val connecting = MediaController.Builder(
                context,
                SessionToken(
                    context,
                    ComponentName(context, PlaybackService::class.java)
                )
            ).buildAsync()
            future = connecting
            val media = connecting.get(20, TimeUnit.SECONDS)
            controller = media

            instrumentation.runOnMainSync {
                activity.openAudioPickerForQa()
            }

            Log.i(
                "MusicFlowRealPickerQA",
                "MULTI_SELECTION_REQUIRED ${titles.joinToString()}"
            )

            assertTrue(
                "Picker did not return",
                selectRealDocument(titles.first())
            )

            var queueUris = emptyList<Uri>()
            var playing = false
            val deadline = SystemClock.uptimeMillis() + 25_000L

            while (SystemClock.uptimeMillis() < deadline) {
                instrumentation.runOnMainSync {
                    queueUris = (0 until media.mediaItemCount).mapNotNull {
                        media.getMediaItemAt(it).localConfiguration?.uri
                    }
                    playing = media.isPlaying
                }

                if (
                    playing &&
                    queueUris.size == 2 &&
                    queueUris.toSet() == created.toSet()
                ) break

                SystemClock.sleep(200)
            }

            assertEquals(2, queueUris.size)
            assertEquals(created.toSet(), queueUris.toSet())

            assertEquals(
                "Queue order differs from picker result",
                activity.lastPickedUrisForQa,
                queueUris
            )
            assertTrue("Queue is not playing", playing)

            Log.i("MusicFlowRealPickerQA", "PASS multiQueue")

            queueUris.forEach { uri ->
                assertTrue("READ not persisted: $uri", persistedRead(uri))

                val header = ByteArray(4)
                val count = context.contentResolver.openInputStream(uri).use {
                    checkNotNull(it).read(header)
                }
                assertEquals(4, count)
                assertEquals("RIFF", String(header, Charsets.US_ASCII))
            }

            Log.i("MusicFlowRealPickerQA", "PASS multiPersistedRead")

            instrumentation.runOnMainSync {
                media.shuffleModeEnabled = false
                media.repeatMode = Player.REPEAT_MODE_OFF
                media.seekTo(0, 0L)
            }

            val nextDeadline = SystemClock.uptimeMillis() + 10_000L
            var firstReady = false

            while (SystemClock.uptimeMillis() < nextDeadline) {
                instrumentation.runOnMainSync {
                    firstReady =
                        media.currentMediaItem?.localConfiguration?.uri == queueUris[0]
                }
                if (firstReady) break
                SystemClock.sleep(100)
            }

            assertTrue("First item not ready", firstReady)

            instrumentation.runOnMainSync {
                media.seekToNextMediaItem()
            }

            var nextReady = false
            val end = SystemClock.uptimeMillis() + 10_000L

            while (SystemClock.uptimeMillis() < end) {
                instrumentation.runOnMainSync {
                    nextReady =
                        media.currentMediaItem?.localConfiguration?.uri == queueUris[1] &&
                            media.isPlaying
                }
                if (nextReady) break
                SystemClock.sleep(100)
            }

            assertTrue("Next item not playing", nextReady)
            Log.i("MusicFlowRealPickerQA", "PASS multiNext")
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

            created.forEach { uri ->
                runCatching {
                    DocumentsContract.deleteDocument(context.contentResolver, uri)
                }
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
