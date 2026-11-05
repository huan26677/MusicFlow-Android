package com.example.musicflow.core.account

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorPrivateFileRulesTest {
    @Test fun onlyDemoSizedAudioOrRightsEvidence() {
        assertTrue(CreatorPrivateFileRules.allowed("audio", "audio/mpeg", 2048))
        assertTrue(CreatorPrivateFileRules.allowed("evidence", "application/pdf", 3000))
        assertFalse(CreatorPrivateFileRules.allowed("audio", "application/pdf", 4000))
        assertFalse(CreatorPrivateFileRules.allowed("evidence", "audio/mpeg", 4000))
        assertFalse(CreatorPrivateFileRules.allowed("audio", "audio/mpeg", 0))
        assertFalse(CreatorPrivateFileRules.allowed("audio", "audio/mpeg", 6291457))
        assertFalse(CreatorPrivateFileRules.allowed("evidence", "text/html", 10))
    }

    @Test fun storagePathsAreServerIssuedAndNotTraversal() {
        val path = "123e4567-e89b-12d3-a456-426614174000/" +
            "123e4567-e89b-12d3-a456-426614174001/" +
            "123e4567-e89b-12d3-a456-426614174002.pdf"
        assertTrue(CreatorPrivateFileRules.validServerPath(path))
        assertFalse(CreatorPrivateFileRules.validServerPath("../user/secret.pdf"))
        assertFalse(CreatorPrivateFileRules.validServerPath("https://malicious.example/pdf"))
        assertFalse(CreatorPrivateFileRules.validServerPath(path + "?download=1"))
    }
}
