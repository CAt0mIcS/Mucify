package com.tachyonmusic.presentation.player

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tachyonmusic.app.R
import com.tachyonmusic.core.domain.TimingData
import com.tachyonmusic.core.domain.TimingDataController
import com.tachyonmusic.core.domain.isNullOrEmpty
import com.tachyonmusic.database.domain.model.SettingsEntity
import com.tachyonmusic.database.domain.repository.DataRepository
import com.tachyonmusic.database.domain.repository.RemixRepository
import com.tachyonmusic.database.domain.repository.SettingsRepository
import com.tachyonmusic.domain.model.RewardAd
import com.tachyonmusic.domain.repository.AdInterface
import com.tachyonmusic.domain.repository.MediaBrowserController
import com.tachyonmusic.domain.use_case.PlayPlayback
import com.tachyonmusic.domain.use_case.player.CreateRemix
import com.tachyonmusic.domain.use_case.player.PauseResumePlayback
import com.tachyonmusic.domain.use_case.player.SaveRemixToDatabase
import com.tachyonmusic.domain.use_case.player.SeekToPosition
import com.tachyonmusic.logger.domain.Logger
import com.tachyonmusic.playback_layers.domain.PredefinedPlaylistsRepository
import com.tachyonmusic.playback_layers.toPlayback
import com.tachyonmusic.presentation.util.update
import com.tachyonmusic.util.Config
import com.tachyonmusic.util.Duration
import com.tachyonmusic.util.Resource
import com.tachyonmusic.util.UiText
import com.tachyonmusic.util.ms
import com.tachyonmusic.util.runOnUiThread
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class RemixError(
    val message: UiText?,
    val code: Int
)


