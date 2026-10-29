package com.example.musicflow.core.playback

object SleepTimerContract {
    const val ACTION =
        "com.example.musicflow.SLEEP_TIMER"

    const val KEY_MINUTES = "minutes"
    const val KEY_REMAINING_MS = "remainingMs"

    // Only accepted in DEBUG builds from our own app's MediaController.
    // Lets instrumentation verify actual expiry without waiting 15 minutes.
    // Never exposed in the user-facing UI or enabled in release.
    const val KEY_DEBUG_DURATION_MS = "debugDurationMs"
}
