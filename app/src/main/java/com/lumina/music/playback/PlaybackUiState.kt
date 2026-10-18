package com.lumina.music.playback

import com.lumina.music.core.model.QueueEntry
import com.lumina.music.core.model.Track

/** Mirrored playback state observed by every UI surface. */
data class PlaybackUiState(
    val currentTrack: Track? = null,
    val hasTrack: Boolean = false,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = 0,
    val queue: List<QueueEntry> = emptyList(),
    val currentIndex: Int = -1
)
