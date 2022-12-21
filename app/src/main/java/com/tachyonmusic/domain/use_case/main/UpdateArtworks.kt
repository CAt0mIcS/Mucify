package com.tachyonmusic.domain.use_case.main

import com.tachyonmusic.database.domain.ArtworkType
import com.tachyonmusic.database.domain.repository.SongRepository
import com.tachyonmusic.database.domain.use_case.LoadArtwork
import com.tachyonmusic.logger.Log
import com.tachyonmusic.logger.domain.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UpdateArtworks(
    private val songRepo: SongRepository,
    private val isFirstAppStart: IsFirstAppStart,
    private val loadArtwork: LoadArtwork,
    private val log: Logger = Log()
) {
    suspend operator fun invoke(ignoreIsFirstAppStart: Boolean = false) =
        withContext(Dispatchers.IO) {
            if (ignoreIsFirstAppStart || isFirstAppStart()) {
                log.debug("Loading song artworks")
                songRepo.getSongsWithArtworkTypes(ArtworkType.NO_ARTWORK).forEach { song ->
                    loadArtwork(song)
                }
            }
        }
}