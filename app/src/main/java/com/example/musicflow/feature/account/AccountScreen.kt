package com.example.musicflow.feature.account

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.example.musicflow.core.account.CloudPendingMatch
import com.example.musicflow.core.account.AccountHomePlan
import com.example.musicflow.core.account.AccountWorkspacePlan
import com.example.musicflow.core.account.DemoBillingStore
import com.example.musicflow.core.account.DemoBillingResult
import com.example.musicflow.BuildConfig
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.musicflow.ui.components.MusicFlowStatusKind
import com.example.musicflow.ui.components.MusicFlowStatusPanel
import com.example.musicflow.ui.components.OceanFlowHeader
import com.example.musicflow.ui.components.OceanFlowCard

@Composable
fun AccountRoute(
    themeMode: String,
    onThemeModeChange: (String) -> Unit,
    onContinueAsGuest: () -> Unit = {}
) {
    val context = LocalContext.current
    val factory = remember(context) {
        AccountViewModel.factory(context.applicationContext)
    }
    val model: AccountViewModel = viewModel(factory = factory)
    val state by model.state.collectAsStateWithLifecycle()
    var rolesOpen by rememberSaveable { mutableStateOf(false) }
    var billingOpen by rememberSaveable { mutableStateOf(false) }
    var demoCenterOpen by rememberSaveable { mutableStateOf(false) }
    val billingStore = remember(context) { DemoBillingStore(context.applicationContext) }

    AccountScreen(
        state, themeMode, onThemeModeChange,
        onEmailChange = model::emailChanged,
        onPasswordChange = model::passwordChanged,
        onConfirmPasswordChange = model::confirmPasswordChanged,
        onAuthModeChange = model::authModeChanged,
        onContinueAsGuest = onContinueAsGuest,
        onDisplayNameChange = model::displayNameChanged,
        onSignIn = model::signIn,
        onSignUp = model::signUp,
        onGoogleSignIn = {
            val authorizeUrl = model.beginGoogleSignIn()
            if (authorizeUrl != null) {
                try {
                    context.startActivity(Intent(
                        Intent.ACTION_VIEW, Uri.parse(authorizeUrl)
                    ).addCategory(Intent.CATEGORY_BROWSABLE))
                } catch (_: Exception) {
                    model.googleLaunchFailed()
                }
            }
        },
        onSaveProfile = model::saveProfile,
        onSignOut = model::signOut,
        onSyncLibrary = model::syncLibrary,
        onSyncHistory = model::syncHistory,
        onRelink = model::relink,
        onDeleteCloudPlaylist = model::deleteRemotePlaylist,
        onDeleteCloudFavorite = model::deleteRemoteFavorite,
        onDemoModeChange = model::demoModeChanged,
        onRefreshAccount = model::refreshAccount,
        onRoleTools = { rolesOpen = true },
        onOpenDemoBilling = { billingOpen = true },
        onOpenDemoCenter = { demoCenterOpen = true }
    )
    val account = state.account
    if (demoCenterOpen) {
        Dialog(onDismissRequest = { demoCenterOpen = false }) {
            DemoCenterRoute(
                onClose = { demoCenterOpen = false },
                onOpenBilling = {
                    demoCenterOpen = false
                    billingOpen = true
                }
            )
        }
    }
    if (billingOpen) {
        // A guest/demo/online account has its own LOCAL simulation scope.
        // These records never enter Supabase or AccountIdentity.tier.
        val scope = account?.userId ?: "guest-offline-demo"
        var receipt by remember(scope) { mutableStateOf(billingStore.receipt(scope)) }
        var feedback by remember(scope) { mutableStateOf<String?>(null) }
        Dialog(onDismissRequest = { billingOpen = false }) {
            DemoBillingScreen(
                receipt = receipt,
                feedback = feedback,
                onPurchase = { plan, method, decline ->
                    runCatching {
                        billingStore.simulatePayment(scope, plan, method, decline)
                    }.onSuccess { result ->
                        receipt = billingStore.receipt(scope)
                        feedback = when (result) {
                            is DemoBillingResult.Success ->
                                "THÀNH CÔNG (giả lập). Đã lưu biên nhận ${result.receipt.id} trên thiết bị."
                            DemoBillingResult.Declined ->
                                "THẤT BẠI (giả lập): giao dịch bị từ chối; gói hiện tại giữ nguyên."
                        }
                    }.onFailure { error ->
                        feedback = error.message ?: "Không lưu được giao dịch mô phỏng."
                    }
                },
                onCancel = {
                    runCatching { billingStore.cancel(scope) }
                        .onSuccess {
                            receipt = billingStore.receipt(scope)
                            feedback = "Đã hủy gói Premium mô phỏng. Gói thật không thay đổi."
                        }.onFailure { feedback = it.message }
                },
                onReset = {
                    runCatching { billingStore.reset(scope) }
                        .onSuccess {
                            receipt = null
                            feedback = "Đã xóa biên nhận mô phỏng trên thiết bị."
                        }.onFailure { feedback = it.message }
                },
                onClose = { billingOpen = false }
            )
        }
    }
    if (rolesOpen && account != null && !state.demoSession) {
        val closeRoleTools = {
            rolesOpen = false
            model.refreshAccount()
        }
        Dialog(onDismissRequest = closeRoleTools) {
            AccountRolePanel(
                identity = account,
                onClose = closeRoleTools,
                rpc = model::roleRpc,
                upload = model::uploadCreatorFile,
                download = model::downloadCreatorFile
            )
        }
    }
}

