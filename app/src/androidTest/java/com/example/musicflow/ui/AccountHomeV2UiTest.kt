package com.example.musicflow.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.musicflow.core.account.AccountIdentity
import com.example.musicflow.core.account.MusicFlowAccess
import com.example.musicflow.feature.account.AccountScreen
import com.example.musicflow.feature.account.AccountUiState
import com.example.musicflow.ui.theme.MusicFlowTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Pure UI fixtures; real passwords, sessions, Supabase and uploads are not used. */
@RunWith(AndroidJUnit4::class)
class AccountHomeV2UiTest {
    @get:Rule val compose = createComposeRule()

    private fun identity(
        tier: String = "free",
        access: MusicFlowAccess = MusicFlowAccess.NONE
    ) = AccountIdentity(
        userId = "00000000-0000-4000-8000-000000000045",
        email = "listener@example.invalid",
        displayName = "QA MusicFlow",
        tier = tier,
        expiresAt = null,
        access = access
    )

    private fun render(
        state: AccountUiState,
        onGuest: () -> Unit = {},
        onRole: () -> Unit = {},
        onRefresh: () -> Unit = {}
    ) {
        compose.setContent {
            MusicFlowTheme {
                AccountScreen(
                    state = state,
                    themeMode = "system",
                    onThemeModeChange = {},
                    onEmailChange = {},
                    onPasswordChange = {},
                    onDisplayNameChange = {},
                    onSignIn = {},
                    onSignUp = {},
                    onGoogleSignIn = {},
                    onSaveProfile = {},
                    onSignOut = {},
                    onSyncLibrary = {},
                    onSyncHistory = {},
                    onRelink = { _, _ -> },
                    onDeleteCloudPlaylist = {},
                    onDeleteCloudFavorite = {},
                    onContinueAsGuest = onGuest,
                    onRoleTools = onRole,
                    onRefreshAccount = onRefresh
                )
            }
        }
    }

    @Test fun guestCanChooseOfflineListeningBeforeSignIn() {
        var count = 0
        render(AccountUiState(loading = false), onGuest = { count++ })
        compose.onNodeWithTag("account-guest").assertExists().performClick()
        assertEquals(1, count)
        compose.onNodeWithTag("account-signin-tab").assertExists()
        compose.onNodeWithTag("account-home-tier").assertDoesNotExist()
        compose.onNodeWithTag("account-google-disabled").assertDoesNotExist()
    }

    @Test fun accountHidesProjectDemoUntilUserExplicitlyRequestsIt() {
        render(AccountUiState(loading = false))
        compose.onNodeWithTag("account-guest").assertExists()
        compose.onNodeWithTag("account-demo-tools-toggle").assertExists()
        compose.onNodeWithTag("account-demo-center-open").assertDoesNotExist()
        compose.onNodeWithTag("account-demo-billing-open").assertDoesNotExist()
        compose.onNodeWithTag("account-demo-tools-toggle").performScrollTo().performClick()
        compose.onNodeWithTag("account-demo-center-open").assertExists()
        compose.onNodeWithTag("account-demo-billing-open").assertExists()
        compose.onNodeWithTag("account-demo-tools-toggle").performScrollTo().performClick()
        compose.onNodeWithTag("account-demo-center-open").assertDoesNotExist()
        compose.onNodeWithTag("account-demo-billing-open").assertDoesNotExist()
    }

    @Test fun signupShowsEmailConfirmationInstructionsWithoutAdminSettings() {
        render(AccountUiState(
            loading = false,
            authMode = com.example.musicflow.feature.account.AccountAuthMode.SIGN_UP,
            emailConfirmationRequired = true
        ))
        compose.onNodeWithText("Sau khi đăng ký, hãy kiểm tra hộp thư", substring = true)
            .assertExists()
        compose.onNodeWithText("Confirm email", substring = true).assertDoesNotExist()
    }

