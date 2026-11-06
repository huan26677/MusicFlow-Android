package com.example.musicflow.core.account

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountTaskGuideTest {
    @Test fun listenerIsDirectedToApplicationNotStudio() {
        val hint = AccountTaskGuide.nextStep(AccountWorkspace.CREATOR, MusicFlowAccess.NONE)
        assertTrue(hint.contains("đăng ký"))
        assertFalse(hint.contains("kho tác phẩm"))
    }

    @Test fun approvedCreatorGetsWorkSteps() {
        val hint = AccountTaskGuide.nextStep(
            AccountWorkspace.CREATOR, MusicFlowAccess(roles = setOf("artist"))
        )
        assertTrue(hint.contains("kho tác phẩm"))
        assertTrue(hint.contains("chứng cứ quyền"))
    }

    @Test fun ownerGetsManagementAndOptionalOverview() {
        val hint = AccountTaskGuide.nextStep(
            AccountWorkspace.MANAGEMENT, MusicFlowAccess(roles = setOf("owner"))
        )
        assertTrue(hint.contains("Tìm tài khoản"))
        assertTrue(hint.contains("tổng quan"))
    }

    @Test fun moderatorDoesNotClaimPublishingRights() {
        val hint = AccountTaskGuide.nextStep(
            AccountWorkspace.MODERATION, MusicFlowAccess(roles = setOf("moderator"))
        )
        assertTrue(hint.contains("không đồng nghĩa xuất bản"))
    }

    @Test fun delegatedAdminOnlyGetsScopedWorkHint() {
        val hint = AccountTaskGuide.nextStep(
            AccountWorkspace.MANAGEMENT,
            MusicFlowAccess(roles = setOf("platform_admin"), permissions = setOf("manage_users"))
        )
        assertTrue(hint.contains("ủy nhiệm"))
    }

    @Test fun developerDescriptionNeverClaimsOwnerAccess() {
        val hint = AccountTaskGuide.nextStep(
            AccountWorkspace.DEVELOPER, MusicFlowAccess(roles = setOf("developer"))
        )
        assertTrue(hint.contains("không thay thế quyền Owner"))
    }
}
