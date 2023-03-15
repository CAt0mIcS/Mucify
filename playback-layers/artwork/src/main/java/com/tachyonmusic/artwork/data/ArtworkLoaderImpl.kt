package com.tachyonmusic.artwork.data

import android.content.Context
import com.tachyonmusic.artwork.R
import com.tachyonmusic.artwork.domain.ArtworkLoader
import com.tachyonmusic.artwork.domain.ArtworkLoader.ArtworkData
import com.tachyonmusic.artworkfetcher.ArtworkFetcher
import com.tachyonmusic.core.ArtworkType
import com.tachyonmusic.core.data.EmbeddedArtwork
import com.tachyonmusic.core.data.RemoteArtwork
import com.tachyonmusic.core.domain.Artwork
import com.tachyonmusic.core.domain.SongMetadataExtractor
import com.tachyonmusic.database.domain.model.SongEntity
import com.tachyonmusic.logger.domain.Logger
import com.tachyonmusic.util.Resource
import com.tachyonmusic.util.UiText
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import java.net.URI

internal class ArtworkLoaderImpl(
    private val artworkFetcher: ArtworkFetcher,
    private val context: Context,
    private val log: Logger,
    private val metadataExtractor: SongMetadataExtractor
) : ArtworkLoader {
    override suspend fun requestLoad(
        entity: SongEntity,
        fetchOnline: Boolean
    ): Resource<ArtworkData> {
        when (entity.artworkType) {
            ArtworkType.NO_ARTWORK -> {
                log.debug("Entity ${entity.title} - ${entity.artist} has ${ArtworkType.NO_ARTWORK}")
                return Resource.Success(ArtworkData())
            }

            ArtworkType.REMOTE -> {
                log.debug("Entity ${entity.title} - ${entity.artist} has ${ArtworkType.REMOTE}")
                if (entity.artworkUrl.isNullOrBlank()) {
                    entity.artworkType = ArtworkType.UNKNOWN
                    entity.artworkUrl = null
                    return Resource.Error(
                        message = UiText.StringResource(
                            R.string.no_artwork_found,
                            "${entity.title} - ${entity.artist}"
                        ),
                        data = ArtworkData(entityToUpdate = entity)
                    )
                }

                return Resource.Success(ArtworkData(RemoteArtwork(URI(entity.artworkUrl!!))))
            }

            ArtworkType.EMBEDDED -> {
                log.debug("Entity ${entity.title} - ${entity.artist} has ${ArtworkType.EMBEDDED}")
                val uri = entity.mediaId.uri
                if (uri != null) {
                    val embedded = EmbeddedArtwork.load(uri, metadataExtractor)
                    return if (embedded != null) {
                        Resource.Success(ArtworkData(EmbeddedArtwork(embedded, uri)))
                    } else {
                        Resource.Error(
                            UiText.StringResource(
                                R.string.no_embedded_artwork_despite_embedded_artwork_type,
                                uri.toString()
                            )
                        )
                    }
                }
                entity.artworkType = ArtworkType.UNKNOWN
                return Resource.Error(
                    message = UiText.StringResource(R.string.unknown_error),
                    data = ArtworkData(entityToUpdate = entity)
                )
            }

            else -> {
                log.debug("Entity ${entity.title} - ${entity.artist} has no artwork type, trying to find artwork...")
                val newArtwork = tryFindArtwork(entity, fetchOnline)
                val ret: Resource<ArtworkData> =
                    when (val artwork = newArtwork.data) {
                        is RemoteArtwork -> {
                            log.debug("Entity ${entity.title} - ${entity.artist} found ${ArtworkType.REMOTE}")
                            entity.artworkType = ArtworkType.REMOTE
                            entity.artworkUrl = artwork.uri.toURL().toString()
                            Resource.Success(ArtworkData(artwork, entity))
                        }

                        is EmbeddedArtwork -> {
                            log.debug("Entity ${entity.title} - ${entity.artist} found ${ArtworkType.EMBEDDED}")
                            entity.artworkType = ArtworkType.EMBEDDED
                            entity.artworkUrl = null
                            Resource.Success(ArtworkData(artwork, entity))
                        }

                        else -> {
                            log.debug("Entity ${entity.title} - ${entity.artist} found ${ArtworkType.NO_ARTWORK}, fetchOnline: $fetchOnline")

                            // Only update entity in database if we also searched the web for artwork
                            if (fetchOnline)
                                entity.artworkType = ArtworkType.NO_ARTWORK
                            Resource.Error(
                                message = UiText.StringResource(
                                    R.string.no_artwork_found,
                                    "${entity.title} - ${entity.artist}"
                                ),
                                data = ArtworkData(entityToUpdate = if(fetchOnline) entity else null)
                            )
                        }
                    }

                return ret
            }
        }
    }

    private suspend fun tryFindArtwork(
        entity: SongEntity,
        fetchOnline: Boolean
    ): Resource<Artwork?> {
        var res = tryFindEmbeddedArtwork(entity)
        if (!fetchOnline || res is Resource.Success) // TODO: Return error message from embedded artwork loading too
            return res

        res = tryFindRemoteArtwork(entity)
        return res
    }


    private suspend fun tryFindRemoteArtwork(
        entity: SongEntity,
    ): Resource<Artwork?> {
        var ret: Resource<Artwork?> =
            Resource.Error(UiText.StringResource(R.string.unknown_error))

        artworkFetcher.query(entity.title, entity.artist, 1000)
            .onEach { res ->
                if (res is Resource.Success) {
                    ret = Resource.Success(RemoteArtwork(URI(res.data!!)))
                } else if (res is Resource.Error) {
                    ret = Resource.Error(
                        message = res.message ?: UiText.StringResource(R.string.unknown_error),
                        exception = res.exception
                    )
                }
            }.collect()

        return ret
    }

    private fun tryFindEmbeddedArtwork(entity: SongEntity): Resource<Artwork?> {
        val uri = entity.mediaId.uri
            ?: return Resource.Error(UiText.StringResource(R.string.invalid_uri, "null"))

        val bitmap = EmbeddedArtwork.load(uri, metadataExtractor) ?: return Resource.Error(
            UiText.StringResource(
                R.string.invalid_uri,
                uri.toString()
            )
        )

        return Resource.Success(EmbeddedArtwork(bitmap, uri))
    }
}