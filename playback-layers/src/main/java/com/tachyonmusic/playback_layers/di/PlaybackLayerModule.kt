package com.tachyonmusic.playback_layers.di

import android.content.Context
import com.tachyonmusic.core.data.FileSongMetadataExtractor
import com.tachyonmusic.core.domain.SongMetadataExtractor
import com.tachyonmusic.database.domain.repository.HistoryRepository
import com.tachyonmusic.database.domain.repository.PlaylistRepository
import com.tachyonmusic.database.domain.repository.RemixRepository
import com.tachyonmusic.database.domain.repository.SettingsRepository
import com.tachyonmusic.database.domain.repository.SongRepository
import com.tachyonmusic.logger.domain.Logger
import com.tachyonmusic.metadata_api.ArtworkFetcher
import com.tachyonmusic.playback_layers.data.AndroidNetworkMonitor
import com.tachyonmusic.playback_layers.data.ArtworkCodexImpl
import com.tachyonmusic.playback_layers.data.ArtworkLoaderImpl
import com.tachyonmusic.playback_layers.data.PlaybackRepositoryImpl
import com.tachyonmusic.playback_layers.data.PredefinedPlaylistsRepositoryImpl
import com.tachyonmusic.playback_layers.data.UriPermissionRepositoryImpl
import com.tachyonmusic.playback_layers.domain.ArtworkCodex
import com.tachyonmusic.playback_layers.domain.ArtworkLoader
import com.tachyonmusic.playback_layers.domain.NetworkMonitor
import com.tachyonmusic.playback_layers.domain.PlaybackRepository
import com.tachyonmusic.playback_layers.domain.PredefinedPlaylistsRepository
import com.tachyonmusic.playback_layers.domain.UriPermissionRepository
import com.tachyonmusic.util.data.EventChannelImpl
import com.tachyonmusic.util.domain.EventChannel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlaybackLayerRepositoryModule {
    @Provides
    @Singleton
    fun provideArtworkFetcher() = ArtworkFetcher()


    @Provides
    @Singleton
    fun provideArtworkLoader(
        artworkFetcher: ArtworkFetcher,
        logger: Logger,
        metadataExtractor: SongMetadataExtractor
    ): ArtworkLoader = ArtworkLoaderImpl(artworkFetcher, logger, metadataExtractor)


    @Provides
    @Singleton
    fun provideArtworkCodex(artworkLoader: ArtworkLoader, logger: Logger): ArtworkCodex =
        ArtworkCodexImpl(artworkLoader, logger)

    @Provides
    @Singleton
    fun providePlaybackRepository(
        songRepository: SongRepository,
        remixRepository: RemixRepository,
        playlistRepository: PlaylistRepository,
        historyRepository: HistoryRepository,
        eventChannel: EventChannel
    ): PlaybackRepository = PlaybackRepositoryImpl(
        songRepository,
        remixRepository,
        playlistRepository,
        historyRepository,
        eventChannel
    )

    @Provides
    @Singleton
    fun providePredefinedPlaylistsRepository(
        playbackRepository: PlaybackRepository,
        settingsRepository: SettingsRepository,
        coroutineScope: CoroutineScope
    ): PredefinedPlaylistsRepository =
        PredefinedPlaylistsRepositoryImpl(playbackRepository, settingsRepository, coroutineScope)

    @Provides
    @Singleton
    fun provideUriPermissionRepository(@ApplicationContext context: Context): UriPermissionRepository =
        UriPermissionRepositoryImpl(context)


    @Provides
    @Singleton
    fun provideErrorChannel(): EventChannel = EventChannelImpl()

    @Provides
    @Singleton
    fun provideSongMetadataExtractor(
        @ApplicationContext context: Context,
        logger: Logger
    ): SongMetadataExtractor = FileSongMetadataExtractor(context.contentResolver, logger)

    @Provides
    @Singleton
    fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor =
        AndroidNetworkMonitor(context)
}