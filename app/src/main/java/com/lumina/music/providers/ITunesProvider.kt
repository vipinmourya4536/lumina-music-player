package com.lumina.music.providers

import com.lumina.music.core.model.DiscoverSection
import com.lumina.music.core.model.PlaybackType
import com.lumina.music.core.model.ProviderFeature
import com.lumina.music.core.model.ProviderInfo
import com.lumina.music.core.model.Track
import com.lumina.music.core.util.Http
import com.lumina.music.core.util.urlEncode
import kotlinx.serialization.Serializable

/**
 * Apple Music catalog via the public iTunes Search API.
 * Official metadata + artwork + 30-second preview streams.
 * Anything beyond the preview is offered as an official link, never extracted.
 */
class ITunesProvider : MusicProvider {

    override val info = ProviderInfo(
        id = "itunes",
        displayName = "Apple Music",
        description = "Official metadata, artwork and 30-second previews",
        features = setOf(
            ProviderFeature.SEARCH,
            ProviderFeature.TRACK_METADATA,
            ProviderFeature.ARTWORK,
            ProviderFeature.DISCOVERY
        ),
        capability = MusicProvider.preview,
        accentColor = 0xFFFA57C1
    )

    @Serializable
    data class ITunesItem(
        val wrapperType: String? = null,
        val trackId: Long? = null,
        val trackName: String? = null,
        val artistName: String? = null,
        val collectionName: String? = null,
        val previewUrl: String? = null,
        val artworkUrl100: String? = null,
        val trackTimeMillis: Long? = null,
        val trackViewUrl: String? = null
    )

    @Serializable
    data class ITunesResp(val results: List<ITunesItem> = emptyList())

    @Serializable
    data class ITunesFeedSong(
        val id: String? = null,
        val name: String? = null,
        val artistName: String? = null,
        val artworkUrl100: String? = null,
        val url: String? = null
    )

    @Serializable
    data class ITunesFeed(val results: List<ITunesFeedSong> = emptyList())

    @Serializable
    data class ITunesFeedResp(val feed: ITunesFeed = ITunesFeed())

    private fun ITunesItem.toTrack(): Track {
        val playable = !previewUrl.isNullOrBlank()
        val numericId = trackId ?: trackName?.hashCode()?.toLong() ?: 0L
        return Track(
            id = "itunes:$numericId",
            title = trackName ?: "Unknown title",
            artist = artistName ?: "Unknown artist",
            album = collectionName,
            durationMs = trackTimeMillis ?: 0,
            artworkUrl = artworkUrl100?.replace("100x100bb", "600x600bb"),
            provider = "itunes",
            providerTrackId = numericId.toString(),
            playbackType = if (playable) PlaybackType.PREVIEW else PlaybackType.OFFICIAL_LINK,
            streamUrl = previewUrl,
            officialUrl = trackViewUrl
        )
    }

    private fun ITunesFeedSong.toTrack(): Track {
        val numericId = id?.substringAfterLast('/')
        return Track(
            id = "itunes:$numericId",
            title = name ?: "Unknown title",
            artist = artistName ?: "Unknown artist",
            durationMs = 0,
            artworkUrl = artworkUrl100?.replace("100x100bb", "600x600bb"),
            provider = "itunes",
            providerTrackId = numericId ?: "",
            playbackType = PlaybackType.OFFICIAL_LINK,
            officialUrl = url
        )
    }

    override suspend fun search(query: String, limit: Int): List<Track> {
        val url = "https://itunes.apple.com/search?term=${urlEncode(query)}&media=music&entity=song&limit=$limit"
        return Http.getJson<ITunesResp>(url)?.results
            ?.filter { it.wrapperType == null || it.wrapperType == "track" }
            ?.map { it.toTrack() }
            ?: emptyList()
    }

    override suspend fun discover(section: DiscoverSection): List<Track> {
        if (section != DiscoverSection.TOP_CHARTS) return emptyList()
        val feed = Http.getJson<ITunesFeedResp>(
            "https://rss.applemarketingtools.com/api/v2/us/music/most-played/25/songs.json"
        ) ?: return emptyList()
        val songs = feed.feed.results.ifEmpty { return emptyList() }
        val ids = songs.mapNotNull { it.id?.substringAfterLast('/')?.takeWhile { c -> c.isDigit() } }
        if (ids.isEmpty()) return songs.map { it.toTrack() }
        // Enrich chart entries with official preview streams via the lookup API.
        val lookup = Http.getJson<ITunesResp>(
            "https://itunes.apple.com/lookup?id=${ids.joinToString(",")}&entity=song"
        )
        val enriched = lookup?.results
            ?.filter { it.wrapperType == "track" }
            ?.map { it.toTrack() }
            .orEmpty()
        if (enriched.isNotEmpty()) return enriched
        return songs.map { it.toTrack() }
    }
}
