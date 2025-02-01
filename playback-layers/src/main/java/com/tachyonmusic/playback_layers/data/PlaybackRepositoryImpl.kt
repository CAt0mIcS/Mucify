package com.tachyonmusic.playback_layers.data

import android.content.Context
import android.net.Uri
import com.tachyonmusic.core.ArtworkType
import com.tachyonmusic.core.data.EmbeddedArtwork
import com.tachyonmusic.core.data.RemoteArtwork
import com.tachyonmusic.core.domain.playback.Playback
import com.tachyonmusic.core.domain.playback.Playlist
import com.tachyonmusic.database.domain.model.HistoryEntity
import com.tachyonmusic.database.domain.model.PlaylistEntity
import com.tachyonmusic.database.domain.model.RemixEntity
import com.tachyonmusic.database.domain.model.SinglePlaybackEntity
import com.tachyonmusic.database.domain.model.SongEntity
import com.tachyonmusic.database.domain.repository.HistoryRepository
import com.tachyonmusic.database.domain.repository.PlaylistRepository
import com.tachyonmusic.database.domain.repository.RemixRepository
import com.tachyonmusic.database.domain.repository.SongRepository
import com.tachyonmusic.playback_layers.R
import com.tachyonmusic.playback_layers.SortingPreferences
import com.tachyonmusic.playback_layers.domain.PlaybackRepository
import com.tachyonmusic.playback_layers.domain.UriPermissionRepository
import com.tachyonmusic.playback_layers.domain.events.InvalidPlaylistItemEvent
import com.tachyonmusic.playback_layers.domain.events.PlaybackNotFoundEvent
import com.tachyonmusic.playback_layers.sortedBy
import com.tachyonmusic.playback_layers.toPlayback
import com.tachyonmusic.util.EventSeverity
import com.tachyonmusic.util.UiText
import com.tachyonmusic.util.domain.EventChannel
import com.tachyonmusic.util.maxAsyncChunked
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.net.URI
import kotlin.math.ceil

