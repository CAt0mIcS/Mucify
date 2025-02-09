package com.tachyonmusic.presentation.entry

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tachyonmusic.data.repository.STATE_LOADING_TASK_STARTUP
import com.tachyonmusic.database.domain.repository.DataRepository
import com.tachyonmusic.database.domain.repository.SettingsRepository
import com.tachyonmusic.domain.repository.MediaBrowserController
import com.tachyonmusic.domain.repository.StateRepository
import com.tachyonmusic.domain.use_case.home.UpdateSettingsDatabase
import com.tachyonmusic.domain.use_case.home.UpdateSongDatabase
import com.tachyonmusic.logger.domain.Logger
import com.tachyonmusic.playback_layers.domain.UriPermissionRepository
import com.tachyonmusic.presentation.theme.ComposeSettings
import com.tachyonmusic.util.domain.EventChannel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val stateRepository: StateRepository,

    eventChannel: EventChannel,
    settingsRepository: SettingsRepository,
    uriPermissionRepository: UriPermissionRepository,
    updateSettingsDatabase: UpdateSettingsDatabase,
    updateSongDatabase: UpdateSongDatabase,

    browser: MediaBrowserController,
    dataRepository: DataRepository,

    private val log: Logger
) : ViewModel() {

    val isLoading = stateRepository.isLoading
    val eventChannel = eventChannel.listen()

    private val _composeSettings = MutableStateFlow(ComposeSettings())
    val composeSettings = _composeSettings.asStateFlow()

    private var cachedMusicDirectories = emptyList<Uri>()


    init {
        log.debug("Initializing MainViewModel")

        viewModelScope.launch {
            // Make sure browser repeat mode is up to date with saved one
            browser.setRepeatMode(withContext(Dispatchers.IO) { dataRepository.getData().repeatMode })
        }

        viewModelScope.launch(Dispatchers.IO) {
            cachedMusicDirectories = settingsRepository.getSettings().musicDirectories
            updateSettingsDatabase()

            combine(
                settingsRepository.observe(),
                uriPermissionRepository.permissions
            ) { settings, _ ->
                _composeSettings.update {
                    ComposeSettings(
                        settings.animateText,
                        settings.dynamicColors,
                        settings.audioUpdateInterval,
                        settings.colorScheme
                    )
                }

                val loadingTaskRunning =
                    stateRepository.isLoadingTaskRunning(STATE_LOADING_TASK_STARTUP)
                if (loadingTaskRunning ||
                    (settings.musicDirectories.isNotEmpty() && cachedMusicDirectories != settings.musicDirectories)
                ) {
                    log.info("Starting song database update due to new music directory or reload")
                    updateSongDatabase(settings)
                    cachedMusicDirectories =
                        settings.musicDirectories.filter { uriPermissionRepository.hasPermission(it) }

                    if (loadingTaskRunning)
                        stateRepository.finishLoadingTask(STATE_LOADING_TASK_STARTUP)
                }
            }.collect()
        }
    }
}