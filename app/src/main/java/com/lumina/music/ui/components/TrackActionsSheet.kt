package com.lumina.music.ui.components

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
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumina.music.ServiceLocator
import com.lumina.music.core.model.PlaybackType
import com.lumina.music.core.model.Playlist
import com.lumina.music.core.model.Track
import com.lumina.music.playback.PlaybackConnection
import com.lumina.music.ui.theme.AppPrimary
import com.lumina.music.ui.theme.AppSurface
import com.lumina.music.ui.theme.GlassFill
import com.lumina.music.ui.theme.LikeColor
import com.lumina.music.ui.theme.TextPrimary
import com.lumina.music.ui.theme.TextSecondary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Unified track actions: play, queue, like, playlist, official link.
 * Never shows "Play" for OFFICIAL_LINK content — it offers the source instead.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackActionsSheet(track: Track, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { ServiceLocator.libraryRepository(context) }
    val playback = remember { ServiceLocator.playbackConnection(context) }
    val scope = rememberCoroutineScope()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var playlists by remember { mutableStateOf(listOf<Playlist>()) }
    var pickingPlaylist by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var liked by remember { mutableStateOf(track.liked) }

    LaunchedEffect(track.id) {
        liked = repo.getTrack(track.id)?.liked ?: track.liked
        playlists = repo.playlists().first()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Artwork(url = track.artworkUrl, contentDescription = track.title, modifier = Modifier.size(48.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(track.title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.artist, color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    ProviderBadge(track.provider)
                }
            }
            Spacer(Modifier.height(14.dp))

            if (track.isPlayable) {
                ActionRow(Icons.Rounded.PlayArrow, "Play now") {
                    playback.playTrack(track)
                    onDismiss()
                }
            }
            if (track.isPlayable) {
                ActionRow(Icons.Rounded.QueueMusic, "Play next") {
                    playback.playNext(track)
                    onDismiss()
                }
            }
            ActionRow(Icons.Rounded.PlaylistAdd, "Add to queue") {
                playback.addToQueue(track)
                onDismiss()
            }
            ActionRow(
                icon = if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                label = if (liked) "Remove from liked" else "Add to liked",
                tint = if (liked) LikeColor else TextPrimary
            ) {
                scope.launch { liked = repo.toggleLikeById(track.id) ?: liked }
            }
            ActionRow(Icons.Rounded.PlaylistAdd, if (pickingPlaylist) "Hide playlists" else "Add to playlist") {
                pickingPlaylist = !pickingPlaylist
            }

            if (pickingPlaylist) {
                playlists.forEach { playlist ->
                    ActionRow(Icons.Rounded.QueueMusic, "Add to “${playlist.name}”") {
                        scope.launch {
                            repo.addTracksToPlaylist(playlist.id, listOf(track))
                        }
                        onDismiss()
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        placeholder = { Text("New playlist name", color = TextSecondary, fontSize = 13.sp) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = GlassFill,
                            unfocusedContainerColor = GlassFill,
                            cursorColor = AppPrimary,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 4.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                if (newPlaylistName.isNotBlank()) {
                                    scope.launch {
                                        val id = repo.createPlaylist(newPlaylistName.trim())
                                        repo.addTracksToPlaylist(id, listOf(track))
                                        newPlaylistName = ""
                                        onDismiss()
                                    }
                                }
                            }
                            .padding(12.dp)
                    ) {
                        Text("Create", color = AppPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }

            if (track.playbackType == PlaybackType.OFFICIAL_LINK || track.officialUrl != null) {
                ActionRow(Icons.Rounded.OpenInNew, "Open official player") {
                    playback.openOfficial(track)
                    onDismiss()
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    label: String,
    tint: Color = TextPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, color = tint, fontSize = 14.5.sp, fontWeight = FontWeight.Medium)
    }
}
