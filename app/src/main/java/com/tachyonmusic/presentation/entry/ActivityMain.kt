package com.tachyonmusic.presentation.entry

import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.view.Menu
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.tachyonmusic.app.R
import com.tachyonmusic.database.domain.repository.DataRepository
import com.tachyonmusic.domain.repository.AdInterface
import com.tachyonmusic.domain.repository.MediaBrowserController
import com.tachyonmusic.domain.use_case.home.LoadUUIDForSongEntity
import com.tachyonmusic.logger.domain.Logger
import com.tachyonmusic.media.util.isGoogleCastAvailable
import com.tachyonmusic.playback_layers.domain.UriPermissionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import javax.inject.Inject


@AndroidEntryPoint
class ActivityMain : AppCompatActivity(), MediaBrowserController.EventListener {

    companion object {
        const val INTENT_ACTION_SHOW_PLAYER = "com.tachyonmusic.ACTION_SHOW_PLAYER"
    }

    @Inject
    lateinit var log: Logger

    @Inject
    lateinit var mediaBrowser: MediaBrowserController

    @Inject
    lateinit var uriPermissionRepository: UriPermissionRepository

    @Inject
    lateinit var adInterface: AdInterface

    @Inject
    lateinit var loadUUIDForSongEntity: LoadUUIDForSongEntity

    @Inject
    lateinit var dataRepository: DataRepository

    private var castContext: CastContext? = null
    private lateinit var appUpdateManager: AppUpdateManager

    private var updateReadyToInstall = MutableStateFlow(false)
    private var miniplayerSnapPosition = MutableStateFlow<SwipingStates?>(null)

    private val onboardingCompleted = MutableStateFlow(false)

    private val installStateListener = InstallStateUpdatedListener {
        if (it.installStatus() == InstallStatus.DOWNLOADED)
            updateReadyToInstall.update { true }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupUi()

        dataRepository.observe().onEach { data ->
            onboardingCompleted.update { data.onboardingCompleted }
        }.launchIn(lifecycleScope + Dispatchers.IO)

        lifecycleScope.launch(Dispatchers.IO) {
            adInterface.initialize(this@ActivityMain)

            // TODO: Load ads only when they might be necessary
            adInterface.loadNativeInstallAds(this@ActivityMain)
            adInterface.loadRewardAd(this@ActivityMain)
        }

        // Initialize the Cast context. This is required so that the media route button can be
        // created in the AppBar
        if (isGoogleCastAvailable(this))
            castContext = CastContext.getSharedInstance(this)


        volumeControlStream = AudioManager.STREAM_MUSIC
        mediaBrowser.registerLifecycle(lifecycle)
        mediaBrowser.registerEventListener(this)

        appUpdateManager = AppUpdateManagerFactory.create(this)
        performUpdateCheck()
    }


    override fun onResume() {
        super.onResume()
        uriPermissionRepository.dispatchUpdate()

        // Update app if user has update downloaded but not installed
        appUpdateManager
            .appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                    updateReadyToInstall.update { true }
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        adInterface.release()

//        /**
//         * Reset app if onboarding was not completed. This can happen if the user closes the app
//         * during onboarding
//         * TODO: This doesn't work in onDestroy since the onboarding state (which page it's on)
//         *  might not be destroyed when the activity is
//         */
//        if(!onboardingCompleted.value) {
//            (getSystemService(ACTIVITY_SERVICE) as ActivityManager).clearApplicationUserData()
//        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.action == INTENT_ACTION_SHOW_PLAYER) {
            // TODO: Not working
            miniplayerSnapPosition.update { SwipingStates.EXPANDED }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.main_activity_menu, menu)

        /**
         * Set up a MediaRouteButton to allow the user to control the current media playback route
         */
        menu?.let {
            CastButtonFactory.setUpMediaRouteButton(this, menu, R.id.media_route_menu_item)
        }
        return true
    }

    private fun setupUi() {
        setContent {
            val shouldInstallUpdate by updateReadyToInstall.collectAsState()
            val miniplayerSnapPos by miniplayerSnapPosition.collectAsState()
            val onboardingDone by onboardingCompleted.collectAsState()
            MainScreen(
                shouldInstallUpdate,
                updateSnackbarResult = { shouldRestart ->
                    if (shouldRestart)
                        appUpdateManager.completeUpdate()
                    updateReadyToInstall.update { false }
                },
                miniplayerSnapPosition = miniplayerSnapPos,
                onMiniplayerSnapCompleted = { miniplayerSnapPosition.update { null } },
                onboardingCompleted = onboardingDone
            )
        }
    }

    // https://developer.android.com/guide/playcore/in-app-updates/kotlin-java#kotlin
    private fun performUpdateCheck() {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        val onUpdateResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
                if (it.resultCode != RESULT_OK)
                    log.warning("Update flow failed! Result ${it.resultCode}")

                appUpdateManager.unregisterListener(installStateListener)
            }

        appUpdateManager.registerListener(installStateListener)
        appUpdateInfoTask.addOnSuccessListener {
            if (it.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                it.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                appUpdateManager.startUpdateFlowForResult(
                    it,
                    onUpdateResultLauncher,
                    AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
                )
            }
        }
    }
}
