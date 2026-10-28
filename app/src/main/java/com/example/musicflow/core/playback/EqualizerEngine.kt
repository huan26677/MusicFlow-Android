package com.example.musicflow.core.playback

import android.content.Context
import android.media.audiofx.Equalizer
import android.os.Bundle
import android.util.Log

/**
 * Audio effects belong to the Media3 playback service, never to the Activity.
 * Each device may expose a different number of frequency bands / gain range.
 */
internal class EqualizerEngine(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences(
        "musicflow_equalizer_v1",
        Context.MODE_PRIVATE
    )

    private var sessionId = 0
    private var effect: Equalizer? = null

    fun onAudioSessionChanged(newSessionId: Int) {
        if (newSessionId == sessionId) return

        releaseEffect()
        sessionId = newSessionId
        if (newSessionId <= 0) return

        try {
            val created = Equalizer(0, newSessionId)
            try {
                val range = created.bandLevelRange
                val bandCount = created.numberOfBands.toInt()
                for (index in 0 until bandCount) {
                    val saved = preferences.getInt("band_$index", 0)
                        .coerceIn(range[0].toInt(), range[1].toInt())
                    created.setBandLevel(index.toShort(), saved.toShort())
                }
                created.setEnabled(
                    preferences.getBoolean("enabled", false)
                )
                effect = created
            } catch (error: Exception) {
                created.release()
                throw error
            }
        } catch (error: Exception) {
            Log.w("MusicFlowEqualizer", "Audio effect unavailable", error)
        }
    }

    fun setEnabled(enabled: Boolean): Boolean {
        val eq = effect ?: return false
        return try {
            eq.setEnabled(enabled)
            preferences.edit().putBoolean("enabled", enabled).apply()
            true
        } catch (error: Exception) {
            Log.w("MusicFlowEqualizer", "Cannot change enabled state", error)
            false
        }
    }

    fun setBandLevel(index: Int, levelMb: Int): Boolean {
        val eq = effect ?: return false
        val range = eq.bandLevelRange
        if (index !in 0 until eq.numberOfBands.toInt() ||
            levelMb !in range[0].toInt()..range[1].toInt()
        ) return false

        return try {
            eq.setBandLevel(index.toShort(), levelMb.toShort())
            preferences.edit().putInt("band_$index", levelMb).apply()
            true
        } catch (error: Exception) {
            Log.w("MusicFlowEqualizer", "Cannot set band $index", error)
            false
        }
    }

    fun stateBundle(): Bundle {
        val eq = effect ?: return Bundle().apply {
            putBoolean(EqualizerContract.KEY_SUPPORTED, false)
        }

        return try {
            val range = eq.bandLevelRange
            val count = eq.numberOfBands.toInt()
            Bundle().apply {
                putBoolean(EqualizerContract.KEY_SUPPORTED, true)
                putBoolean(EqualizerContract.KEY_ENABLED, eq.enabled)
                putInt(EqualizerContract.KEY_MIN_MB, range[0].toInt())
                putInt(EqualizerContract.KEY_MAX_MB, range[1].toInt())
                putIntArray(
                    EqualizerContract.KEY_FREQUENCIES_HZ,
                    IntArray(count) { index ->
                        eq.getCenterFreq(index.toShort()) / 1000
                    }
                )
                putIntArray(
                    EqualizerContract.KEY_LEVELS_MB,
                    IntArray(count) { index ->
                        eq.getBandLevel(index.toShort()).toInt()
                    }
                )
            }
        } catch (error: Exception) {
            Log.w("MusicFlowEqualizer", "Cannot read bands", error)
            Bundle().apply {
                putBoolean(EqualizerContract.KEY_SUPPORTED, false)
            }
        }
    }

    private fun releaseEffect() {
        effect?.let {
            try {
                it.release()
            } catch (error: Exception) {
                Log.w("MusicFlowEqualizer", "Cannot release effect", error)
            }
        }
        effect = null
    }

    fun release() {
        releaseEffect()
        sessionId = 0
    }
}
