package com.example.musicflow.core.online

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnlineRightsPolicyTest {
    private fun verified() = OnlineTrackRights(
        licenseCode = "CC-BY-4.0",
        licenseUrl = "https://creativecommons.org/licenses/by/4.0/",
        sourceUrl = "https://example.com/tracks/123",
        attribution = "Artist — Track, CC BY 4.0",
        rightsConfirmed = true,
        streamingAllowed = true,
        verifiedAt = "2026-10-29T10:00:00Z"
    )

    @Test fun completeRightsPermitStreaming() {
        assertTrue(OnlineRightsPolicy.canStream(verified()))
        assertEquals(OnlineRightsStatus.VERIFIED_STREAMING,
            OnlineRightsPolicy.streamingStatus(verified()))
    }

    @Test fun missingRightsDefaultToDenied() {
        assertFalse(OnlineRightsPolicy.canStream(null))
        assertFalse(OnlineRightsPolicy.canStream(OnlineTrackRights()))
    }

    @Test fun booleanClaimsWithoutReviewDateDoNotGrantRights() {
        assertEquals(OnlineRightsStatus.NOT_VERIFIED,
            OnlineRightsPolicy.streamingStatus(verified().copy(verifiedAt = null)))
        assertFalse(OnlineRightsPolicy.canStream(verified().copy(rightsConfirmed = false)))
    }

    @Test fun streamingIsSeparateFromDownloadAndCommercialPermission() {
        assertTrue(OnlineRightsPolicy.canStream(
            verified().copy(downloadAllowed = false, commercialUseAllowed = false)))
        assertEquals(OnlineRightsStatus.STREAMING_NOT_ALLOWED,
            OnlineRightsPolicy.streamingStatus(
                verified().copy(streamingAllowed = false, downloadAllowed = true)))
    }

    @Test fun missingAttributionOrSourceBlocksPlayback() {
        assertEquals(OnlineRightsStatus.INCOMPLETE_CREDITS,
            OnlineRightsPolicy.streamingStatus(verified().copy(attribution = " ")))
        assertFalse(OnlineRightsPolicy.canStream(verified().copy(sourceUrl = null)))
    }

    @Test fun requiredCcLicenseLinkMustBePresent() {
        assertFalse(OnlineRightsPolicy.canStream(verified().copy(licenseUrl = null)))
    }

    @Test fun cc0StillRequiresCatalogSourceAndAttributionAsAppPolicy() {
        assertTrue(OnlineRightsPolicy.canStream(verified().copy(
            licenseCode = "CC0-1.0",
            licenseUrl = "https://creativecommons.org/publicdomain/zero/1.0/"
        )))
    }

    @Test fun directPermissionAllowsPrivateEvidenceReference() {
        assertTrue(OnlineRightsPolicy.canStream(verified().copy(
            licenseCode = "DIRECT", licenseUrl = null)))
    }

    @Test fun unsupportedAndNcLicensesBlocked() {
        assertFalse(OnlineRightsPolicy.canStream(verified().copy(licenseCode = "UNKNOWN")))
        assertFalse(OnlineRightsPolicy.canStream(verified().copy(licenseCode = "CC-BY-NC-4.0")))
    }

    @Test fun safeLinksMustBeHttpsWithoutCredentialsOrFragments() {
        listOf(null, "", "http://example.com", "file:///tmp/song",
            "https://user:password@example.com/a", "https://example.com/a#secret",
            "https://", "https://example.com%2Fevil.test/path").forEach {
            assertFalse("Incorrectly accepted: $it", OnlineRightsPolicy.isSafeHttpsLink(it))
        }
        assertTrue(OnlineRightsPolicy.isSafeHttpsLink("https://example.com/song?id=1"))
    }
}
