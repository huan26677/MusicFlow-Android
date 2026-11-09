package com.example.musicflow.feature.community

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.musicflow.core.community.CommunityRules
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/** A staff-only window. The real permission check always runs in Postgres. */
@Composable
fun CommunityModerationPanel(
    rpc: suspend (String, JSONObject) -> String
) {
    val scope = rememberCoroutineScope()
    var rows by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var reason by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    fun load() {
        if (busy) return
        scope.launch {
            busy = true
            error = null
            try {
                val response = JSONArray(rpc("musicflow_community_reports",JSONObject()))
                rows = (0 until response.length()).mapNotNull(response::optJSONObject)
                loaded = true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                error = "Không thể tải báo cáo. Hãy kiểm tra kết nối " +
                    "hoặc quyền quản trị rồi thử lại."
            } finally {
                busy = false
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalDivider()
        Text("Báo cáo bình luận",style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() })
        Text("Bạn chỉ có thể xử lý báo cáo khi đã được cấp quyền kiểm duyệt.",
            style = MaterialTheme.typography.bodySmall)
        OutlinedButton(
            onClick = { load() },enabled = !busy,
            modifier = Modifier.testTag("community-moderation-load")
        ) { Text(if (busy) "Đang tải…" else "Tải báo cáo bình luận") }
        error?.let { Text(it,color = MaterialTheme.colorScheme.error) }
        notice?.let { Text(it,color = MaterialTheme.colorScheme.primary) }
        if (loaded && rows.isEmpty()) Text("Không có bình luận cần xử lý.")
        rows.forEach { item ->
            Text("${item.optString("track_title")} · " +
                CommunityUiPresentation.dateLabel(item.optString("created_at")))
            Text(item.optString("body"),style = MaterialTheme.typography.bodyMedium)
            Text("Báo cáo: ${item.optString("reason")}",
                style = MaterialTheme.typography.bodySmall)
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val compact = maxWidth < 350.dp || LocalDensity.current.fontScale >= 1.5f
                val actions: @Composable () -> Unit = {
                    Button(
                        enabled = !busy,
                        onClick = { pending = item.optString("comment_id") to true }
                    ) { Text("Ẩn bình luận") }
                    OutlinedButton(
                        enabled = !busy,
                        onClick = { pending = item.optString("comment_id") to false }
                    ) { Text("Bỏ qua báo cáo") }
                }
                if (compact) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        actions()
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        actions()
                    }
                }
            }
            HorizontalDivider()
        }
    }
    pending?.let { (commentId, hide) ->
        AlertDialog(
            onDismissRequest = { pending = null; reason = "" },
            title = { Text(if (hide) "Xác nhận ẩn bình luận?" else "Bỏ qua báo cáo?") },
            text = {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it.take(CommunityRules.REASON_LIMIT) },
                    label = { Text("Lý do (5–300 ký tự)") },
                    modifier = Modifier.fillMaxWidth().testTag("community-moderation-reason")
                )
            },
            confirmButton = {
                TextButton(
                    enabled = reason.trim().length in 5..300 && !busy,
                    onClick = {
                        val why = reason.trim()
                        pending = null
                        reason = ""
                        scope.launch {
                            busy = true
                            error = null
                            try {
                                check(rpc(
                                    "musicflow_community_moderate",
                                    JSONObject().put("p_comment",commentId)
                                        .put("p_hide",hide).put("p_reason",why)
                                ).trim() == "true") { "Máy chủ không xác nhận quyết định." }
                                rows = rows.filterNot {
                                    it.optString("comment_id") == commentId
                                }
                                notice = if (hide) "Đã ẩn bình luận và xử lý báo cáo."
                                    else "Đã bỏ qua báo cáo. Bình luận giữ nguyên trạng thái."
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (failure: Exception) {
                                error = "Không thể xử lý báo cáo. " +
                                    "Kiểm tra quyền quản trị hoặc thử lại sau."
                            } finally {
                                busy = false
                            }
                        }
                    },
                    modifier = Modifier.testTag("community-moderation-confirm")
                ) { Text("Xác nhận") }
            },
            dismissButton = {
                TextButton(onClick = { pending = null; reason = "" }) { Text("Hủy") }
            }
        )
    }
}
