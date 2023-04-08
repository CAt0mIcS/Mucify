package com.tachyonmusic.database.data.repository

import android.net.Uri
import com.tachyonmusic.database.data.data_source.SettingsDao
import com.tachyonmusic.database.domain.model.SettingsEntity
import com.tachyonmusic.database.domain.repository.SettingsRepository
import com.tachyonmusic.util.Duration
import kotlinx.coroutines.flow.map

class RoomSettingsRepository(
    private val dao: SettingsDao
) : SettingsRepository {

    override suspend fun getSettings() = dao.getSettings() ?: setSettings(SettingsEntity())

    override suspend fun setSettings(settings: SettingsEntity): SettingsEntity {
        dao.setSettings(settings)
        return settings
    }

    override fun observe() = dao.observe().map { it ?: SettingsEntity() }

    override suspend fun removeExcludedFilesRange(toRemove: List<Uri>) {
        val settings = getSettings()
        val newRange = settings.excludedSongFiles.toMutableList().apply { removeAll(toRemove) }
        dao.setExcludedSongFiles(newRange)
    }

    override suspend fun addExcludedFilesRange(toAdd: List<Uri>) {
        val settings = getSettings()
        val newRange = settings.excludedSongFiles.toMutableSet().apply { addAll(toAdd) }
        dao.setExcludedSongFiles(newRange.toList())
    }

    override suspend fun update(
        ignoreAudioFocus: Boolean?,
        autoDownloadAlbumArtwork: Boolean?,
        autoDownloadAlbumArtworkWifiOnly: Boolean?,
        combineDifferentPlaybackTypes: Boolean?,
        audioUpdateInterval: Duration?,
        maxPlaybacksInHistory: Int?,
        seekForwardIncrement: Duration?,
        seekBackIncrement: Duration?,
        animateText: Boolean?,
        shouldMillisecondsBeShown: Boolean?,
        excludedSongFiles: List<Uri>?,
        musicDirectories: List<Uri>?
    ) {
        if (ignoreAudioFocus != null)
            dao.setIgnoreAudioFocus(ignoreAudioFocus)

        if (autoDownloadAlbumArtwork != null)
            dao.setAutoDownloadAlbumArtwork(autoDownloadAlbumArtwork)

        if (autoDownloadAlbumArtworkWifiOnly != null)
            dao.setAutoDownloadAlbumArtworkWifiOnly(autoDownloadAlbumArtworkWifiOnly)

        if (combineDifferentPlaybackTypes != null)
            dao.setCombineDifferentPlaybackTypes(combineDifferentPlaybackTypes)

        if (audioUpdateInterval != null)
            dao.setAudioUpdateInterval(audioUpdateInterval)

        if (maxPlaybacksInHistory != null)
            dao.setMaxPlaybacksInHistory(maxPlaybacksInHistory)

        if (seekForwardIncrement != null)
            dao.setSeekForwardIncrement(seekForwardIncrement)

        if (seekBackIncrement != null)
            dao.setSeekBackIncrement(seekBackIncrement)

        if (animateText != null)
            dao.setAnimateText(animateText)

        if (shouldMillisecondsBeShown != null)
            dao.setShouldMillisecondsBeShown(shouldMillisecondsBeShown)

        if (excludedSongFiles != null)
            dao.setExcludedSongFiles(excludedSongFiles.toSet().toList())

        if (musicDirectories != null)
            dao.setMusicDirectories(musicDirectories.toSet().toList())
    }
}