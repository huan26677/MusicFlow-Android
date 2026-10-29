package com.example.musicflow.core.online

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnlineRightsJsonTest {
    private val verified = """{
      "license_code":"CC-BY-4.0",
      "license_url":"https://creativecommons.org/licenses/by/4.0/",
      "attribution_text":"Artist — Example Track (CC BY 4.0)",
      "source_url":"https://example.com/track",
      "rights_confirmed":true,
      "streaming_allowed":true,
      "download_allowed":false,
      "commercial_use_allowed":true,
      "rights_verified_at":"2026-10-29T10:00:00Z"
    }"""

    @Test fun supabaseRightsDecodeAllFields() {
        val decoded = OnlineRightsJson.fromSupabase(JSONObject(verified))
        assertTrue(OnlineRightsPolicy.canStream(decoded))
        assertFalse(decoded.downloadAllowed)
        assertTrue(decoded.commercialUseAllowed)
        assertEquals("CC-BY-4.0", decoded.licenseCode)
    }

    @Test fun absentPublicFieldsNeverGrantStreaming() {
        val decoded = OnlineRightsJson.fromSupabase(JSONObject("{}"))
        assertFalse(OnlineRightsPolicy.canStream(decoded))
        assertEquals("UNVERIFIED", decoded.licenseCode)
    }

    @Test fun externalCatalogRequiresNestedRightsObject() {
        assertNull(OnlineRightsJson.fromExternal(JSONObject(verified)))
        val outer = JSONObject().put("rights", JSONObject()
            .put("licenseCode", "DIRECT")
            .put("attribution", "MusicFlow QA artist")
            .put("sourceUrl", "https://example.com/track")
            .put("rightsConfirmed", true)
            .put("streamingAllowed", true)
            .put("verifiedAt", "2026-10-29T10:00:00Z"))
        assertTrue(OnlineRightsPolicy.canStream(OnlineRightsJson.fromExternal(outer)))
    }

    @Test fun fakeBooleanAndBrokenSourceFailClosed() {
        val json = JSONObject(verified).put("streaming_allowed", false)
        assertFalse(OnlineRightsPolicy.canStream(OnlineRightsJson.fromSupabase(json)))
        json.put("streaming_allowed", true).put("source_url", "http://example.com")
        assertFalse(OnlineRightsPolicy.canStream(OnlineRightsJson.fromSupabase(json)))
    }
}
