
package com.example.musicflow.core.history

import androidx.room.withTransaction

import com.example.musicflow.core.history.local.RecentHistoryDatabase
import com.example.musicflow.core.history.local.toEntity
import com.example.musicflow.core.history.local.toRecentSong

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomRecentHistoryRepository(
    private val database: RecentHistoryDatabase
) {

    private val dao = database.recentSongDao()

    /**
     * Merge remote metadata only after resolving it to local media.
     * No imported URI is accepted from another device.
     */
    suspend fun mergeCloudRecent(restored: List<RecentSong>): Int =
        database.withTransaction {
            val existing = dao.getAll().map { it.toRecentSong() }
            val merged = CloudHistoryRules.merge(existing, restored)
            val existingKeys = existing.map { it.mediaId }.toSet()
            dao.deleteAll()
            dao.insertAll(merged.mapIndexed { index, item ->
                item.toEntity(position = index)
            })
            merged.count { it.mediaId !in existingKeys }
        }

    suspend fun record(song: RecentSong) {
        if (
            song.mediaId.isBlank() ||
            song.audioUri.isBlank()
        ) {
            return
        }

        database.withTransaction {
            val current = dao.getAll()
                .map { it.toRecentSong() }

            // Tái sử dụng đúng quy tắc đã có unit test.
            val updated = RecentHistory.add(
                existing = current,
                song = song
            )

            // Ghi lại danh sách tối đa 20 bài
            // trong cùng một transaction.
            dao.deleteAll()

            dao.insertAll(
                updated.mapIndexed { index, item ->
                    item.toEntity(position = index)
                }
            )
        }
    }

    suspend fun getRecent(): List<RecentSong> {
        return dao.getAll().map {
            it.toRecentSong()
        }
    }

    suspend fun remove(mediaId: String) {
        if (mediaId.isBlank()) return

        dao.deleteByMediaId(mediaId)
    }

    suspend fun clear() {
        dao.deleteAll()
    }

    fun observeRecent(): Flow<List<RecentSong>> {
        return dao.observeAll().map { entities ->
            entities.map { it.toRecentSong() }
        }
    }
}
