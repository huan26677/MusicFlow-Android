package com.example.musicflow.core.account

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicFlowAccessTest {
    @Test fun guestAndFreeNeverHaveStaffRights() {
        val empty = MusicFlowAccess.NONE
        assertFalse(empty.isOwner)
        assertFalse(empty.canManageUsers)
        assertFalse(empty.isDeveloper)
        assertFalse(empty.canModerateCatalog)
    }

    @Test fun developerDoesNotInheritAdminPower() {
        val access = MusicFlowAccess(roles = setOf("developer"))
        assertTrue(access.isDeveloper)
        assertFalse(access.isOwner)
        assertFalse(access.canManageUsers)
        assertFalse(access.canReviewCreators)
    }

    @Test fun collaboratorRequiresExplicitPermission() {
        val bare = MusicFlowAccess(roles = setOf("platform_admin"))
        assertFalse(bare.canManageUsers)
        assertFalse(bare.canReviewCreators)
        val scoped = bare.copy(permissions = setOf("review_creators"))
        assertTrue(scoped.canReviewCreators)
        assertFalse(scoped.canManageUsers)
    }

    @Test fun permissionsWithoutPlatformAdminRoleHaveNoEffect() {
        val forged = MusicFlowAccess(permissions = setOf("manage_users"))
        assertFalse(forged.canManageUsers)
    }

    @Test fun ownerHasBusinessAdministrationButNotDeveloperRole() {
        val owner = MusicFlowAccess(roles = setOf("owner"))
        assertTrue(owner.canManageUsers)
        assertTrue(owner.canModerateCatalog)
        assertTrue(owner.canReviewCreators)
        assertFalse(owner.isDeveloper)
    }

    @Test fun artistOrAuthorIsNotAdmin() {
        val artist = MusicFlowAccess(roles = setOf("artist"))
        assertTrue(artist.isCreator)
        assertFalse(artist.canManageUsers)
    }
}
