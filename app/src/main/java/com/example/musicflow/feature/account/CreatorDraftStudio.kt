package com.example.musicflow.feature.account

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

private fun draftStatus(value: String): String = when (value) {
    "draft" -> "Bản nháp"
    "submitted" -> "Đang xét duyệt"
    "needs_changes" -> "Cần bổ sung"
    "metadata_approved" -> "Đã duyệt hồ sơ (chưa phát hành)"
    else -> "Chưa rõ"
}

@Composable
internal fun CreatorDraftStudio(rpc: suspend (String, JSONObject) -> String) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    var drafts by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var hasLoaded by remember { mutableStateOf(false) }
    var showEditor by remember { mutableStateOf(false) }
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

    fun reload() {
        if (busy) return
        scope.launch {
            busy = true; error = null; info = null
            try {
                val data = JSONArray(rpc("musicflow_creator_my_drafts", JSONObject()))
                drafts = (0 until data.length()).mapNotNull(data::optJSONObject)
                hasLoaded = true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                error = failure.message ?: "Chưa tải được nháp."
            } finally { busy = false }
        }
    }

    fun newDraft() {
        editingId = null; title = ""; artist = ""; album = ""; holder = ""
        license = "UNVERIFIED"; evidence = ""; streaming = false
        credits = emptyList(); creditName = ""; creditRole = "performer"
        error = null; info = null; showEditor = true
    }

    fun editDraft(row: JSONObject) {
        editingId = row.optString("id")
        title = row.optString("title")
        artist = row.optString("artist")
        album = row.optString("album")
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
                info = "Đã lưu nháp riêng trên Supabase; chưa xuất bản."
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                error = failure.message ?: "Không lưu được bản nháp."
            } finally { busy = false }
        }
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
                "Duyệt hồ sơ không đồng nghĩa được phát hành; chưa có tính năng tải bản thu lên.",
            style = MaterialTheme.typography.bodySmall
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { reload() }, enabled = !busy) { Text("Tải nháp của tôi") }
            Button(onClick = { newDraft() }, enabled = !busy,
                modifier = Modifier.testTag("creator-new-draft")
            ) { Text("Tạo nháp") }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        info?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        if (hasLoaded && drafts.isEmpty()) {
            Text("Chưa có bản nháp nào.", style = MaterialTheme.typography.bodySmall)
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
            OutlinedTextField(
                value = title, onValueChange = { title = it.take(240) },
                label = { Text("Tên bài / tác phẩm") },
                modifier = Modifier.fillMaxWidth().testTag("draft-title")
            )
            OutlinedTextField(
                value = artist, onValueChange = { artist = it.take(180) },
                label = { Text("Nghệ sĩ chính") }, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = album, onValueChange = { album = it.take(180) },
                label = { Text("Album (không bắt buộc)") }, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = holder, onValueChange = { holder = it.take(180) },
                label = { Text("Người / tổ chức nắm quyền bản ghi âm") },
                modifier = Modifier.fillMaxWidth()
            )
            Text("Giấy phép dự kiến", style = MaterialTheme.typography.bodyMedium)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CreatorDraftRules.licenseCodes.sorted().forEach { code ->
                    FilterChip(selected = license == code, onClick = { license = code },
                        label = { Text(code) })
                }
            }
            OutlinedTextField(
                value = evidence, onValueChange = { evidence = it.take(1000) },
                label = { Text("Mã hợp đồng / mô tả bằng chứng quyền phát") },
                supportingText = { Text("Không tải tệp bằng chứng lên ở giai đoạn này.") },
                modifier = Modifier.fillMaxWidth()
            )
            Row {
                Checkbox(checked = streaming, onCheckedChange = { streaming = it },
                    modifier = Modifier.testTag("draft-streaming-claim"))
                Text("Tôi xác nhận có quyền cho phép MusicFlow phát online. " +
                    "Quản trị viên vẫn phải kiểm duyệt.")
            }
            Text("Người tham gia • tối đa 12", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = creditName, onValueChange = { creditName = it.take(100) },
                label = { Text("Tên người tham gia") },
                modifier = Modifier.fillMaxWidth().testTag("draft-credit-name")
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                contributions.forEach { (key, label) ->
                    FilterChip(selected = creditRole == key, onClick = { creditRole = key },
                        label = { Text(label) })
                }
            }
            OutlinedButton(
                onClick = {
                    credits = credits + CreatorCredit(creditName.trim(), creditRole)
                    creditName = ""
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
                    ) && matchesSaved,
                    modifier = Modifier.fillMaxWidth().testTag("draft-submit")
                ) { Text("Gửi bản nháp đã lưu để xét duyệt") }
            }
            TextButton(onClick = { showEditor = false }, enabled = !busy) {
                Text("Đóng trình sửa nháp")
            }
        }
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
internal fun CreatorDraftReviewPanel(rpc: suspend (String, JSONObject) -> String) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var reason by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf<Pair<JSONObject,Boolean>?>(null) }

    fun refresh() {
        if (busy) return
        scope.launch {
            busy = true; error = null; info = null
            try {
                val data = JSONArray(rpc("musicflow_pending_drafts", JSONObject()))
                pending = (0 until data.length()).mapNotNull(data::optJSONObject)
                loaded = true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                error = failure.message ?: "Không tải được danh sách chờ duyệt."
            } finally { busy = false }
        }
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
        if (loaded && pending.isEmpty()) Text("Không có bản nháp chờ duyệt.")
        pending.forEach { row ->
            Text("${row.optString("title")} — ${row.optString("artist")}",
                style = MaterialTheme.typography.titleSmall)
            Text("Người gửi: ${row.optString("creator_email")}")
            Text("Chủ master: ${row.optString("rights_holder")} • ${row.optString("license_code")}")
            Text("Chứng cứ khai báo: ${row.optString("rights_reference")}")
            val credits = row.optJSONArray("credits") ?: JSONArray()
            (0 until credits.length()).forEach { index ->
                credits.optJSONObject(index)?.let { credit ->
                    Text("• ${credit.optString("name")} — ${credit.optString("contribution")}")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { confirm = row to true },
                    enabled = !busy && reason.trim().length in 5..300
                ) { Text("Duyệt hồ sơ") }
                OutlinedButton(
                    onClick = { confirm = row to false },
                    enabled = !busy && reason.trim().length in 5..300
                ) { Text("Yêu cầu bổ sung") }
            }
        }
        if (pending.isNotEmpty()) {
            OutlinedTextField(
                value = reason, onValueChange = { reason = it.take(300) },
                label = { Text("Lý do xét duyệt / yêu cầu bổ sung") },
                modifier = Modifier.fillMaxWidth().testTag("draft-review-reason")
            )
        }
    }
    confirm?.let { (item, approve) ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(if (approve) "Xác nhận duyệt metadata?" else "Yêu cầu chỉnh sửa?") },
            text = { Text("${item.optString("title")}\nLý do: $reason\n" +
                "Không thay đổi catalog nhạc đã phát hành.") },
            confirmButton = {
                TextButton(onClick = {
                    confirm = null
                    scope.launch {
                        busy = true; error = null; info = null
                        try {
                            rpc(
                                "musicflow_review_draft",
                                JSONObject().put("p_id", item.optString("id"))
                                    .put("p_approve", approve).put("p_reason", reason.trim())
                            )
                            pending = pending.filterNot {
                                it.optString("id") == item.optString("id")
                            }
                            info = "Đã ghi quyết định xét duyệt trên Supabase."
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (failure: Exception) {
                            error = failure.message ?: "Không thể duyệt bản nháp."
                        } finally { busy = false }
                    }
                }) { Text("Xác nhận") }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Hủy") } }
        )
    }
}

