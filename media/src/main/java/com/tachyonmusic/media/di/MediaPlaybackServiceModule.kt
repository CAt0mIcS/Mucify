package com.tachyonmusic.media.di

import android.app.Service
import android.app.UiModeManager
import android.content.Context
import android.content.Context.UI_MODE_SERVICE
import android.content.res.Configuration
import androidx.media3.cast.CastPlayer
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.tachyonmusic.database.domain.repository.*
import com.tachyonmusic.logger.domain.Logger
import com.tachyonmusic.media.data.AndroidAudioEffectController
import com.tachyonmusic.media.data.BrowserTree
import com.tachyonmusic.media.data.CustomPlayerImpl
import com.tachyonmusic.media.domain.AudioEffectController
import com.tachyonmusic.media.domain.CustomPlayer
import com.tachyonmusic.media.domain.use_case.*
import com.tachyonmusic.permission.domain.PermissionMapperRepository
import com.tachyonmusic.playback_layers.domain.PlaybackRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.scopes.ServiceScoped
import dagger.hilt.components.SingletonComponent
import javax.annotation.Nullable
import javax.inject.Singleton

@Module
@InstallIn(ServiceComponent::class)
class MediaPlaybackServiceRepositoryModule {

    @Provides
    @ServiceScoped
    @Nullable
    fun provideCastContext(service: Service): CastContext? =
        if (isCastApiAvailable(service)) CastContext.getSharedInstance(service) else null

    @Provides
    @ServiceScoped
    fun provideBrowserTree(
        playbackRepository: PlaybackRepository,
        permissionMapperRepository: PermissionMapperRepository
    ): BrowserTree = BrowserTree(playbackRepository, permissionMapperRepository)

    @Provides
    @ServiceScoped
    @Nullable
    fun provideCastPlayer(context: CastContext?, log: Logger): CustomPlayer? =
        if (context == null) null else CustomPlayerImpl(CastPlayer(context), log)
}


@Module
@InstallIn(SingletonComponent::class)
class MediaPlaybackUseCaseModule {

    @Provides
    @Singleton
    fun provideAddNewPlaybackToHistoryUseCase(
        historyRepository: HistoryRepository,
        settingsRepository: SettingsRepository
    ) = AddNewPlaybackToHistory(historyRepository, settingsRepository)

    @Provides
    @Singleton
    fun provideSaveRecentlyPlayedUseCase(dataRepository: DataRepository) =
        SaveRecentlyPlayed(dataRepository)

    @Provides
    @Singleton
    fun provideGetSettingsUseCase(settingsRepository: SettingsRepository) =
        GetSettings(settingsRepository)
}

@Module
@InstallIn(SingletonComponent::class)
class MediaPlaybackRepositoryModule {
    @Provides
    @Singleton
    fun provideAudioEffectController(): AudioEffectController = AndroidAudioEffectController()
}


fun isCastApiAvailable(context: Context): Boolean {
    val isCastApiAvailable = isCurrentDevicePhone(context)
            && GoogleApiAvailability.getInstance()
        .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
    try {
        CastContext.getSharedInstance(context)
    } catch (e: Exception) {
        // track non-fatal
        return false
    }
    return isCastApiAvailable
}

fun isCurrentDevicePhone(context: Context): Boolean {
    val uiModeManager = context.getSystemService(UI_MODE_SERVICE) as UiModeManager
    return uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_NORMAL
}