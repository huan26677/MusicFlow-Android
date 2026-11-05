package com.example.musicflow.core.account

/** Small demo attachments. Larger audio needs resumable TUS upload in a later milestone. */
object CreatorPrivateFileRules {
    const val BUCKET = "musicflow-creator-submissions"
    const val MAX_BYTES = 6L * 1024 * 1024

    val audioTypes = setOf(
        "audio/mpeg", "audio/mp4", "audio/ogg",
        "audio/flac", "audio/wav", "audio/x-wav"
    )
    val evidenceTypes = setOf("application/pdf", "image/jpeg", "image/png")

    fun allowed(kind: String, mime: String, bytes: Long): Boolean {
        if (bytes !in 1..MAX_BYTES) return false
        return when (kind) {
            "audio" -> mime in audioTypes
            "evidence" -> mime in evidenceTypes
            else -> false
        }
    }

    /** Never accept a storage path from a view or a caller. Only the server generates it. */
    fun validServerPath(path: String): Boolean =
        Regex("^[0-9a-f-]{36}/[0-9a-f-]{36}/[0-9a-f-]{36}\\.[a-z0-9]{3,4}$")
            .matches(path)
}
