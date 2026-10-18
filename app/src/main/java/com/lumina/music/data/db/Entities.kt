package com.lumina.music.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.lumina.music.core.model.PlaybackType
import com.lumina.music.core.model.Playlist
import com.lumina.music.core.model.Track

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String?,
    val durationMs: Long,
    val artworkUrl: String?,
    val provider: String,
    val providerTrackId: String,
    val playbackType: String,
    val streamUrl: String?,
    val localUri: String?,
    val officialUrl: String?,
    val liked: Boolean = false,
    val likedAt: Long? = null,
    val playCount: Int = 0,
    val lastPlayedAt: Long? = null,
    val imported: Boolean = false,
    val cachedAt: Long = System.currentTimeMillis()
)

fun TrackEntity.toTrack(): Track = Track(
    id = id,
    title = title,
    artist = artist,
    album = album,
    durationMs = durationMs,
    artworkUrl = artworkUrl,
    provider = provider,
    providerTrackId = providerTrackId,
    playbackType = runCatching { PlaybackType.valueOf(playbackType) }.getOrDefault(PlaybackType.STREAM),
    streamUrl = streamUrl,
    localUri = localUri,
    officialUrl = officialUrl,
    liked = liked,
    playCount = playCount,
    lastPlayedAt = lastPlayedAt,
    imported = imported
)

fun Track.toEntity(): TrackEntity = TrackEntity(
    id = id,
    title = title,
    artist = artist,
    album = album,
    durationMs = durationMs,
    artworkUrl = artworkUrl,
    provider = provider,
    providerTrackId = providerTrackId,
    playbackType = playbackType.name,
    streamUrl = streamUrl,
    localUri = localUri,
    officialUrl = officialUrl,
    liked = liked,
    playCount = playCount,
    lastPlayedAt = lastPlayedAt,
    imported = imported
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long
)

fun PlaylistEntity.toPlaylist(trackCount: Int = 0): Playlist =
    Playlist(id = id, name = name, createdAt = createdAt, updatedAt = updatedAt, trackCount = trackCount)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "trackId"],
    indices = [Index("trackId")]
)
data class PlaylistTrackEntity(
    val playlistId: String,
    val trackId: String,
    val position: Int
)

@Entity(tableName = "search_history")
data class SearchQueryEntity(
    @PrimaryKey val query: String,
    val timestamp: Long
)
