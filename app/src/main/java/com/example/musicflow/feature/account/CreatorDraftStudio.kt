package com.example.musicflow.feature.account

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.musicflow.core.account.CreatorCredit
import com.example.musicflow.core.account.CreatorDraftRules
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private val contributions = listOf(
    "performer" to "Biểu diễn",
    "composer" to "Sáng tác",
    "lyricist" to "Viết lời",
    "producer" to "Sản xuất",
    "master_owner" to "Chủ master"
)

private data class DraftFormSnapshot(
    val title: String,
    val artist: String,
    val album: String,
    val rightsHolder: String,
    val license: String,
    val rightsReference: String,
    val streamingClaimed: Boolean,
    val nextCreditName: String,
    val nextCreditRole: String,
    val credits: List<CreatorCredit>
)

private data class DraftReviewDecision(
    val item: JSONObject,
    val approved: Boolean,
    val reason: String
)

private fun draftStatus(value: String): String = when (value) {
    "draft" -> "Bản nháp"
    "submitted" -> "Đang xét duyệt"
    "needs_changes" -> "Cần bổ sung"
    "metadata_approved" -> "Đã duyệt hồ sơ (chưa phát hành)"
    else -> "Chưa rõ"
}

@Composable
internal fun CreatorDraftStudio(
    rpc: suspend (String, JSONObject) -> String,
    upload: suspend (String, String, Uri) -> Unit = { _, _, _ -> error("QA only") },
    download: suspend (String, Uri) -> Unit = { _, _ -> error("QA only") }
) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    var drafts by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var hasLoaded by remember { mutableStateOf(false) }
    var showEditor by remember { mutableStateOf(false) }
    var showOptionalAlbum by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var album by remember { mutableStateOf("") }
    var holder by remember { mutableStateOf("") }
    var license by remember { mutableStateOf("UNVERIFIED") }
    var evidence by remember { mutableStateOf("") }
    var streaming by remember { mutableStateOf(false) }
    var creditName by remember { mutableStateOf("") }
    var creditRole by remember { mutableStateOf("performer") }
    var credits by remember { mutableStateOf<List<CreatorCredit>>(emptyList()) }
    var confirmSubmit by remember { mutableStateOf(false) }
    var attachmentsReady by remember { mutableStateOf(false) }
    var exitEditorConfirm by remember { mutableStateOf(false) }
    var savedSnapshot by remember { mutableStateOf<DraftFormSnapshot?>(null) }
    var editorDirty by remember { mutableStateOf(false) }

    fun snapshot() = DraftFormSnapshot(
        title, artist, album, holder, license, evidence, streaming,
        creditName, creditRole, credits
    )

    fun requestCloseEditor() {
        if (busy) return
        if (editorDirty || snapshot() != savedSnapshot) exitEditorConfirm = true
        else showEditor = false
    }

    fun reload() {
        if (busy) return
        scope.launch {
            busy = true; error = null; info = null
            hasLoaded = false; drafts = emptyList()
            try {
                val data = JSONArray(rpc("musicflow_creator_my_drafts", JSONObject()))
                drafts = (0 until data.length()).mapNotNull(data::optJSONObject)
                hasLoaded = true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                error = "Chưa tải được bản nháp. Kiểm tra Internet và nhấn Tải nháp của tôi để thử lại."
            } finally { busy = false }
        }
    }

    fun newDraft() {
        attachmentsReady = false
        editingId = null; title = ""; artist = ""; album = ""; holder = ""
        license = "UNVERIFIED"; evidence = ""; streaming = false
        credits = emptyList(); creditName = ""; creditRole = "performer"
        error = null; info = null; showEditor = true
        showOptionalAlbum = false
        savedSnapshot = snapshot()
        editorDirty = false
        exitEditorConfirm = false
    }

    fun editDraft(row: JSONObject) {
        attachmentsReady = false
        editingId = row.optString("id")
        title = row.optString("title")
        artist = row.optString("artist")
        album = row.optString("album")
        showOptionalAlbum = album.isNotBlank()
        holder = row.optString("rights_holder")
        license = row.optString("license_code")
        evidence = row.optString("rights_reference")
        streaming = row.optBoolean("streaming_claimed")
        val array = row.optJSONArray("credits") ?: JSONArray()
        credits = (0 until array.length()).mapNotNull { i ->
            array.optJSONObject(i)?.let {
                CreatorCredit(it.optString("name"), it.optString("contribution"))
            }
        }
        error = null; info = null; showEditor = true
        savedSnapshot = snapshot()
        editorDirty = false
        exitEditorConfirm = false
    }

    fun saveDraft() {
        if (busy) return
        if (!CreatorDraftRules.canSave(title, artist, holder, license, evidence, credits) ||
            album.length > 180) {
            error = "Kiểm tra tiêu đề, chủ bản quyền và ít nhất một người tham gia."
            return
        }
        val contributorData = JSONArray()
        credits.forEach { credit ->
            contributorData.put(
                JSONObject().put("name", credit.name.trim())
                    .put("contribution", credit.contribution)
            )
        }
        val data = JSONObject()
            .put("p_id", editingId ?: JSONObject.NULL)
            .put("p_title", title.trim())
            .put("p_artist", artist.trim())
            .put("p_album", album.trim())
            .put("p_rights_holder", holder.trim())
            .put("p_license_code", license)
            .put("p_streaming_claimed", streaming)
            .put("p_rights_reference", evidence.trim())
            .put("p_credits", contributorData)
        scope.launch {
            busy = true; error = null; info = null
            try {
                val id = rpc("musicflow_creator_draft_save", data).trim().trim('"')
                editingId = id
                val fetched = JSONArray(rpc("musicflow_creator_my_drafts", JSONObject()))
                drafts = (0 until fetched.length()).mapNotNull(fetched::optJSONObject)
                hasLoaded = true
                savedSnapshot = snapshot()
                editorDirty = false
                info = "Đã lưu nháp riêng trên Supabase; chưa xuất bản."
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                error = failure.message ?: "Không lưu được bản nháp."
            } finally { busy = false }
        }
    }

    BackHandler(enabled = showEditor && !busy && !confirmSubmit && !exitEditorConfirm) {
        requestCloseEditor()
    }

    fun submitDraft() {
        val id = editingId ?: return
        if (busy) return
        scope.launch {
            busy = true; error = null; info = null
            try {
                rpc("musicflow_creator_submit_draft", JSONObject().put("p_id", id))
                val fetched = JSONArray(rpc("musicflow_creator_my_drafts", JSONObject()))
                drafts = (0 until fetched.length()).mapNotNull(fetched::optJSONObject)
                hasLoaded = true
                showEditor = false
                info = "Đã gửi hồ sơ xét duyệt. Chưa được phát nhạc online."
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                error = failure.message ?: "Chưa thể gửi xét duyệt."
            } finally { busy = false }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalDivider()
        Text("Creator Studio • Bản nháp tác phẩm", style = MaterialTheme.typography.titleMedium)
        Text(
            "Lưu thông tin tác phẩm và tín chỉ trên Supabase. " +
                "Sau khi lưu nháp, tải bản thu thử và bằng chứng bản quyền riêng tư. " +
                "Duyệt hồ sơ không đồng nghĩa được phát hành online.",
            style = MaterialTheme.typography.bodySmall
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { reload() }, enabled = !busy,
                modifier = Modifier.testTag("creator-drafts-load")
            ) { Text("Tải nháp của tôi") }
            Button(onClick = { newDraft() }, enabled = !busy,
                modifier = Modifier.testTag("creator-new-draft")
            ) { Text("Tạo nháp") }
        }
        if (!hasLoaded && !busy && error == null) {
            Text("Nhấn Tải nháp của tôi để xem tác phẩm đã lưu, hoặc Tạo nháp để bắt đầu.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.testTag("creator-drafts-start-hint"))
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        info?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        if (hasLoaded && drafts.isEmpty()) {
            Text("Chưa có tác phẩm nào. Nhấn Tạo nháp để bắt đầu hồ sơ tác phẩm đầu tiên.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.testTag("creator-drafts-empty"))
        }
        drafts.forEach { item ->
            Text(
                "${item.optString("title")} — ${draftStatus(item.optString("status"))}",
                style = MaterialTheme.typography.bodyMedium
            )
            if (item.optString("review_note").isNotBlank() &&
                item.optString("review_note") != "null") {
                Text("Góp ý: ${item.optString("review_note")}",
                    style = MaterialTheme.typography.bodySmall)
            }
            if (item.optString("status") in setOf("draft", "needs_changes")) {
                OutlinedButton(onClick = { editDraft(item) }, enabled = !busy) {
                    Text("Sửa nháp")
                }
            }
        }
        if (showEditor) {
            HorizontalDivider()
            Text(if (editingId == null) "Nháp mới" else "Chỉnh sửa nháp",
                style = MaterialTheme.typography.titleMedium)
            Text("1. Thông tin cơ bản", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = title, onValueChange = { title = it.take(240); editorDirty = true },
                label = { Text("Tên bài / tác phẩm") },
                modifier = Modifier.fillMaxWidth().testTag("draft-title")
            )
            OutlinedTextField(
                value = artist, onValueChange = { artist = it.take(180); editorDirty = true },
                label = { Text("Nghệ sĩ chính") }, modifier = Modifier.fillMaxWidth()
            )
            TextButton(
                onClick = { showOptionalAlbum = !showOptionalAlbum },
                modifier = Modifier.testTag("draft-album-toggle")
            ) { Text(if (showOptionalAlbum) "Ẩn thông tin album"
                else "Thêm album (không bắt buộc)") }
            if (showOptionalAlbum) {
                OutlinedTextField(
                    value = album, onValueChange = { album = it.take(180); editorDirty = true },
                    label = { Text("Tên album") },
                    modifier = Modifier.fillMaxWidth().testTag("draft-album")
                )
            }
            OutlinedTextField(
                value = holder, onValueChange = { holder = it.take(180); editorDirty = true },
                label = { Text("Người / tổ chức nắm quyền bản ghi âm") },
                modifier = Modifier.fillMaxWidth()
            )
            Text("2. Chủ sở hữu và giấy phép", style = MaterialTheme.typography.titleSmall)
            Text("Giấy phép dự kiến", style = MaterialTheme.typography.bodyMedium)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CreatorDraftRules.licenseCodes.sorted().forEach { code ->
                    FilterChip(selected = license == code,
                        onClick = { license = code; editorDirty = true },
                        label = { Text(code) })
                }
            }
            OutlinedTextField(
                value = evidence, onValueChange = { evidence = it.take(1000); editorDirty = true },
                label = { Text("Mã hợp đồng / mô tả bằng chứng quyền phát") },
                supportingText = { Text("Không tải tệp bằng chứng lên ở giai đoạn này.") },
                modifier = Modifier.fillMaxWidth()
            )
            Row {
                Checkbox(checked = streaming, onCheckedChange = { streaming = it; editorDirty = true },
                    modifier = Modifier.testTag("draft-streaming-claim"))
                Text("Tôi xác nhận có quyền cho phép MusicFlow phát online. " +
                    "Quản trị viên vẫn phải kiểm duyệt.")
            }
            Text("3. Người tham gia • tối đa 12", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = creditName, onValueChange = { creditName = it.take(100); editorDirty = true },
                label = { Text("Tên người tham gia") },
                modifier = Modifier.fillMaxWidth().testTag("draft-credit-name")
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                contributions.forEach { (key, label) ->
                    FilterChip(selected = creditRole == key,
                        onClick = { creditRole = key; editorDirty = true },
                        label = { Text(label) })
                }
            }
            OutlinedButton(
                onClick = {
                    credits = credits + CreatorCredit(creditName.trim(), creditRole)
                    creditName = ""
                    editorDirty = true
                },
                enabled = creditName.trim().length in 2..100 && credits.size < 12,
                modifier = Modifier.testTag("draft-add-credit")
            ) { Text("Thêm người tham gia") }
            credits.forEachIndexed { index, credit ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${credit.name} • ${credit.contribution}",
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        credits = credits.filterIndexed { i, _ -> i != index }
                        editorDirty = true
                    }) { Text("Xóa") }
                }
            }
            val valid = CreatorDraftRules.canSave(
                title, artist, holder, license, evidence, credits
            ) && album.length <= 180
            Button(
                onClick = { saveDraft() }, enabled = !busy && valid,
                modifier = Modifier.fillMaxWidth().testTag("draft-save")
            ) { Text("Lưu nháp lên Supabase") }
            if (editingId != null) {
                CreatorPrivateAttachmentPanel(
                    draftId = editingId!!,
                    editable = true,
                    rpc = rpc,
                    upload = upload,
                    download = download,
                    onReadyChange = { attachmentsReady = it }
                )
                val saved = drafts.firstOrNull { it.optString("id") == editingId }
                val savedCredits = saved?.optJSONArray("credits")
                val matchesSaved = saved != null &&
                    saved.optString("status") == "draft" &&
                    saved.optString("title") == title.trim() &&
                    saved.optString("artist") == artist.trim() &&
                    saved.optString("album") == album.trim() &&
                    saved.optString("rights_holder") == holder.trim() &&
                    saved.optString("rights_reference") == evidence.trim() &&
                    saved.optString("license_code") == license &&
                    saved.optBoolean("streaming_claimed") == streaming &&
                    savedCredits?.length() == credits.size &&
                    credits.indices.all { index ->
                        savedCredits?.optJSONObject(index)?.let { credit ->
                            credit.optString("name") == credits[index].name.trim() &&
                                credit.optString("contribution") == credits[index].contribution
                        } == true
                    }
                OutlinedButton(
                    onClick = { confirmSubmit = true },
                    enabled = !busy && CreatorDraftRules.canSubmit(
                        title, artist, holder, license, evidence, streaming, credits
                    ) && matchesSaved && attachmentsReady,
                    modifier = Modifier.fillMaxWidth().testTag("draft-submit")
                ) { Text("Gửi bản nháp đã lưu để xét duyệt") }
            }
            TextButton(onClick = { requestCloseEditor() }, enabled = !busy,
                modifier = Modifier.testTag("creator-editor-close")) {
                Text("Đóng trình sửa nháp")
            }
        }
    }
    if (exitEditorConfirm) {
        AlertDialog(
            onDismissRequest = { exitEditorConfirm = false },
            title = { Text("Đóng khi chưa lưu?") },
            text = { Text("Những thay đổi chưa lưu vào Supabase sẽ bị bỏ. Bạn có thể tiếp tục sửa hoặc đóng.") },
            confirmButton = {
                TextButton(onClick = { exitEditorConfirm = false; showEditor = false },
                    modifier = Modifier.testTag("creator-editor-discard")) {
                    Text("Bỏ thay đổi")
                }
            },
            dismissButton = {
                TextButton(onClick = { exitEditorConfirm = false },
                    modifier = Modifier.testTag("creator-editor-keep")) {
                    Text("Tiếp tục sửa")
                }
            }
        )
    }
    if (confirmSubmit) {
        AlertDialog(
            onDismissRequest = { confirmSubmit = false },
            title = { Text("Gửi xét duyệt hồ sơ tác phẩm?") },
            text = { Text("Nháp sẽ khóa chỉnh sửa trong lúc duyệt. " +
                "Duyệt metadata không cấp quyền phát online.") },
            confirmButton = {
                TextButton(onClick = { confirmSubmit = false; submitDraft() }) {
                    Text("Gửi xét duyệt")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmSubmit = false }) { Text("Hủy") }
            }
        )
    }
}

