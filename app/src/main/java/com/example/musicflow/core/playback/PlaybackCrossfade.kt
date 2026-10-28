package com.example.musicflow.core.playback

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Opt-in crossfade for ordinary forward playlist playback.
 *
 * The MediaSession and persisted queue always remain attached to [primary].
 * A temporary player overlaps the next file near the end of the current one.
 * The primary then seeks to the already-playing position of the next file.
 *
 * V1 excludes shuffle, repeat, variable playback speed, very short items and
 * seek/pause/queue changes. Any unsafe transition falls back to Media3.
 */
internal class PlaybackCrossfade(
    private val context: Context,
    private val primary: ExoPlayer
) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "musicflow_crossfade_v1", Context.MODE_PRIVATE
    )

    var durationMs: Int = preferences.getInt("durationMs", 0)
        .takeIf { it in TransitionContract.allowedDurationsMs } ?: 0
        private set

    private data class Candidate(
        val fromIndex: Int,
        val fromId: String,
        val fromUri: String?,
        val toIndex: Int,
        val toId: String,
        val toUri: String?,
        val count: Int
    )

    private enum class Stage { PRELOADING, MIXING, HANDOFF }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var ticker: Job? = null
    private var secondary: ExoPlayer? = null
    private var candidate: Candidate? = null
    private var stage = Stage.PRELOADING
    private var handoffStartedAt = 0L
    private var internalSeekPending = false

    fun onSeek() {
        if (internalSeekPending) {
            internalSeekPending = false
        } else {
            cancelTransition()
        }
    }

    fun start() {
        if (ticker != null) return
        ticker = scope.launch {
            while (isActive) {
                delay(80)
                try {
                    tick()
                } catch (error: Exception) {
                    Log.w("MusicFlowCrossfade", "Transition aborted", error)
                    cancelTransition()
                }
            }
        }
    }

    fun setDurationMs(value: Int): Boolean {
        if (value !in TransitionContract.allowedDurationsMs) return false
        durationMs = value
        preferences.edit().putInt("durationMs", value).apply()
        cancelTransition()
        return true
    }

    private fun eligible(): Boolean =
        durationMs > 0 &&
            primary.isPlaying &&
            primary.playbackState == Player.STATE_READY &&
            primary.repeatMode == Player.REPEAT_MODE_OFF &&
            !primary.shuffleModeEnabled &&
            primary.playbackParameters.speed == 1f &&
            primary.nextMediaItemIndex != C.INDEX_UNSET &&
            primary.duration != C.TIME_UNSET &&
            primary.duration >= durationMs + 3000L

    private fun snapshot(): Candidate? {
        if (!eligible()) return null
        val from = primary.currentMediaItemIndex
        val to = primary.nextMediaItemIndex
        if (from !in 0 until primary.mediaItemCount ||
            to !in 0 until primary.mediaItemCount || to == from
        ) return null
        val first = primary.getMediaItemAt(from)
        val next = primary.getMediaItemAt(to)
        if (next.localConfiguration?.uri == null) return null
        return Candidate(
            from, first.mediaId, first.localConfiguration?.uri?.toString(),
            to, next.mediaId, next.localConfiguration?.uri?.toString(),
            primary.mediaItemCount
        )
    }

    private fun tick() {
        if (stage == Stage.HANDOFF) {
            completeHandoff()
            return
        }
        val current = snapshot()
        val expected = candidate
        if (expected != null && expected != current) cancelTransition()
        if (current == null) return

        val remaining = primary.duration - primary.currentPosition
        if (remaining <= 0L) {
            cancelTransition()
            return
        }

        if (secondary == null) {
            if (remaining > durationMs + 1800L) return
            preloadNext(current)
            return
        }

        val next = secondary ?: return
        if (next.playerError != null) {
            cancelTransition()
            return
        }
        if (stage == Stage.PRELOADING) {
            if (remaining > durationMs || next.playbackState != Player.STATE_READY) {
                return
            }
            next.volume = 0f
            next.play()
            stage = Stage.MIXING
            Log.i("MusicFlowCrossfade", "MIX_STARTED from=${current.fromIndex} to=${current.toIndex}")
        }

        if (stage == Stage.MIXING) {
            if (!next.isPlaying) {
                cancelTransition()
                return
            }
            val fraction = (remaining.toFloat() / durationMs)
                .coerceIn(0f, 1f)
            primary.volume = fraction
            next.volume = 1f - fraction
            if (remaining <= 200L) beginHandoff(current, next)
        }
    }

    private fun preloadNext(item: Candidate) {
        try {
            val next = ExoPlayer.Builder(context)
                .setAudioAttributes(AudioAttributes.DEFAULT, false)
                .setHandleAudioBecomingNoisy(false)
                .build()
            next.volume = 0f
            next.setMediaItem(primary.getMediaItemAt(item.toIndex))
            next.prepare()
            secondary = next
            candidate = item
            stage = Stage.PRELOADING
        } catch (error: Exception) {
            Log.w("MusicFlowCrossfade", "Preload failed", error)
            cancelTransition()
        }
    }

    private fun beginHandoff(item: Candidate, next: ExoPlayer) {
        // Preserve queue index and existing MediaSession ownership.
        // Seek on the primary while the secondary carries the audible signal.
        stage = Stage.HANDOFF
        handoffStartedAt = SystemClock.elapsedRealtime()
        primary.volume = 0f
        internalSeekPending = true
        primary.seekTo(item.toIndex, next.currentPosition.coerceAtLeast(0L))
    }

    private fun completeHandoff() {
        val item = candidate
        val next = secondary
        if (item == null || next == null || !primary.playWhenReady ||
            primary.currentMediaItemIndex != item.toIndex ||
            primary.currentMediaItem?.mediaId != item.toId ||
            primary.mediaItemCount != item.count
        ) {
            cancelTransition()
            return
        }

        val elapsed = SystemClock.elapsedRealtime() - handoffStartedAt
        if (elapsed > 5000L) {
            cancelTransition()
            return
        }
        if (primary.playbackState != Player.STATE_READY || !primary.isPlaying) {
            return
        }

        val fraction = (elapsed / 250f).coerceIn(0f, 1f)
        primary.volume = fraction
        next.volume = 1f - fraction
        if (fraction >= 1f) {
            Log.i("MusicFlowCrossfade", "HANDOFF_DONE index=${item.toIndex}")
            cancelTransition()
        }
    }

    fun cancelTransition() {
        secondary?.let { player ->
            player.stop()
            player.release()
        }
        secondary = null
        candidate = null
        stage = Stage.PRELOADING
        handoffStartedAt = 0L
        internalSeekPending = false
        primary.volume = 1f
    }

    fun release() {
        ticker?.cancel()
        ticker = null
        cancelTransition()
        scope.cancel()
    }
}
