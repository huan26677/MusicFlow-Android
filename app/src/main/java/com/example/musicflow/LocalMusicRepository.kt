package com.example.musicflow

import android.content.Context

interface LocalMusicRepository {
    suspend fun getSongs(): List<DeviceSong>
}

class MediaStoreLocalMusicRepository(
    context: Context
) : LocalMusicRepository {

    private val appContext = context.applicationContext

    override suspend fun getSongs(): List<DeviceSong> {
        return DeviceMusicScanner.scan(appContext)
    }
}