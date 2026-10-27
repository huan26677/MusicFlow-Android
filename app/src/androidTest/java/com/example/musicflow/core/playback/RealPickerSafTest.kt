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
                text.lowercase() in actionLabels
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
        val deadline =
        SystemClock.uptimeMillis() + 90_000L

    var documentVisible = false

    Log.i(
        "MusicFlowRealPickerQA",
        "MANUAL_SELECTION_REQUIRED file=$title"
    )

    while (SystemClock.uptimeMillis() < deadline) {
        val root =
            instrumentation.uiAutomation.rootInActiveWindow

        val currentPackage =
            root?.packageName?.toString()

        if (
            currentPackage ==
                "com.google.android.documentsui"
        ) {
            if (
                findDocumentTitleNode(root, title) != null &&
                !documentVisible
            ) {
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
                "PICKER_RETURNED_TO_APP"
            )

            return documentVisible
        }

        if (
            currentPackage != null &&
            currentPackage !=
                "com.google.android.documentsui"
        ) {
            Log.e(
                "MusicFlowRealPickerQA",
                "UNEXPECTED_PICKER_PACKAGE=$currentPackage"
            )

            return false
        }

        SystemClock.sleep(250)
    }

    Log.e(
        "MusicFlowRealPickerQA",
        "MANUAL_SELECTION_TIMEOUT"
    )

    return false
}

            )

                assertTrue(
                    "KhÃ´ng gá»­i Ä‘Æ°á»£c thao tÃ¡c cháº¡m tÃªn file",
                    tapDocumentTitle(file)
                )

                SystemClock.sleep(500)
                logPickerUi("AFTER_TAP")

                var confirmationClicked = false
                var lastPackage: String? = null

                val returnDeadline =
                    SystemClock.uptimeMillis() + 15_000L

                while (
                    SystemClock.uptimeMillis() < returnDeadline
                ) {
                    val activeRoot =
                        instrumentation.uiAutomation
                            .rootInActiveWindow

                    val currentPackage =
                        activeRoot?.packageName?.toString()

                    if (currentPackage != lastPackage) {
                        Log.i(
                            "MusicFlowRealPickerQA",
                            "ACTIVE_WINDOW_PACKAGE=$currentPackage"
                        )
                        lastPackage = currentPackage
                    }

                    if (currentPackage == context.packageName) {
                        Log.i(
                            "MusicFlowRealPickerQA",
                            "PICKER_RETURNED_TO_APP"
                        )
                        return true
                    }

                    if (
                        currentPackage != null &&
                        currentPackage !=
                            "com.google.android.documentsui"
                    ) {
                        Log.e(
                            "MusicFlowRealPickerQA",
                            "UNEXPECTED_PICKER_PACKAGE=$currentPackage"
                        )
                        return false
                    }

                    if (!confirmationClicked) {
                        val confirmation = findNode(activeRoot) {
                            val label = it.trim().lowercase()

                            label == "open" ||
                                label.startsWith("open (") ||
                                label == "select" ||
                                label.startsWith("select (") ||
                                label == "má»Ÿ" ||
                                label == "chá»n"
                        }

                        if (confirmation != null) {
                            confirmationClicked =
                                clickNode(confirmation)

                            Log.i(
                                "MusicFlowRealPickerQA",
                                "PICKER_CONFIRM_CLICKED=" +
                                    confirmationClicked
                            )
                        }
                    }

                    SystemClock.sleep(200)
                }

                logPickerUi("PICKER_TIMEOUT")

                Log.e(
                    "MusicFlowRealPickerQA",
                    "PICKER_NOT_RETURNED_TO_APP"
                )
                return false
            }

            if (!visitedRecent) {
                visitedRecent = true

                clickNode(
    findNode(root) {
        it.trim().equals("Recent", true) ||
            it.trim().equals("Gần đây", true)
    }
) 
            }

            SystemClock.sleep(200)
        }

        Log.e(
            "MusicFlowRealPickerQA",
            "DOCUMENT_TITLE_NOT_FOUND"
        )
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
