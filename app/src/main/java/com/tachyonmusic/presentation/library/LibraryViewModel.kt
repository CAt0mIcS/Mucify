package com.tachyonmusic.presentation.library

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.ads.nativead.NativeAd
import com.tachyonmusic.core.data.constants.PlaybackType
import com.tachyonmusic.core.domain.Artwork
import com.tachyonmusic.core.domain.MediaId
import com.tachyonmusic.core.domain.playback.Playlist
import com.tachyonmusic.domain.repository.AdInterface
import com.tachyonmusic.domain.repository.MediaBrowserController
import com.tachyonmusic.domain.use_case.DeletePlayback
import com.tachyonmusic.domain.use_case.LoadArtworkForPlayback
import com.tachyonmusic.domain.use_case.PlayPlayback
import com.tachyonmusic.domain.use_case.PlaybackLocation
import com.tachyonmusic.domain.use_case.library.AddSongToExcludedSongs
import com.tachyonmusic.domain.use_case.library.AssignArtworkToPlayback
import com.tachyonmusic.domain.use_case.library.QueryArtworkForPlayback
import com.tachyonmusic.domain.use_case.library.UpdatePlaybackMetadata
import com.tachyonmusic.logger.domain.Logger
import com.tachyonmusic.playback_layers.SortType
import com.tachyonmusic.playback_layers.domain.PlaybackRepository
import com.tachyonmusic.presentation.library.model.LibraryEntity
import com.tachyonmusic.presentation.library.model.toLibraryEntity
import com.tachyonmusic.util.Resource
import com.tachyonmusic.util.UiText
import com.tachyonmusic.util.copy
import com.tachyonmusic.util.findAndSkip
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import javax.inject.Inject

