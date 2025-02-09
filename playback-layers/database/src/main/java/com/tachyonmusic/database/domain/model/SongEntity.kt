package com.tachyonmusic.database.domain.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.tachyonmusic.core.ArtworkType
import com.tachyonmusic.core.domain.MediaId
import com.tachyonmusic.util.Duration
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder.Companion.DECODE_DONE
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

const val SONG_DATABASE_TABLE_NAME = "Songs"

@Entity(tableName = SONG_DATABASE_TABLE_NAME)
@Serializable(with = SongEntitySerializer::class)
class SongEntity(
    mediaId: MediaId,
    title: String,
    artist: String,
    duration: Duration,

    // Whether the song should be hidden in the UI
    var isHidden: Boolean = false,
    var artworkType: String = ArtworkType.UNKNOWN,
    var artworkUrl: String? = null,

    @ColumnInfo(defaultValue = "null")
    var album: String? = null,

    /**
     * MusicBrainz ID used for metadata information
     */
    @ColumnInfo(defaultValue = "null")
    var mbid: String? = null,

    timestampCreatedAddedEdited: Long = System.currentTimeMillis(),
) : SinglePlaybackEntity(mediaId, title, artist, duration, timestampCreatedAddedEdited)

object SongEntitySerializer : KSerializer<SongEntity> {
    override val descriptor = buildClassSerialDescriptor("SongEntity") {
        element<String>("MediaId")
        element<String>("Title")
        element<String>("Artist")
        element<Long>("Duration")
        element<Boolean>("IsHidden", isOptional = true)
        element<String>("ArtworkType", isOptional = true)
        element<String>("ArtworkUrl", isOptional = true)
        element<String>("Album", isOptional = true)
        element<String>("MBID", isOptional = true)
        element<Long>("TimestampCreatedAddedEdited", isOptional = true)
    }

    override fun deserialize(decoder: Decoder) = decoder.decodeStructure(descriptor) {
        var mediaId: MediaId? = null
        var title: String? = null
        var artist: String? = null
        var duration: Duration? = null
        var isHidden = false
        var artworkType: String = ArtworkType.UNKNOWN
        var artworkUrl: String? = null
        var album: String? = null
        var mbid: String? = null
        var timestampCreatedAddedEdited = 0L

        loop@ while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                DECODE_DONE -> break@loop

                0 -> mediaId = MediaId.deserialize(decodeStringElement(descriptor, 0))
                1 -> title = decodeStringElement(descriptor, 1)
                2 -> artist = decodeStringElement(descriptor, 2)
                3 -> duration = Duration(decodeLongElement(descriptor, 3))
                4 -> isHidden = decodeBooleanElement(descriptor, 4)
                5 -> artworkType = decodeStringElement(descriptor, 5)
                6 -> artworkUrl = decodeStringElement(descriptor, 6).ifEmpty { null }
                7 -> album = decodeStringElement(descriptor, 7).ifBlank { null }
                8 -> mbid = decodeStringElement(descriptor, 8).ifBlank { null }
                9 -> timestampCreatedAddedEdited = decodeLongElement(descriptor, 9)

                else -> throw SerializationException("Unexpected index $index")
            }
        }

        SongEntity(
            mediaId!!,
            title!!,
            artist!!,
            duration!!,
            isHidden,
            artworkType,
            artworkUrl,
            album,
            mbid,
            timestampCreatedAddedEdited
        )
    }

    override fun serialize(encoder: Encoder, value: SongEntity) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.mediaId.toString())
            encodeStringElement(descriptor, 1, value.title)
            encodeStringElement(descriptor, 2, value.artist)
            encodeLongElement(descriptor, 3, value.duration.inWholeMilliseconds)
            encodeBooleanElement(descriptor, 4, value.isHidden)
            encodeStringElement(descriptor, 5, value.artworkType)
            encodeStringElement(descriptor, 6, value.artworkUrl ?: "")
            encodeStringElement(descriptor, 7, value.album ?: "")
            encodeStringElement(descriptor, 8, value.mbid ?: "")
            encodeLongElement(descriptor, 9, value.timestampCreatedAddedEdited)
        }
    }
}