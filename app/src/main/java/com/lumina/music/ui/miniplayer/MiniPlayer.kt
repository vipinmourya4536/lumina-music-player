package com.lumina.music.ui.miniplayer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumina.music.playback.PlaybackConnection
import com.lumina.music.playback.PlaybackUiState
import com.lumina.music.ui.components.Artwork
import com.lumina.music.ui.components.GlassCard
import com.lumina.music.ui.theme.AppPrimary
import com.lumina.music.ui.theme.TextPrimary
import com.lumina.music.ui.theme.TextSecondary

/**
 * Persistent mini-player that sits above the bottom navigation.
 * Expands into Now Playing on tap; always mirrors the shared playback state.
 */
@Composable
fun MiniPlayer(
    state: PlaybackUiState,
    playback: PlaybackConnection,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onExpand)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Artwork(
                    url = state.currentTrack?.artworkUrl,
                    contentDescription = state.currentTrack?.title,
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = state.currentTrack?.title ?: "",
                        transitionSpec = { fadeIn().togetherWith(fadeOut()) },
                        label = "miniTitle"
                    ) { title ->
                        Text(
                            title.ifBlank { "Loading…" },
                            color = TextPrimary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        state.currentTrack?.artist ?: "",
                        color = TextSecondary,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = { playback.togglePlay() }, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        tint = TextPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                IconButton(onClick = { playback.next() }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        tint = TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            // Tiny progress indicator
            Box(
                Modifier
                    .padding(horizontal = 10.dp)
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0x26FFFFFF))
            ) {
                val fraction = if (state.durationMs > 0) {
                    (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
                } else 0f
                Box(
                    Modifier
                        .fillMaxWidth(fraction)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(AppPrimary)
                )
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}
