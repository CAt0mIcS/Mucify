package com.tachyonmusic.database.domain.model

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tachyonmusic.core.ColorScheme
import com.tachyonmusic.util.Duration
import com.tachyonmusic.util.ms
import com.tachyonmusic.util.sec
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

const val SETTINGS_DATABASE_TABLE_NAME = "Settings"

@Serializable(with = SettingsEntitySerializer::class)
@Entity(tableName = SETTINGS_DATABASE_TABLE_NAME)
data class SettingsEntity(
    var ignoreAudioFocus: Boolean = false,
    @ColumnInfo(name = "autoDownloadAlbumArtwork")
    var autoDownloadSongMetadata: Boolean = false,
    @ColumnInfo(name = "autoDownloadAlbumArtworkWifiOnly")
    var autoDownloadSongMetadataWifiOnly: Boolean = true,
    var combineDifferentPlaybackTypes: Boolean = false,

    @ColumnInfo(defaultValue = "false")
    var dynamicColors: Boolean = false,
    var audioUpdateInterval: Duration = 100.ms,
    var maxPlaybacksInHistory: Int = 25,
    var seekForwardIncrement: Duration = 10.sec,
    var seekBackIncrement: Duration = 10.sec,
    var animateText: Boolean = true,
    /**
     * Specifies if milliseconds should be shown in the current position and duration texts above
     * the seek bar in the player
     */
    var shouldMillisecondsBeShown: Boolean = false,

    /**
     * The app may revert customization changes to the currently playing media item due to the
     * playlist update that happens when saving a newly created remix. This controls
     * whether you want to play the newly created remix or keep playing the old playback
     * which will revert to either no customization (if it's a song) or customization of the remix
     */
    @ColumnInfo(name = "playNewlyCreatedCustomizedSong")
    var playNewlyCreatedRemix: Boolean = true,

    @ColumnInfo(defaultValue = ColorScheme.SYSTEM_NAME)
    var colorScheme: ColorScheme = ColorScheme.System,

    var excludedSongFiles: List<Uri> = emptyList(),
    var musicDirectories: List<Uri> = emptyList(),
    @PrimaryKey var id: Int = 0
)

object SettingsEntitySerializer : KSerializer<SettingsEntity> {
    override val descriptor = buildClassSerialDescriptor("SettingsEntity") {
        element<Boolean>("ignoreAudioFocus", isOptional = true)
        element<Boolean>("autoDownloadAlbumArtwork", isOptional = true)
        element<Boolean>("autoDownloadAlbumArtworkWifiOnly", isOptional = true)
        element<Boolean>("combineDifferentPlaybackTypes", isOptional = true)
        element<Long>("audioUpdateInterval", isOptional = true)
        element<Int>("maxPlaybacksInHistory", isOptional = true)
        element<Long>("seekForwardIncrement", isOptional = true)
        element<Long>("seekBackIncrement", isOptional = true)
        element<Boolean>("animateText", isOptional = true)
        element<Boolean>("shouldMillisecondsBeShown", isOptional = true)
        element<Boolean>("playNewlyCreatedCustomizedSong", isOptional = true)
        element<List<String>>("excludedSongFiles", isOptional = true)
        element<List<String>>("musicDirectories", isOptional = true)
        element<Int>("id", isOptional = true)

        element<Boolean>("dynamicColors", isOptional = true)
        element<String>("colorScheme", isOptional = true)
    }

