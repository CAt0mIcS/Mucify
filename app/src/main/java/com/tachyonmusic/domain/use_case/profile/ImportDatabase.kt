package com.tachyonmusic.domain.use_case.profile

import android.content.Context
import android.net.Uri
import com.tachyonmusic.database.data.data_source.Database
import com.tachyonmusic.database.domain.repository.SettingsRepository
import com.tachyonmusic.domain.repository.MediaBrowserController
import com.tachyonmusic.playback_layers.domain.PlaybackRepository
import com.tachyonmusic.playback_layers.domain.UriPermissionRepository
import com.tachyonmusic.util.runOnUiThreadAsync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class ImportDatabase(
    private val database: Database,
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val permissionRepository: UriPermissionRepository,
    private val mediaBrowser: MediaBrowserController,
    private val playbackRepository: PlaybackRepository
) {
    /**
     * @return empty list if the database was imported successfully and if all the permissions to access
     * the imported songs already exist, list with missing permission [Uri]s if it was imported successfully but the permissions don't
     * exist and null if there was an error
     */
    suspend operator fun invoke(source: Uri?) =
        withContext(Dispatchers.IO) {
            if (source == null)
                return@withContext null

            try {
                val inputStream =
                    context.contentResolver.openInputStream(source) ?: return@withContext null
                val jsonString = inputStream.readBytes().decodeToString()
                inputStream.close()

                database.checkpoint()
                database.overrideFromJson(jsonString)

                /**
                 * Make sure the [MediaBrowserController] is up to date with a possible new
                 * history item that was loaded in (TODO: Should happen automatically)
                 */
                val latest = playbackRepository.history.find { it.isPlayable }
                runOnUiThreadAsync {
                    mediaBrowser.updatePlayback { latest }
                }

                val settings = settingsRepository.getSettings()
                return@withContext settings.musicDirectories.filter {
                    !permissionRepository.hasPermission(it)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
            null
        }

}