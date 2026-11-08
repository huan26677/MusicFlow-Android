package com.example.musicflow.feature.search

import com.example.musicflow.core.media.DeviceSong
import com.example.musicflow.core.media.LocalMusicRepository
import kotlinx.coroutines.CancellationException

/**
 * A local-only search catalog. Android-wide audio permission is optional:
 * readable SAF documents stay available without READ_MEDIA_AUDIO.
 *
 * Injected sources keep permission changes testable without real accounts.
 */
class OfflineSearchRepository(
    private val mediaStore: LocalMusicRepository,
    private val mayScanDevice: () -> Boolean,
    private val readablePickedSongs: suspend () -> List<DeviceSong>
) : LocalMusicRepository {
    override suspend fun getSongs(): List<DeviceSong> {
        // Never reuse MediaStore results after its permission is revoked.
        val picked = readablePickedSongs()
        val scanned = if (mayScanDevice()) {
            try {
                mediaStore.getSongs()
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                // A provider error cannot hide separately granted documents.
                if (picked.isEmpty()) throw error
                emptyList()
            }
        } else {
            emptyList()
        }
        return combine(scanned, picked)
    }

    companion object {
        /** MediaStore wins ties so album/artist metadata remains rich. */
        fun combine(
            scanned: List<DeviceSong>,
            picked: List<DeviceSong>
        ): List<DeviceSong> = (scanned + picked)
            .distinctBy { it.uri.toString() }
    }
}
