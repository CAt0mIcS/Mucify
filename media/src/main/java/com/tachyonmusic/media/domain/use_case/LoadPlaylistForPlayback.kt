package com.tachyonmusic.media.domain.use_case

import androidx.media3.common.MediaItem
import com.tachyonmusic.util.Resource
import com.tachyonmusic.util.UiText
import com.tachyonmusic.core.data.playback.AbstractLoop
import com.tachyonmusic.core.data.playback.Playback
import com.tachyonmusic.core.data.playback.Playlist
import com.tachyonmusic.core.data.playback.Song
import com.tachyonmusic.media.R
import com.tachyonmusic.user.domain.UserRepository

class LoadPlaylistForPlayback(
    private val repository: UserRepository
) {
    operator fun invoke(playback: Playback?): Resource<Pair<List<MediaItem>, Int>> {
        var initialWindowIndex: Int? = null
        var items: List<MediaItem>? = null

        if (/*repository.combinePlaybackTypes*/false) {
            when (playback) {
                is Song -> {
                    initialWindowIndex = repository.songs.value.indexOf(playback)
                    items =
                        repository.songs.value.map { it.toMediaItem() } + repository.loops.value.map { it.toMediaItem() }
                }
                is AbstractLoop -> {
                    initialWindowIndex = repository.loops.value.indexOf(playback)
                    items =
                        repository.loops.value.map { it.toMediaItem() } + repository.songs.value.map { it.toMediaItem() }
                }
                is Playlist -> {
                    initialWindowIndex = playback.currentPlaylistIndex
                    items = playback.toMediaItemList()
                }
                null -> {
                    return Resource.Error(UiText.StringResource(R.string.invalid_playback))
                }
            }
        } else {
            when (playback) {
                is Song -> {
                    initialWindowIndex = repository.songs.value.indexOf(playback)
                    items = repository.songs.value.map { it.toMediaItem() }
                }
                is AbstractLoop -> {
                    initialWindowIndex = repository.loops.value.indexOf(playback)
                    items = repository.loops.value.map { it.toMediaItem() }
                }
                is Playlist -> {
                    items = playback.toMediaItemList()
                    initialWindowIndex = playback.currentPlaylistIndex
                }
                null -> {
                    return Resource.Error(UiText.StringResource(R.string.invalid_playback))
                }
            }
        }

        return Resource.Success(items to initialWindowIndex)
    }
}