package com.tachyonmusic.playback_layers.data

import com.tachyonmusic.core.domain.playback.SinglePlayback
import com.tachyonmusic.database.domain.repository.SettingsRepository
import com.tachyonmusic.playback_layers.domain.PlaybackRepository
import com.tachyonmusic.playback_layers.domain.PredefinedPlaylistsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.plus

class PredefinedPlaylistsRepositoryImpl(
    playbackRepository: PlaybackRepository,
    settingsRepository: SettingsRepository,
    externalScope: CoroutineScope
) : PredefinedPlaylistsRepository {
    private val _songPlaylist = MutableStateFlow<List<SinglePlayback>>(emptyList())
    override val songPlaylist = _songPlaylist.asStateFlow()

    private val _remixPlaylist = MutableStateFlow<List<SinglePlayback>>(emptyList())
    override val remixPlaylist = _remixPlaylist.asStateFlow()

    init {
        combine(
            playbackRepository.songFlow,
            playbackRepository.remixFlow,
            settingsRepository.observe()
        ) { songs, remixes, settings ->
            val filteredSongs = songs.filter { it.isPlayable && !it.isHidden }
            val filteredRemixes = remixes.filter { it.isPlayable }

            if (settings.combineDifferentPlaybackTypes) {
                _songPlaylist.update { filteredSongs + filteredRemixes }
                _remixPlaylist.update { filteredRemixes + filteredSongs }
            } else {
                _songPlaylist.update { filteredSongs }
                _remixPlaylist.update { filteredRemixes }
            }
        }.launchIn(externalScope + Dispatchers.IO)
    }
}