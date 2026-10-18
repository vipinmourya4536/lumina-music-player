package com.lumina.music.core.model

/**
 * What a track can actually do inside Lumina's playback engine.
 * The UI must never show a native Play action for OFFICIAL_LINK content.
 */
enum class PlaybackType {
    /** Full stream playable by Media3 (e.g. Audius). */
    STREAM,

    /** Short official preview stream (e.g. Apple Music 30s previews). */
    PREVIEW,

    /** File on the device, playable through the same engine. */
    LOCAL,

    /** Not playable natively; the app only offers the official link. */
    OFFICIAL_LINK
}

/** Explicit provider capabilities — inspired by BloomeeTunes' plugin-first design. */
enum class ProviderFeature {
    SEARCH,
    TRACK_METADATA,
    ARTWORK,
    DISCOVERY
}

/** Human-facing capability label of a provider's results. */
enum class PlayCapability(val label: String) {
    DIRECT_PLAYBACK("Full playback"),
    PREVIEW_ONLY("Preview only"),
    OFFICIAL_PLAYER_ONLY("Official player"),
    LOCAL_ONLY("On device")
}

data class ProviderInfo(
    val id: String,
    val displayName: String,
    val description: String,
    val features: Set<ProviderFeature>,
    val capability: PlayCapability,
    val accentColor: Long
)

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val durationMs: Long = 0,
    val artworkUrl: String? = null,
    val provider: String,
    val providerTrackId: String,
    val playbackType: PlaybackType,
    val streamUrl: String? = null,
    val localUri: String? = null,
    val officialUrl: String? = null,
    val liked: Boolean = false,
    val playCount: Int = 0,
    val lastPlayedAt: Long? = null,
    val imported: Boolean = false
) {
    val isPlayable: Boolean
        get() = when (playbackType) {
            PlaybackType.OFFICIAL_LINK -> false
            PlaybackType.LOCAL -> !localUri.isNullOrBlank()
            else -> !streamUrl.isNullOrBlank()
        }

    fun playbackUri(): String? = when (playbackType) {
        PlaybackType.LOCAL -> localUri
        PlaybackType.OFFICIAL_LINK -> null
        else -> streamUrl
    }
}

data class Playlist(
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val trackCount: Int = 0
)

data class QueueEntry(
    val mediaId: String,
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val provider: String?
)

enum class DiscoverSection(val title: String) {
    TRENDING("Trending now"),
    NEW_RELEASES("New & underground"),
    TOP_CHARTS("Top charts")
}

data class SearchOutcome(
    val tracks: List<Track>,
    val providerStatus: Map<String, Boolean>
)

fun providerDisplayName(id: String): String = when (id) {
    "audius" -> "Audius"
    "itunes" -> "Apple Music"
    "youtube_music" -> "YouTube Music"
    "local" -> "On device"
    else -> id.replaceFirstChar { it.uppercase() }
}

fun providerCapabilityLabel(id: String): String = when (id) {
    "audius" -> "Full track"
    "itunes" -> "30s preview"
    "youtube_music" -> "Official link"
    "local" -> "Device file"
    else -> ""
}
