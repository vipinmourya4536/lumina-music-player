package com.lumina.music.playback

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.glance.appwidget.updateAll
import com.google.common.util.concurrent.MoreExecutors
import com.lumina.music.ServiceLocator
import com.lumina.music.core.model.PlaybackType
import com.lumina.music.core.model.QueueEntry
import com.lumina.music.core.model.Track
import com.lumina.music.data.WidgetSnapshot
import com.lumina.music.widget.LuminaWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.guava.await

/**
 * The single source of truth for playback on the UI side.
 * Main screen, mini-player, Now Playing, widget and system surfaces all read
 * this same state, which itself mirrors the MediaSession-backed MediaController.
 */
class PlaybackConnection(private val app: Context) {

    companion object {
        private const val TAG = "PlaybackConnection"
        const val EXTRA_EXPAND_NOW_PLAYING = "expand_now_playing"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val repo get() = ServiceLocator.libraryRepository(app)
    private var controller: MediaController? = null

    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    val connected = MutableStateFlow(false)

    private var positionJob: Job? = null

    init {
        connect()
    }

    private fun connect() {
        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        val future = MediaController.Builder(app, token).buildAsync()
        future.addListener({
            try {
                val c = future.get()
                controller = c
                c.addListener(playerListener)
                connected.value = true
                refreshState()
                startPositionTicker()
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to connect MediaController", t)
            }
        }, MoreExecutors.directExecutor())
    }

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            refreshState()
            startPositionTicker()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            refreshState()
            if (isPlaying) startPositionTicker() else stopPositionTicker()
        }

