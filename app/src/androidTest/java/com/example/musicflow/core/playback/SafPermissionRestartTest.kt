package com.example.musicflow.core.playback

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Process
import android.os.SystemClock
import android.provider.DocumentsContract
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.musicflow.MainActivity
import com.example.musicflow.core.favorites.LocalFavoritesStore
import com.google.common.util.concurrent.ListenableFuture
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Phase-specific test. Run using scripts/run-saf-permission-v1-tests.ps1.
 * Requires Android DocumentsUI and a writable local document root.
 * Uses ACTION_CREATE_DOCUMENT to obtain a REAL persistable URI grant.
 * This DOES NOT replace testing the production OpenMultipleDocuments UI.
 */
@RunWith(AndroidJUnit4::class)
class SafPermissionRestartTest {

    @get:Rule val composeRule = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val prefs = context.getSharedPreferences("musicflow_saf_qa_v1", Context.MODE_PRIVATE)

    private fun requirePhase(phase: String) {
        assumeTrue(
            "Only run via SAF phased QA script",
            InstrumentationRegistry.getArguments()
                .getString("musicflow_saf_phase") == phase
        )
    }

    private fun wavBytes(): ByteArray {
        val rate = 8000
        val length = rate * 30 * 2
        val buffer = ByteBuffer.allocate(44 + length)
            .order(ByteOrder.LITTLE_ENDIAN)

        fun ascii(value: String) {
            buffer.put(value.toByteArray(Charsets.US_ASCII))
        }

        ascii("RIFF")
        buffer.putInt(36 + length)
        ascii("WAVE")
        ascii("fmt ")
        buffer.putInt(16)
        buffer.putShort(1.toShort())
        buffer.putShort(1.toShort())
        buffer.putInt(rate)
        buffer.putInt(rate * 2)
        buffer.putShort(2.toShort())
        buffer.putShort(16.toShort())
        ascii("data")
        buffer.putInt(length)
        return buffer.array()
    }

    private fun findSaveAction(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        val labels = listOfNotNull(
            node.text?.toString(),
            node.contentDescription?.toString(),
            node.viewIdResourceName
        )
        if (labels.any {
                it.trim().equals("Save", true) ||
                    it.trim().equals("Lưu", true) ||
                    it.endsWith(":id/action_menu_save") ||
                    it.endsWith(":id/save")
            }) {
            return node
        }
        for (index in 0 until node.childCount) {
            val found = findSaveAction(node.getChild(index))
            if (found != null) return found
        }
        return null
    }

    private fun logDocumentUiTree() {
        val root = instrumentation.uiAutomation.rootInActiveWindow
        Log.w("MusicFlowSafQA", "SAVE_NOT_FOUND root=${root?.packageName}")
        var n = 0
        fun visit(node: AccessibilityNodeInfo?, depth: Int) {
            if (node == null || n++ >= 80 || depth > 12) return
            Log.w("MusicFlowSafQA",
                "UI depth=$depth class=${node.className} id=${node.viewIdResourceName} " +
                    "text=${node.text} desc=${node.contentDescription} clickable=${node.isClickable}")
            for (i in 0 until node.childCount) visit(node.getChild(i), depth + 1)
        }
        visit(root, 0)
    }

    private fun clickSaveInDocumentsUi(): Boolean {
        // API 36 cold-boot emulator can show a transient SystemUI ANR while
        // the system picker initializes. Explicitly choose "Wait" if shown;
        // never treat this as a successful document selection.
        val timeout = if (android.os.Build.VERSION.SDK_INT >= 36) 90_000L
            else 30_000L
        val deadline = SystemClock.uptimeMillis() + timeout
        while (SystemClock.uptimeMillis() < deadline) {
            val root = instrumentation.uiAutomation.rootInActiveWindow
            val systemWait = findSystemWaitAction(root)
            if (systemWait != null) {
                Log.w("MusicFlowSafQA",
                    "SYSTEM_UI_ANR_ON_EMULATOR waiting for picker to recover")
                systemWait.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                SystemClock.sleep(600)
                continue
            }
            val target = findSaveAction(root)
            if (target != null) {
                var clickable: AccessibilityNodeInfo? = target
                while (clickable != null) {
                    if (clickable.isClickable &&
                        clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    ) return true
                    clickable = clickable.parent
                }
            }
            SystemClock.sleep(200)
        }
        logDocumentUiTree()
        return false
    }

    private fun findSystemWaitAction(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        val id = node.viewIdResourceName.orEmpty()
        if (id == "android:id/aerr_wait" && node.isClickable) return node
        for (index in 0 until node.childCount) {
            findSystemWaitAction(node.getChild(index))?.let { return it }
        }
        return null
    }

    private fun persistedRead(uri: Uri): Boolean =
        context.contentResolver.persistedUriPermissions.any { grant ->
            grant.uri == uri && grant.isReadPermission
        }

