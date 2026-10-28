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
        onSignOut = model::signOut
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
    onSignOut: () -> Unit
) {
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
            Text("Quyền Premium do máy chủ xác nhận. Chưa triển khai " +
                "thu phí, Google Play Billing hoặc đồng bộ đa thiết bị.")
            TextButton(onClick = onSignOut, enabled = !state.loading) {
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
}
