package com.lumina.music.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Upsert
    suspend fun upsert(track: TrackEntity)

    @Upsert
    suspend fun upsertAll(tracks: List<TrackEntity>)

    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun byId(id: String): TrackEntity?

    @Query("SELECT * FROM tracks WHERE liked = 1 ORDER BY likedAt DESC")
    fun liked(): Flow<List<TrackEntity>>

    @Query("SELECT id FROM tracks WHERE liked = 1")
    fun likedIds(): Flow<List<String>>

    @Query("SELECT * FROM tracks WHERE lastPlayedAt IS NOT NULL ORDER BY lastPlayedAt DESC LIMIT 60")
    fun history(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE provider = 'local' AND imported = 0")
    fun localTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE imported = 1")
    fun importedTracks(): Flow<List<TrackEntity>>

    @Query("UPDATE tracks SET liked = :liked, likedAt = :likedAt WHERE id = :id")
    suspend fun setLiked(id: String, liked: Boolean, likedAt: Long?)

    @Query("UPDATE tracks SET playCount = playCount + 1, lastPlayedAt = :at WHERE id = :id")
    suspend fun recordPlay(id: String, at: Long)

    @Query(
        "DELETE FROM tracks WHERE liked = 0 AND lastPlayedAt IS NULL " +
            "AND imported = 0 AND provider != 'local'"
    )
    suspend fun purgeCache()

    @Delete
    suspend fun delete(track: TrackEntity)
}

@Dao
interface PlaylistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Query("SELECT * FROM playlists ORDER BY updatedAt DESC")
    fun playlists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun byId(id: String): PlaylistEntity?

    @Query("UPDATE playlists SET name = :name, updatedAt = :at WHERE id = :id")
    suspend fun rename(id: String, name: String, at: Long)

    @Delete
    suspend fun delete(playlist: PlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addTracks(entries: List<PlaylistTrackEntity>)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun clearTracks(playlistId: String)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrack(playlistId: String, trackId: String)

    @Query("UPDATE playlist_tracks SET position = :position WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun updatePosition(playlistId: String, trackId: String, position: Int)

    @Query(
        "SELECT t.* FROM tracks t INNER JOIN playlist_tracks pt ON pt.trackId = t.id " +
            "WHERE pt.playlistId = :playlistId ORDER BY pt.position ASC"
    )
    fun playlistTracks(playlistId: String): Flow<List<TrackEntity>>

    @Query("SELECT MAX(position) FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun maxPosition(playlistId: String): Int?

    @Query("SELECT COUNT(*) FROM playlist_tracks WHERE playlistId = :playlistId")
    fun trackCount(playlistId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun trackCountOnce(playlistId: String): Int

    @Query("SELECT t.* FROM tracks t INNER JOIN playlist_tracks pt ON pt.trackId = t.id WHERE pt.playlistId = :playlistId ORDER BY pt.position ASC")
    suspend fun playlistTracksOnce(playlistId: String): List<TrackEntity>
}

@Dao
interface SearchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: SearchQueryEntity)

    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 12")
    fun recent(): Flow<List<SearchQueryEntity>>

    @Query("DELETE FROM search_history")
    suspend fun clear()
}
