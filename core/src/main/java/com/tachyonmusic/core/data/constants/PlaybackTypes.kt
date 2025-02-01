package com.tachyonmusic.core.data.constants

sealed class PlaybackType(val value: Int) {
    sealed class Playback(value: Int) : PlaybackType(value)

    sealed class Song(value: Int) : Playback(value) {
        class Local : Song(0)
    }

    sealed class Remix(value: Int) : Playback(value) {
        class Local : Remix(1)
    }

    sealed class Playlist(value: Int) : Playback(value) {
        class Local : Playlist(2)
    }

    sealed class Ad(value: Int) : PlaybackType(value) {
        class Banner : Ad(3)
        class NativeAppInstall : Ad(4)
    }

    companion object {
        fun build(value: String): PlaybackType {
            return when (value) {
                "*0*" -> Song.Local()
                "*1*" -> Remix.Local()
                "*2*" -> Playlist.Local()
                "*3*" -> Ad.Banner()
                "*4*" -> Ad.NativeAppInstall()
                else -> TODO("Unsupported value $value for playback type")
            }
        }
    }

    override fun toString() = "*$value*"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PlaybackType) return false

        if (value != other.value) return false

        return true
    }
}

