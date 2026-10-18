package com.lumina.music.providers

import com.lumina.music.core.model.DiscoverSection
import com.lumina.music.core.model.PlaybackType
import com.lumina.music.core.model.ProviderFeature
import com.lumina.music.core.model.ProviderInfo
import com.lumina.music.core.model.Track
import com.lumina.music.core.util.Http
import com.lumina.music.core.util.urlEncode
import com.lumina.music.data.Prefs
import kotlinx.serialization.Serializable

/**
 * YouTube Music — official-playback-only provider.
 * Uses the official YouTube Data API v3 (user supplies their own key in Settings).
 * Results are never natively playable inside Lumina; the UI shows an
 * "Open in YouTube Music" action instead of pretending to stream.
 */
class YouTubeMusicProvider(private val prefs: Prefs) : MusicProvider {

    override val info = ProviderInfo(
        id = "youtube_music",
        displayName = "YouTube Music",
        description = "Official playback via YouTube — no stream extraction",
        features = setOf(
            ProviderFeature.SEARCH,
            ProviderFeature.TRACK_METADATA,
            ProviderFeature.ARTWORK
        ),
        capability = MusicProvider.official,
        accentColor = 0xFFFF5252
    )

    @Serializable
    data class YtThumb(val url: String? = null)

    @Serializable
    data class YtThumbs(val high: YtThumb? = null, val medium: YtThumb? = null, val default: YtThumb? = null)

    @Serializable
    data class YtSnippet(
        val title: String? = null,
        val channelTitle: String? = null,
        val thumbnails: YtThumbs? = null
    )

    @Serializable
    data class YtId(val videoId: String? = null)

    @Serializable
    data class YtItem(val id: YtId? = null, val snippet: YtSnippet? = null)

    @Serializable
    data class YtSearchResp(val items: List<YtItem> = emptyList())

    val enabled: Boolean
        get() = !apiKey.isNullOrBlank()

    private val apiKey: String?
        get() = kotlinx.coroutines.runBlocking { prefs.ytApiKey() }

    private fun YtItem.toTrack(): Track {
        val videoId = id?.videoId ?: return Track(
            id = "yt:invalid", title = "Unavailable", artist = "", provider = "youtube_music",
            providerTrackId = "", playbackType = PlaybackType.OFFICIAL_LINK
        )
        val rawTitle = snippet?.title ?: "Unknown title"
        return Track(
            id = "yt:$videoId",
            title = rawTitle,
            artist = snippet?.channelTitle ?: "YouTube",
            durationMs = 0,
            artworkUrl = snippet?.thumbnails?.high?.url
                ?: snippet?.thumbnails?.medium?.url
                ?: snippet?.thumbnails?.default?.url,
            provider = "youtube_music",
            providerTrackId = videoId,
            playbackType = PlaybackType.OFFICIAL_LINK,
            officialUrl = "https://music.youtube.com/watch?v=$videoId"
        )
    }

    override suspend fun search(query: String, limit: Int): List<Track> {
        val key = apiKey ?: return emptyList()
        val url = "https://www.googleapis.com/youtube/v3/search" +
            "?part=snippet&type=video&videoCategoryId=10&maxResults=$limit" +
            "&q=${urlEncode(query)}&key=$key"
        val resp = Http.getJson<YtSearchResp>(url) ?: return emptyList()
        return resp.items.mapNotNull { item ->
            if (item.id?.videoId == null) null else item.toTrack()
        }
    }

    override suspend fun discover(section: DiscoverSection): List<Track> = emptyList()
}
