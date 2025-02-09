package com.tachyonmusic.database.data.data_source

import android.net.Uri
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tachyonmusic.core.ColorScheme
import com.tachyonmusic.database.domain.model.SETTINGS_DATABASE_TABLE_NAME
import com.tachyonmusic.database.domain.model.SettingsEntity
import com.tachyonmusic.util.Duration
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {

    /**
     * Selects the first row in the settings table. We can only have one [SettingsEntity] in the table
     */
    @Query("SELECT * FROM $SETTINGS_DATABASE_TABLE_NAME ORDER BY ROWID ASC LIMIT 1")
    suspend fun getSettings(): SettingsEntity?

    @Query("SELECT * FROM $SETTINGS_DATABASE_TABLE_NAME ORDER BY ROWID ASC LIMIT 1")
    fun observe(): Flow<SettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSettings(settings: SettingsEntity)

    @Query("UPDATE $SETTINGS_DATABASE_TABLE_NAME SET ignoreAudioFocus=:value")
    suspend fun setIgnoreAudioFocus(value: Boolean)

    @Query("UPDATE $SETTINGS_DATABASE_TABLE_NAME SET autoDownloadAlbumArtwork=:value")
    suspend fun setAutoDownloadSongMetadata(value: Boolean)

    @Query("UPDATE $SETTINGS_DATABASE_TABLE_NAME SET autoDownloadAlbumArtworkWifiOnly=:value")
    suspend fun setAutoDownloadSongMetadataWifiOnly(value: Boolean)

    @Query("UPDATE $SETTINGS_DATABASE_TABLE_NAME SET combineDifferentPlaybackTypes=:value")
    suspend fun setCombineDifferentPlaybackTypes(value: Boolean)

    @Query("UPDATE $SETTINGS_DATABASE_TABLE_NAME SET dynamicColors=:value")
    suspend fun setDynamicColors(value: Boolean)

    @Query("UPDATE $SETTINGS_DATABASE_TABLE_NAME SET audioUpdateInterval=:value")
    suspend fun setAudioUpdateInterval(value: Duration)

    @Query("UPDATE $SETTINGS_DATABASE_TABLE_NAME SET maxPlaybacksInHistory=:value")
    suspend fun setMaxPlaybacksInHistory(value: Int)

    @Query("UPDATE $SETTINGS_DATABASE_TABLE_NAME SET seekForwardIncrement=:interval")
    suspend fun setSeekForwardIncrement(interval: Duration)

    @Query("UPDATE $SETTINGS_DATABASE_TABLE_NAME SET seekBackIncrement=:interval")
    suspend fun setSeekBackIncrement(interval: Duration)

    @Query("UPDATE $SETTINGS_DATABASE_TABLE_NAME SET animateText=:value")
    suspend fun setAnimateText(value: Boolean)

    @Query("UPDATE $SETTINGS_DATABASE_TABLE_NAME SET shouldMillisecondsBeShown=:value")
    suspend fun setShouldMillisecondsBeShown(value: Boolean)

    @Query("UPDATE $SETTINGS_DATABASE_TABLE_NAME SET playNewlyCreatedCustomizedSong=:value")
    suspend fun setPlayNewlyCreatedCustomizedSong(value: Boolean)

    @Query("UPDATE $SETTINGS_DATABASE_TABLE_NAME SET colorScheme=:value")
    suspend fun setColorScheme(value: ColorScheme)

    @Query("UPDATE $SETTINGS_DATABASE_TABLE_NAME SET excludedSongFiles=:excludedFiles")
    suspend fun setExcludedSongFiles(excludedFiles: List<Uri>)

    @Query("UPDATE $SETTINGS_DATABASE_TABLE_NAME SET musicDirectories=:dirs")
    suspend fun setMusicDirectories(dirs: List<Uri>)
}