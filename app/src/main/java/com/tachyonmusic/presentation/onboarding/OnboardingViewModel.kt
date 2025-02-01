package com.tachyonmusic.presentation.onboarding

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tachyonmusic.database.domain.repository.DataRepository
import com.tachyonmusic.database.domain.repository.SettingsRepository
import com.tachyonmusic.domain.repository.StateRepository
import com.tachyonmusic.domain.use_case.RegisterNewUriPermission
import com.tachyonmusic.domain.use_case.profile.ImportDatabase
import com.tachyonmusic.util.sec
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val dataRepository: DataRepository,
    private val stateRepository: StateRepository,
    private val registerNewUriPermission: RegisterNewUriPermission,
    private val importDatabase: ImportDatabase
) : ViewModel() {

    private val _requiredMusicDirectoriesAfterDatabaseImport = MutableStateFlow(emptyList<String>())
    val requiredMusicDirectoriesAfterDatabaseImport =
        _requiredMusicDirectoriesAfterDatabaseImport.asStateFlow()

    val musicDirectorySelected = combine(
        settingsRepository.observe(),
        requiredMusicDirectoriesAfterDatabaseImport
    ) { settings, requiredDirs ->
        settings.musicDirectories.isNotEmpty() && requiredDirs.isEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    fun saveOnboardingState(completed: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            dataRepository.update(onboardingCompleted = completed)
        }
    }

    fun setNewMusicDirectory(uri: Uri?) {
        viewModelScope.launch {
            stateRepository.queueLoadingTask("OnboardingViewModel::registerNewUriPermission")
            if (registerNewUriPermission(uri)) { // TODO: Test if updated correctly
                _requiredMusicDirectoriesAfterDatabaseImport.update {
                    it.toMutableList().apply { removeAll { path -> uri!!.encodedPath == path } }
                }
            }
            stateRepository.finishLoadingTask(
                "OnboardingViewModel::registerNewUriPermission",
                timeout = .5.sec
            )
        }
    }

    fun onImportDatabase(uri: Uri?) {
        viewModelScope.launch {
            stateRepository.queueLoadingTask("OnboardingViewModel::importDatabase")

            // Place dummy item in here so that [readyToAdvance] doesn't become true for
            // some milliseconds after [importDatabase] is run
            _requiredMusicDirectoriesAfterDatabaseImport.update { listOf("") }
            val missingUri = importDatabase(uri)

            // TODO: Handle null case for missingUri ^ and it.path >
            if (missingUri != null) {
                _requiredMusicDirectoriesAfterDatabaseImport.update { missingUri.mapNotNull { it.encodedPath } }
            } else {
                _requiredMusicDirectoriesAfterDatabaseImport.update { emptyList() }
            }

            stateRepository.finishLoadingTask(
                "OnboardingViewModel::importDatabase",
                timeout = .5.sec
            )
        }
    }
}