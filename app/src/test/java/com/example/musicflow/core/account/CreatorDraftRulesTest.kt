package com.example.musicflow.core.account

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorDraftRulesTest {
    private val credits = listOf(CreatorCredit("Nghệ sĩ kiểm thử", "performer"))

    @Test fun draftCanBeSavedBeforeRightsReviewed() {
        assertTrue(CreatorDraftRules.canSave(
            "Bản nháp thử nghiệm", "Nghệ sĩ kiểm thử", "Chủ sở hữu",
            "UNVERIFIED", "", credits
        ))
    }

    @Test fun submissionRequiresCopyrightEvidenceAndStreamingDeclaration() {
        assertFalse(CreatorDraftRules.canSubmit(
            "Bài hát", "Nghệ sĩ", "Chủ sở hữu", "UNVERIFIED",
            "Hợp đồng cấp phép", true, credits
        ))
        assertFalse(CreatorDraftRules.canSubmit(
            "Bài hát", "Nghệ sĩ", "Chủ sở hữu", "DIRECT",
            "Hợp đồng cấp phép", false, credits
        ))
        assertFalse(CreatorDraftRules.canSubmit(
            "Bài hát", "Nghệ sĩ", "Chủ sở hữu", "DIRECT",
            "thiếu", true, credits
        ))
        assertTrue(CreatorDraftRules.canSubmit(
            "Bài hát", "Nghệ sĩ", "Chủ sở hữu", "DIRECT",
            "Hợp đồng cấp phép streaming 2026", true, credits
        ))
    }

    @Test fun creditIdentityAndRoleAreValidated() {
        assertFalse(CreatorDraftRules.canSave(
            "Bài hát", "Nghệ sĩ", "Chủ sở hữu", "DIRECT", "",
            listOf(CreatorCredit("A", "performer"))
        ))
        assertFalse(CreatorDraftRules.canSave(
            "Bài hát", "Nghệ sĩ", "Chủ sở hữu", "DIRECT", "",
            listOf(CreatorCredit("Nghệ sĩ", "owner"))
        ))
        assertFalse(CreatorDraftRules.canSave(
            "Bài hát", "Nghệ sĩ", "Chủ sở hữu", "DIRECT", "", emptyList()
        ))
    }
}
