package com.tachyonmusic.presentation.library.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tachyonmusic.core.data.constants.PlaybackType
import com.tachyonmusic.core.domain.playback.Playlist
import com.tachyonmusic.domain.model.SearchLocation
import com.tachyonmusic.domain.use_case.LoadArtworkForPlayback
import com.tachyonmusic.domain.use_case.PlayPlayback
import com.tachyonmusic.domain.use_case.PlaybackLocation
import com.tachyonmusic.media.domain.use_case.PlaybackSearchResult
import com.tachyonmusic.media.domain.use_case.SearchStoredPlaybacks
import com.tachyonmusic.playback_layers.domain.PlaybackRepository
import com.tachyonmusic.presentation.library.model.LibraryEntity
import com.tachyonmusic.presentation.library.model.toLibraryEntity
import com.tachyonmusic.util.Config
import com.tachyonmusic.util.copy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import javax.inject.Inject

// TODO: Highlights from [PlaybackSearchResult.*highlightIndices]
//    https://stackoverflow.com/questions/68981311/is-there-a-way-to-change-the-background-color-of-a-specific-word-in-outlined-tex
data class SearchResultUiEntity(
    val playback: LibraryEntity,
    val score: Float
)


@HiltViewModel
class PlaybackSearchViewModel @Inject constructor(
    private val searchStoredPlaybacks: SearchStoredPlaybacks,
    private val playbackRepository: PlaybackRepository,
    private val playPlayback: PlayPlayback,
    private val loadArtworkForPlayback: LoadArtworkForPlayback
) : ViewModel() {
    private var _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // TODO: Only load visible?
    private var itemDisplayRange = MutableStateFlow(10)

    private var _searchLocation = MutableStateFlow<SearchLocation>(SearchLocation.Local)
    val searchLocation = _searchLocation.asStateFlow()

    private var _filterPlaybackType = MutableStateFlow<PlaybackType>(PlaybackType.Song.Local())
    val filterPlaybackType = _filterPlaybackType.asStateFlow()

    val searchResults =
        combine(searchQuery, searchLocation, itemDisplayRange, filterPlaybackType) { query, location, itemRange, playbackType ->
            when (location) {
                SearchLocation.Local -> {
                    loadArtwork(
                        searchStoredPlaybacks(query, playbackType, itemRange),
                        0..Int.MAX_VALUE,
                        Config.SEARCH_ARTWORK_LOAD_QUALITY
                    ).mapNotNull {
                        SearchResultUiEntity(
                            it.playback?.toLibraryEntity() ?: return@mapNotNull null,
                            it.score
                        )
                    } // TODO: playback type
                }
            }.copy()
        }.debounce(500L).stateIn(
            viewModelScope + Dispatchers.IO,
            SharingStarted.WhileSubscribed(),
            emptyList()
        )


    fun search(query: String) {
        _searchQuery.update { query }
    }

    fun updateSearchLocation(location: SearchLocation) {
        _searchLocation.update { location }
    }

    fun increaseLoadingRange() {
        itemDisplayRange.update { searchResults.value.size + 10 }
    }

    fun resetLoadingRange() {
        itemDisplayRange.update { 10 }
    }

    fun onFilterSongs() {
        _filterPlaybackType.update { PlaybackType.Song.Local() }
    }

    fun onFilterRemixes() {
        _filterPlaybackType.update { PlaybackType.Remix.Local() }
    }

    fun onFilterPlaylists() {
        _filterPlaybackType.update { PlaybackType.Playlist.Local() }
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

    private fun LibraryEntity.toPlayback() = when (playbackType) {
        is PlaybackType.Song -> playbackRepository.songs.find { it.mediaId == mediaId }
        is PlaybackType.Remix -> playbackRepository.remixes.find { it.mediaId == mediaId }
        is PlaybackType.Ad -> error("Cannot find ad from playback type")
        is PlaybackType.Playlist -> error("Invalid function for Playlist")
    }

    private fun LibraryEntity.toPlaylist(): Playlist? {
        assert(playbackType is PlaybackType.Playlist)
        return playbackRepository.playlists.find { it.mediaId == mediaId }
    }

    private fun loadArtwork(
        searches: List<PlaybackSearchResult>,
        range: IntRange,
        quality: Int
    ): List<PlaybackSearchResult> {
        return searches
        // TODO: Artwork loading when searching
    }
}