@Composable
internal fun CreatorDraftReviewPanel(
    rpc: suspend (String, JSONObject) -> String,
    download: suspend (String, Uri) -> Unit = { _, _ -> error("QA only") }
) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var reason by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf<DraftReviewDecision?>(null) }
    var expandedReviewId by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        if (busy) return
        pending = emptyList()
        loaded = false
        expandedReviewId = null
        scope.launch {
            busy = true; error = null; info = null
            try {
                val data = JSONArray(rpc("musicflow_pending_drafts", JSONObject()))
                pending = (0 until data.length()).mapNotNull(data::optJSONObject)
                loaded = true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                error = "Chưa tải được danh sách chờ duyệt. Hãy kiểm tra kết nối và thử lại."
            } finally { busy = false }
        }
    }

    BackHandler(enabled = expandedReviewId != null && !busy && confirm == null) {
        expandedReviewId = null
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalDivider()
        Text("Duyệt bản nháp tác phẩm", style = MaterialTheme.typography.titleMedium)
        Text("Duyệt metadata/tín chỉ, không phát hành online hay cấp phép âm thanh.",
            style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = { refresh() }, enabled = !busy,
            modifier = Modifier.testTag("draft-review-load")
        ) { Text("Tải bản nháp chờ duyệt") }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        info?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        if (loaded && pending.isEmpty()) Text("Hiện không có tác phẩm đang chờ duyệt.",
            modifier = Modifier.testTag("draft-review-empty"))
        if (pending.isNotEmpty()) {
            OutlinedTextField(
                value = reason, onValueChange = { reason = it.take(300) },
                label = { Text("Lý do xét duyệt / yêu cầu bổ sung (5–300 ký tự)") },
                modifier = Modifier.fillMaxWidth().testTag("draft-review-reason")
            )
        }
        pending.forEach { row ->
            Text("${row.optString("title")} — ${row.optString("artist")}",
                style = MaterialTheme.typography.titleSmall)
            OutlinedButton(
                onClick = {
                    expandedReviewId = if (expandedReviewId == row.optString("id")) null
                        else row.optString("id")
                },
                modifier = Modifier.fillMaxWidth().testTag("draft-review-open-item")
            ) { Text(if (expandedReviewId == row.optString("id")) "Thu gọn hồ sơ"
                else "Xem hồ sơ và chứng cứ") }
            if (expandedReviewId == row.optString("id")) {
            Text("Người gửi: ${row.optString("creator_email")}")
            Text("Chủ master: ${row.optString("rights_holder")} • ${row.optString("license_code")}")
            Text("Chứng cứ khai báo: ${row.optString("rights_reference")}")
            val credits = row.optJSONArray("credits") ?: JSONArray()
            (0 until credits.length()).forEach { index ->
                credits.optJSONObject(index)?.let { credit ->
                    Text("• ${credit.optString("name")} — ${credit.optString("contribution")}")
                }
            }
            CreatorPrivateAttachmentPanel(
                draftId = row.optString("id"),
                editable = false,
                rpc = rpc,
                upload = { _, _, _ -> error("Reviewer cannot upload") },
                download = download
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { confirm = DraftReviewDecision(row, true, reason.trim()) },
                    enabled = !busy && reason.trim().length in 5..300,
                    modifier = Modifier.testTag("draft-review-approve")
                ) { Text("Duyệt hồ sơ") }
                OutlinedButton(
                    onClick = { confirm = DraftReviewDecision(row, false, reason.trim()) },
                    enabled = !busy && reason.trim().length in 5..300,
                    modifier = Modifier.testTag("draft-review-needs-changes")
                ) { Text("Yêu cầu bổ sung") }
            }
            }
        }
    }
    confirm?.let { decision ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(if (decision.approved) "Xác nhận duyệt metadata?" else "Yêu cầu chỉnh sửa?") },
            text = { Text("${decision.item.optString("title")}\nLý do: ${decision.reason}\n" +
                "Không thay đổi catalog nhạc đã phát hành.") },
            confirmButton = {
                TextButton(onClick = {
                    confirm = null
                    scope.launch {
                        busy = true; error = null; info = null
                        try {
                            val accepted = rpc(
                                "musicflow_review_draft",
                                JSONObject().put("p_id", decision.item.optString("id"))
                                    .put("p_approve", decision.approved)
                                    .put("p_reason", decision.reason)
                            )
                            check(accepted.trim() == "true") {
                                "Máy chủ chưa xác nhận xét duyệt."
                            }
                            pending = pending.filterNot {
                                it.optString("id") == decision.item.optString("id")
                            }
                            expandedReviewId = null
                            reason = ""
                            info = "Đã ghi quyết định xét duyệt trên Supabase."
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (failure: Exception) {
                            error = failure.message ?: "Không thể duyệt bản nháp."
                        } finally { busy = false }
                    }
                }, modifier = Modifier.testTag("draft-review-confirm")) { Text("Xác nhận") }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Hủy") } }
        )
    }
}