    @Test fun freeListenerSeesCompactActionsAndOpensDetailsOnDemand() {
        var roleCalls = 0
        var refreshCalls = 0
        render(AccountUiState(loading = false, account = identity()),
            onRole = { roleCalls++ }, onRefresh = { refreshCalls++ })
        compose.onNodeWithText("Gói tài khoản: Free").assertExists()
        compose.onNodeWithText("Vai trò: Người nghe").assertExists()
        compose.onNodeWithTag("account-profile-name").assertDoesNotExist()
        compose.onNodeWithText("Đồng bộ thư viện").assertDoesNotExist()
        compose.onNodeWithTag("account-role-tools").performScrollTo().performClick()
        compose.onNodeWithTag("account-home-refresh").performScrollTo().performClick()
        assertEquals(1, roleCalls)
        assertEquals(1, refreshCalls)
        compose.onNodeWithTag("account-profile-toggle").performScrollTo().performClick()
        compose.onNodeWithTag("account-profile-name").assertExists()
        compose.onNodeWithTag("account-profile-save").assertExists()
        compose.onNodeWithTag("account-cloud-toggle").performScrollTo().performClick()
        compose.onNodeWithText("Đồng bộ thư viện").assertExists()
        compose.onNodeWithTag("account-home-signout").assertExists()
    }

    @Test fun premiumNeverShowsAdminToolsWithoutBackendRole() {
        render(AccountUiState(loading = false, account = identity(tier = "premium")))
        compose.onNodeWithText("Gói tài khoản: Premium").assertExists()
        compose.onNodeWithText("Vai trò: Người nghe").assertExists()
        compose.onNodeWithText("Mở khu vực quản trị").assertDoesNotExist()
        compose.onNodeWithText("Đăng ký nghệ sĩ / tác giả").assertExists()
    }

    @Test fun ownerAndModeratorHaveDifferentRelevantActions() {
        render(AccountUiState(loading = false,
            account = identity(access = MusicFlowAccess(setOf("owner")))))
        compose.onNodeWithText("Vai trò: Chủ nền tảng").assertExists()
        compose.onNodeWithText("Mở khu vực quản trị").assertExists()
    }

    @Test fun moderatorSeesReviewShortcutWithoutAdminClaim() {
        render(AccountUiState(loading = false,
            account = identity(access = MusicFlowAccess(setOf("moderator")))))
        compose.onNodeWithText("Vai trò: Kiểm duyệt viên").assertExists()
        compose.onNodeWithText("Mở khu vực kiểm duyệt").assertExists()
        compose.onNodeWithText("Mở khu vực quản trị").assertDoesNotExist()
    }

    @Test fun approvedArtistSeesCreatorShortcut() {
        render(AccountUiState(loading = false,
            account = identity(access = MusicFlowAccess(setOf("artist")))))
        compose.onNodeWithText("Vai trò: Nghệ sĩ").assertExists()
        compose.onNodeWithText("Mở hồ sơ nghệ sĩ / tác giả").assertExists()
    }

    @Test fun developerSeesDebugQaShortcutOnly() {
        render(AccountUiState(loading = false,
            account = identity(access = MusicFlowAccess(setOf("developer")))))
        compose.onNodeWithText("Vai trò: Nhà phát triển").assertExists()
        compose.onNodeWithText("Mở công cụ QA").assertExists()
        compose.onNodeWithText("Mở khu vực quản trị").assertDoesNotExist()
    }

    @Test fun demoSessionKeepsOfflineOnlyTools() {
        render(AccountUiState(loading = false, demoSession = true,
            account = identity(tier = "premium", access = MusicFlowAccess(setOf("owner")))))
        compose.onNodeWithText("Gói tài khoản: Demo trên thiết bị").assertExists()
        compose.onNodeWithTag("account-role-tools").assertDoesNotExist()
        compose.onNodeWithTag("account-home-refresh").assertDoesNotExist()
        compose.onNodeWithTag("account-cloud-toggle").assertDoesNotExist()
        compose.onNodeWithTag("account-home-signout").assertExists()
        compose.onNodeWithTag("account-profile-toggle").assertExists()
    }
}
