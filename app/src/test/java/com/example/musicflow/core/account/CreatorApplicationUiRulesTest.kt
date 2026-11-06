package com.example.musicflow.core.account

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorApplicationUiRulesTest {
    private val rules = CreatorApplicationUiRules

    @Test fun statusesAndRolesHaveVietnameseLabels() {
        assertEquals("Đang chờ duyệt", rules.statusLabel("pending"))
        assertEquals("Đã duyệt", rules.statusLabel("approved"))
        assertEquals("Đã từ chối", rules.statusLabel("rejected"))
        assertEquals("Chưa xác định", rules.statusLabel("unknown"))
        assertEquals("Nghệ sĩ", rules.roleLabel("artist"))
        assertEquals("Tác giả", rules.roleLabel("author"))
    }

    @Test fun pendingArtistDoesNotBlockAuthorApplication() {
        val requests = listOf("artist" to "pending", "author" to "rejected")
        assertTrue(rules.hasPendingRole(requests, "artist"))
        assertFalse(rules.hasPendingRole(requests, "author"))
        assertTrue(rules.canSubmit(false, true, false, "Nghệ danh", false))
        assertFalse(rules.canSubmit(false, true, false, "Nghệ danh", true))
    }

    @Test fun creatorSubmissionRequiresLoadedStateAndValidName() {
        assertFalse(rules.canSubmit(false, false, false, "QA", false))
        assertFalse(rules.canSubmit(true, true, false, "QA", false))
        assertFalse(rules.canSubmit(false, true, true, "QA", false))
        assertFalse(rules.canSubmit(false, true, false, " ", false))
        assertFalse(rules.canSubmit(false, true, false, "a".repeat(81), false))
        assertTrue(rules.canSubmit(false, true, false, "  Tác giả QA  ", false))
    }

    @Test fun reviewRequiresAuditableReason() {
        assertFalse(rules.canReview(false, "no"))
        assertFalse(rules.canReview(true, "Lý do hợp lệ"))
        assertFalse(rules.canReview(false, "x".repeat(301)))
        assertTrue(rules.canReview(false, "  Đã kiểm tra hồ sơ QA  "))
    }
}
