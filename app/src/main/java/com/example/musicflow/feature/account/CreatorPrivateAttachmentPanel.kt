package com.example.musicflow.feature.account

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/** Review/download is authorized by Storage RLS, never by a public stream URL. */
@Composable
internal fun CreatorPrivateAttachmentPanel(
    draftId: String,
    editable: Boolean,
    rpc: suspend (String, JSONObject) -> String,
    upload: suspend (String, String, Uri) -> Unit,
    download: suspend (String, Uri) -> Unit,
    onReadyChange: (Boolean) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var busy by remember(draftId) { mutableStateOf(false) }
    var files by remember(draftId) { mutableStateOf<List<JSONObject>>(emptyList()) }
    var error by remember(draftId) { mutableStateOf<String?>(null) }
    var notice by remember(draftId) { mutableStateOf<String?>(null) }
    var pickedPath by remember { mutableStateOf<String?>(null) }

    suspend fun refresh() {
        val data = JSONArray(rpc(
            "musicflow_creator_upload_list", JSONObject().put("p_draft_id", draftId)
        ))
        files = (0 until data.length()).mapNotNull(data::optJSONObject)
        onReadyChange(listOf("audio", "evidence").all { kind ->
            files.any { it.optString("kind") == kind && it.optString("state") == "uploaded" }
        })
    }
    fun uploadFile(kind: String, uri: Uri?) {
        if (uri == null || busy) return
        scope.launch {
            busy = true; error = null; notice = null
            try {
                upload(draftId, kind, uri)
                refresh()
                notice = "Đã lưu tệp riêng tư trên đám mây."
            } catch (c: CancellationException) {
                throw c
            } catch (e: Exception) {
                error = AccountOperationFeedback.failure(
                    e, AccountOperationFeedback.Action.FILE_UPLOAD
                )
            } finally { busy = false }
        }
    }
    val chooseAudio = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        uploadFile("audio", it)
    }
    val chooseEvidence = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        uploadFile("evidence", it)
    }
    val saveFile = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { destination ->
        val path = pickedPath
        pickedPath = null
        if (destination != null && path != null && !busy) {
            scope.launch {
                busy = true; error = null; notice = null
                try {
                    download(path, destination)
                    notice = "Đã lưu tệp để xem. Không chia sẻ khi chưa được phép."
                } catch (c: CancellationException) {
                    throw c
                } catch (e: Exception) {
                    error = AccountOperationFeedback.failure(
                        e, AccountOperationFeedback.Action.FILE_DOWNLOAD
                    )
                } finally { busy = false }
            }
        }
    }
    LaunchedEffect(draftId) {
        try { refresh() } catch (c: CancellationException) { throw c }
        catch (_: Exception) { error = "Chưa tải được tệp, hãy thử làm mới." }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            if (editable) "Tệp đính kèm riêng tư" else "Tệp cần xét duyệt",
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            "Mỗi bản nháp có một bản thu và một chứng cứ PDF/JPG/PNG, " +
                "tối đa 6 MB/tệp. Không tự phát hành online.",
            style = MaterialTheme.typography.bodySmall
        )
        if (editable) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { chooseAudio.launch(arrayOf("audio/*")) },
                    enabled = !busy && files.none {
                        it.optString("kind") == "audio" && it.optString("state") == "uploaded"
                    },
                    modifier = Modifier.testTag("creator-audio-upload")
                ) { Text("Chọn bản thu") }
                OutlinedButton(
                    onClick = { chooseEvidence.launch(
                        arrayOf("application/pdf", "image/jpeg", "image/png")
                    ) },
                    enabled = !busy && files.none {
                        it.optString("kind") == "evidence" && it.optString("state") == "uploaded"
                    },
                    modifier = Modifier.testTag("creator-evidence-upload")
                ) { Text("Chọn chứng cứ") }
            }
        }
        files.forEach { file ->
            val kind = if (file.optString("kind") == "audio") "Bản thu" else "Chứng cứ"
            val state = if (file.optString("state") == "uploaded") {
                "Đã lưu (" + (file.optLong("size") / 1024) + " KB)"
            } else "Chờ tải lên"
            Text(kind + " — " + state)
            if (file.optString("state") == "uploaded") {
                OutlinedButton(onClick = {
                    pickedPath = file.optString("path")
                    val ext = when (file.optString("mime")) {
                        "application/pdf" -> ".pdf"
                        "image/jpeg" -> ".jpg"
                        "image/png" -> ".png"
                        "audio/mpeg" -> ".mp3"
                        "audio/mp4" -> ".m4a"
                        "audio/ogg" -> ".ogg"
                        "audio/flac" -> ".flac"
                        else -> ".wav"
                    }
                    saveFile.launch("musicflow-" + file.optString("kind") +
                        "-" + draftId + ext)
                }, enabled = !busy) { Text("Tải về để xem") }
            }
        }
        OutlinedButton(
            enabled = !busy,
            onClick = {
                scope.launch {
                    busy = true; error = null
                    try { refresh() } catch (c: CancellationException) { throw c }
                    catch (e: Exception) {
                        error = AccountOperationFeedback.failure(
                            e, AccountOperationFeedback.Action.FILE_LIST
                        )
                    }
                    finally { busy = false }
                }
            },
            modifier = Modifier.testTag("creator-attachment-refresh")
        ) { Text("Làm mới danh sách tệp") }
        notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

