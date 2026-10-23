package com.example.musicflow.core.playback

import androidx.media3.common.Player

object PlaybackSkipRecovery {

    fun next(player: Player): Boolean {
        if (
            !player.hasNextMediaItem() ||
            !player.isCommandAvailable(
                Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM
            )
        ) {
            return false
        }

        val needsPrepare =
            player.playbackState == Player.STATE_IDLE

        if (
            needsPrepare &&
            (
                !player.isCommandAvailable(Player.COMMAND_PREPARE) ||
                !player.isCommandAvailable(Player.COMMAND_PLAY_PAUSE)
            )
        ) {
            return false
        }

        player.seekToNextMediaItem()

        if (needsPrepare) {
            player.prepare()
            player.play()
        }

        return true
    }
}
