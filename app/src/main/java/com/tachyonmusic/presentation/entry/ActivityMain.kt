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
import com.tachyonmusic.domain.repository.AdInterface
import com.tachyonmusic.domain.repository.MediaBrowserController
import com.tachyonmusic.domain.use_case.home.LoadUUIDForSongEntity
import com.tachyonmusic.logger.domain.Logger
import com.tachyonmusic.media.util.isGoogleCastAvailable
import com.tachyonmusic.playback_layers.domain.UriPermissionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
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

    private var castContext: CastContext? = null
    private lateinit var appUpdateManager: AppUpdateManager

    private var updateReadyToInstall = MutableStateFlow(false)
    private var miniplayerSnapPosition = MutableStateFlow<SwipingStates?>(null)

    private val installStateListener = InstallStateUpdatedListener {
        if (it.installStatus() == InstallStatus.DOWNLOADED)
            updateReadyToInstall.update { true }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adInterface.initialize(this)

        // TODO: Load ads only when they might be necessary
        adInterface.loadNativeInstallAds(this)
        adInterface.loadRewardAd(this)

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
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.action == INTENT_ACTION_SHOW_PLAYER) {
            // TODO: Not working
            miniplayerSnapPosition.update { SwipingStates.EXPANDED }
        }
    }

    override fun onConnected() {
        setupUi()
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
            MainScreen(
                shouldInstallUpdate,
                updateSnackbarResult = { shouldRestart ->
                    if (shouldRestart)
                        appUpdateManager.completeUpdate()
                    updateReadyToInstall.update { false }
                },
                miniplayerSnapPosition = miniplayerSnapPos,
                onMiniplayerSnapCompleted = { miniplayerSnapPosition.update { null } }
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
