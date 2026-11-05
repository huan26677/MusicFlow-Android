package com.example.musicflow.core.account

data class CreatorCredit(val name: String, val contribution: String)

/** Client validation is for UX; every constraint is rechecked by Supabase. */
object CreatorDraftRules {
    val creditRoles = setOf(
        "performer", "composer", "lyricist", "producer", "master_owner"
    )
    val licenseCodes = setOf("UNVERIFIED", "CC0-1.0", "CC-BY-4.0", "DIRECT")

    fun canSave(
        title: String,
        artist: String,
        rightsHolder: String,
        licenseCode: String,
        evidence: String,
        credits: List<CreatorCredit>
    ): Boolean = title.trim().length in 1..240 &&
        artist.trim().length in 2..180 &&
        rightsHolder.trim().length in 2..180 &&
        licenseCode in licenseCodes &&
        evidence.length <= 1000 &&
        credits.size in 1..12 &&
        credits.all {
            it.name.trim().length in 2..100 && it.contribution in creditRoles
        }

    fun canSubmit(
        title: String,
        artist: String,
        rightsHolder: String,
        licenseCode: String,
        evidence: String,
        streamingClaimed: Boolean,
        credits: List<CreatorCredit>
    ): Boolean = canSave(title, artist, rightsHolder, licenseCode, evidence, credits) &&
        licenseCode != "UNVERIFIED" &&
        streamingClaimed &&
        evidence.trim().length >= 8
}
