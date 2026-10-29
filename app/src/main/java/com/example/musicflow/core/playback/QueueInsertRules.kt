package com.example.musicflow.core.playback

/**
 * Business rules for inserting into an existing Media3 playlist.
 *
 * This stays independent of Android/Media3 so a future queue UI, podcast
 * source, or remote catalog can use the same ordering rules and unit tests.
 */
enum class QueuePlacement { NEXT, END }

object QueueInsertRules {
    /**
     * Returns null if the action cannot be guaranteed to behave as requested.
     * Under shuffle, inserting at currentIndex + 1 is NOT necessarily "next".
     */
    fun insertionIndex(
        count: Int,
        currentIndex: Int,
        placement: QueuePlacement,
        shuffleEnabled: Boolean,
        repeatOneEnabled: Boolean = false
    ): Int? {
        if (count <= 0 || currentIndex !in 0 until count) return null
        if (placement == QueuePlacement.NEXT && (shuffleEnabled || repeatOneEnabled)) {
            return null
        }
        return when (placement) {
            QueuePlacement.NEXT -> (currentIndex + 1).coerceAtMost(count)
            QueuePlacement.END -> count
        }
    }
}