    override fun deserialize(decoder: Decoder) = decoder.decodeStructure(descriptor) {
        var ignoreAudioFocus = false
        var autoDownloadAlbumArtwork = true
        var autoDownloadAlbumArtworkWifiOnly = true
        var combineDifferentPlaybackTypes = false
        var dynamicColors = true
        var audioUpdateInterval: Duration = 100.ms
        var maxPlaybacksInHistory = 25
        var seekForwardIncrement: Duration = 10.sec
        var seekBackIncrement: Duration = 10.sec
        var animateText = true
        var shouldMillisecondsBeShown = false
        var playNewlyCreatedRemix = true
        var colorScheme: ColorScheme = ColorScheme.System
        var excludedSongFiles: List<Uri> = emptyList()
        var musicDirectories: List<Uri> = emptyList()
        var id = 0

        loop@ while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break@loop

                0 -> ignoreAudioFocus = decodeBooleanElement(descriptor, 0)
                1 -> autoDownloadAlbumArtwork = decodeBooleanElement(descriptor, 1)
                2 -> autoDownloadAlbumArtworkWifiOnly = decodeBooleanElement(descriptor, 2)
                3 -> combineDifferentPlaybackTypes = decodeBooleanElement(descriptor, 3)
                4 -> audioUpdateInterval = Duration(decodeLongElement(descriptor, 4))
                5 -> maxPlaybacksInHistory = decodeIntElement(descriptor, 5)
                6 -> seekForwardIncrement = Duration(decodeLongElement(descriptor, 6))
                7 -> seekBackIncrement = Duration(decodeLongElement(descriptor, 7))
                8 -> animateText = decodeBooleanElement(descriptor, 8)
                9 -> shouldMillisecondsBeShown = decodeBooleanElement(descriptor, 9)
                10 -> playNewlyCreatedRemix = decodeBooleanElement(descriptor, 10)
                11 -> excludedSongFiles = decodeSerializableElement(
                    descriptor,
                    11,
                    ListSerializer(String.serializer())
                ).map { Uri.parse(it) }

                12 -> musicDirectories = decodeSerializableElement(
                    descriptor,
                    12,
                    ListSerializer(String.serializer())
                ).map { Uri.parse(it) }

                13 -> id = decodeIntElement(descriptor, 13)
                14 -> dynamicColors = decodeBooleanElement(descriptor, 14)
                15 -> colorScheme = ColorScheme.fromString(decodeStringElement(descriptor, 15))

                else -> throw SerializationException("Unexpected index $index")
            }
        }

        SettingsEntity(
            ignoreAudioFocus,
            autoDownloadAlbumArtwork,
            autoDownloadAlbumArtworkWifiOnly,
            combineDifferentPlaybackTypes,
            dynamicColors,
            audioUpdateInterval,
            maxPlaybacksInHistory,
            seekForwardIncrement,
            seekBackIncrement,
            animateText,
            shouldMillisecondsBeShown,
            playNewlyCreatedRemix,
            colorScheme,
            excludedSongFiles,
            musicDirectories,
            id
        )
    }

    override fun serialize(encoder: Encoder, value: SettingsEntity) {
        encoder.encodeStructure(descriptor) {
            encodeBooleanElement(descriptor, 0, value.ignoreAudioFocus)
            encodeBooleanElement(descriptor, 1, value.autoDownloadSongMetadata)
            encodeBooleanElement(descriptor, 2, value.autoDownloadSongMetadataWifiOnly)
            encodeBooleanElement(descriptor, 3, value.combineDifferentPlaybackTypes)
            encodeLongElement(descriptor, 4, value.audioUpdateInterval.inWholeMilliseconds)
            encodeIntElement(descriptor, 5, value.maxPlaybacksInHistory)
            encodeLongElement(descriptor, 6, value.seekForwardIncrement.inWholeMilliseconds)
            encodeLongElement(descriptor, 7, value.seekBackIncrement.inWholeMilliseconds)
            encodeBooleanElement(descriptor, 8, value.animateText)
            encodeBooleanElement(descriptor, 9, value.shouldMillisecondsBeShown)
            encodeBooleanElement(descriptor, 10, value.playNewlyCreatedRemix)
            encodeSerializableElement(
                descriptor,
                11,
                ListSerializer(String.serializer()),
                value.excludedSongFiles.map { it.toString() })
            encodeSerializableElement(
                descriptor,
                12,
                ListSerializer(String.serializer()),
                value.musicDirectories.map { it.toString() })
            encodeIntElement(descriptor, 13, value.id)
            encodeBooleanElement(descriptor, 14, value.dynamicColors)
            encodeStringElement(descriptor, 15, value.colorScheme.name)
        }
    }
}