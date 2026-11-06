package com.example.musicflow.core.account

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountWorkspacePlanTest {
    @Test fun guestLikeListenerAndPremiumOnlySeeCreatorArea() {
        val workspace = AccountWorkspacePlan.available(MusicFlowAccess.NONE, true)
        assertEquals(listOf(AccountWorkspace.CREATOR), workspace)
        assertEquals(AccountWorkspace.CREATOR,
            AccountWorkspacePlan.initial(MusicFlowAccess.NONE, false))
    }

    @Test fun delegatedAdminOnlySeesPermittedArea() {
        val scoped = MusicFlowAccess(
            roles = setOf("platform_admin"), permissions = setOf("review_creators")
        )
        assertEquals(listOf(AccountWorkspace.CREATOR, AccountWorkspace.MODERATION),
            AccountWorkspacePlan.available(scoped, false))
        assertEquals(AccountWorkspace.MODERATION,
            AccountWorkspacePlan.initial(scoped, false))
        assertFalse(AccountWorkspace.MANAGEMENT in AccountWorkspacePlan.available(scoped, false))
    }

    @Test fun ownerStartsWithManagementAndHasModeration() {
        val owner = MusicFlowAccess(roles = setOf("owner"))
        assertEquals(AccountWorkspace.MANAGEMENT, AccountWorkspacePlan.initial(owner, false))
        assertTrue(AccountWorkspace.MODERATION in AccountWorkspacePlan.available(owner, false))
    }

    @Test fun moderatorAndCreatorReceiveDifferentLandings() {
        val moderator = MusicFlowAccess(roles = setOf("moderator"))
        assertEquals(AccountWorkspace.MODERATION, AccountWorkspacePlan.initial(moderator, false))
        val creator = MusicFlowAccess(roles = setOf("artist"))
        assertEquals(listOf(AccountWorkspace.CREATOR), AccountWorkspacePlan.available(creator, false))
    }

    @Test fun developerToolsOnlyAvailableInDebugBuild() {
        val developer = MusicFlowAccess(roles = setOf("developer"))
        assertEquals(AccountWorkspace.DEVELOPER, AccountWorkspacePlan.initial(developer, true))
        assertEquals(listOf(AccountWorkspace.CREATOR), AccountWorkspacePlan.available(developer, false))
    }

    @Test fun revokedAccessDoesNotRetainOldWorkspace() {
        val admin = MusicFlowAccess(roles = setOf("platform_admin"),
            permissions = setOf("manage_users"))
        assertTrue(AccountWorkspace.MANAGEMENT in AccountWorkspacePlan.available(admin, false))
        assertFalse(AccountWorkspace.MANAGEMENT in AccountWorkspacePlan.available(MusicFlowAccess.NONE, false))
    }

    @Test fun roleLabelsAndPrimaryActionsAreAudienceFriendly() {
        assertEquals("Chủ nền tảng", AccountWorkspacePlan.roleLabel("owner"))
        assertEquals("Quản trị viên", AccountWorkspacePlan.roleLabel("platform_admin"))
        assertEquals("Kiểm duyệt viên", AccountWorkspacePlan.roleLabel("moderator"))
        assertEquals("Đăng ký nghệ sĩ / tác giả", AccountWorkspacePlan.primaryAction(
            MusicFlowAccess.NONE, false))
        assertEquals("Mở khu vực kiểm duyệt", AccountWorkspacePlan.primaryAction(
            MusicFlowAccess(roles = setOf("moderator")), false))
    }
}
