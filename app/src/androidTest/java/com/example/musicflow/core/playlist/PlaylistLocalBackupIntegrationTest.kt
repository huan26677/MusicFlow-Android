package com.example.musicflow.core.playlist

import android.net.Uri
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.musicflow.core.history.local.RecentHistoryDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PlaylistLocalBackupIntegrationTest {
    private fun track(id: Int, title: String = "Bài $id") = PlaylistTrack(
        mediaId = "device-$id",
        audioUri = "content://media/external/audio/media/$id",
        title = title,
        artist = "MusicFlow QA",
        addedAtMs = 10L + id
    )

    @Test fun jsonRoundTripPreservesVietnameseAndOrderedTrackMetadata() {
        val values = listOf(PlaylistBackupEntry("Nhạc đường về", listOf(
            track(12, "Đường về nhà"), track(25, "Người thương")
        )), PlaylistBackupEntry("Nhạc trống", emptyList()))
        val bytes = PlaylistLocalBackup.encode(values)
        val decoded = PlaylistLocalBackup.decode(bytes)
        assertEquals(values, decoded)
        assertTrue(bytes.isNotEmpty())
        assertFalse(String(bytes).contains("password"))
    }

    @Test fun atomicImportCreatesCopiesWithSameOrderAndLeavesOriginalsUntouched() = runBlocking {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(ctx, RecentHistoryDatabase::class.java).build()
        try {
            val repo = RoomPlaylistRepository(db)
            val original = checkNotNull(repo.create("Nhạc Việt"))
            assertTrue(repo.addTrack(original.id, track(2)))
            assertTrue(repo.addTrack(original.id, track(1)))
            val document = PlaylistLocalBackup.encode(repo.backupEntries())
            assertEquals(1, repo.importBackup(PlaylistLocalBackup.decode(document)))
            val records = repo.backupEntries()
            assertEquals(2, records.size)
            val imported = records.first { it.name.endsWith("(nhập)") }
            assertEquals(listOf("device-2", "device-1"), imported.tracks.map { it.mediaId })
            assertEquals(listOf("device-2", "device-1"), repo.getTracks(original.id)
                .map { it.mediaId })
            assertEquals("Nhạc Việt", repo.getPlaylist(original.id)?.name)
            assertTrue(repo.importBackup(emptyList()) == 0)
        } finally { db.close() }
    }

    @Test fun invalidDocumentCannotPartiallyModifyRoom() = runBlocking {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(ctx, RecentHistoryDatabase::class.java).build()
        try {
            val repo = RoomPlaylistRepository(db)
            val original = checkNotNull(repo.create("Gốc"))
            val entries = listOf(
                PlaylistBackupEntry("Hợp lệ", listOf(track(1))),
                PlaylistBackupEntry("Sai", listOf(track(2).copy(audioUri = "https://unsafe.invalid/x")))
            )
            try { repo.importBackup(entries); fail("Expected invalid backup") }
            catch (_: IllegalArgumentException) { /* intentional */ }
            assertEquals(1, repo.backupEntries().size)
            assertEquals("Gốc", repo.getPlaylist(original.id)?.name)
        } finally { db.close() }
    }

    @Test fun rejectsCorruptionWrongVersionAndOversizedInput() {
        val valid = PlaylistLocalBackup.encode(listOf(PlaylistBackupEntry("OK", emptyList())))
        for (value in listOf("{}", "[]", "not json", "null")) {
            assertTrue(runCatching { PlaylistLocalBackup.decode(value.toByteArray()) }.isFailure)
        }
        assertTrue(runCatching {
            PlaylistLocalBackup.decode(String(valid).replace("\"version\": 1", "\"version\": 5")
                .toByteArray())
        }.isFailure)
        assertTrue(runCatching {
            PlaylistLocalBackup.decode(ByteArray(PlaylistLocalBackup.MAX_FILE_BYTES + 1))
        }.isFailure)
    }

    @Test fun relinkingPreservesTrackPositionAndRejectsDuplicateOrUnknown() = runBlocking {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(ctx, RecentHistoryDatabase::class.java).build()
        try {
            val repo = RoomPlaylistRepository(db)
            val list = checkNotNull(repo.create("Nhạc học tập"))
            assertTrue(repo.addTrack(list.id, track(1)))
            assertTrue(repo.addTrack(list.id, track(2)))
            assertFalse(repo.relinkTrack(list.id, "device-9", "content://qa/document/9"))
            assertFalse(repo.relinkTrack(list.id, "device-1", "https://example.invalid/song"))
            val newUri = "content://qa/document/relinked"
            assertTrue(repo.relinkTrack(list.id, "device-1", newUri))
            val current = repo.getTracks(list.id)
            assertEquals(listOf(newUri, "device-2"), current.map { it.mediaId })
            assertEquals("Bài 1", current.first().title)
            assertEquals(11L, current.first().addedAtMs)
            assertFalse(repo.relinkTrack(list.id, "device-2", newUri))
            assertEquals(2, repo.getTracks(list.id).size)
        } finally { db.close() }
    }

    @Test fun onDemandFileCheckDistinguishesReadableLocalFileAndMissingFile() = runBlocking {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val resolver = ctx.contentResolver
        val file = File(ctx.cacheDir, "musicflow-local-access-${System.nanoTime()}.wav")
        file.writeBytes(byteArrayOf(82, 73, 70, 70, 0, 0, 0, 0))
        try {
            val available = PlaylistTrack("qa-readable", Uri.fromFile(file).toString(),
                "Kiểm tra", "QA", 1L)
            val missing = track(999999)
            assertTrue(PlaylistLocalAccess.readable(resolver, available.audioUri))
            assertFalse(PlaylistLocalAccess.readable(resolver, missing.audioUri))
            assertEquals(setOf(missing.mediaId), PlaylistLocalAccess.check(resolver,
                listOf(available, missing)))
        } finally { file.delete() }
    }
}
