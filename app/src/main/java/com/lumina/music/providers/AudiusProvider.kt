package com.lumina.music.providers

import com.lumina.music.core.model.DiscoverSection
import com.lumina.music.core.model.PlaybackType
import com.lumina.music.core.model.ProviderFeature
import com.lumina.music.core.model.ProviderInfo
import com.lumina.music.core.model.Track
import com.lumina.music.core.util.Http
import com.lumina.music.core.util.urlEncode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Audius — legal, key-free, full-length streaming from independent artists.
 * Streams resolve through the official /stream endpoint (302 to CDN audio).
 */
class AudiusProvider : MusicProvider {

    override val info = ProviderInfo(
        id = "audius",
        displayName = "Audius",
        description = "Full-length streams from independent artists",
        features = setOf(
            ProviderFeature.SEARCH,
            ProviderFeature.TRACK_METADATA,
            ProviderFeature.ARTWORK,
            ProviderFeature.DISCOVERY
        ),
        capability = MusicProvider.direct,
        accentColor = 0xFFCC4FF5
    )

    @Serializable
    data class AudiusHosts(val data: List<String> = emptyList())

    @Serializable
    data class AudiusUser(val name: String? = null)

    @Serializable
    data class AudiusArtwork(
        @SerialName("150x150") val small: String? = null,
        @SerialName("480x480") val medium: String? = null,
        @SerialName("1000x1000") val large: String? = null
    )

    @Serializable
    data class AudiusTrack(
        val id: String,
        val title: String = "Unknown title",
        val duration: Int = 0,
        val user: AudiusUser? = null,
        val artwork: AudiusArtwork? = null,
        val genre: String? = null,
        val permalink: String? = null
    )

    @Serializable
    data class AudiusList(val data: List<AudiusTrack> = emptyList())

    private var host: String = "https://discoveryprovider.audius.co"
    private var hostResolved = false

    private suspend fun ensureHost() {
        if (hostResolved) return
        hostResolved = true
        try {
            val hosts = Http.getJson<AudiusHosts>("https://api.audius.co")
            val first = hosts?.data?.firstOrNull()
            if (!first.isNullOrBlank()) host = first
        } catch (_: Throwable) {
        }
    }

    private fun AudiusTrack.toTrack(): Track = Track(
        id = "audius:$id",
        title = title,
        artist = user?.name?.takeIf { it.isNotBlank() } ?: "Unknown artist",
        album = genre?.takeIf { it.isNotBlank() },
        durationMs = duration.toLong() * 1000,
        artworkUrl = artwork?.medium ?: artwork?.large ?: artwork?.small,
        provider = "audius",
        providerTrackId = id,
        playbackType = PlaybackType.STREAM,
        streamUrl = "$host/v1/tracks/$id/stream?app_name=Lumina",
        officialUrl = permalink
    )

    override suspend fun search(query: String, limit: Int): List<Track> {
        ensureHost()
        val url = "$host/v1/tracks/search?query=${urlEncode(query)}&app_name=Lumina&limit=$limit"
        return Http.getJson<AudiusList>(url)?.data?.map { it.toTrack() } ?: emptyList()
    }

    override suspend fun discover(section: DiscoverSection): List<Track> {
        ensureHost()
        val url = when (section) {
            DiscoverSection.TRENDING -> "$host/v1/tracks/trending?app_name=Lumina&limit=25"
            DiscoverSection.NEW_RELEASES -> "$host/v1/tracks/trending/underground?app_name=Lumina&limit=25"
            DiscoverSection.TOP_CHARTS -> "$host/v1/tracks/trending?app_name=Lumina&limit=25&time=week"
        }
        return Http.getJson<AudiusList>(url)?.data?.map { it.toTrack() } ?: emptyList()
    }

    override suspend fun resolveStreamUrl(track: Track): String? {
        ensureHost()
        return "$host/v1/tracks/${track.providerTrackId}/stream?app_name=Lumina"
    }
}
