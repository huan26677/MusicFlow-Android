package com.example.musicflow.core.playback

/**
 * Queue editing policy independent of Media3 and Compose.
 *
 * The currently playing item is protected. Move operations stay on the
 * same side of that item so neither playback nor its position is disturbed.
 * Shuffle changes playback order independently from visible timeline order,
 * therefore manual reordering is disabled while shuffle is enabled.
 */
object QueueEditRules {
    fun canRemove(count: Int, currentIndex: Int, index: Int): Boolean =
        count > 1 &&
            currentIndex in 0 until count &&
            index in 0 until count &&
            index != currentIndex

    fun canMove(
        count: Int,
        currentIndex: Int,
        fromIndex: Int,
        toIndex: Int,
        shuffleEnabled: Boolean
    ): Boolean =
        !shuffleEnabled &&
            canRemove(count, currentIndex, fromIndex) &&
            toIndex in 0 until count &&
            kotlin.math.abs(fromIndex - toIndex) == 1 &&
            (fromIndex < currentIndex && toIndex < currentIndex ||
                fromIndex > currentIndex && toIndex > currentIndex)

    fun canClearOthers(count: Int, currentIndex: Int): Boolean =
        count > 1 && currentIndex in 0 until count
}
