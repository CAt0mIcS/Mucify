package com.tachyonmusic.media.data

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.google.common.collect.ImmutableList
import com.tachyonmusic.artwork.toLoop
import com.tachyonmusic.artwork.toPlaylist
import com.tachyonmusic.artwork.toSong
import com.tachyonmusic.core.domain.MediaId
import com.tachyonmusic.core.domain.playback.Playlist
import com.tachyonmusic.media.util.getItemsOnPageWithPageSize
import com.tachyonmusic.media.util.toMediaItems
import com.tachyonmusic.permission.domain.PermissionMapperRepository
import com.tachyonmusic.permission.domain.model.LoopPermissionEntity
import com.tachyonmusic.permission.domain.model.SongPermissionEntity
import com.tachyonmusic.playback_layers.PlaybackRepository
import kotlinx.coroutines.*


class BrowserTree(
    private val playbackRepository: PlaybackRepository,
    private val playbackPermissionRepository: PermissionMapperRepository
) {
    companion object {
        /**
         * Use this to get all playbacks
         */
        const val ROOT = "/"

        const val SONG_ROOT = ROOT + "Song/"

        const val PLAYLIST_ROOT = ROOT + "Playlist/"

        const val LOOP_ROOT = ROOT + "Loop/"
    }

    val root: MediaItem
        get() = MediaItem.Builder().apply {
            setMediaId(ROOT)
            setMediaMetadata(MediaMetadata.Builder().apply {
                setFolderType(MediaMetadata.FOLDER_TYPE_MIXED)
                setIsPlayable(false)
            }.build())
        }.build()

    suspend fun get(parentId: String, page: Int, pageSize: Int): ImmutableList<MediaItem>? =
        withContext(Dispatchers.IO) {
            when (parentId) {
                ROOT -> constraintItems(getSongs() + getLoops() + getPlaylists(), page, pageSize)
                SONG_ROOT -> constraintItems(getSongs(), page, pageSize)
                LOOP_ROOT -> constraintItems(getLoops(), page, pageSize)
                PLAYLIST_ROOT -> constraintItems(getPlaylists(), page, pageSize)
                else -> {
                    /**
                     * Assume that [parentId] is the id of a [Playback].
                     * If the id points to a [Playlist], return the items in the playlist
                     */
                    val mediaId = MediaId.deserializeIfValid(parentId)
                    if (mediaId != null) {
                        val playback = playbackPermissionRepository.getPlaylists()
                            .find { it.mediaId == mediaId }?.let {
                                // TODO: Artwork not included
                                it.toPlaylist(it.items.map { singlePb ->
                                    when (singlePb) {
                                        is SongPermissionEntity -> singlePb.toSong()
                                        is LoopPermissionEntity -> singlePb.toLoop()
                                        else -> TODO("Invalid single playback permission entity type ${singlePb::class.java.name}")
                                    }
                                })
                            }
                        if (playback != null)
                            return@withContext constraintItems(
                                playback.playbacks.toMediaItems(),
                                page,
                                pageSize
                            )
                    }

                    null
                }
            }
        }

    // TODO
    /** Type for a folder containing only playable media.  */
    /** Type for a folder containing media categorized by album.  */
    /** Type for a folder containing media categorized by artist.  */
    /** Type for a folder containing media categorized by genre.  */
    /** Type for a folder containing a playlist.  */
    /** Type for a folder containing media categorized by year.  */


    // TODO: Nullable?
    private suspend fun getSongs() = playbackRepository.getSongs().map { it.toMediaItem() }
    private suspend fun getLoops() = playbackRepository.getLoops().map { it.toMediaItem() }
    private suspend fun getPlaylists() = playbackRepository.getPlaylists().map { it.toMediaItem() }

    private fun constraintItems(
        playbacks: List<MediaItem>,
        page: Int,
        pageSize: Int
    ): ImmutableList<MediaItem> = getItemsOnPageWithPageSize(playbacks, page, pageSize)
}