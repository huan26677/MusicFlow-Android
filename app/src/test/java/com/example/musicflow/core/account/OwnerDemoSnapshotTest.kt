package com.example.musicflow.core.account

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OwnerDemoSnapshotTest {
    private fun summary(
        owners: Int = 1,
        admins: Int = 0,
        mods: Int = 0,
        artists: Int = 0
    ) = OwnerDemoSnapshot(
        ownerCount = owners,
        userCount = 3,
        platformAdmins = admins,
        moderators = mods,
        artists = artists,
        authors = 0,
        developers = 0,
        pendingCreatorRequests = 0,
        pendingDrafts = 0,
        approvedMetadata = 0,
        uploadedPrivateFiles = 0
    )

    @Test fun oneVerifiedOwnerAndNoTestActorsMeansGuidanceNeeded() {
        val snapshot = summary()
        assertTrue(snapshot.hasExactlyOneOwner)
        assertTrue(snapshot.needsTestActors)
    }

    @Test fun rolesReadyForDemoDoNotImplyPublishingOrProduction() {
        val snapshot = summary(admins = 1, mods = 1, artists = 1)
        assertTrue(snapshot.hasExactlyOneOwner)
        assertFalse(snapshot.needsTestActors)
    }

    @Test fun missingOrDuplicateOwnerIsNotHealthy() {
        assertFalse(summary(owners = 0).hasExactlyOneOwner)
        assertFalse(summary(owners = 2).hasExactlyOneOwner)
    }
}