class PlaybackRepositoryImpl(
    private val songRepository: SongRepository,
    private val remixRepository: RemixRepository,
    private val playlistRepository: PlaylistRepository,
    private val historyRepository: HistoryRepository,

    uriPermissionRepository: UriPermissionRepository,

    private val context: Context,
    private val eventChannel: EventChannel
) : PlaybackRepository {

    // PlaybackRepository is alive until the end of the program, so it doesn't need to be cancelled
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var cacheLock = Any()
    private val permissionCache = mutableMapOf<Uri, Boolean>()

    private val _sortingPreferences = MutableStateFlow(SortingPreferences())
    override val sortingPreferences = _sortingPreferences.asStateFlow()

    private val flowRecompute = MutableStateFlow(false)

    init {
        // Invalidate the isPlayable cache every time the permissions change
        uriPermissionRepository.permissions.onEach {
            synchronized(cacheLock) {
                permissionCache.clear()
            }

            flowRecompute.update { !flowRecompute.value }
        }.launchIn(ioScope)
    }

    override val songFlow =
        combine(
            songRepository.observe().distinctUntilChanged(),
            sortingPreferences,
            flowRecompute
        ) { songEntities, sorting, _ ->
            transformSongs(songEntities, sorting)
        }.shareIn(ioScope, SharingStarted.Eagerly, replay = 1)

    override val remixFlow =
        combine(
            remixRepository.observe().distinctUntilChanged(),
            songFlow,
            sortingPreferences
        ) { remixEntities, songs, sorting ->
            transformRemixes(remixEntities, songs, sorting)
        }.shareIn(ioScope, SharingStarted.Eagerly, replay = 1)

    override val playlistFlow = combine(
        playlistRepository.observe().distinctUntilChanged(),
        songFlow,
        remixFlow,
        sortingPreferences
    ) { playlistEntities, songs, remixes, sorting ->
        transformPlaylists(playlistEntities, songs, remixes, sorting)
    }.shareIn(ioScope, SharingStarted.Eagerly, replay = 1)

    override val historyFlow = combine(
        historyRepository.observe().distinctUntilChanged(),
        songFlow,
        remixFlow
    ) { historyEntities, songs, remixes ->
        transformHistory(historyEntities, songs, remixes)
    }.shareIn(ioScope, SharingStarted.Eagerly, replay = 1)

    override val songs: List<Playback>
        get() = songFlow.replayCache.first()

    override val remixes: List<Playback>
        get() = remixFlow.replayCache.first()

    override val playlists: List<Playlist>
        get() = playlistFlow.replayCache.first()

    override val history: List<Playback>
        get() = historyFlow.replayCache.first()


    override fun setSortingPreferences(sortPrefs: SortingPreferences) {
        _sortingPreferences.update { sortPrefs }
    }

    private suspend fun transformSongs(
        entities: List<SongEntity>,
        sorting: SortingPreferences
    ): List<Playback> = withContext(Dispatchers.IO) {
        val playbacks = mutableListOf<Deferred<List<Playback>>>()

        if (entities.isEmpty())
            return@withContext emptyList()

        for (entityChunk in entities.maxAsyncChunked()) {
            playbacks += async {
                entityChunk.map { entity ->
                    if (entity.mediaId.isLocalSong) {
                        entity.toPlayback(
                            when (entity.artworkType) {
                                ArtworkType.REMOTE -> RemoteArtwork(URI(entity.artworkUrl))
                                ArtworkType.EMBEDDED -> EmbeddedArtwork(null, entity.mediaId.uri!!)
                                else -> null
                            },
                            entity.checkIfPlayable(context) // This takes long
                        )
                    } else
                        TODO("Invalid media id ${entity.mediaId}")
                }
            }
        }

        playbacks.awaitAll().flatten().sortedBy(sorting)
    }


    private fun transformRemixes(
        entities: List<RemixEntity>,
        songs: List<Playback>,
        sorting: SortingPreferences
    ): List<Playback> {
        assert(songs.all { it.isSong })

        return entities.map { entity ->
            val underlyingSong = songs.find { it.mediaId == entity.mediaId.underlyingMediaId }

//            if (underlyingSong == null) {
//                eventChannel.push(
//                    PlaybackNotFoundEvent(
//                        UiText.StringResource(
//                            R.string.playback_for_remix_not_found,
//                            entity.songTitle,
//                            entity.title
//                        ),
//                        EventSeverity.Warning
//                    )
//                )
//            }

            entity.toPlayback(underlyingSong)
        }.sortedBy(sorting)
    }

    private fun transformPlaylists(
        entities: List<PlaylistEntity>,
        songs: List<Playback>,
        remixes: List<Playback>,
        sorting: SortingPreferences
    ): List<Playlist> {
        assert(songs.all { it.isSong })
        assert(remixes.all { it.isRemix })

        return entities.map { entity ->
            // TODO: Somehow display deleted songs and remixes
            val items = entity.items.mapNotNull { playlistItem ->
                val foundItem = if (playlistItem.isLocalSong) {
                    songs.find { playlistItem == it.mediaId }
                } else if (playlistItem.isLocalRemix) {
                    remixes.find { playlistItem == it.mediaId }
                } else {
                    TODO("Invalid playlist item $playlistItem")
                }

//                if (foundItem == null) {
//                    eventChannel.push(
//                        InvalidPlaylistItemEvent(
//                            UiText.StringResource(
//                                R.string.playback_for_playlist_not_found,
//                                playlistItem.uri?.path ?: "Unknown",
//                                entity.name
//                            ),
//                            EventSeverity.Warning
//                        )
//                    )
//                }

                foundItem
            }

            if (entity.mediaId.isLocalPlaylist)
                Playlist(
                    entity.mediaId,
                    items,
                    entity.currentItemIndex,
                    entity.timestampCreatedAddedEdited
                )
            else
                TODO("Invalid playlist conversion media id ${entity.mediaId}")
        }.sortedBy(sorting)
    }

    private fun transformHistory(
        entities: List<HistoryEntity>,
        songs: List<Playback>,
        remixes: List<Playback>
    ): List<Playback> {
        assert(songs.all { it.isSong })
        assert(remixes.all { it.isRemix })

        // TODO: If remixes get loaded before songs then there will be many [items == null] and eventChannel calls

        return entities.mapNotNull { historyItem ->
            val item = songs.find { historyItem.mediaId == it.mediaId }
                ?: remixes.find { historyItem.mediaId == it.mediaId }

//            if (item == null) {
//                eventChannel.push(
//                    PlaybackNotFoundEvent(
//                        UiText.StringResource(
//                            R.string.playback_not_found,
//                            historyItem.mediaId.uri?.path ?: "Unknown"
//                        ),
//                        EventSeverity.Debug
//                    )
//                )
//            }

            item
        }
    }


    private fun Uri.isPlayable(context: Context) = try {
        context.contentResolver.openInputStream(this)?.close() ?: false
        true
    } catch (e: Exception) {
        when (e) {
            is FileNotFoundException, is IllegalArgumentException, is SecurityException -> false
            else -> throw e
        }
    }

    private fun SinglePlaybackEntity.checkIfPlayable(context: Context): Boolean {
        val key = mediaId.uri ?: return false

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