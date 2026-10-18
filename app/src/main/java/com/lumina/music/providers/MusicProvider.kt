package com.lumina.music.providers

import com.lumina.music.core.model.DiscoverSection
import com.lumina.music.core.model.PlayCapability
import com.lumina.music.core.model.ProviderFeature
import com.lumina.music.core.model.ProviderInfo
import com.lumina.music.core.model.Track

/**
 * A modular music source. Providers are independent plugins behind a
 * central manager — they never leak their own data structures into the UI.
 */
interface MusicProvider {
    val info: ProviderInfo

    /** Search this source. Returns normalized Lumina tracks (may be empty). */
    suspend fun search(query: String, limit: Int = 20): List<Track>

    /** Discovery feeds; providers declare which sections they support. */
    suspend fun discover(section: DiscoverSection): List<Track>

    /**
     * Produce a currently-valid playable URI for the track, or null.
     * Default: the stored playback URI (static streams / local files).
     */
    suspend fun resolveStreamUrl(track: Track): String? = track.playbackUri()

    val supportsSearch: Boolean get() = ProviderFeature.SEARCH in info.features
    val supportsDiscovery: Boolean get() = ProviderFeature.DISCOVERY in info.features

    companion object {
        val direct = PlayCapability.DIRECT_PLAYBACK
        val preview = PlayCapability.PREVIEW_ONLY
        val official = PlayCapability.OFFICIAL_PLAYER_ONLY
        val local = PlayCapability.LOCAL_ONLY
        val features: Set<ProviderFeature> = ProviderFeature.entries.toSet()
    }
}
