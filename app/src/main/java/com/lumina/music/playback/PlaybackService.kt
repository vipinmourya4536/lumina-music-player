package com.lumina.music.playback

import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.glance.appwidget.updateAll
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.lumina.music.ServiceLocator
import com.lumina.music.data.QueueSnapshot
import com.lumina.music.widget.LuminaWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Owns the player and the MediaSession. The Activity and UI never own playback;
 * they observe and command this service through MediaController.
 */
class PlaybackService : MediaSessionService() {

    companion object {
        private const val TAG = "PlaybackService"
        const val CUSTOM_COMMAND_TOGGLE_LIKE = "com.lumina.music.TOGGLE_LIKE"
    }

    private var session: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var persistJob: Job? = null
    private var errorRetryAttempted = false

    override fun onCreate() {
        super.onCreate()
        val repo = ServiceLocator.libraryRepository(this)
        val prefs = ServiceLocator.prefs(this)
        val providers = ServiceLocator.providerManager(this)

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        session = MediaSession.Builder(this, player)
            .setCallback(SessionCallback(repo, providers))
            .build()

        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                errorRetryAttempted = false
                val id = mediaItem?.mediaId
                if (!id.isNullOrBlank()) {
                    serviceScope.launch { repo.recordPlay(id) }
                }
                pushWidget()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                pushWidget()
            }

            override fun onEvents(player: Player, events: Player.Events) {
                if (events.containsAny(
                        Player.EVENT_TIMELINE_CHANGED,
                        Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
                        Player.EVENT_REPEAT_MODE_CHANGED
                    )
                ) {
                    persistQueueDebounced()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                // One silent recovery attempt: re-resolve + re-prepare at same position.
                if (errorRetryAttempted) return
                errorRetryAttempted = true
                val p = session?.player ?: return
                val index = p.currentMediaItemIndex
                val position = p.currentPosition
                p.seekTo(index, position)
                p.prepare()
                p.play()
            }
        })

        // Keep widget progress fresh while playing.
        serviceScope.launch {
            while (isActive) {
                if (session?.player?.isPlaying == true) pushWidget()
                delay(15_000)
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    private fun pushWidget() {
        serviceScope.launch {
            try {
                val prefs = ServiceLocator.prefs(this@PlaybackService)
                val repo = ServiceLocator.libraryRepository(this@PlaybackService)
                val p = session?.player ?: return@launch
                val item = p.currentMediaItem
                val id = item?.mediaId
                val liked = if (id != null) repo.getTrack(id)?.liked ?: false else false
                val snapshot = com.lumina.music.data.WidgetSnapshot(
                    hasTrack = item != null,
                    mediaId = id ?: "",
                    title = item?.mediaMetadata?.title?.toString() ?: "",
                    artist = item?.mediaMetadata?.artist?.toString() ?: "",
                    artworkUrl = item?.mediaMetadata?.artworkUri?.toString(),
                    isPlaying = p.isPlaying,
                    liked = liked,
                    positionMs = p.currentPosition.coerceAtLeast(0),
                    durationMs = p.duration.coerceAtLeast(0),
                    provider = item?.mediaMetadata?.extras?.getString("provider") ?: ""
                )
                prefs.saveWidgetSnapshot(snapshot)
                LuminaWidget().updateAll(applicationContext)
            } catch (t: Throwable) {
                Log.w(TAG, "widget update failed", t)
            }
        }
    }

    private fun persistQueueDebounced() {
        persistJob?.cancel()
        persistJob = serviceScope.launch {
            delay(1500)
            try {
                val p = session?.player ?: return@launch
                if (p.mediaItemCount == 0) {
                    ServiceLocator.prefs(this@PlaybackService).saveQueue(QueueSnapshot(emptyList(), 0))
                    return@launch
                }
                val ids = (0 until p.mediaItemCount).map { p.getMediaItemAt(it).mediaId }
                ServiceLocator.prefs(this@PlaybackService).saveQueue(
                    QueueSnapshot(ids, p.currentMediaItemIndex, p.currentPosition.coerceAtLeast(0))
                )
            } catch (_: Throwable) {
            }
        }
    }

    private inner class SessionCallback(
        private val repo: com.lumina.music.data.LibraryRepository,
        private val providers: com.lumina.music.providers.ProviderManager
    ) : MediaSession.Callback {

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult
                .DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
                .add(SessionCommand(CUSTOM_COMMAND_TOGGLE_LIKE, android.os.Bundle.EMPTY))
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: android.os.Bundle
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == CUSTOM_COMMAND_TOGGLE_LIKE) {
                serviceScope.launch {
                    val mediaId = args.getString("mediaId") ?: session.player.currentMediaItem?.mediaId
                    if (!mediaId.isNullOrBlank()) {
                        repo.toggleLikeById(mediaId)
                        pushWidget()
                    }
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>
        ): ListenableFuture<List<MediaItem>> {
            val future = SettableFuture.create<List<MediaItem>>()
            serviceScope.launch {
                try {
                    val resolved = mediaItems.mapNotNull { item -> resolveItem(item) }
                    future.set(resolved)
                } catch (t: Throwable) {
                    future.setException(t)
                }
            }
            return future
        }

        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val future = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
            serviceScope.launch {
                try {
                    val snap = ServiceLocator.prefs(this@PlaybackService).loadQueue()
                    if (snap == null) {
                        future.setException(IllegalStateException("No saved queue"))
                        return@launch
                    }
                    val tracks = snap.ids
                        .mapNotNull { repo.getTrack(it) }
                        .filter { it.isPlayable && it.playbackUri() != null }
                    if (tracks.isEmpty()) {
                        future.setException(IllegalStateException("No saved queue"))
                        return@launch
                    }
                    val items = tracks.map { it.toMediaItem() }
                    val index = snap.index.coerceIn(0, tracks.size - 1)
                    future.set(MediaSession.MediaItemsWithStartPosition(items, index, snap.positionMs.coerceAtLeast(0)))
                } catch (t: Throwable) {
                    future.setException(t)
                }
            }
            return future
        }

        private suspend fun resolveItem(item: MediaItem): MediaItem? {
            if (item.mediaId.isBlank()) return null
            val track = repo.getTrack(item.mediaId) ?: return null
            if (!track.isPlayable) return null
            var uri = track.playbackUri()
            // Re-resolve streams that may expire (Audius endpoints are stable but cheap to refresh).
            if (track.playbackType == com.lumina.music.core.model.PlaybackType.STREAM) {
                uri = providers.resolveStreamUrl(track) ?: uri
            }
            if (uri.isNullOrBlank()) return null
            val metadata: MediaMetadata = item.mediaMetadata
            val builder = item.buildUpon()
                .setUri(android.net.Uri.parse(uri))
                .setMediaMetadata(track.toMediaMetadata())
            if (metadata.title == null) builder.setMediaMetadata(track.toMediaMetadata())
            return builder.build()
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        session?.run {
            player.release()
            release()
        }
        session = null
        serviceScope.cancel()
        super.onDestroy()
    }
}
