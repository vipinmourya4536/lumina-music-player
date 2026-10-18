package com.lumina.music.ui.nowplaying

import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.SaveAs
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Size
import com.lumina.music.core.model.PlaybackType
import com.lumina.music.core.model.Track
import com.lumina.music.core.model.providerCapabilityLabel
import com.lumina.music.core.model.providerDisplayName
import com.lumina.music.core.util.Format
import com.lumina.music.playback.PlaybackConnection
import com.lumina.music.playback.PlaybackUiState
import com.lumina.music.ui.components.Artwork
import com.lumina.music.ui.components.CircleIconButton
import com.lumina.music.ui.components.GlassCard
import com.lumina.music.ui.theme.AppBg
import com.lumina.music.ui.theme.AppPrimary
import com.lumina.music.ui.theme.LikeColor
import com.lumina.music.ui.theme.TextPrimary
import com.lumina.music.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NowPlayingScreen(playback: PlaybackConnection, onClose: () -> Unit) {
    val state by playback.state.collectAsStateWithLifecycle()
    val track = state.currentTrack
    var paletteColor by remember { mutableStateOf(Color(0xFF2A2440)) }
    var showQueue by remember { mutableStateOf(false) }
    var showSaveQueue by remember { mutableStateOf(false) }
    var queueName by remember { mutableStateOf("") }
    var draggingPosition by remember { mutableStateOf<Long?>(null) }

    val context = LocalContext.current
    val artworkUrl = track?.artworkUrl
    LaunchedEffect(artworkUrl) {
        if (artworkUrl == null) {
            paletteColor = Color(0xFF2A2440)
        } else {
            val color = extractPaletteColor(context, artworkUrl)
            if (color != null) paletteColor = color
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to paletteColor.copy(alpha = 0.6f),
                    0.45f to paletteColor.copy(alpha = 0.28f),
                    1f to AppBg
                )
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
        ) {
            // Header
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircleIconButton(
                    icon = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Close",
                    onClick = onClose,
                    sizeDp = 44
                )
                Spacer(Modifier.width(12.dp))
                Text("NOW PLAYING", color = TextSecondary, fontSize = 12.sp, letterSpacing = 2.sp)
                Spacer(Modifier.weight(1f))
                if (state.queue.isNotEmpty()) {
                    IconButton(onClick = { showSaveQueue = true }) {
                        Icon(Icons.Rounded.SaveAs, contentDescription = "Save queue as playlist", tint = TextSecondary)
                    }
                }
            }

            // Artwork
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Artwork(
                    url = artworkUrl,
                    contentDescription = track?.title,
                    modifier = Modifier
                        .size(300.dp)
                        .shadow(
                            elevation = 48.dp,
                            shape = RoundedCornerShape(36.dp),
                            ambientColor = paletteColor,
                            spotColor = paletteColor
                        )
                        .clip(RoundedCornerShape(36.dp)),
                    shape = RoundedCornerShape(36.dp)
                )
            }

            // Title block
            if (track != null) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        track.title,
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        track.artist,
                        color = TextSecondary,
                        fontSize = 14.5.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${providerDisplayName(track.provider)} · ${providerCapabilityLabel(track.provider)}" +
                            if (track.playbackType == PlaybackType.PREVIEW) " · previews play 30s" else "",
                        color = AppPrimary.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Seek bar
                val sliderPosition = draggingPosition ?: state.positionMs
                Slider(
                    value = if (state.durationMs > 0) sliderPosition.toFloat() / state.durationMs.toFloat() else 0f,
                    onValueChange = { fraction ->
                        draggingPosition = (fraction * state.durationMs).toLong()
                    },
                    onValueChangeFinished = {
                        draggingPosition?.let { playback.seekTo(it) }
                        draggingPosition = null
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = TextPrimary,
                        activeTrackColor = AppPrimary,
                        inactiveTrackColor = Color(0x2AFFFFFF)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth()) {
                    Text(Format.duration(sliderPosition), color = TextSecondary, fontSize = 11.sp)
                    Spacer(Modifier.weight(1f))
                    Text(Format.duration(state.durationMs), color = TextSecondary, fontSize = 11.sp)
                }

                Spacer(Modifier.height(10.dp))

                // Main controls
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { playback.toggleShuffle() }) {
                        Icon(
                            Icons.Rounded.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (state.shuffleEnabled) AppPrimary else TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(onClick = { playback.previous() }) {
                        Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous", tint = TextPrimary, modifier = Modifier.size(34.dp))
                    }
                    Box(
                        Modifier
                            .size(74.dp)
                            .clip(CircleShape)
                            .background(AppPrimary)
                            .clickableNoRipple { playback.togglePlay() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.isBuffering) {
                            androidx.compose.material3.CircularProgressIndicator(
                                color = Color(0xFF16102E),
                                modifier = Modifier.size(30.dp),
                                strokeWidth = 3.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (state.isPlaying) "Pause" else "Play",
                                tint = Color(0xFF16102E),
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }
                    IconButton(onClick = { playback.next() }) {
                        Icon(Icons.Rounded.SkipNext, contentDescription = "Next", tint = TextPrimary, modifier = Modifier.size(34.dp))
                    }
                    IconButton(onClick = { playback.cycleRepeat() }) {
                        Icon(
                            imageVector = if (state.repeatMode == 2) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                            contentDescription = "Repeat",
                            tint = if (state.repeatMode != 0) AppPrimary else TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Secondary actions
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { track?.let { playback.toggleLike(it) } }) {
                        Icon(
                            imageVector = if (track?.liked == true) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (track?.liked == true) LikeColor else TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(onClick = { showQueue = true }) {
                        Icon(Icons.Rounded.QueueMusic, contentDescription = "Queue", tint = TextSecondary, modifier = Modifier.size(22.dp))
                    }
                    IconButton(onClick = { playback.openOfficial(track) }) {
                        Icon(Icons.Rounded.OpenInNew, contentDescription = "Open official player", tint = TextSecondary, modifier = Modifier.size(22.dp))
                    }
                    IconButton(onClick = { track?.let { playback.addToQueue(it) } }) {
                        Icon(Icons.Rounded.PlaylistAdd, contentDescription = "Add to queue", tint = TextSecondary, modifier = Modifier.size(22.dp))
                    }
                }
            } else {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Nothing is playing yet", color = TextSecondary, fontSize = 15.sp)
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        // Queue sheet
        if (showQueue) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showQueue = false },
                sheetState = sheetState,
                containerColor = Color(0xFF14141E),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                QueueSheet(state = state, playback = playback, onDismiss = { showQueue = false })
            }
        }

        if (showSaveQueue) {
            AlertDialog(
                onDismissRequest = { showSaveQueue = false },
                title = { Text("Save queue as playlist", color = TextPrimary) },
                text = {
                    androidx.compose.material3.OutlinedTextField(
                        value = queueName,
                        onValueChange = { queueName = it },
                        placeholder = { Text("Playlist name", color = TextSecondary) },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (queueName.isNotBlank()) {
                            playback.saveQueueAsPlaylist(queueName.trim())
                        }
                        queueName = ""
                        showSaveQueue = false
                    }) { Text("Save", color = AppPrimary) }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveQueue = false }) { Text("Cancel", color = TextSecondary) }
                },
                containerColor = Color(0xFF161622)
            )
        }
    }
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(onClick = onClick))

