package com.tachyonmusic.di

import android.app.Application
import com.tachyonmusic.artworkfetcher.ArtworkFetcher
import com.tachyonmusic.core.domain.SongMetadataExtractor
import com.tachyonmusic.data.repository.ArtworkCodexImpl
import com.tachyonmusic.data.repository.FileRepositoryImpl
import com.tachyonmusic.data.repository.MediaPlaybackServiceMediaBrowserController
import com.tachyonmusic.database.domain.repository.DataRepository
import com.tachyonmusic.database.domain.repository.HistoryRepository
import com.tachyonmusic.database.domain.repository.LoopRepository
import com.tachyonmusic.database.domain.repository.PlaylistRepository
import com.tachyonmusic.database.domain.repository.SettingsRepository
import com.tachyonmusic.database.domain.repository.SongRepository
import com.tachyonmusic.domain.repository.ArtworkCodex
import com.tachyonmusic.domain.repository.FileRepository
import com.tachyonmusic.domain.repository.MediaBrowserController
import com.tachyonmusic.domain.use_case.*
import com.tachyonmusic.domain.use_case.authentication.RegisterUser
import com.tachyonmusic.domain.use_case.authentication.SignInUser
import com.tachyonmusic.domain.use_case.main.*
import com.tachyonmusic.domain.use_case.player.*
import com.tachyonmusic.domain.use_case.search.SearchStoredPlaybacks
import com.tachyonmusic.domain.use_case.GetOrLoadArtwork
import com.tachyonmusic.logger.Log
import com.tachyonmusic.logger.domain.Logger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object AppUseCaseModule {
    @Provides
    @Singleton
    fun provideRegisterUserUseCase() = RegisterUser()

    @Provides
    @Singleton
    fun provideSignInUserUseCase() = SignInUser()

    @Provides
    @Singleton
    fun provideItemClickedUseCase(browser: MediaBrowserController) = ItemClicked(browser)

    @Provides
    @Singleton
    fun provideObserveSongsUseCase(songRepository: SongRepository) = ObserveSongs(songRepository)

    @Provides
    @Singleton
    fun provideGetPagedSongsUseCase(songRepository: SongRepository) = GetPagedSongs(songRepository)

    @Provides
    @Singleton
    fun provideGetSongsUseCase(songRepository: SongRepository) = GetSongs(songRepository)

    @Provides
    @Singleton
    fun provideUpdateSongDatabaseUseCase(
        songRepository: SongRepository,
        settingsRepository: SettingsRepository,
        fileRepository: FileRepository,
        metadataExtractor: SongMetadataExtractor
    ) = UpdateSongDatabase(songRepository, settingsRepository, fileRepository, metadataExtractor)

    @Provides
    @Singleton
    fun provideUpdateSettingsDatabaseUseCase(settingsRepository: SettingsRepository) =
        UpdateSettingsDatabase(settingsRepository)

    @Provides
    @Singleton
    fun provideObserveSettingsUseCase(settingsRepository: SettingsRepository) =
        ObserveSettings(settingsRepository)

    @Provides
    @Singleton
    fun provideGetOrLoadArtworkUseCase(
        songRepository: SongRepository,
        loopRepository: LoopRepository,
        artworkCodex: ArtworkCodex
    ) = GetOrLoadArtwork(songRepository, loopRepository, artworkCodex)

    @Provides
    @Singleton
    fun provideUnloadArtworksUseCase(
        songRepository: SongRepository
    ) = UnloadArtworks(songRepository)

    @Provides
    @Singleton
    fun provideObserveLoopsUseCase(loopRepository: LoopRepository) = ObserveLoops(loopRepository)

    @Provides
    @Singleton
    fun provideGetPagedLoopsUseCase(loopRepository: LoopRepository) = GetPagedLoops(loopRepository)

    @Provides
    @Singleton
    fun provideObservePlaylistsUseCase(playlistRepository: PlaylistRepository) =
        ObservePlaylists(playlistRepository)

    @Provides
    @Singleton
    fun provideGetPagedPlaylistsUseCase(playlistRepository: PlaylistRepository) =
        GetPagedPlaylists(playlistRepository)

    @Provides
    @Singleton
    fun provideGetHistoryUseCase(historyRepository: HistoryRepository) =
        GetHistory(historyRepository)

    @Provides
    @Singleton
    fun provideObserveHistoryUseCase(historyRepository: HistoryRepository) =
        ObserveHistory(historyRepository)

    @Provides
    @Singleton
    fun provideSearchStoredPlaybacksUseCase(
        songRepository: SongRepository,
        loopRepository: LoopRepository,
        playlistRepository: PlaylistRepository
    ) = SearchStoredPlaybacks(songRepository, loopRepository, playlistRepository)

    @Provides
    @Singleton
    fun provideCreateNewLoopUseCase(
        songRepository: SongRepository,
        loopRepository: LoopRepository,
        browser: MediaBrowserController
    ) = CreateAndSaveNewLoop(songRepository, loopRepository, browser)

    @Provides
    @Singleton
    fun provideMillisecondsToReadableStringUseCase() = MillisecondsToReadableString()

    @Provides
    @Singleton
    fun providePauseResumePlaybackUseCase(browser: MediaBrowserController) =
        PauseResumePlayback(browser)

    @Provides
    @Singleton
    fun provideGetCurrentPositionUseCase(browser: MediaBrowserController) =
        GetCurrentPosition(browser)

    @Provides
    @Singleton
    fun provideGetCurrentPositionNormalizedUseCase(browser: MediaBrowserController) =
        NormalizePosition(browser)

    @Provides
    @Singleton
    fun provideGetAudioUpdateIntervalUseCase(settingsRepository: SettingsRepository) =
        GetAudioUpdateInterval(settingsRepository)

    @Provides
    @Singleton
    fun provideSeekPositionUseCase(browser: MediaBrowserController) = SeekPosition(browser)

    @Provides
    @Singleton
    fun provideSetCurrentPlaybackUseCase(browser: MediaBrowserController) =
        SetCurrentPlayback(browser)

    @Provides
    @Singleton
    fun provideIsFirstAppStartUseCase(app: Application) = IsFirstAppStart(app)

    @Provides
    @Singleton
    fun provideGetRecentlyPlayedPositionUseCase(dataRepository: DataRepository) =
        GetRecentlyPlayed(dataRepository)

    @Provides
    @Singleton
    fun provideLogger(): Logger = Log()
}


@Module
@InstallIn(SingletonComponent::class)
object AppRepositoryModule {
    @Provides
    @Singleton
    fun provideFileRepository(): FileRepository = FileRepositoryImpl()

    @Provides
    @Singleton
    fun provideMediaBrowserController(): MediaBrowserController =
        MediaPlaybackServiceMediaBrowserController()

    @Provides
    @Singleton
    fun provideArtworkFetcher(): ArtworkFetcher = ArtworkFetcher()

    @Provides
    @Singleton
    fun provideArtworkCodex(
        artworkFetcher: ArtworkFetcher,
        log: Logger
    ): ArtworkCodex = ArtworkCodexImpl(artworkFetcher, log)
}
