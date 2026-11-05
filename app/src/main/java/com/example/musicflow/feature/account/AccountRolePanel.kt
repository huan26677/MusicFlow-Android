package com.example.musicflow.feature.account

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import android.net.Uri
import com.example.musicflow.BuildConfig
import com.example.musicflow.core.account.AccountIdentity
import com.example.musicflow.core.account.MusicFlowAccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/** Role UI uses an existing authenticated session; all authorization lives in Supabase RPCs. */
@Composable
internal fun AccountRolePanel(
    identity: AccountIdentity,
    onClose: () -> Unit,
    upload: suspend (String, String, Uri) -> Unit = { _, _, _ -> error("QA only") },
    download: suspend (String, Uri) -> Unit = { _, _ -> error("QA only") },
    rpc: suspend (String, JSONObject) -> String
) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    var access by remember(identity.userId) { mutableStateOf(MusicFlowAccess.NONE) }
    var checked by remember(identity.userId) { mutableStateOf(false) }

    var search by remember { mutableStateOf("") }
    var users by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var selected by remember { mutableStateOf<JSONObject?>(null) }
    var role by remember { mutableStateOf("platform_admin") }
    var permission by remember { mutableStateOf("manage_users") }
    var reason by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    var creatorType by remember { mutableStateOf("artist") }
    var creatorName by remember { mutableStateOf("") }
    var creatorBio by remember { mutableStateOf("") }
    var myRequests by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var pending by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var tracks by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var audits by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var developerOpen by remember { mutableStateOf(false) }
    var qaConnection by remember { mutableStateOf<String?>(null) }

    fun runAction(name: String, args: JSONObject = JSONObject(), ok: (String) -> Unit) {
        if (busy) return
        scope.launch {
            busy = true
            error = null
            info = null
            try {
                ok(rpc(name, args))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                error = failure.message ?: "Không thể hoàn thành thao tác. Kiểm tra kết nối và quyền."
            } finally {
                busy = false
            }
        }
    }

    LaunchedEffect(identity.userId) {
        access = MusicFlowAccess.NONE
        try {
            access = MusicFlowAccess.fromResponse(
                JSONObject(rpc("musicflow_my_access", JSONObject()))
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            error = "Không xác minh được quyền với Supabase. Các thao tác quản trị bị khóa."
        } finally {
            checked = true
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Vai trò và quản trị MusicFlow", style = MaterialTheme.typography.titleLarge)
            Text(identity.email, style = MaterialTheme.typography.bodySmall)
            Text(if (!checked) "Đang kiểm tra vai trò..." else
                "Vai trò: " + access.roles.ifEmpty { setOf("Người nghe") }.joinToString(", "))
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            info?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

            if (checked) {
                HorizontalDivider()
                Text("Tài khoản tác giả / nghệ sĩ", style = MaterialTheme.typography.titleMedium)
                if (!access.isCreator) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("artist" to "Nghệ sĩ", "author" to "Tác giả").forEach { (key, label) ->
                            FilterChip(
                                selected = creatorType == key,
                                onClick = { creatorType = key },
                                label = { Text(label) }
                            )
                        }
                    }
                } else {
                    Text("Bạn đã có vai trò sáng tạo được phê duyệt.")
                }
                OutlinedTextField(
                    value = creatorName, onValueChange = { creatorName = it.take(80) },
                    label = { Text("Nghệ danh / bút danh") },
                    modifier = Modifier.fillMaxWidth().testTag("creator-stage-name")
                )
                OutlinedTextField(
                    value = creatorBio, onValueChange = { creatorBio = it.take(1000) },
                    label = { Text("Giới thiệu / thông tin tác phẩm") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (!access.isCreator) {
                    Button(
                        onClick = {
                            runAction(
                                "musicflow_apply_creator",
                                JSONObject().put("p_role", creatorType)
                                    .put("p_name", creatorName.trim())
                                    .put("p_note", creatorBio.trim())
                            ) { info = "Đã gửi yêu cầu. Vai trò chỉ có sau khi được duyệt." }
                        },
                        enabled = !busy && creatorName.trim().length in 2..80,
                        modifier = Modifier.fillMaxWidth().testTag("creator-request")
                    ) { Text("Đề nghị xét duyệt") }
                } else {
                    OutlinedButton(
                        enabled = !busy,
                        onClick = {
                            runAction("musicflow_my_creator_profile") {
                                val profile = JSONObject(it)
                                creatorName = profile.optString("stage_name")
                                creatorBio = profile.optString("bio")
                            }
                        }
                    ) { Text("Tải hồ sơ sáng tạo") }
                    Button(
                        enabled = !busy && creatorName.trim().length in 2..80,
                        onClick = {
                            runAction(
                                "musicflow_save_creator_profile",
                                JSONObject().put("p_name", creatorName.trim())
                                    .put("p_bio", creatorBio.trim())
                            ) { info = "Đã lưu hồ sơ trên Supabase." }
                        }
                    ) { Text("Lưu hồ sơ sáng tạo") }
                }
                OutlinedButton(
                    enabled = !busy,
                    onClick = {
                        runAction("musicflow_my_creator_requests") {
                            myRequests = JSONArray(it).rows()
                        }
                    }
                ) { Text("Xem tình trạng xét duyệt") }
                myRequests.forEach {
                    Text(
                        "${it.optString("requested_role")}: " +
                            "${it.optString("requested_name")} — ${it.optString("status")}"
                    )
                }

                if (access.isCreator) {
                    CreatorDraftStudio(rpc = rpc, upload = upload, download = download)
                }
                if (BuildConfig.DEBUG && access.isDeveloper) {
                    HorizontalDivider()
                    Text("Developer — Lối tắt QA", style = MaterialTheme.typography.titleMedium)
                    Text("MusicFlow ${BuildConfig.VERSION_NAME} • Debug")
                    Text("Chỉ dùng chẩn đoán và dữ liệu thử. Không bỏ qua đăng nhập hay mở khóa Admin.")
                    OutlinedButton(
                        onClick = { developerOpen = !developerOpen },
                        modifier = Modifier.fillMaxWidth().testTag("developer-quick-access")
                    ) { Text(if (developerOpen) "Đóng công cụ QA" else "Mở công cụ kiểm thử nhanh") }
                    if (developerOpen) {
                        Text("Môi trường: Debug nội bộ")
                        Text(
                            "Kết nối Supabase: " +
                                if (BuildConfig.SUPABASE_URL.startsWith("https://")) {
                                    "Có cấu hình URL"
                                } else "Chưa cấu hình URL"
                        )
                        OutlinedButton(
                            enabled = !busy,
                            onClick = {
                                runAction("musicflow_my_access") { raw ->
                                    val verified = MusicFlowAccess.fromResponse(JSONObject(raw))
                                    qaConnection = if (verified.isDeveloper) {
                                        "Phiên hợp lệ; role Developer được xác nhận bởi Supabase."
                                    } else {
                                        "Phiên còn hoạt động nhưng không còn vai trò Developer."
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("developer-check-session")
                        ) { Text("Kiểm tra phiên và quyền hiện tại") }
                        qaConnection?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    }
                }
                if (access.canManageUsers) {
                    HorizontalDivider()
                    Text("Quản lý người dùng", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = search, onValueChange = { search = it.take(80) },
                        label = { Text("Tìm email người dùng") },
                        modifier = Modifier.fillMaxWidth().testTag("admin-search-user")
                    )
                    Button(
                        onClick = {
                            runAction(
                                "musicflow_admin_users",
                                JSONObject().put("p_search", search.trim()).put("p_limit", 20)
                            ) { users = JSONArray(it).rows(); info = "Đã tải ${users.size} người dùng." }
                        },
                        enabled = !busy
                    ) { Text("Tải danh sách tài khoản") }
                    users.forEach { user ->
                        OutlinedButton(
                            onClick = { selected = user },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("${user.optString("email")} • ${user.optString("tier")}") }
                    }
                }
                if (access.isOwner) {
                    HorizontalDivider()
                    Text("Owner — phân quyền", style = MaterialTheme.typography.titleMedium)
                    Text("Chỉ chọn tài khoản từ danh sách; không thể cấp hay thay Owner.")
                    selected?.let {
                        Text("Đã chọn: ${it.optString("email")}")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("platform_admin","developer","moderator","artist","author")
                            .forEach { item ->
                                FilterChip(selected = role == item, onClick = { role = item },
                                    label = { Text(item) })
                            }
                    }
                    OutlinedTextField(
                        value = reason, onValueChange = { reason = it.take(300) },
                        label = { Text("Lý do cấp/thu hồi quyền") },
                        modifier = Modifier.fillMaxWidth().testTag("admin-reason")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { confirm = "role" to true },
                            enabled = !busy && selected != null && reason.trim().length >= 5
                        ) { Text("Cấp vai trò") }
                        OutlinedButton(
                            onClick = { confirm = "role" to false },
                            enabled = !busy && selected != null && reason.trim().length >= 5
                        ) { Text("Thu hồi") }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("manage_users","review_creators","moderate_catalog").forEach { item ->
                            FilterChip(selected = permission == item,
                                onClick = { permission = item }, label = { Text(item) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { confirm = "permission" to true },
                            enabled = !busy && selected != null && reason.trim().length >= 5
                        ) { Text("Cấp quyền cộng tác") }
                        OutlinedButton(
                            onClick = { confirm = "permission" to false },
                            enabled = !busy && selected != null && reason.trim().length >= 5
                        ) { Text("Thu hồi quyền") }
                    }
                    OutlinedButton(
                        onClick = { runAction("musicflow_owner_audit") { audits = JSONArray(it).rows() } },
                        enabled = !busy
                    ) { Text("Xem nhật ký quản trị") }
                    audits.take(30).forEach {
                        Text("${it.optString("created_at")} — ${it.optString("action")}")
                    }
                }
                if (access.canReviewCreators) {
                    HorizontalDivider()
                    Text("Duyệt yêu cầu sáng tạo", style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(
                        onClick = {
                            runAction("musicflow_pending_creators") {
                                pending = JSONArray(it).rows()
                            }
                        }, enabled = !busy
                    ) { Text("Tải yêu cầu đang chờ") }
                    pending.forEach { application ->
                        Text("${application.optString("email")} — ${application.optString("requested_name")}")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(true to "Duyệt", false to "Từ chối").forEach { (approved, label) ->
                                OutlinedButton(
                                    enabled = !busy && reason.trim().length >= 5,
                                    onClick = {
                                        runAction(
                                            "musicflow_review_creator",
                                            JSONObject().put("p_application", application.optString("id"))
                                                .put("p_approve", approved)
                                                .put("p_reason", reason.trim())
                                        ) {
                                            pending = pending.filterNot {
                                                it.optString("id") == application.optString("id")
                                            }
                                            info = "Đã xử lý xét duyệt."
                                        }
                                    }
                                ) { Text(label) }
                            }
                        }
                    }
                }
                if (access.canReviewMetadata) {
                    CreatorDraftReviewPanel(rpc = rpc, download = download)
                }
                if (access.canModerateCatalog) {
                    HorizontalDivider()
                    Text("Quản lý phát nhạc online", style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(
                        onClick = { runAction("musicflow_admin_tracks") { tracks = JSONArray(it).rows() } },
                        enabled = !busy
                    ) { Text("Tải danh mục cần kiểm duyệt") }
                    tracks.forEach { track ->
                        Text("${track.optString("title")} • ${track.optString("license_code")}")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(true to "Xuất bản", false to "Gỡ phát").forEach { (published, label) ->
                                OutlinedButton(
                                    enabled = !busy && reason.trim().length >= 5,
                                    onClick = {
                                        runAction(
                                            "musicflow_review_track",
                                            JSONObject().put("p_track", track.optString("id"))
                                                .put("p_publish", published)
                                                .put("p_reason", reason.trim())
                                        ) { info = "Máy chủ đã xử lý quyền xuất bản." }
                                    }
                                ) { Text(label) }
                            }
                        }
                    }
                }
                if (!access.isOwner &&
                    (access.canReviewCreators || access.canModerateCatalog)) {
                    OutlinedTextField(
                        value = reason, onValueChange = { reason = it.take(300) },
                        label = { Text("Lý do xét duyệt (ít nhất 5 ký tự)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            HorizontalDivider()
            TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Đóng") }
        }
    }

    confirm?.let { (kind, granting) ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(if (granting) "Xác nhận cấp quyền?" else "Xác nhận thu hồi?") },
            text = { Text("${selected?.optString("email")} — " +
                "${if (kind == "role") role else permission}\nLý do: $reason") },
            confirmButton = {
                TextButton(onClick = {
                    confirm = null
                    val target = selected?.optString("id").orEmpty()
                    val payload = JSONObject().put("p_user_id", target)
                        .put("p_grant", granting).put("p_reason", reason.trim())
                    val rpcName = if (kind == "role") {
                        payload.put("p_role", role)
                        "musicflow_manage_role"
                    } else {
                        payload.put("p_permission", permission)
                        "musicflow_set_admin_permission"
                    }
                    runAction(rpcName, payload) { info = "Đã cập nhật quyền trên Supabase." }
                }) { Text("Xác nhận") }
            },
            dismissButton = {
                TextButton(onClick = { confirm = null }) { Text("Hủy") }
            }
        )
    }
}

private fun JSONArray.rows(): List<JSONObject> =
    (0 until length()).mapNotNull { optJSONObject(it) }

