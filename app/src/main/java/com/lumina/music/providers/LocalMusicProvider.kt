package com.lumina.music.providers

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.lumina.music.core.model.DiscoverSection
import com.lumina.music.core.model.PlaybackType
import com.lumina.music.core.model.ProviderFeature
import com.lumina.music.core.model.ProviderInfo
import com.lumina.music.core.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * On-device music via MediaStore. Fully playable through the same Media3 engine.
 */
class LocalMusicProvider(private val context: Context) : MusicProvider {

    override val info = ProviderInfo(
        id = "local",
        displayName = "On device",
        description = "Your downloaded and imported music files",
        features = setOf(
            ProviderFeature.SEARCH,
            ProviderFeature.TRACK_METADATA,
            ProviderFeature.ARTWORK
        ),
        capability = MusicProvider.local,
        accentColor = 0xFF5EE6A8
    )

    override suspend fun search(query: String, limit: Int): List<Track> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        return scan().filter {
            it.title.contains(q, ignoreCase = true) || it.artist.contains(q, ignoreCase = true)
        }.take(limit)
    }

    override suspend fun discover(section: DiscoverSection): List<Track> = emptyList()

    suspend fun scan(): List<Track> = withContext(Dispatchers.IO) {
        val tracks = ArrayList<Track>(64)
        val collection = if (android.os.Build.VERSION.SDK_INT >= 29) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 30000"
        try {
            context.contentResolver.query(collection, projection, selection, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                while (cursor.moveToNext()) {
                    val audioId = cursor.getLong(idCol)
                    val albumId = cursor.getLong(albumIdCol)
                    val playableUri = ContentUris.withAppendedId(collection, audioId)
                    val artist = cursor.getString(artistCol) ?: "Unknown artist"
                    tracks.add(
                        Track(
                            id = "local:$audioId",
                            title = cursor.getString(titleCol) ?: "Unknown title",
                            artist = artist,
                            album = cursor.getString(albumCol),
                            durationMs = cursor.getLong(durCol),
                            artworkUrl = Uri.parse("content://media/external/audio/albumart/$albumId").toString(),
                            provider = "local",
                            providerTrackId = audioId.toString(),
                            playbackType = PlaybackType.LOCAL,
                            localUri = playableUri.toString()
                        )
                    )
                }
            }
        } catch (_: Throwable) {
        }
        tracks
    }
}
