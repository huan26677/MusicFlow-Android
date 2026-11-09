package com.example.musicflow.feature.community

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.musicflow.core.community.CommunityRules
import com.example.musicflow.core.community.CommunitySnapshot
import com.example.musicflow.core.community.DemoCommunityStore
import com.example.musicflow.core.community.SupabaseCommunityRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Community is intentionally a secondary screen; player transport is untouched. */
@Composable
fun CommunityRoute(
    trackId: String,
    title: String,
    demo: Boolean,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val realRepository = remember(context) {
        SupabaseCommunityRepository(context.applicationContext)
    }
    val demoStore = remember(trackId) { DemoCommunityStore() }
    val scope = rememberCoroutineScope()
    var snapshot by remember(trackId, demo) { mutableStateOf<CommunitySnapshot?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    var commentInput by remember { mutableStateOf("") }
    var editId by remember { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<String?>(null) }
    var pendingReport by remember { mutableStateOf<String?>(null) }
    var reportReason by remember { mutableStateOf("") }
    var offset by remember { mutableStateOf(0) }

    suspend fun reload(page: Int = 0, append: Boolean = false) {
        val updated = if (demo) demoStore.read(page) else realRepository.read(trackId,page)
        snapshot = if (append && snapshot != null) updated.copy(
            comments = snapshot!!.comments + updated.comments
        ) else updated
        offset = page
    }
    fun action(refreshAfter: Boolean = true, block: suspend () -> Unit) {
        if (busy) return
        scope.launch {
            busy = true
            error = null
            info = null
            try {
                block()
                if (refreshAfter) reload()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                error = failure.message ?: "Không thể hoàn thành. Hãy thử lại."
            } finally {
                busy = false
            }
        }
    }
    LaunchedEffect(trackId, demo) {
        busy = true
        try {
            reload()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            error = failure.message ?: "Không thể tải đánh giá và bình luận."
        } finally {
            busy = false
        }
    }

    Dialog(onDismissRequest = { if (!busy) onClose() }) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 620.dp)
                    .verticalScroll(rememberScrollState()).padding(16.dp)
                    .testTag("community-panel"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Đánh giá & bình luận",style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = onClose,enabled = !busy) { Text("Đóng") }
                }
                Text(title,style = MaterialTheme.typography.titleMedium)
                if (demo) {
                    Text(
                        "CHẾ ĐỘ DEMO — dữ liệu mẫu, không gửi lên Supabase.",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("community-demo-label")
                    )
                }
                if (busy) CircularProgressIndicator()
                error?.let { Text(it,color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("community-error")) }
                info?.let { Text(it,color = MaterialTheme.colorScheme.primary) }
                val current = snapshot
                if (current != null) {
                    Text(
                        "${"%.1f".format(current.averageStars)}/5 sao · " +
                            "${current.ratingCount} lượt đánh giá",
                        modifier = Modifier.testTag("community-rating-summary")
                    )
                    Text("Đánh giá của bạn",style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..5).forEach { stars ->
                            TextButton(
                                enabled = !busy && current.authenticated,
                                onClick = {
                                    action {
                                        if (demo) demoStore.rate(stars)
                                        else realRepository.rate(trackId,stars)
                                        info = "Đã lưu ${stars} sao."
                                    }
                                },
                                modifier = Modifier.semantics {
                                    contentDescription = "Đánh giá $stars sao"
                                    stateDescription = if (stars == current.myStars)
                                        "Đã chọn" else "Chưa chọn"
                                }.testTag("community-rate-$stars")
                            ) {
                                Text(if (stars <= current.myStars) "★" else "☆",
                                    style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                    if (!current.authenticated) {
                        Text(
                            "Bạn có thể đọc bình luận. Để chấm sao hoặc viết bình luận, " +
                                "hãy đăng nhập tài khoản Supabase thật ở mục Tài khoản.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    HorizontalDivider()
                    Text(
                        "Bình luận (${current.commentCount})",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (current.comments.isEmpty()) {
                        Text("Chưa có bình luận nào. Hãy là người đầu tiên chia sẻ cảm nhận.")
                    }
                    current.comments.forEach { comment ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "${comment.displayName} · ${comment.createdAt}",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(comment.body)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (comment.mine) {
                                    TextButton(onClick = {
                                        editId = comment.id
                                        commentInput = comment.body
                                    },enabled = !busy) { Text("Sửa") }
                                    TextButton(onClick = {
                                        pendingDelete = comment.id
                                    },enabled = !busy) { Text("Xóa") }
                                } else if (current.authenticated) {
                                    TextButton(onClick = {
                                        pendingReport = comment.id
                                        reportReason = ""
                                    },enabled = !busy) { Text("Báo cáo") }
                                }
                            }
                        }
                        HorizontalDivider()
                    }
                    if (current.comments.size < current.commentCount &&
                        current.comments.size < 1020) {
                        OutlinedButton(
                            onClick = {
                                action(refreshAfter = false) {
                                    reload(offset + CommunityRules.PAGE_SIZE,append = true)
                                }
                            },
                            enabled = !busy,modifier = Modifier.testTag("community-load-more")
                        ) { Text("Xem thêm bình luận") }
                    }
                    if (current.authenticated) {
                        OutlinedTextField(
                            value = commentInput,
                            onValueChange = { commentInput = it.take(CommunityRules.COMMENT_LIMIT) },
                            label = { Text(if (editId != null) "Sửa bình luận" else "Viết bình luận") },
                            supportingText = { Text("${commentInput.length}/500 ký tự") },
                            modifier = Modifier.fillMaxWidth().testTag("community-comment-input"),
                            minLines = 2
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    action {
                                        val text = CommunityRules.cleanComment(commentInput)
                                        if (demo) {
                                            if (editId == null) demoStore.create(text)
                                            else demoStore.edit(editId!!,text)
                                        } else if (editId == null) realRepository.create(trackId,text)
                                        else realRepository.edit(editId!!,text)
                                        commentInput = ""
                                        editId = null
                                    }
                                },
                                enabled = !busy && commentInput.trim().isNotEmpty(),
                                modifier = Modifier.testTag("community-comment-submit")
                            ) { Text(if (editId == null) "Đăng bình luận" else "Lưu chỉnh sửa") }
                            if (editId != null) {
                                TextButton(onClick = {
                                    editId = null
                                    commentInput = ""
                                }) { Text("Hủy") }
                            }
                        }
                    }
                }
                if (snapshot == null && !busy) {
                    OutlinedButton(onClick = { action { reload() } }) { Text("Thử lại") }
                }
            }
        }
    }
    pendingDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Xóa bình luận?") },
            text = { Text("Bình luận của bạn sẽ không còn hiển thị.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    action {
                        if (demo) demoStore.delete(id) else realRepository.delete(id)
                    }
                }) { Text("Xóa") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Hủy") }
            }
        )
    }
    pendingReport?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingReport = null },
            title = { Text("Báo cáo bình luận") },
            text = {
                OutlinedTextField(
                    value = reportReason,
                    onValueChange = { reportReason = it.take(CommunityRules.REASON_LIMIT) },
                    label = { Text("Lý do (5–300 ký tự)") },
                    modifier = Modifier.testTag("community-report-reason")
                )
            },
            confirmButton = {
                TextButton(
                    enabled = reportReason.trim().length in 5..300,
                    onClick = {
                        val reason = reportReason
                        pendingReport = null
                        action {
                            if (demo) demoStore.report(id,reason)
                            else realRepository.report(id,reason)
                            info = "Đã ghi nhận báo cáo${if (demo) " (Demo)" else ""}."
                        }
                    }
                ) { Text("Gửi báo cáo") }
            },
            dismissButton = {
                TextButton(onClick = { pendingReport = null }) { Text("Hủy") }
            }
        )
    }
}
