package com.example.musicflow.feature.account

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
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
        onRoleTools = { rolesOpen = true }
    )
    val account = state.account
    if (rolesOpen && account != null && !state.demoSession) {
        Dialog(onDismissRequest = { rolesOpen = false }) {
            AccountRolePanel(
                identity = account,
                onClose = { rolesOpen = false },
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
    onRoleTools: () -> Unit = {}
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
        Text("Tài khoản MusicFlow", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Đăng nhập để lưu hồ sơ và đồng bộ dữ liệu với Supabase. " +
                "Bạn vẫn nghe nhạc offline bình thường khi chưa có tài khoản.",
            style = MaterialTheme.typography.bodyMedium
        )
        if (state.loading) {
            MusicFlowStatusPanel(
                kind = MusicFlowStatusKind.LOADING,
                title = "Đang xử lý tài khoản...",
                description = "Bạn vẫn có thể dùng nhạc offline mà không cần đăng nhập."
            )
        }
        if (state.account == null) {
            Text("Chào mừng bạn!", style = MaterialTheme.typography.titleLarge)
            PrimaryTabRow(
                selectedTabIndex = if (state.authMode == AccountAuthMode.SIGN_IN) 0 else 1
            ) {
                Tab(
                    selected = state.authMode == AccountAuthMode.SIGN_IN,
                    onClick = {
                        passwordVisible = false
                        confirmPasswordVisible = false
                        onAuthModeChange(AccountAuthMode.SIGN_IN)
                    },
                    enabled = !state.loading,
                    text = { Text("Đăng nhập") },
                    modifier = Modifier.testTag("account-signin-tab")
                )
                Tab(
                    selected = state.authMode == AccountAuthMode.SIGN_UP,
                    onClick = {
                        passwordVisible = false
                        confirmPasswordVisible = false
                        onAuthModeChange(AccountAuthMode.SIGN_UP)
                    },
                    enabled = !state.loading,
                    text = { Text("Tạo tài khoản") },
                    modifier = Modifier.testTag("account-signup-tab")
                )
            }
            if (state.authMode == AccountAuthMode.SIGN_UP) {
                val confirmationNote = when (state.emailConfirmationRequired) {
                    false -> "Không cần xác nhận email. Tài khoản được lưu trên Supabase."
                    true -> "Supabase đang yêu cầu xác nhận email. Bạn cần tắt " +
                        "Confirm email trong Supabase trước khi đăng ký không cần thư."
                    null -> "Đăng ký bằng Supabase. Vui lòng kiểm tra kết nối mạng."
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
                        isError = state.confirmPasswordError != null,
                        supportingText = state.confirmPasswordError?.let { message ->
                            { Text(message) }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("account-confirm-password")
                    )
                }
                Button(
                    onClick = if (state.authMode == AccountAuthMode.SIGN_UP) onSignUp else onSignIn,
                    enabled = !state.loading && state.configured,
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
            HorizontalDivider()
            Text("Hoặc", style = MaterialTheme.typography.bodySmall)
            OutlinedButton(
                onClick = onGoogleSignIn,
                enabled = false,  // Reserved for a later release; never start OAuth here.
                modifier = Modifier.fillMaxWidth().testTag("account-google-disabled")
            ) { Text("Tiếp tục với Google") }
            Text(
                "Đăng nhập Google chưa hoạt động trong phiên bản này.",
                style = MaterialTheme.typography.bodySmall
            )
            TextButton(
                onClick = onContinueAsGuest,
                modifier = Modifier.fillMaxWidth().testTag("account-guest")
            ) { Text("Tiếp tục nghe nhạc không đăng nhập") }
        } else {
            val identity = state.account
            Text(
                if (state.demoSession) "Tài khoản Demo — Free"
                else if (identity.tier == "premium") "Premium" else "Free",
                style = MaterialTheme.typography.titleLarge
            )
            Text(identity.email)
            OutlinedTextField(
                value = state.displayName,
                onValueChange = onDisplayNameChange,
                label = { Text("Tên hiển thị") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = onSaveProfile, enabled = !state.loading) {
                Text("Lưu hồ sơ")
            }
            if (!state.demoSession) {
                HorizontalDivider()
                Text("Vai trò và quyền truy cập", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Gói ${identity.tier.uppercase()} — quyền quản trị là quyền riêng " +
                        "do Supabase cấp, không đi kèm Premium.",
                    style = MaterialTheme.typography.bodySmall
                )
                if (identity.access.roles.isNotEmpty()) {
                    Text(
                        "Vai trò: ${identity.access.roles.joinToString(", ")}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                OutlinedButton(
                    onClick = onRoleTools,
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth().testTag("account-role-tools")
                ) {
                    Text(
                        if (identity.access.isOwner || identity.access.isPlatformAdmin) {
                            "Quản trị nền tảng và vai trò"
                        } else if (identity.access.isCreator) {
                            "Hồ sơ nghệ sĩ / tác giả"
                        } else "Đăng ký nghệ sĩ / tác giả và xem quyền"
                    )
                }
                if (BuildConfig.DEBUG && identity.access.isDeveloper) {
                    OutlinedButton(
                        onClick = onRoleTools,
                        modifier = Modifier.fillMaxWidth().testTag("account-developer-shortcut")
                    ) { Text("Công cụ nhà phát triển (QA)") }
                }
            }
            if (state.demoSession) {
                Text(
                    "Tài khoản Demo được lưu trên thiết bị, không dùng Supabase. " +
                        "Tài khoản này không có quyền Premium và không đồng bộ cloud.",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
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
            Text("Premium do máy chủ xác nhận. Chưa có thanh toán, giới hạn " +
                "thiết bị hoặc đồng bộ tự động.")
            }
            TextButton(
                onClick = onSignOut,
                enabled = !state.loading && !state.syncing
            ) {
                Text("Đăng xuất")
            }
        }
        if (state.account != null) {
            state.error?.let {
                MusicFlowStatusPanel(
                    kind = MusicFlowStatusKind.ERROR,
                    title = "Thao tác tài khoản không thành công",
                    description = it
                )
            }
            state.message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
        HorizontalDivider()
        Text("Giao diện", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("system" to "Hệ thống", "dark" to "Tối", "light" to "Sáng")
                .forEach { (mode, label) ->
                    FilterChip(
                        selected = themeMode == mode,
                        onClick = { onThemeModeChange(mode) },
                        label = { Text(label) }
                    )
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
