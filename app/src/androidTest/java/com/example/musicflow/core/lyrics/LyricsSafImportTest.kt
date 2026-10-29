package com.example.musicflow.core.lyrics

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.provider.DocumentsContract
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.result.contract.ActivityResultContracts
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.musicflow.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.security.MessageDigest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Creates an actual SAF document using DocumentsUI and imports the returned
 * URI through the production repository. No fake URI/grant is used.
 */
@RunWith(AndroidJUnit4::class)
class LyricsSafImportTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val resolver = context.contentResolver

    private fun saveAction(root: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (root == null) return null
        val labels = listOfNotNull(root.text?.toString(),
            root.contentDescription?.toString(), root.viewIdResourceName)
        if (labels.any {
                it.equals("Save", true) || it.equals("Lưu", true) ||
                    it.endsWith(":id/action_menu_save")
            }) return root
        for (index in 0 until root.childCount) {
            saveAction(root.getChild(index))?.let { return it }
        }
        return null
    }

    private fun confirmSave(): Boolean {
        val end = SystemClock.uptimeMillis() + 35_000L
        while (SystemClock.uptimeMillis() < end) {
            var node = saveAction(instrumentation.uiAutomation.rootInActiveWindow)
            while (node != null) {
                if (node.isClickable &&
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
                node = node.parent
            }
            SystemClock.sleep(250)
        }
        return false
    }

    private fun preferenceKey(audioUri: String): String {
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(audioUri.toByteArray(Charsets.UTF_8))
        return "track_" + hash.joinToString("") {
            "%02x".format(it.toInt() and 255)
        }
    }

    @Test
    fun realSafDocumentImportAndPersistentAudioAssociation() {
        val unique = System.nanoTime()
        val filename = "musicflow_lyrics_qa_$unique.lrc"
        val audioUri = "content://musicflow-lyrics-qa/audio/$unique"
        val unrelatedAudioUri = "content://musicflow-lyrics-qa/other/$unique"
        val originalText = "[ar:QA]\n[offset:+200]\n[00:01.00]First\n[00:02.50]Second"
        val completion = CountDownLatch(1)
        val result = AtomicReference<Uri?>()
        val failure = AtomicReference<Throwable?>()
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        val preferences = context.getSharedPreferences(
            "musicflow_local_lyrics_v1", Context.MODE_PRIVATE)
        try {
            scenario.onActivity { activity ->
                val launcher = activity.activityResultRegistry.register(
                    "musicflow-lyrics-runtime-qa",
                    ActivityResultContracts.CreateDocument("text/plain")
                ) { uri ->
                    try {
                        val chosen = checkNotNull(uri) {
                            "DocumentsUI did not return a real URI"
                        }
                        activity.contentResolver.openOutputStream(chosen, "w").use {
                            checkNotNull(it).write(originalText.toByteArray(Charsets.UTF_8))
                        }
                        result.set(chosen)
                    } catch (e: Throwable) {
                        failure.set(e)
                    } finally {
                        completion.countDown()
                    }
                }
                launcher.launch(filename)
            }
            assertTrue("DocumentsUI Save was not clickable", confirmSave())
            assertTrue("CreateDocument timed out", completion.await(20, TimeUnit.SECONDS))
            assertEquals(null, failure.get())
            val uri = checkNotNull(result.get())
            assertEquals("content", uri.scheme)
            val repo = LocalLyricsRepository(context)
            val parsed = repo.import(audioUri, uri)
            assertEquals(listOf(1200L, 2700L), parsed.map { it.timeMs })
            assertEquals(listOf("First", "Second"), parsed.map { it.text })
            assertTrue("LRC READ grant was not persisted",
                resolver.persistedUriPermissions.any {
                    it.uri == uri && it.isReadPermission
                })
            assertEquals(parsed, LocalLyricsRepository(context).load(audioUri))
            assertTrue(repo.load(unrelatedAudioUri).isEmpty())
            android.util.Log.i("MusicFlowLyricsQA", "PASS realSafImportAndPersistedRead")

            // A failed import must not change the link of another audio file.
            // Some document providers do NOT truncate in "w" mode; trailing
            // valid lyric lines would remain and make this a false negative.
            resolver.openOutputStream(uri, "rwt").use {
                checkNotNull(it).write("[ar:No timestamp]".toByteArray())
            }
            assertEquals("[ar:No timestamp]",
                resolver.openInputStream(uri).use {
                    checkNotNull(it).bufferedReader().readText()
                })
            var rejected = false
            try { repo.import(unrelatedAudioUri, uri) }
            catch (_: IllegalArgumentException) { rejected = true }
            assertTrue("LRC without timestamps must be rejected", rejected)
            assertTrue(repo.load(unrelatedAudioUri).isEmpty())
            android.util.Log.i("MusicFlowLyricsQA", "PASS invalidLrcNotLinked")
        } finally {
            val uri = result.get()
            if (uri != null) {
                runCatching { DocumentsContract.deleteDocument(resolver, uri) }
                runCatching {
                    resolver.releasePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
            preferences.edit()
                .remove(preferenceKey(audioUri))
                .remove(preferenceKey(unrelatedAudioUri))
                .commit()
            scenario.close()
        }
    }
}
