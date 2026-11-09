package com.example.musicflow.feature.playlist

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.musicflow.core.history.local.RecentHistoryDatabase
import com.example.musicflow.core.playlist.PlaylistBackupEntry
import com.example.musicflow.core.playlist.PlaylistLocalBackup
import com.example.musicflow.core.playlist.RoomPlaylistRepository
import com.example.musicflow.ui.feedback.MusicFlowActionFeedback
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** System document picker: a user-controlled file, not automatic cloud sync. */
@Composable
internal fun PlaylistBackupControls() {
    val context = LocalContext.current.applicationContext
    val resolver = context.contentResolver
    val repository = remember(context) {
        RoomPlaylistRepository(RecentHistoryDatabase.getInstance(context))
    }
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<List<PlaylistBackupEntry>?>(null) }
    var expanded by rememberSaveable { mutableStateOf(false) }

    val createDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(PlaylistLocalBackup.MIME)
    ) { uri ->
        if (uri != null) scope.launch {
            busy = true
            try {
                val bytes = withContext(Dispatchers.IO) {
                    PlaylistLocalBackup.encode(repository.backupEntries())
                }
                withContext(Dispatchers.IO) {
                    checkNotNull(resolver.openOutputStream(uri, "wt")) {
                        "Không ghi được tệp sao lưu."
                    }.use { it.write(bytes) }
                }
                message = "Đã xuất playlist. Tệp chỉ chứa thông tin bài hát, không chứa nhạc hay mật khẩu."
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                message = MusicFlowActionFeedback.fileFailure(
                    error, MusicFlowActionFeedback.FileAction.EXPORT)
            } finally { busy = false }
        }
    }

    val openDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) scope.launch {
            busy = true
            try {
                val entries = withContext(Dispatchers.IO) {
                    val bytes = checkNotNull(resolver.openInputStream(uri)) {
                        "Không đọc được tệp được chọn."
                    }.use { input ->
                        val buffer = ByteArray(8192)
                        val output = java.io.ByteArrayOutputStream()
                        while (true) {
                            val n = input.read(buffer)
                            if (n < 0) break
                            require(output.size() + n <= PlaylistLocalBackup.MAX_FILE_BYTES) {
                                "Tệp lớn hơn giới hạn 2 MB."
                            }
                            output.write(buffer, 0, n)
                        }
                        output.toByteArray()
                    }
                    PlaylistLocalBackup.decode(bytes)
                }
                if (entries.isEmpty()) {
                    message = "Bản sao lưu không có playlist nào để nhập."
                } else {
                    pending = entries
                    message = null
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                message = MusicFlowActionFeedback.fileFailure(
                    error, MusicFlowActionFeedback.FileAction.IMPORT)
            } finally { busy = false }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedButton(onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .testTag("playlist-backup-toggle")) {
            Text(if (expanded) "Thu gọn sao lưu / khôi phục" else "Sao lưu / khôi phục playlist")
        }
        if (expanded) {
            Text("Xuất/nhập danh sách phát bằng tệp sao lưu (.json), không sao chép tệp nhạc. " +
                "Khi chuyển thiết bị, hãy liên kết lại những bài bị mất quyền đọc.",
                style = MaterialTheme.typography.bodySmall)
            OutlinedButton(
                onClick = { createDocument.launch("MusicFlow-Playlists-v1.json") },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .testTag("playlist-backup-export")
            ) { Text("Xuất sao lưu playlist (.json)") }
            OutlinedButton(
                // Some providers label a .json document as application/octet-stream
                // despite our CreateDocument MIME. Let DocumentsUI show it and
                // enforce the format/version/limits with our strict decoder.
                onClick = { openDocument.launch(arrayOf("*/*")) },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .testTag("playlist-backup-import")
            ) { Text("Nhập bản sao lưu playlist") }
        }
        if (busy) Text("Đang xử lý tệp sao lưu…",
            modifier = Modifier.testTag("playlist-backup-busy"))
        message?.let {
            Text(it, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.testTag("playlist-backup-result"))
        }
    }

    pending?.let { entries ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("Nhập ${entries.size} playlist?") },
            text = {
                Text("Thêm bản sao playlist, không xóa hoặc ghi đè danh sách hiện có. " +
                    "${entries.sumOf { it.tracks.size }} bài chỉ chứa liên kết tệp; " +
                    "bài chưa đọc được sẽ cần chọn lại tệp sau khi nhập.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pending = null
                        scope.launch {
                            busy = true
                            try {
                                val imported = withContext(Dispatchers.IO) {
                                    repository.importBackup(entries)
                                }
                                message = "Đã thêm $imported playlist từ bản sao lưu; " +
                                    "danh sách cũ không thay đổi."
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (error: Exception) {
                                message = MusicFlowActionFeedback.fileFailure(
                                    error, MusicFlowActionFeedback.FileAction.IMPORT)
                            } finally { busy = false }
                        }
                    }, modifier = Modifier.testTag("playlist-backup-confirm-import")
                ) { Text("Thêm bản sao") }
            },
            dismissButton = {
                TextButton(onClick = { pending = null },
                    modifier = Modifier.testTag("playlist-backup-cancel-import")) {
                    Text("Hủy")
                }
            }
        )
    }
}
