package com.example.musicflow.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.example.musicflow.core.account.CloudPendingMatch
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun AccountRoute(
    themeMode: String,
    onThemeModeChange: (String) -> Unit
) {
    val context = LocalContext.current
    val factory = remember(context) {
        AccountViewModel.factory(context.applicationContext)
    }
    val model: AccountViewModel = viewModel(factory = factory)
    val state by model.state.collectAsStateWithLifecycle()

    AccountScreen(
        state, themeMode, onThemeModeChange,
        onEmailChange = model::emailChanged,
        onPasswordChange = model::passwordChanged,
        onDisplayNameChange = model::displayNameChanged,
        onSignIn = model::signIn,
        onSignUp = model::signUp,
        onSaveProfile = model::saveProfile,
        onSignOut = model::signOut,
        onSyncLibrary = model::syncLibrary,
        onSyncHistory = model::syncHistory,
        onRelink = model::relink,
        onDeleteCloudPlaylist = model::deleteRemotePlaylist,
        onDeleteCloudFavorite = model::deleteRemoteFavorite
    )
}

@Composable
fun AccountScreen(
    state: AccountUiState,
    themeMode: String,
    onThemeModeChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onSignIn: () -> Unit,
    onSignUp: () -> Unit,
    onSaveProfile: () -> Unit,
    onSignOut: () -> Unit,
    onSyncLibrary: () -> Unit,
    onSyncHistory: () -> Unit,
    onRelink: (CloudPendingMatch, String) -> Unit,
    onDeleteCloudPlaylist: (String) -> Unit,
    onDeleteCloudFavorite: (String) -> Unit
) {
    var confirming by remember { mutableStateOf<Pair<String, String>?>(null) }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Tài khoản", style = MaterialTheme.typography.headlineMedium)
        if (state.loading) CircularProgressIndicator()
        if (state.account == null) {
            Text("Khách — Miễn phí", style = MaterialTheme.typography.titleMedium)
            Text("Không cần đăng nhập để nghe nhạc offline, dùng Playlist, " +
                "Lyrics, Equalizer và các tính năng trên thiết bị.")
            if (!state.configured) {
                Text("Chưa cấu hình Supabase Auth cho bản build này.",
                    color = MaterialTheme.colorScheme.error)
            } else {
                OutlinedTextField(
                    value = state.email, onValueChange = onEmailChange,
                    label = { Text("Email") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.password, onValueChange = onPasswordChange,
                    label = { Text("Mật khẩu") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onSignIn, enabled = !state.loading) {
                        Text("Đăng nhập")
                    }
                    OutlinedButton(onClick = onSignUp, enabled = !state.loading) {
                        Text("Đăng ký Free")
                    }
                }
            }
        } else {
            val identity = state.account
            Text(
                if (identity.tier == "premium") "Premium" else "Free",
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
            TextButton(
                onClick = onSignOut,
                enabled = !state.loading && !state.syncing
            ) {
                Text("Đăng xuất")
            }
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        HorizontalDivider()
        Text("Giao diện", style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "system" to "Hệ thống",
                "dark" to "Tối",
                "light" to "Sáng"
            ).forEach { (mode, label) ->
                FilterChip(
                    selected = themeMode == mode,
                    onClick = { onThemeModeChange(mode) },
                    label = { Text(label) }
                )
            }
        }
        Text("Giao diện và nhạc offline vẫn dùng được khi không đăng nhập.",
            style = MaterialTheme.typography.bodySmall)
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
