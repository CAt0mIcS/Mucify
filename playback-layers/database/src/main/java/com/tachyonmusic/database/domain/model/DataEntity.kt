package com.tachyonmusic.database.domain.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tachyonmusic.core.ArtworkType
import com.tachyonmusic.core.RepeatMode
import com.tachyonmusic.core.domain.MediaId
import com.tachyonmusic.util.Duration
import com.tachyonmusic.util.ms
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

const val DATA_DATABASE_TABLE_NAME = "Data"

@Serializable(with = DataEntitySerializer::class)
@Entity(tableName = DATA_DATABASE_TABLE_NAME)
data class DataEntity(
    var recentlyPlayedMediaId: MediaId? = null,
    var currentPositionInRecentlyPlayedPlayback: Duration = 0.ms,
    var recentlyPlayedDuration: Duration = 0.ms,
    var recentlyPlayedArtworkType: String = ArtworkType.UNKNOWN,
    var recentlyPlayedArtworkUrl: String? = null,

    var repeatMode: RepeatMode = RepeatMode.All,

    /**
     * Presents the maximum allowed number of stored remixes. This number can be increased by watching
     * an ad
     */
    var maxRemixCount: Int = 2,
    var onboardingCompleted: Boolean = false,

    @PrimaryKey var id: Int = 0,
)


object DataEntitySerializer : KSerializer<DataEntity> {
    override val descriptor = buildClassSerialDescriptor("DataEntity") {
        element<String>("RecentlyPlayedMediaId", isOptional = true)
        element<Long>("CurrentPositionInRecentlyPlayedPlayback", isOptional = true)
        element<Long>("RecentlyPlayedDuration", isOptional = true)
        element<String>("RecentlyPlayedArtworkType", isOptional = true)
        element<String>("RecentlyPlayedArtworkUrl", isOptional = true)
        element<Int>("RepeatMode", isOptional = true)
        element<Int>("MaxRemixCount", isOptional = true)
        element<Int>("Id", isOptional = true)
        element<Boolean>("OnboardingCompleted", isOptional = true)
    }

    override fun deserialize(decoder: Decoder) = decoder.decodeStructure(descriptor) {
        var recentlyPlayedMediaId: MediaId? = null
        var currentPositionInRecentlyPlayedPlayback: Duration = 0.ms
        var recentlyPlayedDuration: Duration = 0.ms
        var recentlyPlayedArtworkType: String = ArtworkType.UNKNOWN
        var recentlyPlayedArtworkUrl: String? = null
        var repeatMode: RepeatMode = RepeatMode.All
        var maxRemixCount = 0
        var id = 0
        var onboardingCompleted = false

        loop@ while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break@loop

                0 -> recentlyPlayedMediaId = MediaId.deserializeIfValid(
                    decodeStringElement(
                        descriptor, 0
                    ).ifEmpty { null }
                )

                1 -> currentPositionInRecentlyPlayedPlayback =
                    Duration(decodeLongElement(descriptor, 1))

                2 -> recentlyPlayedDuration = Duration(decodeLongElement(descriptor, 2))
                3 -> recentlyPlayedArtworkType = decodeStringElement(descriptor, 3)
                4 -> recentlyPlayedArtworkUrl = decodeStringElement(descriptor, 4).ifEmpty { null }
                5 -> repeatMode = RepeatMode.fromId(decodeIntElement(descriptor, 5))
                6 -> maxRemixCount = decodeIntElement(descriptor, 6)
                7 -> id = decodeIntElement(descriptor, 7)
                8 -> onboardingCompleted = decodeBooleanElement(descriptor, 8)

                else -> throw SerializationException("Unexpected index $index")
            }
        }

        DataEntity(
            recentlyPlayedMediaId,
            currentPositionInRecentlyPlayedPlayback,
            recentlyPlayedDuration,
            recentlyPlayedArtworkType,
            recentlyPlayedArtworkUrl,
            repeatMode,
            maxRemixCount,
            onboardingCompleted,
            id
        )
    }

    override fun serialize(encoder: Encoder, value: DataEntity) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.recentlyPlayedMediaId?.toString() ?: "")
            encodeLongElement(
                descriptor,
                1,
                value.currentPositionInRecentlyPlayedPlayback.inWholeMilliseconds
            )
            encodeLongElement(descriptor, 2, value.recentlyPlayedDuration.inWholeMilliseconds)
            encodeStringElement(descriptor, 3, value.recentlyPlayedArtworkType)
            encodeStringElement(descriptor, 4, value.recentlyPlayedArtworkUrl ?: "")
            encodeIntElement(descriptor, 5, value.repeatMode.id)
            encodeIntElement(descriptor, 6, value.maxRemixCount)
            encodeIntElement(descriptor, 7, value.id)
            encodeBooleanElement(descriptor, 8, value.onboardingCompleted)
        }
    }
}