@Composable
private fun QueueSheet(state: PlaybackUiState, playback: PlaybackConnection, onDismiss: () -> Unit) {
    Column(Modifier.padding(horizontal = 18.dp)) {
        Text("Up next (${state.queue.size})", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        LazyColumn(Modifier.height(420.dp)) {
            itemsIndexed(
                state.queue,
                key = { _, e -> e.mediaId + e.title }
            ) { index, entry ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            if (index == state.currentIndex) AppPrimary.copy(alpha = 0.15f) else Color.Transparent,
                            RoundedCornerShape(14.dp)
                        )
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { playback.skipTo(index); onDismiss() }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Artwork(url = entry.artworkUrl, contentDescription = entry.title, modifier = Modifier.size(42.dp), shape = RoundedCornerShape(10.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(entry.title, color = TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text(entry.artist, color = TextSecondary, fontSize = 11.5.sp, maxLines = 1)
                    }
                    if (index == state.currentIndex) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = AppPrimary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { playback.removeFromQueue(index) }) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Remove",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(30.dp))
    }
}

private suspend fun extractPaletteColor(context: android.content.Context, url: String): Color? {
    return withContext(Dispatchers.IO) {
        try {
            val request = ImageRequest.Builder(context)
                .data(url)
                .allowHardware(false)
                .size(Size(120, 120))
                .build()
            val drawable = context.imageLoader.execute(request).drawable
            val bitmap = (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
            if (bitmap == null) null else {
                val palette = Palette.from(bitmap).generate()
                val rgb = palette.getDominantColor(palette.getMutedColor(0xFF2A2440.toInt()))
                Color(rgb)
            }
        } catch (_: Throwable) {
            null
        }
    }
}
