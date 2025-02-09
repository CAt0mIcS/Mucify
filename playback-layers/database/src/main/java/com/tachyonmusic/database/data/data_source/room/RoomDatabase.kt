package com.tachyonmusic.database.data.data_source.room

import androidx.room.AutoMigration
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tachyonmusic.database.data.data_source.DataDao
import com.tachyonmusic.database.data.data_source.Database
import com.tachyonmusic.database.data.data_source.HistoryDao
import com.tachyonmusic.database.data.data_source.PlaylistDao
import com.tachyonmusic.database.data.data_source.RemixDao
import com.tachyonmusic.database.data.data_source.SettingsDao
import com.tachyonmusic.database.data.data_source.SongDao
import com.tachyonmusic.database.domain.Converters
import com.tachyonmusic.database.domain.model.DATA_DATABASE_TABLE_NAME
import com.tachyonmusic.database.domain.model.DataEntity
import com.tachyonmusic.database.domain.model.HistoryEntity
import com.tachyonmusic.database.domain.model.PlaybackEntity
import com.tachyonmusic.database.domain.model.PlaylistEntity
import com.tachyonmusic.database.domain.model.RemixEntity
import com.tachyonmusic.database.domain.model.SettingsEntity
import com.tachyonmusic.database.domain.model.SongEntity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

@androidx.room.Database(
    entities = [
        SettingsEntity::class,
        SongEntity::class,
        RemixEntity::class,
        PlaylistEntity::class,
        PlaybackEntity::class,
        HistoryEntity::class,
        DataEntity::class
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2)
    ]
)
@TypeConverters(Converters::class)
abstract class RoomDatabase : androidx.room.RoomDatabase(), Database {
    abstract override val settingsDao: SettingsDao
    abstract override val songDao: SongDao
    abstract override val remixDao: RemixDao
    abstract override val playlistDao: PlaylistDao
    abstract override val historyDao: HistoryDao
    abstract override val dataDao: DataDao


    @Suppress("JSON_FORMAT_REDUNDANT") // Won't be called often, Json {} creation not slow
    override suspend fun toJson() = Json {
        prettyPrint = true
    }.encodeToString(
        buildJsonObject {
            put("Settings", Json.encodeToJsonElement(settingsDao.getSettings()))
            put("Songs", Json.encodeToJsonElement(songDao.getSongs()))
            put("CustomizedSongs", Json.encodeToJsonElement(remixDao.getRemixes()))
            put("Playlists", Json.encodeToJsonElement(playlistDao.getPlaylists()))
            put("History", Json.encodeToJsonElement(historyDao.getHistory()))
            put("Data", Json.encodeToJsonElement(dataDao.getData()))
        }
    )

    override suspend fun overrideFromJson(json: String) {
        clearAllTables()

        val obj = Json.decodeFromString<JsonObject>(json)
        val settings =
            Json.decodeFromJsonElement<SettingsEntity>(obj["Settings"] ?: buildJsonObject { })
        val songs = Json.decodeFromJsonElement<List<SongEntity>>(obj["Songs"]!!)
        val remixes =
            Json.decodeFromJsonElement<List<RemixEntity>>(obj["CustomizedSongs"]!!)
        val playlists = Json.decodeFromJsonElement<List<PlaylistEntity>>(obj["Playlists"]!!)
        val history = Json.decodeFromJsonElement<List<HistoryEntity>>(obj["History"]!!)
        val data = Json.decodeFromJsonElement<DataEntity>(obj["Data"] ?: buildJsonObject { })

        settingsDao.setSettings(settings)
        songDao.addAll(songs)
        remixDao.addAll(remixes)
        playlistDao.addAll(playlists)
        historyDao.addAll(history)
        dataDao.setData(data)
    }

    override fun checkpoint() {
        val db = openHelper.writableDatabase
        db.query("PRAGMA wal_checkpoint(FULL);", emptyArray())
        db.query("PRAGMA wal_checkpoint(TRUNCATE);", emptyArray())
    }

    override val readableDatabasePath: String
        get() = openHelper.readableDatabase.path!!
}