package com.tachyonmusic.domain.use_case

import android.content.Context
import android.net.Uri
import com.tachyonmusic.app.R
import com.tachyonmusic.core.data.constants.PlaybackType
import com.tachyonmusic.core.domain.playback.Playback
import com.tachyonmusic.core.domain.playback.Playlist
import com.tachyonmusic.database.domain.model.SinglePlaybackEntity
import com.tachyonmusic.database.util.isPlayable
import com.tachyonmusic.domain.repository.MediaBrowserController
import com.tachyonmusic.logger.domain.Logger
import com.tachyonmusic.media.domain.use_case.AddNewPlaybackToHistory
import com.tachyonmusic.playback_layers.domain.GetPlaylistForPlayback
import com.tachyonmusic.playback_layers.domain.UriPermissionRepository
import com.tachyonmusic.playback_layers.domain.events.PlaybackNotFoundEvent
import com.tachyonmusic.util.Duration
import com.tachyonmusic.util.EventSeverity
import com.tachyonmusic.util.UiText
import com.tachyonmusic.util.domain.EventChannel
import com.tachyonmusic.util.replaceWith
import com.tachyonmusic.util.runOnUiThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

enum class PlaybackLocation {
    PREDEFINED_PLAYLIST,
    CUSTOM_PLAYLIST
}

class PlayPlayback(
    private val browser: MediaBrowserController,
    private val getPlaylistForPlayback: GetPlaylistForPlayback,
    private val addNewPlaybackToHistory: AddNewPlaybackToHistory,
    private val log: Logger,
    private val eventChannel: EventChannel,
    private val context: Context,
    private val uriPermissionRepository: UriPermissionRepository
) {
    /**
     * TODO: Move the caching mechanism and playability checking mechanism somewhere else
     */

    private var cacheLock = Any()
    private val permissionCache = mutableMapOf<Uri, Boolean>()
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        uriPermissionRepository.permissions.onEach {
            synchronized(cacheLock) {
                permissionCache.clear()
            }
        }.launchIn(ioScope)
    }

    @JvmName("invokePlayback")
    suspend operator fun invoke(
        playback: Playback?,
        position: Duration? = null,
        playbackLocation: PlaybackLocation? = null
    ) = runOnUiThread {
        if (playback == null) {
            eventChannel.push(
                PlaybackNotFoundEvent(
                    UiText.StringResource(R.string.invalid_playback, "null"),
                    EventSeverity.Error
                )
            )
            return@runOnUiThread
        }

        when (playback.playbackType) {
            is PlaybackType.Song, is PlaybackType.Remix -> {
                val uri = playback.uri ?: playback.songMediaId?.uri

                if (!checkIfPlayable(uri, context)) {
                    eventChannel.push(
                        PlaybackNotFoundEvent(
                            UiText.StringResource(
                                R.string.could_not_open_playback,
                                "${playback.title}, ${playback.artist}"
                            ), EventSeverity.Error
                        )
                    )
                    return@runOnUiThread
                }
            }

            is PlaybackType.Playback, is PlaybackType.Ad -> {}
        }


        if (playbackLocation == PlaybackLocation.CUSTOM_PLAYLIST)
            browser.seekTo(playback.mediaId, position)
        else
            invokeOnNewPlaylist(playback, position)
        browser.play()
    }

    @JvmName("invokePlaylist")
    suspend operator fun invoke(
        playlist: Playlist?,
        position: Duration? = null
    ) {
        if (playlist == null)
            return

        log.info("Setting playlist to ${playlist.mediaId}")
        browser.setPlaylist(playlist, position)
        browser.prepare()
        addNewPlaybackToHistory(playlist.current)
        browser.play()
    }


    private suspend fun invokeOnNewPlaylist(playback: Playback, position: Duration?) {
        val playlist = getPlaylistForPlayback(playback) ?: return
        invoke(
            playlist.copy(
                playbacks = playlist.playbacks.toMutableList()
                    .replaceWith(playback) { it.mediaId == playback.mediaId }), position
        )
    }


    private fun checkIfPlayable(uri: Uri?, context: Context): Boolean {
        val key = uri ?: return false

        var isPlayable = synchronized(cacheLock) { permissionCache[key] }
        if (isPlayable != null)
            return isPlayable

        isPlayable = key.isPlayable(context)

        return synchronized(cacheLock) {
            permissionCache[key] = isPlayable
            isPlayable
        }
    }
}