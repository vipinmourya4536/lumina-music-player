package com.lumina.music.widget

import android.content.ComponentName
import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.lumina.music.ServiceLocator
import com.lumina.music.playback.PlaybackService
import com.lumina.music.playback.toMediaItem
import kotlinx.coroutines.guava.await

/**
 * Widget actions drive the REAL playback session: they connect a
 * MediaController to the app's MediaSession and forward commands.
 */
abstract class MediaActionCallback : ActionCallback {
    protected suspend fun withController(context: Context, block: suspend (MediaController) -> Unit) {
        try {
            val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
            val future = MediaController.Builder(context, token).buildAsync()
            val controller = future.await()
            try {
                block(controller)
            } finally {
                controller.release()
            }
        } catch (_: Throwable) {
        }
    }
}

class PlayPauseAction : MediaActionCallback() {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        withController(context) { controller ->
            if (controller.mediaItemCount == 0) {
                // Process was killed; restore the persisted queue.
                val repo = ServiceLocator.libraryRepository(context)
                val snap = ServiceLocator.prefs(context).loadQueue()
                val tracks = snap?.ids?.mapNotNull { repo.getTrack(it) }
                    ?.filter { it.isPlayable && it.playbackUri() != null }
                    .orEmpty()
                if (tracks.isNotEmpty() && snap != null) {
                    controller.setMediaItems(
                        tracks.map { it.toMediaItem() },
                        snap.index.coerceIn(0, tracks.size - 1),
                        snap.positionMs.coerceAtLeast(0)
                    )
                    controller.prepare()
                    controller.play()
                }
            } else if (controller.isPlaying) {
                controller.pause()
            } else {
                if (controller.playbackState == Player.STATE_IDLE) controller.prepare()
                controller.play()
            }
        }
        LuminaWidget().updateAll(context)
    }
}

class NextAction : MediaActionCallback() {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        withController(context) { it.seekToNextMediaItem() }
    }
}

class PreviousAction : MediaActionCallback() {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        withController(context) { it.seekToPreviousMediaItem() }
    }
}

class ShuffleAction : MediaActionCallback() {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        withController(context) { it.shuffleModeEnabled = !it.shuffleModeEnabled }
        LuminaWidget().updateAll(context)
    }
}

class LikeAction : MediaActionCallback() {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val prefs = ServiceLocator.prefs(context)
        val mediaId = prefs.widgetSnapshot().mediaId
        if (mediaId.isNotBlank()) {
            ServiceLocator.libraryRepository(context).toggleLikeById(mediaId)
            prefs.updateWidgetSnapshot { it.copy(liked = !(it.liked)) }
        }
        LuminaWidget().updateAll(context)
    }
}
