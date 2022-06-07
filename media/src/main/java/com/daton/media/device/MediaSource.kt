package com.daton.media.device

import android.os.Environment
import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import com.daton.media.data.MediaId
import com.daton.media.data.SongMetadata
import com.daton.media.ext.*
import java.io.File


/**
 * Class which manages and holds access to the entire media library. Once loaded, all songs, loops
 * and playlists available on the device as device files will be accessible through this class.
 */
class MediaSource {

    companion object {
        const val TAG = "MediaSource"

        /**
         * State indicating the source was created, but no initialization has performed.
         */
        const val STATE_CREATED = 1

        /**
         * State indicating initialization of the source is in progress.
         */
        const val STATE_INITIALIZING = 2

        /**
         * State indicating the source has been initialized and is ready to be used.
         */
        const val STATE_INITIALIZED = 3

        /**
         * State indicating an error has occurred.
         */
        const val STATE_ERROR = 4

        // TODO: Move somewhere else as MediaStore is no longer loading loops and playlists
        const val LoopFileExtension = "loop"
        const val PlaylistFileExtension = "playlist"

        // TODO: Does ExoPlayer support more audio formats? Does it support all of the listed ones? Do all of them work?
        val SupportedAudioExtensions: List<String> = listOf(
            "3gp",
            "mp4",
            "m4a",
            "aac",
            "ts",
            "flac",
            "imy",
            "mp3",
            "mkv",
            "ogg",
            "wav"
        )

        fun loadSong(file: File): MediaMetadataCompat {
            return MediaMetadataCompat.Builder().apply {
                mediaId = MediaId.fromSongFile(file)
//                this.path = file

                SongMetadata(file).let { songMetadata ->
                    title = songMetadata.title
                    artist = songMetadata.artist
                    albumArt = songMetadata.albumArt
                    duration = songMetadata.duration
                }


            }.build()
        }
    }

    /**
     * Path to the external storage music directory
     */
    lateinit var musicDirectory: File
        private set


    var state: Int = STATE_CREATED
        private set(value) {
            Log.d(TAG, "Setting state to $value")
            if (value == STATE_INITIALIZED || value == STATE_ERROR) {
                synchronized(onReadyListeners) {
                    field = value
                    onReadyListeners.forEach { listener ->
                        listener(state == STATE_INITIALIZED)
                    }
                }
            } else {
                field = value
            }
        }

    /**
     * List of all available media items
     * TODO Should have separate classes for Song, Playlist, ...
     */
    val songs = mutableListOf<MediaMetadataCompat>()
    val loops = mutableListOf<Loop>()
    val playlists = mutableListOf<Playlist>()

    private var onReadyListeners = mutableListOf<(Boolean) -> Unit>()
    private var onChangedListeners = mutableListOf<() -> Unit>()

    @JvmName("plusAssignLoop")
    operator fun plusAssign(items: List<Loop>) {
        loops += items
        invokeOnChanged()
    }

    @JvmName("plusAssignPlaylist")
    operator fun plusAssign(items: List<Playlist>) {
        playlists += items
        invokeOnChanged()
    }


    /**
     * Should be called after storage permission is granted to load music that is stored in the phone's
     * external storage
     */
    fun loadDeviceFiles() {
        /**
         * Music directory may not be available, if so we'll set the state to STATE_ERROR
         */
        // TODO: What if music is stored somewhere else?
        val musicDir =
            File(Environment.getExternalStorageDirectory().absolutePath + "/Music")
        if (musicDir.exists()) {
            musicDirectory = musicDir
            Log.d(TAG, "Settings music directory to ${musicDirectory.absolutePath}")
        } else {
            Log.e(TAG, "Music directory not available")
            state = STATE_ERROR
        }


        loadSongs(musicDirectory)
        state = STATE_INITIALIZED
        invokeOnChanged()
    }


    fun whenReady(performAction: (Boolean) -> Unit): Boolean {
        return when (state) {
            STATE_CREATED, STATE_INITIALIZING -> {
                onReadyListeners += performAction
                false
            }
            else -> {
                performAction(state != STATE_ERROR)
                true
            }
        }
    }

    fun onChanged(performAction: () -> Unit) {
        synchronized(onChangedListeners) {
            onChangedListeners += performAction
        }
    }

    fun getSong(mediaId: MediaId) = songs.find { it.mediaId == mediaId }
    fun getLoop(mediaId: MediaId) = loops.find { it.mediaId == mediaId }
    fun getPlaylist(mediaId: MediaId) = playlists.find { it.mediaId == mediaId }

    private fun invokeOnChanged() {
        synchronized(onChangedListeners) {
            onChangedListeners.forEach { listener -> listener() }
        }
    }

    fun forEachSong(perSong: (MediaMetadataCompat) -> Unit) {
        for (song in songs)
            perSong(song)
    }

    fun forEachLoop(perLoop: (Loop) -> Unit) {
        for (loop in loops)
            perLoop(loop)
    }

    fun forEachPlaylist(perPlaylist: (Playlist) -> Unit) {
        for (playlist in playlists)
            perPlaylist(playlist)
    }

    fun findSong(pred: (MediaMetadataCompat) -> Boolean): MediaMetadataCompat? {
        for (song in songs)
            if (pred(song))
                return song
        return null
    }

    fun findLoop(pred: (Loop) -> Boolean): Loop? {
        for (loop in loops)
            if (pred(loop))
                return loop
        return null
    }

    fun findPlaylist(pred: (Playlist) -> Boolean): Playlist? {
        for (playlist in playlists)
            if (pred(playlist))
                return playlist
        return null
    }


    fun indexOfSong(pred: (MediaMetadataCompat) -> Boolean): Int {
        for (i in 0 until songs.size)
            if (pred(songs[i]))
                return i
        return -1
    }

    fun indexOfLoop(pred: (Loop) -> Boolean): Int {
        for (i in 0 until loops.size)
            if (pred(loops[i]))
                return i
        return -1
    }

    fun indexOfPlaylist(pred: (Playlist) -> Boolean): Int {
        for (i in 0 until playlists.size)
            if (pred(playlists[i]))
                return i
        return -1
    }


    private fun loadSongs(path: File?) {
        if (path == null || !path.exists()) return
        val files = path.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) loadSongs(file) else {
                if (file.isSongFile) {
                    songs += loadSong(file)
                }
            }
        }
    }
}