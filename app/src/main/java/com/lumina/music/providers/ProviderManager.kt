package com.lumina.music.providers

import android.content.Context
import com.lumina.music.core.model.DiscoverSection
import com.lumina.music.core.model.SearchOutcome
import com.lumina.music.core.model.Track
import com.lumina.music.data.Prefs
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout

/**
 * Central provider manager. Every provider can fail independently;
 * failures never take the rest of the app down.
 */
class ProviderManager(private val context: Context, private val prefs: Prefs) {

    val audius by lazy { AudiusProvider() }
    val itunes by lazy { ITunesProvider() }
    val youtube by lazy { YouTubeMusicProvider(prefs) }
    val local by lazy { LocalMusicProvider(context) }

    val onlineProviders: List<MusicProvider>
        get() = listOf(audius, itunes) + if (youtube.enabled) listOf(youtube) else emptyList()

    val allProviders: List<MusicProvider>
        get() = listOf(audius, itunes, youtube, local)

    /** Merged, interleaved search across all available providers. */
    suspend fun searchAll(query: String): SearchOutcome = coroutineScope {
        val providers = onlineProviders
        val results = providers.map { provider ->
            async {
                try {
                    withTimeout(15_000) { provider.search(query) }
                } catch (_: Throwable) {
                    emptyList()
                }
            }
        }.awaitAll()

        val status = providers.mapIndexed { i, p -> p.info.id to results[i].isNotEmpty() }.toMap()
        val merged = interleave(results.filter { it.isNotEmpty() })
        SearchOutcome(tracks = merged, providerStatus = status)
    }

    suspend fun discover(section: DiscoverSection): List<Track> = coroutineScope {
        onlineProviders.map { provider ->
            async {
                try {
                    withTimeout(15_000) { provider.discover(section) }
                } catch (_: Throwable) {
                    emptyList()
                }
            }
        }.awaitAll().flatten()
    }

    suspend fun resolveStreamUrl(track: Track): String? {
        val provider = allProviders.firstOrNull { it.info.id == track.provider }
        return try {
            provider?.resolveStreamUrl(track)
        } catch (_: Throwable) {
            track.playbackUri()
        }
    }

    /** Round-robin merge so search results mix sources instead of stacking them. */
    private fun interleave(lists: List<List<Track>>): List<Track> {
        val merged = ArrayList<Track>()
        var index = 0
        while (merged.size < 80) {
            var added = false
            for (list in lists) {
                if (index < list.size) {
                    merged.add(list[index])
                    added = true
                    if (merged.size >= 80) break
                }
            }
            if (!added) break
            index++
        }
        return merged
    }
}
