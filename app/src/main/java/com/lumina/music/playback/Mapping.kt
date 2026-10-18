package com.lumina.music.playback

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.lumina.music.core.model.Track

/** Converts normalized Lumina tracks into Media3 media items. */
fun Track.toMediaMetadata(): MediaMetadata = MediaMetadata.Builder()
    .setTitle(title)
    .setArtist(artist)
    .setAlbumTitle(album)
    .setArtworkUri(artworkUrl?.let { Uri.parse(it) })
    .setDurationMs(if (durationMs > 0) durationMs else C.TIME_UNSET)
    .setExtras(Bundle().apply { putString("provider", provider) })
    .build()

fun Track.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(id)
    .setUri(playbackUri()?.let { Uri.parse(it) })
    .setMediaMetadata(toMediaMetadata())
    .build()
