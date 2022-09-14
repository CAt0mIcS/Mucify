package com.tachyonmusic.media.data

import android.os.Looper
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.PlayerMessage
import com.tachyonmusic.core.domain.TimingData
import com.tachyonmusic.core.domain.TimingDataController
import com.tachyonmusic.media.data.ext.timingData
import com.tachyonmusic.media.domain.CustomPlayer

/**
 * Override player to always enable SEEK_PREVIOUS and SEEK_NEXT commands
 */
class CustomPlayerImpl(player: Player) : ForwardingPlayer(player), CustomPlayer, Player.Listener {
    private var loopMessage: PlayerMessage? = null

    init {
        addListener(this)
    }

    override fun getAvailableCommands(): Player.Commands {
        return wrappedPlayer.availableCommands.run {
            if (mediaItemCount > 1) {
                return@run buildUpon()
                    .add(COMMAND_SEEK_TO_NEXT)
                    .add(COMMAND_SEEK_TO_PREVIOUS)
                    .build()
            }
            this
        }
    }

    // TODO: [COMMAND_SEEK_TO_NEXT/-PREVIOUS] still appears when playing playlist
    override fun isCommandAvailable(command: @Player.Command Int): Boolean {
        return wrappedPlayer.isCommandAvailable(command) || (command == COMMAND_SEEK_TO_NEXT && !isPlayingAd && mediaItemCount > 1) ||
                (command == COMMAND_SEEK_TO_PREVIOUS && !isPlayingAd && mediaItemCount > 1)
    }

    fun createMessage(target: PlayerMessage.Target): PlayerMessage {
        if (wrappedPlayer is ExoPlayer) {
            return (wrappedPlayer as ExoPlayer).createMessage(target)
        }
        TODO("createMessage for other types of players")
    }

    override fun seekToNext() {
        if (currentTimeline.isEmpty || isPlayingAd) {
            return
        }

        if (wrappedPlayer.hasNextMediaItem()) {
            // Dispatch to the wrapped player if not at the final media item
            wrappedPlayer.seekToNextMediaItem()
        } else {
            // Seek to the beginning of the playlist if no next media item is present
            wrappedPlayer.seekToDefaultPosition(0)
        }
    }

    override fun seekToPrevious() {
        if (currentTimeline.isEmpty || isPlayingAd) {
            return
        }

        // Only the first start time matters when choosing if we play the previous playback
        // or seek back to the beginning of the current one
        val startTime = mediaMetadata.timingData?.getOrNull(0)?.startTime ?: 0

        // Seek to either the previous song or to the last one if we don't have a previous one
        if (isCurrentMediaItemLive && !isCurrentMediaItemSeekable || currentPosition <= maxSeekToPreviousPosition + startTime) {
            if (hasPreviousMediaItem())
                seekToPreviousMediaItem()
            else
                wrappedPlayer.seekToDefaultPosition(wrappedPlayer.mediaItemCount - 1)

        } else {
            // If the player position is less than [maxSeekToPreviousPosition], we'll seek to
            // the beginning of the song
            seekTo(startTime)
        }
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int
    ) {
        /**
         * When seeking we need to update the [currentTimingDataIndex] depending on the seek position
         */
        val timingData = currentMediaItem!!.mediaMetadata.timingData
        if (timingData != null && timingData.isNotEmpty() && reason == Player.DISCONTINUITY_REASON_SEEK)
            updateTimingDataInternal(timingData)
    }


    override fun updateTimingData(newTimingData: TimingDataController) {
        currentMediaItem!!.mediaMetadata.timingData = newTimingData
        updateTimingDataInternal(newTimingData)
    }

    private fun updateTimingDataInternal(newTimingData: TimingDataController) {
        if (newTimingData.size == 0)
            return

        newTimingData.advanceToCurrentPosition(currentPosition)
        postLoopMessage(
            newTimingData.next.startTime,
            newTimingData.current.endTime
        )
        seekWithoutCallback(newTimingData.current.startTime)
    }

    private fun postLoopMessage(startTime: Long, endTime: Long) {
        // Cancel any previous messages
        loopMessage?.cancel()

        loopMessage = createMessage { _, payload ->
            seekWithoutCallback(payload as Long)
            val timingData = mediaMetadata.timingData
            if (timingData != null) {
                timingData.advanceToNext()
                postLoopMessage(
                    timingData.next.startTime,
                    timingData.current.endTime
                )
            }

        }.apply {
            looper = Looper.getMainLooper()
            deleteAfterDelivery = false
            payload = startTime
            setPosition(endTime)
            send()
        }
    }

    private val registerOldSeekHandlerCallback = object : Player.Listener {
        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            removeListener(this)
            addListener(this@CustomPlayerImpl)
        }
    }

    private fun seekWithoutCallback(positionMs: Long) {
        removeListener(this)
        addListener(registerOldSeekHandlerCallback)
        seekTo(positionMs)
    }
}