package com.example.musicflow.feature.account

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import android.net.Uri
import com.example.musicflow.BuildConfig
import com.example.musicflow.core.account.AccountIdentity
import com.example.musicflow.core.account.AccountWorkspace
import com.example.musicflow.core.account.AccountWorkspacePlan
import com.example.musicflow.core.account.AccountTaskGuide
import com.example.musicflow.core.account.AccountReviewArea
import com.example.musicflow.core.account.AccountReviewPlan
import com.example.musicflow.core.account.CreatorApplicationUiRules
import com.example.musicflow.core.account.MusicFlowAccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private data class AccountRoleDecision(
    val userId: String,
    val email: String,
    val kind: String,
    val name: String,
    val granting: Boolean,
    val reason: String
)

private data class AccountCatalogDecision(
    val trackId: String,
    val title: String,
    val publishing: Boolean,
    val reason: String
)

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
    var selectedWorkspace by remember(identity.userId) {
        mutableStateOf<AccountWorkspace?>(null)
    }

    var search by remember { mutableStateOf("") }
    var users by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var selected by remember { mutableStateOf<JSONObject?>(null) }
    var role by remember { mutableStateOf("platform_admin") }
    var permission by remember { mutableStateOf("manage_users") }
    var reason by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf<AccountRoleDecision?>(null) }
    var reviewConfirm by remember { mutableStateOf<Pair<JSONObject, Boolean>?>(null) }
    var catalogConfirm by remember { mutableStateOf<AccountCatalogDecision?>(null) }
    var reviewArea by remember(identity.userId) { mutableStateOf<AccountReviewArea?>(null) }

    var creatorType by remember { mutableStateOf("artist") }
    var creatorName by remember { mutableStateOf("") }
    var creatorBio by remember { mutableStateOf("") }
    var myRequests by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var requestsLoaded by remember(identity.userId) { mutableStateOf(false) }
    var pending by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var tracks by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var audits by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var developerOpen by remember { mutableStateOf(false) }
    var qaConnection by remember { mutableStateOf<String?>(null) }
    var studioExpanded by remember(identity.userId) { mutableStateOf(false) }
    var creatorProfileExpanded by remember(identity.userId) { mutableStateOf(false) }
    var ownerOverviewExpanded by remember(identity.userId) { mutableStateOf(false) }
    var usersLoaded by remember(identity.userId) { mutableStateOf(false) }
    var pendingLoaded by remember(identity.userId) { mutableStateOf(false) }
    var tracksLoaded by remember(identity.userId) { mutableStateOf(false) }
    var auditLoaded by remember(identity.userId) { mutableStateOf(false) }

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

    fun prepareRoleDecision(kind: String, granting: Boolean) {
        val target = selected ?: return
        val why = reason.trim()
        if (busy || why.length !in 5..300) return
        confirm = AccountRoleDecision(
            userId = target.optString("id"), email = target.optString("email"),
            kind = kind, name = if (kind == "role") role else permission,
            granting = granting, reason = why
        )
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
        // Read-only: determine existing pending applications before enabling submit.
        try {
            myRequests = JSONArray(rpc("musicflow_my_creator_requests", JSONObject())).rows()
            requestsLoaded = true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            requestsLoaded = false
        }
    }

    val hasPendingSelectedRole = CreatorApplicationUiRules.hasPendingRole(
        myRequests.map { it.optString("requested_role") to it.optString("status") }, creatorType
    )
    val workspaces = AccountWorkspacePlan.available(access, BuildConfig.DEBUG)
    val currentWorkspace = selectedWorkspace?.takeIf { it in workspaces }
        ?: AccountWorkspacePlan.initial(access, BuildConfig.DEBUG)
    val reviewAreas = AccountReviewPlan.available(access)
    val activeReviewArea = AccountReviewPlan.active(access, reviewArea)
    val contentScroll = rememberScrollState()
    LaunchedEffect(currentWorkspace, activeReviewArea) { contentScroll.scrollTo(0) }

    // Back first exits a subordinate task; the enclosing Dialog closes when
    // no nested task remains. The child editor owns its Back handler too.
    BackHandler(
        enabled = !busy && (studioExpanded || creatorProfileExpanded ||
            ownerOverviewExpanded || developerOpen || selected != null ||
            reviewArea != null || selectedWorkspace != null)
    ) {
        when {
            studioExpanded -> studioExpanded = false
            creatorProfileExpanded -> creatorProfileExpanded = false
            ownerOverviewExpanded -> ownerOverviewExpanded = false
            developerOpen -> developerOpen = false
            selected != null -> selected = null
            reviewArea != null -> reviewArea = null
            selectedWorkspace != null -> selectedWorkspace = null
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Keep actor navigation visible even when editing a long form.
            if (checked) {
                Text("Chọn công việc cần làm", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() })
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val compact = maxWidth < 380.dp || LocalDensity.current.fontScale >= 1.5f
                    val workspaceButton: @Composable (AccountWorkspace, Modifier) -> Unit =
                        { workspace, buttonModifier ->
                        FilterChip(
                            selected = currentWorkspace == workspace,
                            onClick = {
                                selectedWorkspace = workspace
                                confirm = null
                                reviewConfirm = null
                                catalogConfirm = null
                            },
                            label = { Text(workspace.title) },
                            modifier = buttonModifier.heightIn(min = 48.dp)
                                .testTag("role-workspace-${workspace.name.lowercase()}")
                        )
                        }
                    if (compact) {
                        Column(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 156.dp)
                                .verticalScroll(rememberScrollState())
                                .testTag("role-workspace-tabs"),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            workspaces.forEach { workspace ->
                                workspaceButton(workspace, Modifier.fillMaxWidth())
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                                .testTag("role-workspace-tabs"),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            workspaces.forEach { workspace ->
                                workspaceButton(workspace, Modifier)
                            }
                        }
                    }
                }
                Text(
                    "Đang làm: ${currentWorkspace.title} · ${currentWorkspace.description}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.testTag("role-current-workspace-summary")
                )
            }
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(contentScroll),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
            Text("Vai trò và quản trị MusicFlow", style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() })
            Text(identity.email, style = MaterialTheme.typography.bodySmall)
            Text(if (!checked) "Đang kiểm tra vai trò..." else
                "Vai trò: " + access.roles.sorted().takeIf { it.isNotEmpty() }
                    ?.joinToString(", ") { AccountWorkspacePlan.roleLabel(it) }
                    .orEmpty().ifBlank { "Người nghe" })
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            info?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

            if (checked) {
                OutlinedButton(
                    enabled = !busy,
                    onClick = {
                        // Hide previously granted actions before fetching fresh server access.
                        // When the network/RPC fails, the panel stays fail-closed.
                        access = MusicFlowAccess.NONE
                        qaConnection = null
                        users = emptyList()
                        selected = null
                        pending = emptyList()
                        tracks = emptyList()
                        audits = emptyList()
                        confirm = null
                        reviewConfirm = null
                        catalogConfirm = null
                        developerOpen = false
                        selectedWorkspace = null
                        reviewArea = null
                        ownerOverviewExpanded = false
                        studioExpanded = false
                        creatorProfileExpanded = false
                        usersLoaded = false
                        pendingLoaded = false
                        tracksLoaded = false
                        auditLoaded = false
                        runAction("musicflow_my_access") { raw ->
                            access = MusicFlowAccess.fromResponse(JSONObject(raw))
                            info = "Đã kiểm tra lại vai trò trực tiếp từ Supabase."
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("role-access-refresh")
                ) { Text("Kiểm tra lại quyền từ Supabase") }
            }

            if (checked) {
                Text("Khu vực làm việc", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() })
                Text(currentWorkspace.description, style = MaterialTheme.typography.bodySmall)
                Text("Bước tiếp theo dành cho bạn:",
                    style = MaterialTheme.typography.titleSmall)
                Text(AccountTaskGuide.nextStep(currentWorkspace, access),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag("workspace-next-step"))
                if (currentWorkspace == AccountWorkspace.CREATOR) {
                HorizontalDivider()
                Text("Tài khoản tác giả / nghệ sĩ", style = MaterialTheme.typography.titleMedium)
                if (!access.isCreator) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("artist" to "Nghệ sĩ", "author" to "Tác giả").forEach { (key, label) ->
                            FilterChip(
                                selected = creatorType == key,
                                onClick = { creatorType = key },
                                label = { Text(label) },
                                modifier = Modifier.heightIn(min = 48.dp)
                                    .testTag("creator-type-$key")
                            )
                        }
                    }
                    if (!requestsLoaded) {
                        Text("Chưa kiểm tra được đơn hiện có. Hãy tải tình trạng xét duyệt trước khi gửi đơn mới.")
                    } else if (hasPendingSelectedRole) {
                        Text("Bạn đã có đơn ${CreatorApplicationUiRules.roleLabel(creatorType)} đang chờ duyệt. Không cần gửi lại.",
                            modifier = Modifier.testTag("creator-pending-hint"))
                    }
                } else {
                    Text("Bạn đã được duyệt. Mở kho tác phẩm để bắt đầu, hoặc sửa hồ sơ khi cần.")
                }
                OutlinedButton(
                    enabled = !busy,
                    onClick = {
                        myRequests = emptyList()
                        requestsLoaded = false
                        runAction("musicflow_my_creator_requests") {
                            myRequests = JSONArray(it).rows()
                            requestsLoaded = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("creator-load-requests")
                ) { Text("Cập nhật trạng thái xét duyệt") }
                if (requestsLoaded && myRequests.isEmpty()) {
                    Text("Bạn chưa có yêu cầu Creator nào.",
                        modifier = Modifier.testTag("creator-requests-empty"))
                }
                myRequests.forEach {
                    Text(
                        "${CreatorApplicationUiRules.roleLabel(it.optString("requested_role"))}: " +
                            "${it.optString("requested_name")} — " +
                            CreatorApplicationUiRules.statusLabel(it.optString("status")),
                        modifier = Modifier.testTag("creator-request-status")
                    )
                    val note = it.optString("review_note").trim()
                    if (note.isNotBlank()) Text("Phản hồi xét duyệt: $note")
                }

                if (!access.isCreator && requestsLoaded && !hasPendingSelectedRole) {
                    Text("Đăng ký vai trò sáng tạo", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(
                        value = creatorName, onValueChange = { creatorName = it.take(80) },
                        label = { Text("Nghệ danh / bút danh") },
                        modifier = Modifier.fillMaxWidth().testTag("creator-stage-name")
                    )
                    OutlinedTextField(
                        value = creatorBio, onValueChange = { creatorBio = it.take(1000) },
                        label = { Text("Giới thiệu ngắn (không bắt buộc)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            runAction(
                                "musicflow_apply_creator",
                                JSONObject().put("p_role", creatorType)
                                    .put("p_name", creatorName.trim())
                                    .put("p_note", creatorBio.trim())
                            ) {
                                // Server accepted the request; display pending immediately.
                                myRequests = listOf(JSONObject()
                                    .put("requested_role", creatorType)
                                    .put("requested_name", creatorName.trim())
                                    .put("status", "pending")) + myRequests
                                requestsLoaded = true
                                info = "Đã gửi yêu cầu. Quyền sáng tạo chỉ có sau khi được duyệt."
                            }
                        },
                        enabled = CreatorApplicationUiRules.canSubmit(
                            access.isCreator, requestsLoaded, busy, creatorName, hasPendingSelectedRole
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("creator-request")
                    ) { Text("Gửi đăng ký xét duyệt") }
                }
                if (access.isCreator) {
                    OutlinedButton(
                        onClick = { studioExpanded = !studioExpanded },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .testTag("creator-studio-toggle")
                    ) { Text(if (studioExpanded) "Thu gọn kho tác phẩm"
                        else "Mở kho tác phẩm và bản nháp") }
                    if (studioExpanded) {
                        CreatorDraftStudio(rpc = rpc, upload = upload, download = download)
                    }
                    OutlinedButton(
                        enabled = !busy,
                        onClick = {
                            if (creatorProfileExpanded) {
                                creatorProfileExpanded = false
                            } else {
                                runAction("musicflow_my_creator_profile") {
                                    val profile = JSONObject(it)
                                    creatorName = profile.optString("stage_name")
                                    creatorBio = profile.optString("bio")
                                    creatorProfileExpanded = true
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("creator-profile-toggle")
                    ) { Text(if (creatorProfileExpanded) "Thu gọn hồ sơ sáng tạo"
                        else "Chỉnh sửa hồ sơ Nghệ sĩ / Tác giả") }
                    if (creatorProfileExpanded) {
                        OutlinedTextField(
                            value = creatorName, onValueChange = { creatorName = it.take(80) },
                            label = { Text("Nghệ danh / bút danh") },
                            modifier = Modifier.fillMaxWidth().testTag("creator-stage-name")
                        )
                        OutlinedTextField(
                            value = creatorBio, onValueChange = { creatorBio = it.take(1000) },
                            label = { Text("Giới thiệu") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            enabled = !busy && creatorName.trim().length in 2..80,
                            onClick = {
                                runAction(
                                    "musicflow_save_creator_profile",
                                    JSONObject().put("p_name", creatorName.trim())
                                        .put("p_bio", creatorBio.trim())
                                ) {
                                    creatorProfileExpanded = false
                                    info = "Đã lưu hồ sơ sáng tạo trên Supabase."
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("creator-profile-save")
                        ) { Text("Lưu hồ sơ sáng tạo") }
                    }
                }
                }
                if (currentWorkspace == AccountWorkspace.DEVELOPER &&
                    BuildConfig.DEBUG && access.isDeveloper) {
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
                if (currentWorkspace == AccountWorkspace.MANAGEMENT) {
                if (access.isOwner) {
                    OutlinedButton(
                        onClick = { ownerOverviewExpanded = !ownerOverviewExpanded },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .testTag("owner-overview-toggle")
                    ) { Text(if (ownerOverviewExpanded) "Thu gọn tổng quan Owner"
                        else "Xem tổng quan và tiến độ demo") }
                    if (ownerOverviewExpanded) OwnerDemoDashboard(rpc = rpc)
                }
                if (access.canManageUsers) {
                    HorizontalDivider()
                    Text("Quản lý người dùng", style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { heading() })
                    OutlinedTextField(
                        value = search, onValueChange = { search = it.take(80) },
                        label = { Text("Tìm email người dùng") },
                        modifier = Modifier.fillMaxWidth().testTag("admin-search-user")
                    )
                    Button(
                        onClick = {
                            users = emptyList()
                            selected = null
                            usersLoaded = false
                            runAction(
                                "musicflow_admin_users",
                                JSONObject().put("p_search", search.trim()).put("p_limit", 20)
                            ) {
                                users = JSONArray(it).rows()
                                usersLoaded = true
                                info = "Đã tải ${users.size} người dùng."
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("admin-users-load")
                    ) { Text("Tải danh sách tài khoản") }
                    if (usersLoaded && users.isEmpty()) {
                        Text("Không có tài khoản phù hợp. Thử đổi từ khóa rồi tải lại.",
                            modifier = Modifier.testTag("admin-users-empty"))
                    }
                    users.forEach { user ->
                        OutlinedButton(
                            onClick = {
                                selected = user
                                reason = ""
                                confirm = null
                            },
                            modifier = Modifier.fillMaxWidth().testTag("admin-user-item")
                        ) { Text("${user.optString("email")} • ${user.optString("tier")}") }
                    }
                }
                if (access.isOwner) {
                    HorizontalDivider()
                    Text("Owner — phân quyền", style = MaterialTheme.typography.titleMedium)
                    Text("Chọn tài khoản ở danh sách phía trên để mở các thao tác cấp/thu hồi. Không thể thay Owner.")
                    selected?.let {
                        Text("Đang quản lý: ${it.optString("email")}",
                            modifier = Modifier.testTag("owner-selected-user"))
                        TextButton(
                            onClick = { selected = null; reason = "" },
                            modifier = Modifier.testTag("owner-clear-selection")
                        ) { Text("Bỏ chọn tài khoản") }
                        Text("Vai trò", style = MaterialTheme.typography.titleSmall)
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("platform_admin","developer","moderator","artist","author")
                                .forEach { item ->
                                    FilterChip(
                                        selected = role == item, onClick = { role = item },
                                        label = { Text(AccountWorkspacePlan.roleLabel(item)) }
                                    )
                                }
                        }
                        OutlinedTextField(
                            value = reason, onValueChange = { reason = it.take(300) },
                            label = { Text("Lý do cấp/thu hồi quyền (5–300 ký tự)") },
                            modifier = Modifier.fillMaxWidth().testTag("admin-reason")
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { prepareRoleDecision("role", true) },
                                enabled = !busy && reason.trim().length in 5..300
                            ) { Text("Cấp vai trò") }
                            OutlinedButton(
                                onClick = { prepareRoleDecision("role", false) },
                                enabled = !busy && reason.trim().length in 5..300
                            ) { Text("Thu hồi") }
                        }
                        Text("Quyền cộng tác quản trị", style = MaterialTheme.typography.titleSmall)
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("manage_users","review_creators","moderate_catalog")
                                .forEach { item ->
                                    FilterChip(
                                        selected = permission == item,
                                        onClick = { permission = item },
                                        label = { Text(AccountReviewPlan.permissionLabel(item)) }
                                    )
                                }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { prepareRoleDecision("permission", true) },
                                enabled = !busy && reason.trim().length in 5..300
                            ) { Text("Cấp quyền") }
                            OutlinedButton(
                                onClick = { prepareRoleDecision("permission", false) },
                                enabled = !busy && reason.trim().length in 5..300
                            ) { Text("Thu hồi quyền") }
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            audits = emptyList()
                            auditLoaded = false
                            runAction("musicflow_owner_audit") {
                                audits = JSONArray(it).rows()
                                auditLoaded = true
                            }
                        },
                        enabled = !busy
                    ) { Text("Xem nhật ký quản trị") }
                    if (auditLoaded && audits.isEmpty()) {
                        Text("Chưa có bản ghi quản trị để hiển thị.",
                            modifier = Modifier.testTag("owner-audit-empty"))
                    }
                    audits.take(30).forEach {
                        Text("${it.optString("created_at")} — ${it.optString("action")}")
                    }
                }
                }
                if (currentWorkspace == AccountWorkspace.MODERATION) {
                if (reviewAreas.size > 1) {
                    Text("Chọn công việc kiểm duyệt", style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        reviewAreas.forEach { area ->
                            FilterChip(
                                selected = area == activeReviewArea,
                                onClick = {
                                    reviewArea = area
                                    reviewConfirm = null
                                    catalogConfirm = null
                                },
                                label = { Text(area.label) },
                                modifier = Modifier.heightIn(min = 48.dp)
                                    .testTag("review-area-${area.name.lowercase()}")
                            )
                        }
                    }
                }
                activeReviewArea?.let {
                    Text(it.guidance, style = MaterialTheme.typography.bodySmall)
                }
                if (activeReviewArea == AccountReviewArea.APPLICATIONS ||
                    activeReviewArea == AccountReviewArea.CATALOG) {
                    OutlinedTextField(
                        value = reason, onValueChange = { reason = it.take(300) },
                        label = { Text("Lý do xét duyệt (5–300 ký tự)") },
                        modifier = Modifier.fillMaxWidth().testTag("creator-review-reason")
                    )
                }
                if (activeReviewArea == AccountReviewArea.APPLICATIONS && access.canReviewCreators) {
                    HorizontalDivider()
                    Text("Duyệt yêu cầu sáng tạo", style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(
                        onClick = {
                            pending = emptyList()
                            pendingLoaded = false
                            runAction("musicflow_pending_creators") {
                                pending = JSONArray(it).rows()
                                pendingLoaded = true
                            }
                        }, enabled = !busy,
                        modifier = Modifier.testTag("creator-review-load-pending")
                    ) { Text("Tải yêu cầu đang chờ") }
                    if (pendingLoaded && pending.isEmpty()) {
                        Text("Hiện không có yêu cầu Creator đang chờ duyệt.",
                            modifier = Modifier.testTag("creator-review-empty"))
                    }
                    pending.forEach { application ->
                        Text(
                            "${application.optString("email")} — ${application.optString("requested_name")} " +
                                "(${CreatorApplicationUiRules.roleLabel(application.optString("requested_role"))})"
                        )
                        if (application.optString("note").isNotBlank()) {
                            Text("Giới thiệu: ${application.optString("note")}")
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(true to "Duyệt", false to "Từ chối").forEach { (approved, label) ->
                                OutlinedButton(
                                    enabled = CreatorApplicationUiRules.canReview(busy, reason),
                                    onClick = { reviewConfirm = application to approved },
                                    modifier = Modifier.testTag(if (approved) "creator-review-approve" else "creator-review-reject")
                                ) { Text(label) }
                            }
                        }
                    }
                }
                if (activeReviewArea == AccountReviewArea.DRAFTS && access.canReviewMetadata) {
                    CreatorDraftReviewPanel(rpc = rpc, download = download)
                }
                if (activeReviewArea == AccountReviewArea.CATALOG && access.canModerateCatalog) {
                    HorizontalDivider()
                    Text("Quản lý phát nhạc online", style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(
                        onClick = {
                            tracks = emptyList()
                            tracksLoaded = false
                            runAction("musicflow_admin_tracks") {
                                tracks = JSONArray(it).rows()
                                tracksLoaded = true
                            }
                        },
                        enabled = !busy
                    ) { Text("Tải danh mục cần kiểm duyệt") }
                    if (tracksLoaded && tracks.isEmpty()) {
                        Text("Không có bản ghi online cần kiểm tra lúc này.",
                            modifier = Modifier.testTag("moderation-tracks-empty"))
                    }
                    tracks.forEach { track ->
                        Text("${track.optString("title")} • ${track.optString("license_code")}")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(true to "Xuất bản", false to "Gỡ phát").forEach { (published, label) ->
                                OutlinedButton(
                                    enabled = !busy && reason.trim().length in 5..300,
                                    onClick = {
                                        catalogConfirm = AccountCatalogDecision(
                                            trackId = track.optString("id"),
                                            title = track.optString("title"),
                                            publishing = published,
                                            reason = reason.trim()
                                        )
                                    },
                                    modifier = Modifier.testTag(
                                        if (published) "catalog-confirm-publish" else "catalog-confirm-unpublish"
                                    )
                                ) { Text(label) }
                            }
                        }
                    }
                }
                }
            }
            HorizontalDivider()
            TextButton(onClick = onClose,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Đóng") }
            }
        }
    }

    confirm?.let { decision ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = {
                Text(if (decision.granting) "Xác nhận cấp quyền?" else "Xác nhận thu hồi?")
            },
            text = {
                Text("Tài khoản: ${decision.email}\n" +
                    "${if (decision.kind == "role") "Vai trò" else "Quyền"}: " +
                    "${if (decision.kind == "role") AccountWorkspacePlan.roleLabel(decision.name)
                    else AccountReviewPlan.permissionLabel(decision.name)}\n" +
                    "Lý do: ${decision.reason}\nQuyết định sẽ được ghi trên Supabase.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirm = null
                        val payload = JSONObject()
                            .put("p_user_id", decision.userId)
                            .put("p_grant", decision.granting)
                            .put("p_reason", decision.reason)
                        val rpcName = if (decision.kind == "role") {
                            payload.put("p_role", decision.name)
                            "musicflow_manage_role"
                        } else {
                            payload.put("p_permission", decision.name)
                            "musicflow_set_admin_permission"
                        }
                        runAction(rpcName, payload) { raw ->
                            if (decision.kind == "role") {
                                check(JSONObject(raw).has("roles")) {
                                    "Máy chủ chưa xác nhận thay đổi vai trò."
                                }
                            } else check(raw.trim() == "true") {
                                "Máy chủ chưa xác nhận thay đổi quyền."
                            }
                            selected = null
                            users = emptyList()
                            usersLoaded = false
                            reason = ""
                            info = "Đã cập nhật quyền. Tải lại danh sách để kiểm tra kết quả."
                        }
                    },
                    modifier = Modifier.testTag("owner-role-confirm")
                ) { Text("Xác nhận") }
            },
            dismissButton = {
                TextButton(onClick = { confirm = null }) { Text("Hủy") }
            }
        )
    }

    reviewConfirm?.let { (application, approved) ->
        AlertDialog(
            onDismissRequest = { reviewConfirm = null },
            title = { Text(if (approved) "Xác nhận duyệt Creator?" else "Xác nhận từ chối Creator?") },
            text = {
                Text(
                    "${application.optString("requested_name")} — " +
                        CreatorApplicationUiRules.roleLabel(application.optString("requested_role")) +
                        "\nTài khoản: ${application.optString("email")}" +
                        "\nLý do ghi vào nhật ký: ${reason.trim()}" +
                        "\nQuyết định này được ghi lại trên Supabase."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = CreatorApplicationUiRules.canReview(busy, reason),
                    onClick = {
                        reviewConfirm = null
                        val id = application.optString("id")
                        runAction(
                            "musicflow_review_creator",
                            JSONObject().put("p_application", id)
                                .put("p_approve", approved)
                                .put("p_reason", reason.trim())
                        ) { raw ->
                            check(raw.trim() == "true") { "Máy chủ chưa xác nhận quyết định." }
                            pending = pending.filterNot { it.optString("id") == id }
                            info = "Đã xử lý xét duyệt trên Supabase."
                        }
                    },
                    modifier = Modifier.testTag("creator-review-confirm")
                ) { Text("Xác nhận") }
            },
            dismissButton = {
                TextButton(
                    onClick = { reviewConfirm = null },
                    modifier = Modifier.testTag("creator-review-cancel")
                ) { Text("Hủy") }
            }
        )
    }

    catalogConfirm?.let { decision ->
        AlertDialog(
            onDismissRequest = { catalogConfirm = null },
            title = {
                Text(if (decision.publishing) "Xác nhận xuất bản nhạc online?"
                    else "Xác nhận gỡ phát nhạc online?")
            },
            text = {
                Text("Tác phẩm: ${decision.title}\nLý do: ${decision.reason}\n" +
                    "Máy chủ sẽ kiểm tra giấy phép và quyền phát trước khi thực hiện.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        catalogConfirm = null
                        runAction(
                            "musicflow_review_track",
                            JSONObject().put("p_track", decision.trackId)
                                .put("p_publish", decision.publishing)
                                .put("p_reason", decision.reason)
                        ) { raw ->
                            check(raw.trim() == "true") {
                                "Máy chủ chưa xác nhận thay đổi nhạc online."
                            }
                            tracks = emptyList()
                            tracksLoaded = false
                            info = "Đã xử lý. Tải lại danh mục để xem trạng thái mới."
                        }
                    },
                    modifier = Modifier.testTag("catalog-action-confirm")
                ) { Text("Xác nhận") }
            },
            dismissButton = {
                TextButton(
                    onClick = { catalogConfirm = null },
                    modifier = Modifier.testTag("catalog-action-cancel")
                ) { Text("Hủy") }
            }
        )
    }
}

private fun JSONArray.rows(): List<JSONObject> =
    (0 until length()).mapNotNull { optJSONObject(it) }