    @Test
    fun prepareSafDocument() {
        requirePhase("prepare")
        assertNull("SAF QA còn dữ liệu từ vòng trước", prefs.getString("uri", null))

        val title = "musicflow_saf_qa_${System.nanoTime()}.wav"
        val completed = CountDownLatch(1)
        val problem = AtomicReference<String?>(null)
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        try {
            scenario.onActivity { activity ->
                val launcher = activity.activityResultRegistry.register(
                    "musicflow-saf-qa-create-v1",
                    ActivityResultContracts.CreateDocument("audio/wav")
                ) { uri ->
                    try {
                        val document = checkNotNull(uri) {
                            "DocumentsUI did not return a document"
                        }
                        activity.contentResolver.takePersistableUriPermission(
                            document,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                        activity.contentResolver.openOutputStream(document, "w").use { stream ->
                            checkNotNull(stream).write(wavBytes())
                        }
                        LocalFavoritesStore.getInstance(activity)
                            .toggleSong(document, title, "MusicFlow QA")
                        check(
                            prefs.edit()
                                .putString("uri", document.toString())
                                .putString("title", title)
                                .commit()
                        ) { "Could not save SAF QA document URI" }
                    } catch (e: Exception) {
                        problem.set(e.toString())
                    } finally {
                        completed.countDown()
                    }
                }
                launcher.launch(title)
            }

            assertTrue("DocumentsUI Save action was not found", clickSaveInDocumentsUi())
            assertTrue("DocumentsUI did not finish", completed.await(25, TimeUnit.SECONDS))
            assertNull("SAF setup failed: ${problem.get()}", problem.get())

            val uri = Uri.parse(checkNotNull(prefs.getString("uri", null)))
            assertTrue("Read grant not persisted", persistedRead(uri))
            assertTrue("Document is not readable", context.contentResolver.openInputStream(uri).use {
                it != null && it.read() >= 0
            })
            Log.i("MusicFlowSafQA", "SAF_PREPARE_PID=${Process.myPid()}")
            Log.i("MusicFlowSafQA", "SAF_PREPARE_PASS")
        } finally {
            scenario.close()
        }
    }

    @Test
    fun verifySafAfterForceStop() {
        requirePhase("verify")
        val uri = Uri.parse(checkNotNull(prefs.getString("uri", null)))
        val title = checkNotNull(prefs.getString("title", null))
        assertTrue("Persisted grant lost after force-stop", persistedRead(uri))

        val header = ByteArray(4)
        context.contentResolver.openInputStream(uri).use { input ->
            checkNotNull(input).read(header)
        }
        assertEquals("RIFF", String(header, Charsets.US_ASCII))
        assertTrue(
            "Favorite lost after process restart",
            uri.toString() in LocalFavoritesStore.getInstance(context).songUris.value
        )
        Log.i("MusicFlowSafQA", "SAF_VERIFY_PID=${Process.myPid()}")
        Log.i("MusicFlowSafQA", "SAF_PERSISTED_READ_PASS")

        val scenario = ActivityScenario.launch(MainActivity::class.java)
        var future: ListenableFuture<MediaController>? = null
        var controller: MediaController? = null
        try {
            val token = SessionToken(
                context,
                ComponentName(context, PlaybackService::class.java)
            )
            future = MediaController.Builder(context, token).buildAsync()
            val media = future.get(20, TimeUnit.SECONDS)
            controller = media

            val favoriteCount = LocalFavoritesStore.getInstance(context).songUris.value.size
            composeRule.onNodeWithText("♥ Bài hát yêu thích ($favoriteCount)")
                .performScrollTo()
                .performClick()
            composeRule.onNodeWithText(title).performClick()

            var plays = false
            val deadline = SystemClock.uptimeMillis() + 20_000L
            while (!plays && SystemClock.uptimeMillis() < deadline) {
                instrumentation.runOnMainSync {
                    plays = media.isPlaying &&
                        media.currentMediaItem?.mediaId == uri.toString()
                }
                if (!plays) SystemClock.sleep(100)
            }
            assertTrue("Favorite SAF URI did not play after force-stop", plays)
            Log.i("MusicFlowSafQA", "SAF_FAVORITE_PLAYBACK_PASS")
        } finally {
            controller?.let { media ->
                instrumentation.runOnMainSync {
                    media.pause()
                    media.stop()
                    media.clearMediaItems()
                }
            }
            future?.let { f ->
                instrumentation.runOnMainSync { MediaController.releaseFuture(f) }
            }
            scenario.close()
        }
    }

    @Test
    fun cleanupSafDocument() {
        requirePhase("cleanup")
        val value = prefs.getString("uri", null)
        if (value != null) {
            val uri = Uri.parse(value)
            val store = LocalFavoritesStore.getInstance(context)
            if (value in store.songUris.value) store.toggleSong(uri)

            // Persisted write access was acquired in prepare.
            runCatching {
                DocumentsContract.deleteDocument(context.contentResolver, uri)
            }.onFailure { Log.w("MusicFlowSafQA", "Could not delete QA document", it) }
            runCatching {
                context.contentResolver.releasePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
        }
        assertTrue(prefs.edit().clear().commit())
        Log.i("MusicFlowSafQA", "SAF_CLEANUP_PASS")
    }
}
