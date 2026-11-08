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
import com.example.musicflow.core.playlist.PlaylistBackupEntry
import com.example.musicflow.core.playlist.PlaylistLocalBackup
import com.example.musicflow.core.playlist.RoomPlaylistRepository
import com.example.musicflow.feature.playlist.PlaylistBackupControls
import com.example.musicflow.ui.theme.MusicFlowTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Real Android DocumentsUI JSON import, isolated file and playlist cleanup. */
@RunWith(AndroidJUnit4::class)
class PlaylistBackupRealPickerUiTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    private fun createQaBackup(name: String, playlistName: String): Uri {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
            if (Build.VERSION.SDK_INT >= 29) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        val uri = checkNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
        try {
            resolver.openOutputStream(uri)?.use {
                it.write(PlaylistLocalBackup.encode(listOf(
                    PlaylistBackupEntry(playlistName, emptyList())
                )))
            } ?: error("Cannot open QA backup output")
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

    private fun selectBackupDocument(filename: String): Boolean {
        val deadline = SystemClock.uptimeMillis() + 40_000L
        var openedDownloads = false
        var tappedFile = false
        while (SystemClock.uptimeMillis() < deadline) {
            // Android 13+ DocumentsUI sometimes needs more than 450 ms to
            // deliver ActivityResult. Do not repeatedly tap after selection.
            if (tappedFile && device.currentPackageName == context.packageName) return true
            // Android 13 DocumentsUI uses multi-select mode for some providers:
            // tapping the JSON highlights it and reveals a bottom SELECT
            // confirmation. Without that click the picker never returns.
            if (tappedFile) {
                val select = device.findObject(By.text("SELECT"))
                    ?: device.findObject(By.text("Select"))
                    ?: device.findObject(By.desc("Select"))
                if (select != null && !select.visibleBounds.isEmpty) {
                    select.click()
                    SystemClock.sleep(250)
                }
            }
            val file = device.findObject(By.text(filename))
                ?: device.findObject(By.desc(filename))
                ?: device.findObject(By.textContains(filename))
            if (!tappedFile && file != null) {
                val rect = file.visibleBounds
                if (!rect.isEmpty) {
                    device.click(rect.centerX(), rect.centerY())
                    tappedFile = true
                    val returnDeadline = SystemClock.uptimeMillis() + 6_000L
                    while (SystemClock.uptimeMillis() < returnDeadline) {
                        if (device.currentPackageName == context.packageName) return true
                        val select = device.findObject(By.text("SELECT"))
                            ?: device.findObject(By.text("Select"))
                        if (select != null && !select.visibleBounds.isEmpty) {
                            select.click()
                        }
                        SystemClock.sleep(150)
                    }
                }
            }
            if (!openedDownloads) {
                val downloads = device.findObject(By.text("Downloads"))
                    ?: device.findObject(By.text("Download"))
                if (downloads != null) {
                    val rect = downloads.visibleBounds
                    if (!rect.isEmpty) {
                        device.click(rect.centerX(), rect.centerY())
                        openedDownloads = true
                        SystemClock.sleep(550)
                        continue
                    }
                }
                val drawer = device.findObject(By.desc("Show roots"))
                    ?: device.findObject(By.desc("Show navigation drawer"))
                if (drawer != null) {
                    drawer.click()
                    SystemClock.sleep(400)
                    continue
                }
            }
            SystemClock.sleep(350)
        }
        val visible = device.findObjects(By.clazz("android.widget.TextView"))
            .take(25).mapNotNull { it.text }.joinToString(" | ")
        Log.e("MusicFlowJsonPickerQA",
            "PICKER_TIMEOUT tapped=$tappedFile root=$openedDownloads " +
                "package=${device.currentPackageName} visible=$visible")
        return false
    }

    @Test fun actualJsonDocumentPickerImportsOnlyAfterConfirmation() {
        val name = "musicflow_backup_import_${System.nanoTime()}.json"
        val playlistName = "MusicFlow Backup QA ${System.nanoTime()}"
        val filenameUri = createQaBackup(name, playlistName)
        val database = RecentHistoryDatabase.getInstance(context)
        val repository = RoomPlaylistRepository(database)
        try {
            compose.setContent { MusicFlowTheme { PlaylistBackupControls() } }
            compose.onNodeWithTag("playlist-backup-toggle").performClick()
            compose.onNodeWithTag("playlist-backup-import").performClick()
            assertTrue("DocumentsUI cannot select QA JSON", selectBackupDocument(name))
            compose.waitUntil(25_000) {
                compose.onAllNodesWithTag("playlist-backup-confirm-import")
                    .fetchSemanticsNodes().isNotEmpty()
            }
            assertTrue(runBlocking {
                repository.backupEntries().none { it.name == "$playlistName (nhập)" }
            })
            compose.onNodeWithTag("playlist-backup-confirm-import").performClick()
            compose.waitUntil(25_000) {
                runBlocking {
                    repository.backupEntries().any { it.name == "$playlistName (nhập)" }
                }
            }
            compose.onNodeWithTag("playlist-backup-result").assertExists()
        } finally {
            val imported = runBlocking {
                database.playlistDao().getAllPlaylists().filter {
                    it.name == "$playlistName (nhập)"
                }
            }
            imported.forEach { row ->
                runBlocking { repository.deletePlaylist(row.id) }
            }
            context.contentResolver.delete(filenameUri, null, null)
        }
    }
}
