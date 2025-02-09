package com.tachyonmusic.presentation.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tachyonmusic.core.ColorScheme
import com.tachyonmusic.domain.repository.StateRepository
import com.tachyonmusic.database.domain.model.SettingsEntity
import com.tachyonmusic.database.domain.repository.SettingsRepository
import com.tachyonmusic.domain.use_case.RegisterNewUriPermission
import com.tachyonmusic.domain.use_case.player.PauseResumePlayback
import com.tachyonmusic.domain.use_case.profile.ExportDatabase
import com.tachyonmusic.domain.use_case.profile.ImportDatabase
import com.tachyonmusic.util.Duration
import com.tachyonmusic.util.sec
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val registerNewUriPermission: RegisterNewUriPermission,
    private val stateRepository: StateRepository,
    private val exportDatabase: ExportDatabase,
    private val importDatabase: ImportDatabase,
    private val pauseResumePlayback: PauseResumePlayback
) : ViewModel() {

    val settings = settingsRepository.observe().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(),
        SettingsEntity()
    )


    fun seekForwardIncrementChanged(inc: Duration) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.update(seekForwardIncrement = inc)
        }
    }

    fun seekBackIncrementChanged(inc: Duration) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.update(seekBackIncrement = inc)
        }
    }

    fun onAnimateTextChanged(animateText: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.update(animateText = animateText)
        }
    }

    fun ignoreAudioFocusChanged(ignore: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.update(ignoreAudioFocus = ignore)
        }
        // TODO: Audio in Android Auto won't play if audio focus is not requested
    }

    fun autoDownloadSongMetadataChanged(autoDownload: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.update(autoDownloadSongMetadata = autoDownload)
        }
    }

    fun autoDownloadSongMetadataWifiOnly(downloadWifiOnly: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.update(autoDownloadSongMetadataWifiOnly = downloadWifiOnly)
        }
    }

    fun combineDifferentPlaybackTypesChanged(combine: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.update(combineDifferentPlaybackTypes = combine)
        }
    }

    fun maxPlaybacksInHistoryChanged(maxPlaybacks: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.update(maxPlaybacksInHistory = maxPlaybacks)
        }
    }

    fun audioUpdateIntervalChanged(interval: Duration) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.update(audioUpdateInterval = interval)
        }
    }

    fun playNewlyCreatedRemix(playNewly: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.update(playNewlyCreatedRemix = playNewly)
        }
    }

    fun shouldMillisecondsBeShownChanged(shouldShow: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.update(shouldMillisecondsBeShown = shouldShow)
        }
    }

    fun dynamicColorsChanged(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.update(dynamicColors = enabled)
        }
    }

    fun colorSchemeChanged(colorScheme: ColorScheme) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.update(colorScheme = colorScheme)
        }
    }

    fun onUriPermissionResult(uri: Uri?) {
        viewModelScope.launch {
            stateRepository.queueLoadingTask("ProfileViewModel::registerNewUriPermission")
            registerNewUriPermission(uri)
            stateRepository.finishLoadingTask(
                "ProfileViewModel::registerNewUriPermission",
                timeout = .5.sec
            )
        }
    }

    fun onExportDatabase(uri: Uri?) {
        viewModelScope.launch(Dispatchers.IO) {
            stateRepository.queueLoadingTask("ProfileViewModel::exportDatabase")
            exportDatabase(uri)
            stateRepository.finishLoadingTask("ProfileViewModel::exportDatabase")
        }
    }

    fun onImportDatabase(uri: Uri?) {
        pauseResumePlayback(PauseResumePlayback.Action.Pause)
        viewModelScope.launch(Dispatchers.IO) {
            stateRepository.queueLoadingTask("ProfileViewModel::importDatabase")
            if (importDatabase(uri) != null) { // TODO: Display required music paths
//                updateSongDatabase(settingsRepository.getSettings)    Done in [MainViewModel::init]

            }
            stateRepository.finishLoadingTask("ProfileViewModel::importDatabase")
        }
    }
}