
package com.example.musicflow.core.history

class RecentPlaybackTracker {

    private var lastRecordedKey: Pair<String, String>? = null

    fun shouldRecord(
        isPlaying: Boolean,
        mediaId: String?,
        audioUri: String?
    ): Boolean {
        if (
            !isPlaying ||
            mediaId.isNullOrBlank() ||
            audioUri.isNullOrBlank()
        ) {
            return false
        }

        val key = mediaId to audioUri

        // Không ghi lại khi artwork đổi,
        // seek hoặc pause/resume cùng một bài.
        if (lastRecordedKey == key) {
            return false
        }

        lastRecordedKey = key
        return true
    }
}