// TODO: Move
const val AD_INSERT_INTERVAL = 15

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val playbackRepository: PlaybackRepository,

    private val playPlayback: PlayPlayback,
    private val browser: MediaBrowserController,

    private val addSongToExcludedSongs: AddSongToExcludedSongs,
    private val deletePlayback: DeletePlayback,

    private val loadArtworkForPlayback: LoadArtworkForPlayback,
    private val queryArtworkForPlayback: QueryArtworkForPlayback,
    private val assignArtworkToPlayback: AssignArtworkToPlayback,

    private val updatePlaybackMetadata: UpdatePlaybackMetadata,
    private val log: Logger,

    adInterface: AdInterface
) : ViewModel() {

    val sortParams = playbackRepository.sortingPreferences

    private var songs = playbackRepository.songFlow.map { songs ->
        songs.filter { !it.isHidden }.map { it.copy() }
    }.stateIn(viewModelScope + Dispatchers.IO, SharingStarted.WhileSubscribed(), emptyList())

    private var remixes = playbackRepository.remixFlow.map { remixes ->
        remixes.map {
            it.copy()
        }
    }.stateIn(viewModelScope + Dispatchers.IO, SharingStarted.WhileSubscribed(), emptyList())

    private var playlists = playbackRepository.playlistFlow.map { playlists ->
        playlists.map {
            it.copy()
        }
    }.stateIn(viewModelScope + Dispatchers.IO, SharingStarted.WhileSubscribed(), emptyList())

    private var _filterType = MutableStateFlow<PlaybackType>(PlaybackType.Song.Local())
    val filterType = _filterType.asStateFlow()

    val availableSortTypes = filterType.map {
        when (it) {
            is PlaybackType.Song -> listOf(
                SortType.TitleAlphabetically, SortType.ArtistAlphabetically,
                SortType.DateCreatedOrEdited
            )

            is PlaybackType.Remix -> listOf(
                SortType.SubtitleAlphabetically, SortType.TitleAlphabetically,
                SortType.ArtistAlphabetically, SortType.DateCreatedOrEdited
            )

            is PlaybackType.Playlist -> listOf(
                SortType.SubtitleAlphabetically, SortType.DateCreatedOrEdited
            )

            is PlaybackType.Ad -> emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    private var _queriedArtwork = MutableStateFlow(emptyList<Artwork>())
    val queriedArtwork = _queriedArtwork.asStateFlow()

    private var _artworkLoadingError = MutableStateFlow<UiText?>(null)
    val artworkLoadingError = _artworkLoadingError.asStateFlow()

    val nativeAppInstallAdCache: StateFlow<List<NativeAd>> =
        adInterface.nativeAppInstallAdCache.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            emptyList()
        )

    private val artworkLoadingRange = MutableStateFlow(0..10)

    val items =
        combine(
            songs,
            remixes,
            playlists,
            filterType,
            artworkLoadingRange
        ) { songs, remixes, playlists, filterType, itemsToLoad ->
            val quality = 50

            when (filterType) {
                is PlaybackType.Song -> loadArtworkForPlayback(songs, itemsToLoad, quality).map {
                    it.toLibraryEntity()
                }

                is PlaybackType.Remix -> loadArtworkForPlayback(
                    remixes,
                    itemsToLoad,
                    quality
                ).map {
                    it.toLibraryEntity()
                }

                is PlaybackType.Playlist -> loadArtworkForPlayback(
                    playlists,
                    itemsToLoad,
                    quality
                ).map {
                    it.toLibraryEntity()
                }

                else -> emptyList()
            }.insertBeforeEvery(AD_INSERT_INTERVAL) {
                LibraryEntity(
                    mediaId = MediaId("${it / AD_INSERT_INTERVAL}"),
                    playbackType = PlaybackType.Ad.NativeAppInstall()
                )
            }
        }.stateIn(
            viewModelScope + Dispatchers.IO,
            SharingStarted.WhileSubscribed(),
            emptyList()
        )

    fun onFilterSongs() {
        _filterType.value = PlaybackType.Song.Local()
        onSortTypeChanged(SortType.TitleAlphabetically)
    }

    fun onFilterRemixes() {
        _filterType.value = PlaybackType.Remix.Local()
        onSortTypeChanged(SortType.TitleAlphabetically)
    }

    fun onFilterPlaylists() {
        _filterType.value = PlaybackType.Playlist.Local()
        onSortTypeChanged(SortType.TitleAlphabetically)
    }

    fun onSortTypeChanged(type: SortType) {
        playbackRepository.setSortingPreferences(sortParams.value.copy(type = type))
    }

    fun onItemClicked(entity: LibraryEntity) {
        viewModelScope.launch {
            if (entity.playbackType is PlaybackType.Playlist)
                playPlayback(entity.toPlaylist())
            else
                playPlayback(
                    entity.toPlayback(),
                    playbackLocation = PlaybackLocation.PREDEFINED_PLAYLIST
                )
        }
    }

    fun excludePlayback(entity: LibraryEntity) {
        viewModelScope.launch {
            val playback = entity.toPlayback() ?: return@launch
            if (playback.isSong)
                addSongToExcludedSongs(playback)
            else {
                if (browser.currentPlayback.value == playback)
                    if ((browser.currentPlaylist.value?.playbacks?.size ?: 0) > 1)
                        browser.seekToNext()
                    else {
                        browser.stop()
                        val newPlayback = withContext(Dispatchers.IO) {
                            playbackRepository.history.findAndSkip(skip = 1) { it.isPlayable }
                        }
                        browser.updatePlayback { newPlayback }
                    }
                deletePlayback(playback)
            }
        }
    }

    fun loadArtwork(range: IntRange, playbackItems: List<LibraryEntity>) {
        val rangeToUpdate = removeAdIndices(range, playbackItems)
        artworkLoadingRange.update { rangeToUpdate }
    }

    /**
     * Starts loading all artwork we can find for [playback] into [queriedArtwork]
     */
    fun queryArtwork(playback: LibraryEntity, searchQuery: String? = null) {
        _queriedArtwork.update { emptyList() }
        queryArtworkForPlayback(
            playback,
            searchQuery ?: playback.albumArtworkSearchQuery
        ).onEach { res ->
            if (res is Resource.Success)
                _queriedArtwork.update { (it + res.data!!).copy() }
            else if (res is Resource.Error)
                _artworkLoadingError.update { res.message!! }
        }.launchIn(viewModelScope + Dispatchers.IO)
    }

    fun assignArtworkToPlayback(artwork: Artwork, playback: LibraryEntity) {
        viewModelScope.launch {
            assignArtworkToPlayback(playback.mediaId, artwork)
        }
    }

    fun updateMetadata(
        playback: LibraryEntity,
        title: String?,
        artist: String?,
        name: String?,
        album: String?
    ) {
        viewModelScope.launch {
            updatePlaybackMetadata(
                playback.mediaId,
                oldTitle = playback.title,
                newTitle = title,
                oldArtist = playback.artist,
                newArtist = artist,
                oldName = playback.displayTitle,
                newName = name,
                oldAlbum = playback.album,
                newAlbum = album
            )
        }
    }

    private fun LibraryEntity.toPlayback() =
        when (playbackType) {
            is PlaybackType.Song -> songs.value.find { it.mediaId == mediaId }
            is PlaybackType.Remix -> remixes.value.find { it.mediaId == mediaId }
            is PlaybackType.Ad -> error("Cannot find ad from playback type")
            is PlaybackType.Playlist -> error("Invalid function for Playlist")
        }

    private fun LibraryEntity.toPlaylist(): Playlist? {
        assert(playbackType is PlaybackType.Playlist)
        return playlists.value.find { it.mediaId == mediaId }
    }

    private fun <E> List<E>.insertBeforeEvery(insertBeforeIdx: Int, elem: (i: Int) -> E): List<E> {
        val result = mutableListOf<E>()
        for (i in indices) {
            if (i % insertBeforeIdx == 0)
                result.add(elem(i))
            result.add(this[i])
        }
        return result
    }

    /**
     * The range received from the UI includes all ad indices which we don't need for ranged
     * loading of playback artwork. Function removes these unwanted indices
     */
    private fun removeAdIndices(range: IntRange, playbackItems: List<LibraryEntity>): IntRange {
        val numAds =
            playbackItems.count { it.playbackType is PlaybackType.Ad && nativeAppInstallAdCache.value.isNotEmpty() }
        return (range.first - numAds)..(range.last - numAds)
    }
}