package com.example.musicflow.core.account

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OwnerDemoSnapshotTest {
    private fun summary(
        owners: Int = 1,
        admins: Int = 0,
        mods: Int = 0,
        artists: Int = 0,
        authors: Int = 0,
        developers: Int = 0,
        delegatedAdmins: Int = 0,
        activePremium: Int = 0,
        nonPremium: Int = 3,
        pendingCreators: Int = 0
    ) = OwnerDemoSnapshot(
        ownerCount = owners,
        userCount = 3,
        platformAdmins = admins,
        moderators = mods,
        artists = artists,
        authors = authors,
        developers = developers,
        pendingCreatorRequests = pendingCreators,
        pendingDrafts = 0,
        approvedMetadata = 0,
        uploadedPrivateFiles = 0,
        activePremiumUsers = activePremium,
        nonPremiumUsers = nonPremium,
        fullyDelegatedAdmins = delegatedAdmins
    )

    @Test fun oneVerifiedOwnerAndNoTestActorsMeansGuidanceNeeded() {
        val snapshot = summary()
        assertTrue(snapshot.hasExactlyOneOwner)
        assertTrue(snapshot.needsTestActors)
    }

    @Test fun rolesReadyForDemoDoNotImplyPublishingOrProduction() {
        val snapshot = summary(admins = 1, mods = 1, artists = 1, authors = 1,
            developers = 1, delegatedAdmins = 1, activePremium = 1, pendingCreators = 1)
        assertTrue(snapshot.hasExactlyOneOwner)
        assertFalse(snapshot.needsTestActors)
        assertTrue(snapshot.hasDemoSubscriptionExamples)
        assertTrue(snapshot.hasPendingCreatorExample)
    }

    @Test fun platformAdminWithoutThreePermissionsIsNotReady() {
        assertTrue(summary(admins = 1, mods = 1, artists = 1, authors = 1,
            developers = 1, delegatedAdmins = 0).needsTestActors)
    }

    @Test fun missingTierAndPendingExamplesAreReportedIndependently() {
        val snapshot = summary(admins = 1, mods = 1, artists = 1, authors = 1,
            developers = 1, delegatedAdmins = 1)
        assertFalse(snapshot.needsTestActors)
        assertFalse(snapshot.hasDemoSubscriptionExamples)
        assertFalse(snapshot.hasPendingCreatorExample)
    }

    @Test fun missingOrDuplicateOwnerIsNotHealthy() {
        assertFalse(summary(owners = 0).hasExactlyOneOwner)
        assertFalse(summary(owners = 2).hasExactlyOneOwner)
    }
}
