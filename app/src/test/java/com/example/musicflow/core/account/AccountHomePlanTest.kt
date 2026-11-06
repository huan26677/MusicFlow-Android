package com.example.musicflow.core.account

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountHomePlanTest {
    private fun identity(
        name: String = "",
        tier: String = "free",
        access: MusicFlowAccess = MusicFlowAccess.NONE
    ) = AccountIdentity("qa-user", "listener@example.invalid", name, tier, null, access)

    @Test fun freeListenerSeesSimpleWelcomeAndNoAdminClaims() {
        val home = AccountHomePlan.overview(identity(), false, false)
        assertEquals("Xin chào, listener", home.greeting)
        assertEquals("Free", home.tierLabel)
        assertEquals("Người nghe", home.rolesLabel)
        assertEquals("Đăng ký nghệ sĩ / tác giả", home.primaryAction)
        assertFalse(home.tierExplanation.contains("quản trị"))
    }

    @Test fun premiumDoesNotGrantAdministrativeOrCreatorPrivilege() {
        val home = AccountHomePlan.overview(identity(name = "Bạn QA", tier = "premium"), false, false)
        assertEquals("Xin chào, Bạn QA", home.greeting)
        assertEquals("Premium", home.tierLabel)
        assertEquals("Người nghe", home.rolesLabel)
        assertEquals("Đăng ký nghệ sĩ / tác giả", home.primaryAction)
    }

    @Test fun demoIsClearlyOfflineOnlyWithoutPrivilegedLinks() {
        val home = AccountHomePlan.overview(identity(tier = "premium", access =
            MusicFlowAccess(setOf("owner"))), true, true)
        assertEquals("Demo trên thiết bị", home.tierLabel)
        assertEquals("Người nghe (Demo)", home.rolesLabel)
        assertNull(home.primaryAction)
        assertTrue(home.tierExplanation.contains("Không đồng bộ Supabase"))
    }

    @Test fun ownerGetsFriendlyLabelsAndManagementShortcut() {
        val home = AccountHomePlan.overview(identity(name = "Owner QA", access =
            MusicFlowAccess(setOf("owner"))), false, false)
        assertEquals("Chủ nền tảng", home.rolesLabel)
        assertEquals("Mở khu vực quản trị", home.primaryAction)
    }

    @Test fun reviewerAndArtistSeeTheirActualTask() {
        val reviewer = AccountHomePlan.overview(identity(access = MusicFlowAccess(
            setOf("platform_admin"), setOf("review_creators"))), false, false)
        assertEquals("Mở khu vực kiểm duyệt", reviewer.primaryAction)
        val artist = AccountHomePlan.overview(identity(access = MusicFlowAccess(
            setOf("artist"))), false, false)
        assertEquals("Nghệ sĩ", artist.rolesLabel)
        assertEquals("Mở hồ sơ nghệ sĩ / tác giả", artist.primaryAction)
    }

    @Test fun unverifiedRoleDisplaysNoPreviousPrivilege() {
        val original = identity(access = MusicFlowAccess(setOf("owner")))
        assertEquals("Mở khu vực quản trị", AccountHomePlan.overview(original, false, false).primaryAction)
        val unverified = original.copy(access = MusicFlowAccess.NONE, tier = "free")
        val home = AccountHomePlan.overview(unverified, false, false)
        assertEquals("Người nghe", home.rolesLabel)
        assertEquals("Đăng ký nghệ sĩ / tác giả", home.primaryAction)
    }
}
