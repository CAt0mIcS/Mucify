package com.tachyonmusic.database.util

import android.content.Context
import android.net.Uri
import com.tachyonmusic.core.ArtworkType
import com.tachyonmusic.core.data.RemoteArtwork
import com.tachyonmusic.core.domain.playback.Playback
import com.tachyonmusic.core.domain.playback.Playlist
import com.tachyonmusic.database.domain.model.PlaylistEntity
import com.tachyonmusic.database.domain.model.RemixEntity
import com.tachyonmusic.database.domain.model.SinglePlaybackEntity
import com.tachyonmusic.database.domain.model.SongEntity
import java.io.FileNotFoundException

fun Playback.toEntity(): SinglePlaybackEntity =
    if (isSong) toSongEntity()
    else if (isRemix) toRemixEntity()
    else TODO("Invalid playback type ${this::class.java.name}")


fun Playback.toSongEntity(): SongEntity {
    val artworkType = ArtworkType.getType(this)
    return SongEntity(
        mediaId,
        title,
        artist,
        duration,
        isHidden,
        artworkType,
        if (artwork is RemoteArtwork)
            (artwork as RemoteArtwork).uri.toURL().toString()
        else null
    )
}

fun Playback.toRemixEntity(): RemixEntity {
    return RemixEntity(
        mediaId,
        title,
        artist,
        duration,
        timingData.timingData,
        timingData.currentIndex,
        bassBoost, virtualizerStrength, equalizerBands, playbackParameters, reverb
    )
}

fun Playlist.toEntity() = PlaylistEntity(
    name,
    mediaId,
    playbacks.map { it.mediaId },
    currentPlaylistIndex,
    timestampCreatedAddedEdited
)

fun Uri.isPlayable(context: Context) = try {
    context.contentResolver.openInputStream(this)?.close() ?: false
    true
} catch (e: Exception) {
    when (e) {
        is FileNotFoundException, is IllegalArgumentException, is SecurityException -> false
        else -> throw e
    }
}
