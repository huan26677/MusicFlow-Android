package com.example.musicflow.core.lyrics

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import com.example.musicflow.MainActivity
import com.example.musicflow.core.playback.PlaybackService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Starts from MusicFlow's actual Now Playing button, uses the OS OpenDocument
 * picker, invokes MainActivity's real result callback and verifies that the
 * Compose lyrics view follows the real MediaSession seek position.
 */
@RunWith(AndroidJUnit4::class)
class LyricsImportUiE2eTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val resolver = context.contentResolver
    private val device by lazy { UiDevice.getInstance(instrumentation) }

    private fun main(action: () -> Unit) = instrumentation.runOnMainSync(action)

    private fun wav(): File {
        val samples = 8000 * 30
        val bytes = ByteBuffer.allocate(44 + samples * 2)
            .order(ByteOrder.LITTLE_ENDIAN)
        fun put(s: String) { bytes.put(s.toByteArray(Charsets.US_ASCII)) }
        put("RIFF"); bytes.putInt(36 + samples * 2)
        put("WAVE"); put("fmt "); bytes.putInt(16)
        bytes.putShort(1.toShort()); bytes.putShort(1.toShort())
        bytes.putInt(8000); bytes.putInt(16000)
        bytes.putShort(2.toShort()); bytes.putShort(16.toShort())
        put("data"); bytes.putInt(samples * 2)
        return File.createTempFile("musicflow_lrc_ui_", ".wav", context.cacheDir)
            .apply { writeBytes(bytes.array()) }
    }

    private fun findSave(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        val labels = listOfNotNull(node.text?.toString(),
            node.contentDescription?.toString(), node.viewIdResourceName)
        if (labels.any { it.equals("Save", true) || it.equals("Lưu", true) ||
                it.endsWith(":id/action_menu_save") }) return node
        for (i in 0 until node.childCount) {
            findSave(node.getChild(i))?.let { return it }
        }
        return null
    }

    private fun createLrcDocument(fileName: String, contents: String): Uri {
        val result = AtomicReference<Uri?>()
        val failure = AtomicReference<Throwable?>()
        val complete = CountDownLatch(1)
        compose.activityRule.scenario.onActivity { activity ->
            val launcher = activity.activityResultRegistry.register(
                "musicflow-lyrics-ui-e2e-document",
                ActivityResultContracts.CreateDocument("text/plain")
            ) { uri ->
                try {
                    val created = checkNotNull(uri) { "No SAF CreateDocument URI" }
                    activity.contentResolver.openOutputStream(created, "rwt").use {
                        checkNotNull(it).write(contents.toByteArray(Charsets.UTF_8))
                    }
                    result.set(created)
                } catch (e: Throwable) {
                    failure.set(e)
                } finally {
                    complete.countDown()
                }
            }
            launcher.launch(fileName)
        }

        val deadline = SystemClock.uptimeMillis() + 35_000L
        var saved = false
        while (SystemClock.uptimeMillis() < deadline && !saved) {
            var node = findSave(instrumentation.uiAutomation.rootInActiveWindow)
            while (node != null) {
                if (node.isClickable &&
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    saved = true
                    break
                }
                node = node.parent
            }
            if (!saved) SystemClock.sleep(200L)
        }
        assertTrue("DocumentsUI Save was not clicked", saved)
        assertTrue("SAF CreateDocument callback timed out",
            complete.await(20, TimeUnit.SECONDS))
        assertEquals(null, failure.get())
        return checkNotNull(result.get())
    }

    private fun documentName(uri: Uri): String =
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME),
            null, null, null).use { cursor ->
            assertNotNull("DocumentsProvider returned no document metadata", cursor)
            checkNotNull(cursor).run {
                assertTrue(moveToFirst())
                getString(0)
            }
        }

    private fun pickRealLrc(fileName: String): Boolean {
        val deadline = SystemClock.uptimeMillis() + 40_000L
        var clicked = false
        var confirmed = false
        while (SystemClock.uptimeMillis() < deadline) {
            val pkg = instrumentation.uiAutomation.rootInActiveWindow
                ?.packageName?.toString()
            if (clicked && pkg == context.packageName) {
                // Foreground app alone is not proof of a successful
                // ActivityResult. The test verifies saved URI + displayed
                // lyrics from MainActivity's actual callback afterward.
                Log.i("MusicFlowLyricsUiQA", "PICKER_RETURNED_TO_MUSICFLOW")
                return true
            }
            if (pkg == "com.google.android.documentsui") {
                if (!clicked) {
                    val title = device.findObject(By.text(fileName))
                    if (title != null) {
                        var card = title
                        var parent = title.parent
                        while (parent != null) {
                            if (parent.resourceName?.endsWith(":id/item_root") == true) {
                                card = parent
                                break
                            }
                            parent = parent.parent
                        }
                        card.longClick()
                        clicked = true
                        Log.i("MusicFlowLyricsUiQA", "PICKER_TOUCHED_REAL_DOCUMENT")
                    }
                } else if (!confirmed) {
                    val select = device.findObject(
                        By.res("com.google.android.documentsui", "action_menu_select")
                    )
                    if (select != null) {
                        select.click()
                        confirmed = true
                        Log.i("MusicFlowLyricsUiQA", "PICKER_SELECT_CLICKED")
                    }
                }
            }
            SystemClock.sleep(250L)
        }
        Log.e("MusicFlowLyricsUiQA",
            "PICKER_TIMEOUT clicked=$clicked confirmed=$confirmed " +
                "package=${instrumentation.uiAutomation.rootInActiveWindow?.packageName}")
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
    fun playerButtonOpensRealLrcAndAutomaticallyHighlightsAfterSeek() {
        val audio = wav()
        val audioUri = Uri.fromFile(audio).toString()
        val name = "musicflow_lyrics_ui_${System.nanoTime()}.lrc"
        val lines = "[ar:MusicFlow QA]\n[00:01.00]UI first line\n" +
            "[00:03.00]UI second line\n[00:06.00]UI last line"
        val preferences = context.getSharedPreferences(
            "musicflow_local_lyrics_v1", Context.MODE_PRIVATE
        )
        val key = preferenceKey(audioUri)
        val previousLink = preferences.getString(key, null)
        var doc: Uri? = null
        var media: MediaController? = null
        val future = MediaController.Builder(
            context, SessionToken(context, ComponentName(
                context, PlaybackService::class.java
            ))
        ).buildAsync()
        var originalItems = emptyList<MediaItem>()
        var originalIndex = 0
        var originalPosition = 0L
        var originalWasPlaying = false
        var originalShuffle = false
        var originalRepeat = Player.REPEAT_MODE_OFF
        try {
            doc = createLrcDocument(name, lines)
            val documentName = documentName(doc)
            Log.i("MusicFlowLyricsUiQA", "PASS realLrcCreated")
            assertFalse("Must not pre-import lyrics for E2E test",
                resolver.persistedUriPermissions.any {
                    it.uri == doc && it.isReadPermission
                })

            media = future.get(20, TimeUnit.SECONDS)
            val player = media
            main {
                originalItems = (0 until player.mediaItemCount)
                    .map(player::getMediaItemAt)
                originalIndex = player.currentMediaItemIndex
                originalPosition = player.currentPosition.coerceAtLeast(0L)
                originalWasPlaying = player.playWhenReady
                originalShuffle = player.shuffleModeEnabled
                originalRepeat = player.repeatMode
                player.setMediaItem(
                    MediaItem.Builder().setMediaId("lyrics-ui-e2e")
                        .setUri(Uri.parse(audioUri))
                        .setMediaMetadata(MediaMetadata.Builder()
                            .setTitle("Lyrics UI E2E track")
                            .setArtist("MusicFlow QA").build())
                        .build()
                )
                player.prepare()
                player.play()
            }
            compose.waitUntil(15_000L) {
                compose.onAllNodesWithText("Lyrics UI E2E track")
                    .fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithContentDescription(
                "Mở trình phát: Lyrics UI E2E track, MusicFlow QA"
            ).performClick()
            compose.onNodeWithText("Nhập file .lrc")
                .performScrollTo().performClick()
            Log.i("MusicFlowLyricsUiQA", "PASS realNowPlayingImportButton")

            assertTrue("OpenDocument did not return actual selected LRC",
                pickRealLrc(documentName))
            compose.waitUntil(15_000L) {
                compose.onAllNodesWithText("UI first line")
                    .fetchSemanticsNodes().isNotEmpty()
            }
            assertEquals(doc.toString(), preferences.getString(key, null))
            assertTrue("Missing persisted SAF READ permission",
                resolver.persistedUriPermissions.any {
                    it.uri == doc && it.isReadPermission
                })
            assertEquals(3, LocalLyricsRepository(context).load(audioUri).size)
            Log.i("MusicFlowLyricsUiQA", "PASS productionCallbackPersistedAndRendered")

            main { player.pause() }
            for ((seek, expected) in listOf(
                1400L to "UI first line",
                3400L to "UI second line",
                6400L to "UI last line",
                1600L to "UI first line"
            )) {
                main { player.seekTo(seek) }
                compose.waitUntil(10_000L) {
                    runCatching {
                        compose.onNodeWithTag("lyrics-active-line")
                            .assertTextEquals(expected)
                        true
                    }.getOrDefault(false)
                }
                compose.onNodeWithTag("lyrics-active-line")
                    .assertTextEquals(expected)
            }
            Log.i("MusicFlowLyricsUiQA", "PASS realPlayerSeekUpdatesImportedLyrics")
        } finally {
            media?.let { player ->
                main {
                    player.pause()
                    if (originalItems.isEmpty()) player.clearMediaItems()
                    else {
                        player.setMediaItems(originalItems,
                            originalIndex.coerceIn(0, originalItems.lastIndex),
                            originalPosition)
                        player.prepare()
                    }
                    player.shuffleModeEnabled = originalShuffle
                    player.repeatMode = originalRepeat
                    if (originalWasPlaying && originalItems.isNotEmpty()) player.play()
                }
            }
            main { MediaController.releaseFuture(future) }
            preferences.edit().apply {
                if (previousLink == null) remove(key)
                else putString(key, previousLink)
            }.commit()
            doc?.let { uri ->
                runCatching { DocumentsContract.deleteDocument(resolver, uri) }
                runCatching {
                    resolver.releasePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            }
            audio.delete()
        }
    }
}