@Composable
fun AccountScreen(
    state: AccountUiState,
    themeMode: String,
    onThemeModeChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit = {},
    onAuthModeChange: (AccountAuthMode) -> Unit = {},
    onContinueAsGuest: () -> Unit = {},
    onDisplayNameChange: (String) -> Unit,
    onSignIn: () -> Unit,
    onSignUp: () -> Unit,
    onGoogleSignIn: () -> Unit,
    onSaveProfile: () -> Unit,
    onSignOut: () -> Unit,
    onSyncLibrary: () -> Unit,
    onSyncHistory: () -> Unit,
    onRelink: (CloudPendingMatch, String) -> Unit,
    onDeleteCloudPlaylist: (String) -> Unit,
    onDeleteCloudFavorite: (String) -> Unit,
    onDemoModeChange: (Boolean) -> Unit = {},
    onRefreshAccount: () -> Unit = {},
    onRoleTools: () -> Unit = {},
    onOpenDemoBilling: () -> Unit = {},
    onOpenDemoCenter: () -> Unit = {}
) {
    var confirming by remember { mutableStateOf<Pair<String, String>?>(null) }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .testTag("account-scroll")
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OceanFlowHeader(
            eyebrow = "Ocean Flow · Không gian của bạn",
            title = "Tài khoản MusicFlow",
            subtitle = if (state.account == null) {
                "Nghe offline không cần đăng nhập. Khi cần, hãy lưu hồ sơ và đồng bộ."
            } else {
                "Gói thành viên, hồ sơ và công cụ theo vai trò của bạn."
            }
        )
        OceanFlowCard {
            Text("GÓC TRÌNH DIỄN", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary)
            OutlinedButton(
                onClick = onOpenDemoCenter,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .testTag("account-demo-center-open")
            ) { Text("Trình diễn 9 loại tài khoản (giả lập)") }
            Text("Xem nghiệp vụ Owner, quản trị, nghệ sĩ và các vai trò khác — không cần Internet.",
                style = MaterialTheme.typography.bodySmall)
            OutlinedButton(
                onClick = onOpenDemoBilling,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .testTag("account-demo-billing-open")
            ) { Text("Trải nghiệm thanh toán Premium (giả lập)") }
            Text("Chỉ mô phỏng, không thu tiền hoặc cấp Premium thật.",
                style = MaterialTheme.typography.bodySmall)
        }
        if (state.loading) {
            MusicFlowStatusPanel(
                kind = MusicFlowStatusKind.LOADING,
                title = "Đang xử lý tài khoản...",
                description = "Bạn vẫn có thể dùng nhạc offline mà không cần đăng nhập."
            )
        }
        if (state.account == null) {
            Text("Chào mừng bạn!", style = MaterialTheme.typography.titleLarge)
            OutlinedButton(
                onClick = onContinueAsGuest,
                modifier = Modifier.fillMaxWidth().testTag("account-guest")
            ) { Text("Nghe nhạc ngay, không cần đăng nhập") }
            Text(
                "Thư viện và các tệp nhạc offline của bạn vẫn dùng được khi chưa có tài khoản.",
                style = MaterialTheme.typography.bodySmall
            )
            Text("Đăng nhập và tạo tài khoản", style = MaterialTheme.typography.titleMedium)
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val compact = maxWidth < 360.dp ||
                    LocalDensity.current.fontScale >= 1.75f
                val choices: @Composable (Modifier) -> Unit = { item ->
                    FilterChip(
                        selected = !state.demoMode,
                        onClick = { onDemoModeChange(false) },
                        enabled = !state.loading,
                        label = { Text("Supabase") },
                        modifier = item.heightIn(min = 48.dp)
                            .testTag("account-auth-online-mode")
                    )
                    FilterChip(
                        selected = state.demoMode,
                        onClick = { onDemoModeChange(true) },
                        enabled = !state.loading,
                        label = { Text("Demo offline") },
                        modifier = item.heightIn(min = 48.dp)
                            .testTag("account-auth-demo-mode")
                    )
                }
                if (compact) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        choices(Modifier.fillMaxWidth())
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        choices(Modifier)
                    }
                }
            }
            if (state.demoMode) {
                Text("Chế độ Demo chỉ lưu tài khoản trên thiết bị. Không gửi email, " +
                    "không tạo phiên Supabase và không cấp quyền Premium/quản trị thật.",
                    modifier = Modifier.testTag("account-auth-demo-explanation"))
            }
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val compact = maxWidth < 360.dp ||
                    LocalDensity.current.fontScale >= 1.75f
                val signin = {
                    passwordVisible = false
                    confirmPasswordVisible = false
                    onAuthModeChange(AccountAuthMode.SIGN_IN)
                }
                val signup = {
                    passwordVisible = false
                    confirmPasswordVisible = false
                    onAuthModeChange(AccountAuthMode.SIGN_UP)
                }
                if (compact) {
                    // Two 200%-font labels cannot safely share one tab row.
                    // These retain the same test tags and selection semantics.
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = state.authMode == AccountAuthMode.SIGN_IN,
                            onClick = signin,
                            enabled = !state.loading,
                            label = { Text("Đăng nhập") },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .testTag("account-signin-tab")
                        )
                        FilterChip(
                            selected = state.authMode == AccountAuthMode.SIGN_UP,
                            onClick = signup,
                            enabled = !state.loading,
                            label = { Text("Tạo tài khoản") },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .testTag("account-signup-tab")
                        )
                    }
                } else {
                    PrimaryTabRow(
                        selectedTabIndex = if (state.authMode == AccountAuthMode.SIGN_IN) 0 else 1
                    ) {
                        Tab(
                            selected = state.authMode == AccountAuthMode.SIGN_IN,
                            onClick = signin,
                            enabled = !state.loading,
                            text = { Text("Đăng nhập") },
                            modifier = Modifier.testTag("account-signin-tab")
                        )
                        Tab(
                            selected = state.authMode == AccountAuthMode.SIGN_UP,
                            onClick = signup,
                            enabled = !state.loading,
                            text = { Text("Tạo tài khoản") },
                            modifier = Modifier.testTag("account-signup-tab")
                        )
                    }
                }
            }
            if (state.authMode == AccountAuthMode.SIGN_UP) {
                val confirmationNote = if (state.demoMode) {
                    "Tài khoản Demo được tạo ngay trên máy, không cần xác nhận email."
                } else when (state.emailConfirmationRequired) {
                    false -> "Không cần xác nhận email. Tài khoản được lưu trên Supabase."
                    true -> "Sau khi đăng ký, hãy kiểm tra hộp thư để xác nhận email trước khi đăng nhập."
                    null -> "Tài khoản được tạo trên Supabase. Có thể cần xác nhận email."
                }
                Text(
                    confirmationNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state.emailConfirmationRequired == true) {
                        MaterialTheme.colorScheme.error
                    } else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!state.configured && !state.demoMode) {
                Text("Chưa cấu hình Supabase Auth cho bản build này.",
                    color = MaterialTheme.colorScheme.error)
            } else {
                OutlinedTextField(
                    value = state.email, onValueChange = onEmailChange,
                    label = { Text("Email") },
                    placeholder = { Text("ten@example.com") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email, imeAction = ImeAction.Next
                    ),
                    isError = state.emailError != null,
                    supportingText = state.emailError?.let { message ->
                        { Text(message) }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("account-email")
                )
                OutlinedTextField(
                    value = state.password, onValueChange = onPasswordChange,
                    label = { Text("Mật khẩu") },
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { passwordVisible = !passwordVisible }) {
                            Text(if (passwordVisible) "Ẩn" else "Hiện")
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = if (state.authMode == AccountAuthMode.SIGN_UP) {
                            ImeAction.Next
                        } else ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        if (state.authMode == AccountAuthMode.SIGN_IN &&
                            !state.loading && (state.configured || state.demoMode)) onSignIn()
                    }),
                    isError = state.passwordError != null,
                    supportingText = {
                        Text(state.passwordError ?: "Bắt buộc — tối thiểu 6 ký tự")
                    },
                    modifier = Modifier.fillMaxWidth().testTag("account-password")
                )
                if (state.authMode == AccountAuthMode.SIGN_UP) {
                    OutlinedTextField(
                        value = state.confirmPassword,
                        onValueChange = onConfirmPasswordChange,
                        label = { Text("Nhập lại mật khẩu") },
                        visualTransformation = if (confirmPasswordVisible) {
                            VisualTransformation.None
                        } else PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton(onClick = {
                                confirmPasswordVisible = !confirmPasswordVisible
                            }) { Text(if (confirmPasswordVisible) "Ẩn" else "Hiện") }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password, imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            if (!state.loading && (state.configured || state.demoMode)) onSignUp()
                        }),
                        isError = state.confirmPasswordError != null,
                        supportingText = state.confirmPasswordError?.let { message ->
                            { Text(message) }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("account-confirm-password")
                    )
                }
                Button(
                    onClick = if (state.authMode == AccountAuthMode.SIGN_UP) onSignUp else onSignIn,
                    enabled = !state.loading && (state.configured || state.demoMode),
                    modifier = Modifier.fillMaxWidth().testTag("account-submit")
                ) {
                    Text(if (state.authMode == AccountAuthMode.SIGN_UP) {
                        "Tạo tài khoản"
                    } else "Đăng nhập")
                }
            }
            state.error?.let {
                MusicFlowStatusPanel(
                    kind = MusicFlowStatusKind.ERROR,
                    title = "Không thể xác thực tài khoản",
                    description = it
                )
            }
            state.message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            // Do not advertise a disabled Google sign-in: it has no usable workflow
            // in this demo. OAuth implementation stays dormant until officially enabled.
        } else {
            val identity = state.account
            val overview = AccountHomePlan.overview(identity, state.demoSession, BuildConfig.DEBUG)
            var profileExpanded by rememberSaveable(identity.userId) { mutableStateOf(false) }
            var cloudExpanded by rememberSaveable(identity.userId) { mutableStateOf(false) }
            Text(
                overview.greeting,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.testTag("account-home-greeting")
            )
            Text(identity.email, style = MaterialTheme.typography.bodySmall)
            Text("Gói tài khoản: ${overview.tierLabel}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.testTag("account-home-tier"))
            Text(overview.tierExplanation, style = MaterialTheme.typography.bodySmall)
            Text("Vai trò: ${overview.rolesLabel}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag("account-home-roles"))
            if (!state.demoSession) {
                Button(
                    onClick = onRoleTools,
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth().testTag("account-role-tools")
                ) { Text(overview.primaryAction ?: "Xem vai trò") }
                OutlinedButton(
                    onClick = onRefreshAccount,
                    enabled = !state.loading && !state.syncing,
                    modifier = Modifier.fillMaxWidth().testTag("account-home-refresh")
                ) { Text("Cập nhật gói và quyền") }
            }
            TextButton(
                onClick = onSignOut,
                enabled = !state.loading && !state.syncing,
                modifier = Modifier.testTag("account-home-signout")
            ) { Text("Đăng xuất") }
            state.error?.let {
                MusicFlowStatusPanel(
                    kind = MusicFlowStatusKind.ERROR,
                    title = "Không thể cập nhật tài khoản",
                    description = it
                )
            }
            state.message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            HorizontalDivider()
            OutlinedButton(
                onClick = { profileExpanded = !profileExpanded },
                modifier = Modifier.fillMaxWidth().testTag("account-profile-toggle")
            ) { Text(if (profileExpanded) "Thu gọn hồ sơ cá nhân" else "Chỉnh sửa hồ sơ cá nhân") }
            if (profileExpanded) {
                OutlinedTextField(
                    value = state.displayName,
                    onValueChange = onDisplayNameChange,
                    label = { Text("Tên hiển thị") },
                    modifier = Modifier.fillMaxWidth().testTag("account-profile-name")
                )
                Button(
                    onClick = onSaveProfile,
                    enabled = !state.loading &&
                        state.displayName.trim().length in 2..80 &&
                        state.displayName.trim() != identity.displayName.trim(),
                    modifier = Modifier.testTag("account-profile-save")
                ) { Text("Lưu hồ sơ") }
                Text("Tên cần từ 2 đến 80 ký tự. Nút Lưu chỉ bật khi có thay đổi.",
                    style = MaterialTheme.typography.bodySmall)
            }
            if (state.demoSession) {
                Text(
                    "Tài khoản Demo được lưu trên thiết bị, không dùng Supabase. " +
                        "Tài khoản này không có quyền Premium và không đồng bộ cloud.",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
            HorizontalDivider()
            OutlinedButton(
                onClick = { cloudExpanded = !cloudExpanded },
                modifier = Modifier.fillMaxWidth().testTag("account-cloud-toggle")
            ) { Text(if (cloudExpanded) "Thu gọn đồng bộ đám mây" else "Đồng bộ và bản sao đám mây") }
            if (cloudExpanded) {
            Text("Đồng bộ Playlist và Favorites (thủ công). Chỉ gửi tên bài, " +
                "nghệ sĩ và danh sách phát; không tải file nhạc lên cloud.")
            Button(
                onClick = onSyncLibrary,
                enabled = !state.loading && !state.syncing
            ) {
                Text(if (state.syncing) "Đang đồng bộ..." else "Đồng bộ thư viện")
            }
            if (state.syncing) CircularProgressIndicator()
            OutlinedButton(
                onClick = onSyncHistory,
                enabled = !state.loading && !state.syncing
            ) { Text("Đồng bộ lịch sử nghe") }
            Text("Đồng bộ thủ công: tên bài, nghệ sĩ, thời điểm nghe; " +
                "Free chỉ xem 30 ngày, Premium xem dữ liệu cloud còn lưu.",
                style = MaterialTheme.typography.bodySmall)
            state.syncSummary?.let { summary ->
                Text("Bài chưa liên kết: ${summary.pending.size}",
                    style = MaterialTheme.typography.titleMedium)
                summary.pending.take(30).forEach { pending ->
                    var expanded by remember(pending) { mutableStateOf(false) }
                    Text("${pending.title} — ${pending.artist}")
                    TextButton(onClick = { expanded = true }, enabled = !state.syncing) {
                        Text("Chọn bài trên thiết bị")
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = {
                        expanded = false
                    }) {
                        summary.deviceSongs.take(150).forEach { song ->
                            DropdownMenuItem(
                                text = { Text("${song.title} — ${song.artist}") },
                                onClick = {
                                    expanded = false
                                    onRelink(pending, song.uri.toString())
                                }
                            )
                        }
                    }
                }
                Text("Bản sao cloud (xóa không ảnh hưởng dữ liệu trên máy)",
                    style = MaterialTheme.typography.titleMedium)
                summary.cloudPlaylists.take(30).forEach { playlist ->
                    TextButton(
                        enabled = !state.syncing,
                        onClick = { confirming = "playlist" to playlist.id }
                    ) { Text("Xóa cloud: ${playlist.name}") }
                }
                summary.cloudFavorites.take(30).forEach { favorite ->
                    TextButton(
                        enabled = !state.syncing,
                        onClick = { confirming = "favorite" to favorite.key }
                    ) { Text("Xóa cloud: ${favorite.title}") }
                }
            }
            Text("Bài nhạc ở điện thoại khác chỉ được liên kết lại nếu có " +
                "một bài trùng duy nhất trên máy này. Không xóa Playlist local.")
            Text("Premium thật do máy chủ xác nhận; thanh toán demo chỉ là " +
                "mô phỏng cục bộ, không mở quyền cloud hay đồng bộ tự động.")
            }
            }
        }
        HorizontalDivider()
        Text("Giao diện", style = MaterialTheme.typography.titleMedium)
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val choices = listOf("system" to "Hệ thống", "dark" to "Tối", "light" to "Sáng")
            val stacked = maxWidth < 360.dp || LocalDensity.current.fontScale >= 1.35f
            if (stacked) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    choices.forEach { (mode, label) ->
                        FilterChip(
                            selected = themeMode == mode,
                            onClick = { onThemeModeChange(mode) },
                            label = { Text(label) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .testTag("account-theme-$mode")
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    choices.forEach { (mode, label) ->
                        FilterChip(
                            selected = themeMode == mode,
                            onClick = { onThemeModeChange(mode) },
                            label = { Text(label) },
                            modifier = Modifier.heightIn(min = 48.dp)
                                .testTag("account-theme-$mode")
                        )
                    }
                }
            }
        }
    }
    confirming?.let { (type, id) ->
        AlertDialog(
            onDismissRequest = { confirming = null },
            title = { Text("Xóa bản sao trên cloud?") },
            text = { Text("Thao tác này đánh dấu mục đã xóa trên Supabase; " +
                "bản lưu trên máy sẽ không bị xóa.") },
            confirmButton = {
                TextButton(onClick = {
                    if (type == "playlist") onDeleteCloudPlaylist(id)
                    else onDeleteCloudFavorite(id)
                    confirming = null
                }) { Text("Xóa trên cloud") }
            },
            dismissButton = {
                TextButton(onClick = { confirming = null }) { Text("Hủy") }
            }
        )
    }
}