@HiltViewModel
class RemixEditorViewModel @Inject constructor(
    private val mediaBrowser: MediaBrowserController,
    private val seekToPosition: SeekToPosition,
    private val createRemix: CreateRemix,
    private val playPlayback: PlayPlayback,
    private val predefinedPlaylistsRepository: PredefinedPlaylistsRepository,
    settingsRepository: SettingsRepository,
    private val pauseResumePlayback: PauseResumePlayback,
    private val saveRemix: SaveRemixToDatabase,
    private val adInterface: AdInterface,
    private val dataRepository: DataRepository,
    private val remixRepository: RemixRepository,
    private val log: Logger
) : ViewModel() {
    private val _remixError = MutableStateFlow<RemixError?>(null)
    val remixError = _remixError.asStateFlow()

    val duration = mediaBrowser.currentPlayback.map {
        it?.duration ?: Long.MAX_VALUE.ms
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), Long.MAX_VALUE.ms)

    val currentRemixName = mediaBrowser.currentPlayback.map {
        if (it?.isRemix == true) it.name!! else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val needsToShowAd =
        combine(dataRepository.observe(), remixRepository.observe()) { data, remixes ->
            remixes.size >= data.maxRemixCount
        }.stateIn(viewModelScope + Dispatchers.IO, SharingStarted.WhileSubscribed(), false)

    val timingData = mutableStateListOf<TimingData>()
    var currentIndex by mutableIntStateOf(0)
        private set

    val rewardAdQuestionResource: Int
        get() = when (val type = adInterface.rewardAdType) {
            is RewardAd.Type.NewRemixes -> if (type.amount == 1) R.string.watch_reward_ad_for_one_remix else R.string.watch_reward_ad_for_more_remixes
            else -> R.string.watch_reward_ad_for_one_remix
        }

    val settings = settingsRepository.observe()
        .stateIn(
            viewModelScope + Dispatchers.IO,
            SharingStarted.WhileSubscribed(),
            SettingsEntity()
        )

    init {
        mediaBrowser.currentPlayback.onEach {
            val newTimingData = if (it == null)
                null
            else if (it.timingData.isNullOrEmpty())
                TimingDataController.default(it.duration)
            else
                it.timingData

            timingData.update { newTimingData?.timingData ?: emptyList() }
            currentIndex = newTimingData?.currentIndex ?: 0
        }.launchIn(viewModelScope)
    }

    fun setAndPlayTimingDataAt(i: Int, startFromEnd: Boolean = false) {
        if (startFromEnd)
            seekToPosition(timingData[i].endTime - Config.TIMING_DATA_END_TIME_ADJUSTMENT)
        else
            seekToPosition(timingData[i].startTime)
        pauseResumePlayback(PauseResumePlayback.Action.Resume)

        currentIndex = i
        setNewTimingData()
    }

    fun updateTimingData(i: Int, startTime: Duration, endTime: Duration) {
        timingData.update {
            it[i].startTime = startTime
            it[i].endTime = endTime
            it
        }
    }

    fun updateStartToCurrentPosition(i: Int) {
        timingData.update {
            it[i].startTime = mediaBrowser.currentPosition ?: return@update it
            it
        }
    }

    fun updateEndToCurrentPosition(i: Int) {
        timingData.update {
            it[i].endTime = mediaBrowser.currentPosition ?: return@update it
            it
        }
    }

    fun setNewTimingData() {
        mediaBrowser.updatePlayback {
            it?.copy(
                timingData = TimingDataController(timingData.toList(), currentIndex)
            )
        }
    }

    fun addNewTimingData(i: Int) {
        mediaBrowser.updatePlayback {
            it?.copy(
                timingData = TimingDataController(timingData.toMutableList().apply {
                    add(i, TimingData(0.ms, duration.value))
                }, currentIndex)
            )
        }
    }

    fun removeTimingData(i: Int) {
        mediaBrowser.updatePlayback {
            it?.copy(
                timingData = TimingDataController(timingData.toMutableList().apply {
                    removeAt(i)

                    // Add default back if last timingData was deleted
                    if (i == 0 && isEmpty())
                        add(TimingData(0.ms, duration.value))
                }, currentIndex)
            )
        }
    }

    fun moveTimingData(from: Int, to: Int) {
        mediaBrowser.updatePlayback {
            it?.copy(
                timingData = TimingDataController(timingData.toMutableList().apply {
                    if (isValidTimingDataMove(from, to)) {
                        val tdToMove = removeAt(from)
                        add(to, tdToMove)
                    }
                }, currentIndex)
            )
        }
    }

    fun saveNewRemix(
        name: String,
        ignoreMaxRemixCount: Boolean = false,
        replaceExisting: Boolean = false
    ) {
        viewModelScope.launch {
            val mediaPosBefore = mediaBrowser.currentPosition
            val currentPlayback = mediaBrowser.currentPlayback.value

            withContext(Dispatchers.IO) {
                val createRes = createRemix(name, currentPlayback)

                if (createRes is Resource.Success) {
                    _remixError.update { null }

                    val dbRes = saveRemix(
                        createRes.data!!,
                        settings.value.playNewlyCreatedRemix,
                        ignoreMaxRemixCount,
                        replaceExisting
                    )
                    if (dbRes is Resource.Success && settings.value.playNewlyCreatedRemix) {
                        runOnUiThread {
                            playPlayback(
                                createRes.data?.toPlayback(currentPlayback),
                                mediaPosBefore
                            )
                        }
                    } else if (dbRes is Resource.Error) {
                        _remixError.update { RemixError(dbRes.message, dbRes.code ?: -1) }
                    }
                } else
                    _remixError.update { RemixError(createRes.message, -1) }
            }
        }
    }

    fun isValidTimingDataMove(from: Int, to: Int): Boolean {
        if (timingData.size <= 1) return false
        if (timingData.size == to) return false
        if (from < 0 || to < 0) return false

        return true
    }

    fun clearRemixError() {
        _remixError.update { null }
    }

    suspend fun playAd(activity: ComponentActivity?) = withContext(Dispatchers.Main) {
        val wasPlaying = mediaBrowser.isPlaying.value
        pauseResumePlayback(PauseResumePlayback.Action.Pause)

        adInterface.showRewardAdSuspend(activity!!) { _, amount ->
            val numStoredRemixes = remixRepository.getRemixes().size
            dataRepository.update(maxRemixCount = numStoredRemixes + amount)
            log.info("Reward of $amount new remixes granted")
        }

        if (wasPlaying && !settings.value.playNewlyCreatedRemix)
            runOnUiThread { pauseResumePlayback(PauseResumePlayback.Action.Resume) }
    }
}