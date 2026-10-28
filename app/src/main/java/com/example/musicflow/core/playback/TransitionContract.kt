package com.example.musicflow.core.playback

object TransitionContract {
    const val ACTION = "com.example.musicflow.TRANSITION_V1"
    const val KEY_DURATION_MS = "durationMs"
    const val KEY_OPERATION = "operation"
    const val OP_GET = 0
    const val OP_SET = 1

    val allowedDurationsMs = setOf(0, 1000, 2000, 3000)
}