        override fun onPlaybackStateChanged(playbackState: Int) = refreshState()

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) = refreshState()

        override fun onRepeatModeChanged(repeatMode: Int) = refreshState()

        override fun onTimelineChanged(timeline: Timeline, reason: Int) = refreshState()

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) = refreshState()
    }

    private fun startPositionTicker() {
        stopPositionTicker()
        if (controller?.isPlaying != true) return
        positionJob = scope.launch {
            while (isActive) {
                updatePosition()
                delay(500)
            }
        }
    }

    private fun stopPositionTicker() {
        positionJob?.cancel()
        positionJob = null
    }

    private fun updatePosition() {
        val c = controller ?: return
        _state.update {
            it.copy(
                positionMs = c.currentPosition.coerceAtLeast(0),
                durationMs = c.duration.coerceAtLeast(0)
            )
        }
    }

    private fun refreshState() {
        val c = controller ?: return
        scope.launch {
            val item = c.currentMediaItem
            val track = item?.mediaId?.takeIf { it.isNotBlank() }?.let { repo.getTrack(it) }
            val queue = buildQueueSnapshot(c)
            _state.update { current ->
                current.copy(
                    currentTrack = track,
                    hasTrack = item != null && c.mediaItemCount > 0,
                    isPlaying = c.isPlaying,
                    isBuffering = c.playbackState == Player.STATE_BUFFERING,
                    positionMs = c.currentPosition.coerceAtLeast(0),
                    durationMs = if (c.duration > 0) c.duration else track?.durationMs ?: 0,
                    shuffleEnabled = c.shuffleModeEnabled,
                    repeatMode = c.repeatMode,
                    queue = queue,
                    currentIndex = c.currentMediaItemIndex
                )
            }
        }
    }

    private fun buildQueueSnapshot(c: MediaController): List<QueueEntry> {
        if (c.mediaItemCount == 0) return emptyList()
        return (0 until c.mediaItemCount).map { i ->
            val m = c.getMediaItemAt(i)
            QueueEntry(
                mediaId = m.mediaId,
                title = m.mediaMetadata.title?.toString() ?: "Unknown",
                artist = m.mediaMetadata.artist?.toString() ?: "",
                artworkUrl = m.mediaMetadata.artworkUri?.toString(),
                provider = m.mediaMetadata.extras?.getString("provider")
            )
        }
    }

    // ------------------------------------------------------------------
    // Commands
    // ------------------------------------------------------------------

    fun playTracks(tracks: List<Track>, startIndex: Int = 0) {
        val playable = tracks.filter { it.isPlayable && it.playbackUri() != null }
        if (playable.isEmpty()) return
        scope.launch {
            repo.cacheTracks(playable)
            val c = controller ?: return@launch
            c.setMediaItems(
                playable.map { it.toMediaItem() },
                startIndex.coerceIn(0, playable.size - 1),
                0
            )
            c.prepare()
            c.play()
        }
    }

    fun playTrack(track: Track) = playTracks(listOf(track), 0)

    fun addToQueue(track: Track) {
        if (!track.isPlayable) return
        scope.launch {
            repo.cacheTracks(listOf(track))
            controller?.addMediaItem(track.toMediaItem())
        }
    }

    fun playNext(track: Track) {
        if (!track.isPlayable) return
        scope.launch {
            repo.cacheTracks(listOf(track))
            val c = controller ?: return@launch
            c.addMediaItem(c.currentMediaItemIndex + 1, track.toMediaItem())
        }
    }

    fun togglePlay() {
        val c = controller ?: return
        if (c.isPlaying) {
            c.pause()
        } else {
            if (c.playbackState == Player.STATE_IDLE && c.mediaItemCount == 0) {
                resumeLastQueue()
            } else {
                if (c.playbackState == Player.STATE_IDLE) c.prepare()
                c.play()
            }
        }
    }

    /** Widget play button after process death: restore the persisted queue. */
    private fun resumeLastQueue() {
        scope.launch {
            val snap = ServiceLocator.prefs(app).loadQueue() ?: return@launch
            val tracks = snap.ids.mapNotNull { repo.getTrack(it) }
                .filter { it.isPlayable && it.playbackUri() != null }
            if (tracks.isEmpty()) return@launch
            val c = controller ?: return@launch
            c.setMediaItems(
                tracks.map { it.toMediaItem() },
                snap.index.coerceIn(0, tracks.size - 1),
                snap.positionMs.coerceAtLeast(0)
            )
            c.prepare()
            c.play()
        }
    }

    fun next() {
        controller?.seekToNextMediaItem()
    }

    fun previous() {
        controller?.seekToPreviousMediaItem()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    fun toggleShuffle() {
        controller?.shuffleModeEnabled = !(controller?.shuffleModeEnabled ?: false)
    }

    fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun skipTo(index: Int) {
        controller?.seekTo(index, 0)
    }

    fun removeFromQueue(index: Int) {
        controller?.removeMediaItem(index)
    }

    fun toggleLike(track: Track) {
        scope.launch {
            // Make sure the track exists in the library before flipping the flag.
            repo.cacheTracks(listOf(track))
            repo.toggleLikeById(track.id)
            refreshState()
            pushWidgetSnapshot()
        }
    }

    suspend fun isLiked(trackId: String?): Boolean {
        if (trackId == null) return false
        return repo.getTrack(trackId)?.liked ?: false
    }

    fun saveQueueAsPlaylist(name: String, onDone: (Boolean) -> Unit = {}) {
        scope.launch {
            try {
                val tracks = _state.value.queue.mapNotNull { repo.getTrack(it.mediaId) }
                if (tracks.isEmpty()) {
                    onDone(false)
                    return@launch
                }
                val id = repo.createPlaylist(name)
                repo.addTracksToPlaylist(id, tracks)
                onDone(true)
            } catch (t: Throwable) {
                onDone(false)
            }
        }
    }

    fun openOfficial(track: Track) {
        val url = track.officialUrl ?: return
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            app.startActivity(intent)
        } catch (_: Throwable) {
        }
    }

    fun expandIntent(intent: Intent?): Boolean =
        intent?.getBooleanExtra(EXTRA_EXPAND_NOW_PLAYING, false) ?: false

    /** Push current controller state into the widget snapshot + refresh widgets. */
    fun pushWidgetSnapshot() {
        scope.launch {
            try {
                val s = _state.value
                val liked = s.currentTrack?.let { repo.getTrack(it.id)?.liked } ?: false
                ServiceLocator.prefs(app).saveWidgetSnapshot(
                    WidgetSnapshot(
                        hasTrack = s.hasTrack,
                        mediaId = s.currentTrack?.id ?: "",
                        title = s.currentTrack?.title ?: s.queue.getOrNull(s.currentIndex)?.title ?: "",
                        artist = s.currentTrack?.artist ?: s.queue.getOrNull(s.currentIndex)?.artist ?: "",
                        artworkUrl = s.currentTrack?.artworkUrl
                            ?: s.queue.getOrNull(s.currentIndex)?.artworkUrl,
                        isPlaying = s.isPlaying,
                        liked = liked,
                        positionMs = s.positionMs,
                        durationMs = s.durationMs,
                        provider = s.currentTrack?.provider
                            ?: s.queue.getOrNull(s.currentIndex)?.provider ?: ""
                    )
                )
                LuminaWidget().updateAll(app)
            } catch (_: Throwable) {
            }
        }
    }
}
