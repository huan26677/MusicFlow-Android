package com.example.musicflow.core.playback

import android.os.Bundle

object EqualizerContract {
    const val ACTION = "com.example.musicflow.EQUALIZER_V1"
    const val KEY_OPERATION = "operation"
    const val OP_GET = 0
    const val OP_ENABLE = 1
    const val OP_SET_BAND = 2
    const val KEY_ENABLED = "enabled"
    const val KEY_BAND_INDEX = "bandIndex"
    const val KEY_LEVEL_MB = "levelMb"
    const val KEY_SUPPORTED = "supported"
    const val KEY_MIN_MB = "minMb"
    const val KEY_MAX_MB = "maxMb"
    const val KEY_FREQUENCIES_HZ = "frequenciesHz"
    const val KEY_LEVELS_MB = "levelsMb"
}

data class EqualizerBandUi(
    val index: Int,
    val frequencyHz: Int,
    val levelMb: Int
)

data class EqualizerUiState(
    val supported: Boolean = false,
    val enabled: Boolean = false,
    val minLevelMb: Int = 0,
    val maxLevelMb: Int = 0,
    val bands: List<EqualizerBandUi> = emptyList()
) {
    companion object {
        fun fromBundle(data: Bundle): EqualizerUiState {
            if (!data.getBoolean(EqualizerContract.KEY_SUPPORTED, false)) {
                return EqualizerUiState()
            }
            val frequencies = data.getIntArray(
                EqualizerContract.KEY_FREQUENCIES_HZ
            ) ?: return EqualizerUiState()
            val levels = data.getIntArray(
                EqualizerContract.KEY_LEVELS_MB
            ) ?: return EqualizerUiState()
            if (frequencies.size != levels.size) return EqualizerUiState()

            return EqualizerUiState(
                supported = true,
                enabled = data.getBoolean(EqualizerContract.KEY_ENABLED),
                minLevelMb = data.getInt(EqualizerContract.KEY_MIN_MB),
                maxLevelMb = data.getInt(EqualizerContract.KEY_MAX_MB),
                bands = frequencies.indices.map { index ->
                    EqualizerBandUi(index, frequencies[index], levels[index])
                }
            )
        }
    }
}
