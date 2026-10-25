package com.example.musicflow.core.playback

import android.content.Context
import android.net.Uri
import android.util.Log

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import kotlinx.coroutines.runBlocking

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

import java.io.File

@RunWith(AndroidJUnit4::class)
class PlaybackQueueHardeningTest {

    private val instrumentation =
        InstrumentationRegistry.getInstrumentation()

    private val context = instrumentation.targetContext

    private fun name() =
        "musicflow_hardening_${System.nanoTime()}"

    private fun item(id: String, uri: Uri) =
        MediaItem.Builder()
            .setMediaId(id)
            .setUri(uri)
            .build()

    private suspend fun withSavedQueue(
        items: List<MediaItem>,
        index: Int,
        positionMs: Long,
        block: suspend (
            ExoPlayer,
            PlaybackQueueStore,
            String
        ) -> Unit
    ) {
        val name = name()
        val store = PlaybackQueueStore(context, name)
        lateinit var restored: ExoPlayer

        instrumentation.runOnMainSync {
            val original =
                ExoPlayer.Builder(context).build()

            try {
                original.setMediaItems(
                    items,
                    index,
                    positionMs
                )
                store.saveFromPlayer(original)
            } finally {
                original.release()
            }

            restored = ExoPlayer.Builder(context).build()

            assertTrue(
                PlaybackQueueStore(
                    context,
                    name
                ).restoreInto(restored)
            )
        }

        try {
            block(restored, store, name)
        } finally {
            instrumentation.runOnMainSync {
                restored.release()
            }

            store.clear()
            context.deleteSharedPreferences(name)
        }
    }

    @Test
    fun deletedSelectedFileIsRemoved() =
        runBlocking<Unit> {
            val first = File.createTempFile(
                "qa_first_", ".wav", context.cacheDir
            )

            val third = File.createTempFile(
                "qa_third_", ".wav", context.cacheDir
            )

            val missing = File(
                context.cacheDir,
                "qa_missing_${System.nanoTime()}.wav"
            )

            try {
                withSavedQueue(
                    listOf(
                        item("first", Uri.fromFile(first)),
                        item("missing", Uri.fromFile(missing)),
                        item("third", Uri.fromFile(third))
                    ),
                    index = 1,
                    positionMs = 4_200L
                ) { player, store, name ->

                    assertEquals(
                        1,
                        PlaybackQueueRecovery(
                            context,
                            store
                        ).pruneUnavailable(player)
                    )

                    instrumentation.runOnMainSync {
                        assertEquals(2, player.mediaItemCount)

                        assertEquals(
                            listOf("first", "third"),
                            (0 until player.mediaItemCount)
                                .map {
                                    player.getMediaItemAt(it)
                                        .mediaId
                                }
                        )

                        assertEquals(
                            "third",
                            player.currentMediaItem?.mediaId
                        )

                        assertEquals(
                            0L,
                            player.currentPosition
                        )

                        assertFalse(player.isPlaying)
                        assertFalse(player.playWhenReady)

                        val reopened =
                            ExoPlayer.Builder(context).build()

                        try {
                            assertTrue(
                                PlaybackQueueStore(
                                    context,
                                    name
                                ).restoreInto(reopened)
                            )

                            assertEquals(
                                2,
                                reopened.mediaItemCount
                            )

                            assertEquals(
                                "third",
                                reopened.currentMediaItem
                                    ?.mediaId
                            )
                        } finally {
                            reopened.release()
                        }
                    }

                    Log.i(
                        "MusicFlowQueueHardeningTest",
                        "PASS deletedSelectedFilePruned"
                    )
                }
            } finally {
                first.delete()
                third.delete()
            }
        }


    @Test
    fun validSelectionRetainsPlaybackPosition() =
        runBlocking<Unit> {
            val selected = File.createTempFile(
                "qa_selected_", ".wav", context.cacheDir
            )

            val missing = File(
                context.cacheDir,
                "qa_deleted_${System.nanoTime()}.wav"
            )

            try {
                withSavedQueue(
                    listOf(
                        item("missing", Uri.fromFile(missing)),
                        item("selected", Uri.fromFile(selected))
                    ),
                    index = 1,
                    positionMs = 5_300L
                ) { player, store, _ ->

                    assertEquals(
                        1,
                        PlaybackQueueRecovery(
                            context,
                            store
                        ).pruneUnavailable(player)
                    )

                    instrumentation.runOnMainSync {
                        assertEquals(1, player.mediaItemCount)
                        assertEquals(0, player.currentMediaItemIndex)

                        assertEquals(
                            "selected",
                            player.currentMediaItem?.mediaId
                        )

                        assertEquals(
                            5_300L,
                            player.currentPosition
                        )

                        assertFalse(player.isPlaying)
                    }

                    Log.i(
                        "MusicFlowQueueHardeningTest",
                        "PASS validSelectionPositionKept"
                    )
                }
            } finally {
                selected.delete()
            }
        }


    @Test
    fun deniedContentAccessClearsUnplayableQueue() =
        runBlocking<Unit> {
            val missing = File(
                context.cacheDir,
                "qa_absent_${System.nanoTime()}.wav"
            )

            withSavedQueue(
                listOf(
                    item(
                        "denied",
                        Uri.parse(
                            "content://qa.invalid/item/1"
                        )
                    ),
                    item(
                        "missing",
                        Uri.fromFile(missing)
                    )
                ),
                index = 0,
                positionMs = 0L
            ) { player, store, name ->

                val recovery = PlaybackQueueRecovery(
                    context,
                    store
                ) { uri ->
                    if (uri.scheme == "content") {
                        throw SecurityException(
                            "QA simulated revoked permission"
                        )
                    }
                    false
                }

                assertEquals(
                    2,
                    recovery.pruneUnavailable(player)
                )

                instrumentation.runOnMainSync {
                    assertEquals(0, player.mediaItemCount)
                    assertFalse(player.isPlaying)
                }

                val raw = context.getSharedPreferences(
                    name,
                    Context.MODE_PRIVATE
                ).getString("snapshot", null)

                assertNull(raw)

                Log.i(
                    "MusicFlowQueueHardeningTest",
                    "PASS permissionDeniedClearsQueue"
                )
            }
        }


    @Test
    fun malformedSnapshotIsCleared() {
        val name = name()

        val prefs = context.getSharedPreferences(
            name,
            Context.MODE_PRIVATE
        )

        assertTrue(
            prefs.edit()
                .putString(
                    "snapshot",
                    "{broken-json"
                )
                .commit()
        )

        var player: ExoPlayer? = null

        try {
            instrumentation.runOnMainSync {
                val exo =
                    ExoPlayer.Builder(context).build()

                player = exo

                assertFalse(
                    PlaybackQueueStore(
                        context,
                        name
                    ).restoreInto(exo)
                )

                assertEquals(0, exo.mediaItemCount)
            }

            assertNull(
                prefs.getString("snapshot", null)
            )

            Log.i(
                "MusicFlowQueueHardeningTest",
                "PASS malformedSnapshotCleared"
            )
        } finally {
            player?.let {
                instrumentation.runOnMainSync {
                    it.release()
                }
            }

            context.deleteSharedPreferences(name)
        }
    }
}

