package com.example.musicflow.core.playback

import android.content.Context
import android.net.Uri
import android.util.Log

import androidx.media3.common.MediaItem
import androidx.media3.common.Player

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import java.io.File

class PlaybackQueueRecovery(
    private val context: Context,
    private val store: PlaybackQueueStore,
    private val canOpen: (Uri) -> Boolean = { uri ->
        when (uri.scheme) {
            "file" -> {
                val path = uri.path
                path != null &&
                    File(path).isFile &&
                    File(path).canRead()
            }

            "content" -> {
                context.contentResolver
                    .openFileDescriptor(uri, "r")
                    ?.use { true } ?: false
            }

            else -> true
        }
    }
) {
    private data class Snapshot(
        val items: List<MediaItem>,
        val index: Int,
        val positionMs: Long
    )

    private fun itemKeys(items: List<MediaItem>) =
        items.map {
            it.mediaId to
                it.localConfiguration?.uri?.toString()
        }

    private fun readable(item: MediaItem): Boolean {
        val uri = item.localConfiguration?.uri
            ?: return false

        if (uri.scheme !in listOf("file", "content")) {
            // Không tự ý xóa URI mạng hoặc scheme khác.
            return true
        }

        return try {
            canOpen(uri)
        } catch (_: Exception) {
            // Bao gồm SecurityException, FileNotFoundException
            // và các lỗi provider.
            false
        }
    }

    suspend fun pruneUnavailable(
        player: Player
    ): Int {
        val original = withContext(
            Dispatchers.Main.immediate
        ) {
            if (
                player.mediaItemCount == 0 ||
                player.playWhenReady ||
                player.playbackState != Player.STATE_IDLE
            ) {
                null
            } else {
                Snapshot(
                    items = (0 until player.mediaItemCount)
                        .map { player.getMediaItemAt(it) },
                    index = player.currentMediaItemIndex,
                    positionMs = player.currentPosition
                        .coerceAtLeast(0L)
                )
            }
        } ?: return 0

        val validIndices = withContext(
            Dispatchers.IO
        ) {
            original.items.indices.filter {
                readable(original.items[it])
            }
        }

        val removed = original.items.size -
            validIndices.size

        if (removed == 0) return 0

        return withContext(
            Dispatchers.Main.immediate
        ) {
            val currentItems =
                (0 until player.mediaItemCount)
                    .map { player.getMediaItemAt(it) }

            // Người dùng có thể đã thay queue hoặc bấm Play.
            // Không sửa phiên phát đang hoạt động.
            if (
                player.playWhenReady ||
                player.playbackState != Player.STATE_IDLE ||
                itemKeys(currentItems) !=
                    itemKeys(original.items)
            ) {
                Log.i(
                    "MusicFlowQueueRecovery",
                    "SKIPPED_BUSY"
                )
                return@withContext 0
            }

            if (validIndices.isEmpty()) {
                player.clearMediaItems()
                store.clear()

                Log.i(
                    "MusicFlowQueueRecovery",
                    "ALL_UNAVAILABLE_CLEARED"
                )

                return@withContext removed
            }

            val remaining = validIndices.map {
                original.items[it]
            }

            val selectedStillExists =
                original.index in validIndices

            val newIndex = if (selectedStillExists) {
                validIndices.indexOf(original.index)
            } else {
                validIndices.indexOfFirst {
                    it > original.index
                }.takeIf { it >= 0 }
                    ?: validIndices.lastIndex
            }

            val newPosition = if (selectedStillExists) {
                original.positionMs
            } else {
                0L
            }

            player.setMediaItems(
                remaining,
                newIndex,
                newPosition
            )

            // Vẫn ở trạng thái tạm dừng.
            store.saveFromPlayer(player)

            Log.i(
                "MusicFlowQueueRecovery",
                "PRUNED removed=$removed " +
                    "kept=${remaining.size} index=$newIndex"
            )

            removed
        }
    }
}
