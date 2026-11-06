package com.example.musicflow.core.account

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountReviewPlanTest {
    @Test fun guestPremiumAndArtistDoNotSeeReviewAreas() {
        assertEquals(emptyList<AccountReviewArea>(), AccountReviewPlan.available(MusicFlowAccess.NONE))
        assertEquals(emptyList<AccountReviewArea>(), AccountReviewPlan.available(
            MusicFlowAccess(roles = setOf("artist"))))
        assertNull(AccountReviewPlan.active(MusicFlowAccess.NONE, AccountReviewArea.CATALOG))
    }

    @Test fun metadataOnlyModeratorCanReviewDraftsNotCreatorOrCatalog() {
        val moderator = MusicFlowAccess(roles = setOf("moderator"))
        assertEquals(listOf(AccountReviewArea.DRAFTS), AccountReviewPlan.available(moderator))
        assertEquals(AccountReviewArea.DRAFTS,
            AccountReviewPlan.active(moderator, AccountReviewArea.CATALOG))
    }

    @Test fun scopedAdminGetsCreatorAndMetadataReviewButNotCatalog() {
        val admin = MusicFlowAccess(
            roles = setOf("platform_admin"), permissions = setOf("review_creators")
        )
        assertEquals(listOf(AccountReviewArea.APPLICATIONS, AccountReviewArea.DRAFTS),
            AccountReviewPlan.available(admin))
    }

    @Test fun ownerCanSwitchBetweenAllReviewTasks() {
        val owner = MusicFlowAccess(roles = setOf("owner"))
        assertEquals(listOf(
            AccountReviewArea.APPLICATIONS, AccountReviewArea.DRAFTS, AccountReviewArea.CATALOG
        ), AccountReviewPlan.available(owner))
        assertEquals(AccountReviewArea.CATALOG,
            AccountReviewPlan.active(owner, AccountReviewArea.CATALOG))
    }

    @Test fun revokedPermissionNeverKeepsPreviouslySelectedReviewTask() {
        val granted = MusicFlowAccess(roles = setOf("platform_admin"),
            permissions = setOf("review_creators", "moderate_catalog"))
        assertTrue(AccountReviewArea.CATALOG in AccountReviewPlan.available(granted))
        val revoked = granted.copy(permissions = setOf("review_creators"))
        assertFalse(AccountReviewArea.CATALOG in AccountReviewPlan.available(revoked))
        assertEquals(AccountReviewArea.APPLICATIONS,
            AccountReviewPlan.active(revoked, AccountReviewArea.CATALOG))
    }

    @Test fun permissionsShowReadableLabelsInsteadOfTechnicalCodes() {
        assertEquals("Quản lý người dùng", AccountReviewPlan.permissionLabel("manage_users"))
        assertEquals("Duyệt Creator", AccountReviewPlan.permissionLabel("review_creators"))
        assertEquals("Kiểm duyệt nhạc online", AccountReviewPlan.permissionLabel("moderate_catalog"))
    }
}
