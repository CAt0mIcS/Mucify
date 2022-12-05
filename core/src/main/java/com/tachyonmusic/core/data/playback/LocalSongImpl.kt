package com.tachyonmusic.core.data.playback

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import com.tachyonmusic.core.constants.PlaybackType
import com.tachyonmusic.core.data.SongMetadata
import com.tachyonmusic.core.domain.MediaId
import com.tachyonmusic.core.domain.playback.Song
import java.io.File

/**
 * Song stored in local storage with a path in the filesystem
 */
class LocalSongImpl(
    mediaId: MediaId,
    title: String,
    artist: String,
    duration: Long
) : AbstractSong(mediaId, title, artist, duration) {

    override val playbackType = PlaybackType.Song.Local()

    val path: File
        get() = mediaId.path!!

    override val uri: Uri
        get() = Uri.fromFile(path)

    constructor(parcel: Parcel) : this(
        MediaId(parcel.readString()!!),
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readLong()
    )

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<LocalSongImpl> {
            override fun createFromParcel(parcel: Parcel) = LocalSongImpl(parcel)
            override fun newArray(size: Int): Array<LocalSongImpl?> = arrayOfNulls(size)
        }

        fun build(path: File): Song =
            SongMetadata(path).run {
                return@run LocalSongImpl(MediaId.ofLocalSong(path), title, artist, duration)
            }

        fun build(mediaId: MediaId) = build(mediaId.path!!)

        fun build(map: Map<String, Any?>) = build(MediaId(map["mediaId"]!! as String))
    }
}