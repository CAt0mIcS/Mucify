package com.tachyonmusic.core.data.playback

import android.net.Uri
import android.os.Bundle
import android.os.Parcel
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.tachyonmusic.core.data.EmbeddedArtwork
import com.tachyonmusic.core.data.RemoteArtwork
import com.tachyonmusic.core.data.constants.MetadataKeys
import com.tachyonmusic.core.data.constants.PlaybackType
import com.tachyonmusic.core.domain.Artwork
import com.tachyonmusic.core.domain.MediaId
import com.tachyonmusic.core.domain.TimingDataController
import com.tachyonmusic.core.domain.playback.Song
import com.tachyonmusic.core.data.ext.toByteArray
import com.tachyonmusic.core.data.ext.toInt
import com.tachyonmusic.util.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow

abstract class AbstractSong(
    final override val mediaId: MediaId,
    final override val title: String,
    final override val artist: String,
    final override val duration: Long,
) : Song, AbstractPlayback() {

    final override var timingData = TimingDataController(emptyList())

    abstract override val playbackType: PlaybackType.Song

    override val artwork = MutableStateFlow<Artwork?>(null)
    override val isArtworkLoading = MutableStateFlow(false)

    override fun toHashMap(): HashMap<String, Any?> = hashMapOf(
        "mediaId" to mediaId.toString()
    )

    override fun toMediaItem() = MediaItem.Builder().apply {
        setMediaId(mediaId.toString())
        setUri(uri)
        setMediaMetadata(toMediaMetadata())
    }.build()

    override fun toMediaMetadata() = MediaMetadata.Builder().apply {
        setFolderType(MediaMetadata.FOLDER_TYPE_NONE)
        setIsPlayable(true)

        when (val artworkVal = artwork.value) {
            null -> {}
            is EmbeddedArtwork -> launch(Dispatchers.IO) {
                setArtworkData(
                    artworkVal.bitmap.toByteArray(),
                    MediaMetadata.PICTURE_TYPE_FRONT_COVER
                )
            }
            is RemoteArtwork -> setArtworkUri(Uri.parse(artworkVal.uri.toURL().toString()))
        }

        setTitle(title)
        setArtist(artist)
        setExtras(Bundle().apply {
            putLong(MetadataKeys.Duration, duration)

            // Empty here to allow custom setting of timing data
            putParcelable(MetadataKeys.TimingData, TimingDataController())
            putParcelable(MetadataKeys.Playback, this@AbstractSong)
        })
    }.build()

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(mediaId.source)
        parcel.writeString(title)
        parcel.writeString(artist)
        parcel.writeLong(duration)
        parcel.writeParcelable(artwork.value, flags)
        parcel.writeInt(isArtworkLoading.value.toInt())
    }
}