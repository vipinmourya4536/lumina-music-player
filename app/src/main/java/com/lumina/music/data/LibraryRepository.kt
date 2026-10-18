package com.lumina.music.data

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.lumina.music.core.model.PlaybackType
import com.lumina.music.core.model.Playlist
import com.lumina.music.core.model.Track
import com.lumina.music.data.db.LuminaDatabase
import com.lumina.music.data.db.PlaylistEntity
import com.lumina.music.data.db.PlaylistTrackEntity
import com.lumina.music.data.db.SearchQueryEntity
import com.lumina.music.data.db.toEntity
import com.lumina.music.data.db.toPlaylist
import com.lumina.music.data.db.toTrack
import com.lumina.music.providers.LocalMusicProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Unified library facade: likes, playlists, history, local + imported music,
 * search history and metadata caching. All UI flows read from here.
 */
class LibraryRepository(
    private val context: Context,
    private val db: LuminaDatabase,
    private val prefs: Prefs
) {
    private val trackDao get() = db.trackDao()
    private val playlistDao get() = db.playlistDao()
    private val searchDao get() = db.searchDao()

    // ---------- Tracks / caching ----------

    suspend fun getTrack(id: String): Track? = trackDao.byId(id)?.toTrack()

    /** Caches normalized metadata without clobbering like/history state. */
    suspend fun cacheTracks(tracks: List<Track>) {
        for (track in tracks) {
            val existing = trackDao.byId(track.id)
            trackDao.upsert(
                track.toEntity().copy(
                    liked = existing?.liked ?: track.liked,
                    likedAt = existing?.likedAt,
                    playCount = existing?.playCount ?: 0,
                    lastPlayedAt = existing?.lastPlayedAt,
                    imported = existing?.imported ?: track.imported
                )
            )
        }
    }

    suspend fun toggleLikeById(id: String): Boolean? {
        val existing = trackDao.byId(id) ?: return null
        val newLiked = !existing.liked
        trackDao.setLiked(id, newLiked, if (newLiked) System.currentTimeMillis() else null)
        return newLiked
    }

    suspend fun recordPlay(id: String) {
        try {
            trackDao.recordPlay(id, System.currentTimeMillis())
        } catch (_: Throwable) {
        }
    }

    // ---------- Flows ----------

    fun likedTracks(): Flow<List<Track>> = trackDao.liked().map { list -> list.map { it.toTrack() } }

    fun likedIds(): Flow<Set<String>> = trackDao.likedIds().map { it.toSet() }

    fun history(): Flow<List<Track>> = trackDao.history().map { list -> list.map { it.toTrack() } }

    fun localAndImported(): Flow<List<Track>> =
        combine(trackDao.localTracks(), trackDao.importedTracks()) { a, b ->
            (a + b).map { it.toTrack() }
        }

    // ---------- Playlists ----------

    fun playlists(): Flow<List<Playlist>> = playlistDao.playlists().map { list ->
        list.map { it.toPlaylist() }
    }

    fun playlistTrackCounts(): Flow<Map<String, Int>> = playlistDao.playlists().map { list ->
        list.associate { it.id to playlistDao.trackCountOnce(it.id) }
    }

    fun playlistTracks(playlistId: String): Flow<List<Track>> =
        playlistDao.playlistTracks(playlistId).map { list -> list.map { it.toTrack() } }

    suspend fun playlist(id: String): Playlist? = playlistDao.byId(id)?.toPlaylist()

    suspend fun createPlaylist(name: String): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        playlistDao.insertPlaylist(PlaylistEntity(id = id, name = name, createdAt = now, updatedAt = now))
        return id
    }

    suspend fun renamePlaylist(id: String, name: String) {
        playlistDao.rename(id, name, System.currentTimeMillis())
    }

    suspend fun deletePlaylist(id: String) {
        playlistDao.clearTracks(id)
        playlistDao.byId(id)?.let { playlistDao.delete(it) }
    }

    suspend fun addTracksToPlaylist(playlistId: String, tracks: List<Track>) {
        cacheTracks(tracks)
        var next = (playlistDao.maxPosition(playlistId) ?: -1) + 1
        val entries = tracks.map {
            PlaylistTrackEntity(playlistId = playlistId, trackId = it.id, position = next++)
        }
        playlistDao.addTracks(entries)
        playlistDao.rename(playlistId, playlistDao.byId(playlistId)?.name ?: "Playlist", System.currentTimeMillis())
    }

    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        playlistDao.removeTrack(playlistId, trackId)
        normalizePositions(playlistId)
    }

    suspend fun moveTrack(playlistId: String, trackId: String, delta: Int) {
        val tracks = playlistDao.playlistTracksOnce(playlistId)
        val index = tracks.indexOfFirst { it.id == trackId }
        if (index < 0) return
        val target = index + delta
        if (target < 0 || target >= tracks.size) return
        val mutable = tracks.toMutableList()
        val item = mutable.removeAt(index)
        mutable.add(target, item)
        mutable.forEachIndexed { i, t -> playlistDao.updatePosition(playlistId, t.id, i) }
    }

    private suspend fun normalizePositions(playlistId: String) {
        val tracks = playlistDao.playlistTracksOnce(playlistId)
        tracks.forEachIndexed { i, t -> playlistDao.updatePosition(playlistId, t.id, i) }
    }

    // ---------- Local + imported music ----------

    private val localProvider by lazy { LocalMusicProvider(context) }

    suspend fun scanLocalMusic(): Int {
        val tracks = localProvider.scan()
        val existing = tracks.mapNotNull { trackDao.byId(it.id) }.associateBy { it.id }
        tracks.forEach { track ->
            val old = existing[track.id]
            trackDao.upsert(
                track.toEntity().copy(
                    liked = old?.liked ?: false,
                    playCount = old?.playCount ?: 0,
                    lastPlayedAt = old?.lastPlayedAt,
                    likedAt = old?.likedAt,
                    imported = old?.imported ?: false
                )
            )
        }
        return tracks.size
    }

    suspend fun importUris(uris: List<Uri>): Int = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        var count = 0
        val appContext = context.applicationContext
        for (uri in uris) {
            try {
                runCatching {
                    appContext.contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                val retriever = MediaMetadataRetriever()
                val track = try {
                    retriever.setDataSource(appContext, uri)
                    val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                        ?: uri.lastPathSegment?.substringAfterLast('/') ?: "Imported track"
                    val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                        ?: "Unknown artist"
                    val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                    val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull() ?: 0L
                    Track(
                        id = "import:${uri.toString().hashCode()}",
                        title = title,
                        artist = artist,
                        album = album,
                        durationMs = duration,
                        provider = "local",
                        providerTrackId = uri.toString(),
                        playbackType = PlaybackType.LOCAL,
                        localUri = uri.toString(),
                        imported = true
                    )
                } finally {
                    runCatching { retriever.release() }
                }
                val old = trackDao.byId(track.id)
                trackDao.upsert(track.toEntity().copy(liked = old?.liked ?: false))
                count++
            } catch (_: Throwable) {
            }
        }
        count
    }

    // ---------- Maintenance ----------

    suspend fun purgeCachedMetadata() {
        trackDao.purgeCache()
    }

    // ---------- Search history ----------

    fun recentQueries(): Flow<List<String>> =
        searchDao.recent().map { list -> list.map { it.query } }

    suspend fun addQuery(query: String) {
        val q = query.trim()
        if (q.length < 2) return
        searchDao.insert(SearchQueryEntity(query = q, timestamp = System.currentTimeMillis()))
    }

    suspend fun clearSearchHistory() {
        searchDao.clear()
    }
}
