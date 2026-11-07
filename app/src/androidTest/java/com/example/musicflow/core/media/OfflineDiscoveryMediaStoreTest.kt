package com.example.musicflow.core.media

import android.content.ContentValues
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** A real 31-second WAV in Music/, not an injected DeviceSong list. */
@RunWith(AndroidJUnit4::class)
class OfflineDiscoveryMediaStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun wav(): ByteArray {
        val rate = 8000
        val bytes = rate * 31 * 2
        val out = ByteBuffer.allocate(44 + bytes).order(ByteOrder.LITTLE_ENDIAN)
        fun write(text: String) { out.put(text.toByteArray(Charsets.US_ASCII)) }
        write("RIFF"); out.putInt(36 + bytes); write("WAVE")
        write("fmt "); out.putInt(16); out.putShort(1); out.putShort(1)
        out.putInt(rate); out.putInt(rate * 2)
        out.putShort(2); out.putShort(16)
        write("data"); out.putInt(bytes)
        return out.array()
    }

    @Test fun scansLongWavInMusicWhenProviderInitiallyMarksItNonMusic() {
        val resolver = context.contentResolver
        val filename = "musicflow_discovery_${System.nanoTime()}.wav"
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, filename)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
            put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/MusicFlowDiscoveryQA/")
            put(MediaStore.Audio.Media.IS_MUSIC, 0)
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        val uri = checkNotNull(resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values))
        try {
            checkNotNull(resolver.openOutputStream(uri, "w")).use { it.write(wav()) }
            assertEquals(1, resolver.update(uri, ContentValues().apply {
                put(MediaStore.Audio.Media.IS_PENDING, 0)
            }, null, null))

            val actual = runBlocking {
                withTimeout(20_000) {
                    var result: DeviceSong? = null
                    while (result == null) {
                        result = DeviceMusicScanner.scan(context).firstOrNull { it.uri == uri }
                        if (result == null) delay(250)
                    }
                    result
                }
            }
            assertNotNull("The long WAV should be discoverable in Music/", actual)
            assertEquals(uri, actual.uri)
        } finally {
            resolver.delete(uri, null, null)
        }
    }

    @Test fun lightweightScanSkipsGenreLookupButStillFindsRealMusic() {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME,
                "musicflow_fastscan_${System.nanoTime()}.wav")
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
            put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/MusicFlowDiscoveryQA/")
            put(MediaStore.Audio.Media.IS_MUSIC, 1)
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        val uri = checkNotNull(resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values))
        try {
            checkNotNull(resolver.openOutputStream(uri, "w")).use { it.write(wav()) }
            resolver.update(uri, ContentValues().apply {
                put(MediaStore.Audio.Media.IS_PENDING, 0)
            }, null, null)
            val fast = runBlocking { DeviceMusicScanner.scan(context, includeGenres = false) }
                .firstOrNull { it.uri == uri }
            val full = runBlocking { DeviceMusicScanner.scan(context, includeGenres = true) }
                .firstOrNull { it.uri == uri }
            assertNotNull("Lightweight scan should include the real WAV", fast)
            assertNotNull("Genre scan should keep the same real WAV", full)
            assertTrue("Fast scan must not populate genre metadata",
                checkNotNull(fast).genres.isEmpty())
        } finally {
            resolver.delete(uri, null, null)
        }
    }
}
