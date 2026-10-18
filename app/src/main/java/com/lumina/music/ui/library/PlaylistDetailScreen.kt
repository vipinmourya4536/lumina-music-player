package com.lumina.music.ui.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumina.music.ServiceLocator
import com.lumina.music.core.model.Playlist
import com.lumina.music.core.model.Track
import com.lumina.music.playback.PlaybackConnection
import com.lumina.music.ui.components.Artwork
import com.lumina.music.ui.components.CircleIconButton
import com.lumina.music.ui.components.EmptyState
import com.lumina.music.ui.components.GlassCard
import com.lumina.music.ui.components.PrimaryPillButton
import com.lumina.music.ui.components.TrackRow
import com.lumina.music.ui.theme.AppSurfaceHi
import com.lumina.music.ui.theme.Danger
import com.lumina.music.ui.theme.TextPrimary
import com.lumina.music.ui.theme.TextSecondary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    playback: PlaybackConnection,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repo = remember { ServiceLocator.libraryRepository(context) }
    val scope = rememberCoroutineScope()
    var playlist by remember { mutableStateOf<Playlist?>(null) }
    var tracks by remember { mutableStateOf(listOf<Track>()) }
    var menuTrack by remember { mutableStateOf<Track?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val statusTop = androidx.compose.foundation.layout.WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    LaunchedEffect(playlistId) {
        playlist = repo.playlist(playlistId)
        repo.playlistTracks(playlistId).collect { tracks = it }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(top = statusTop)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleIconButton(icon = Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", onClick = onBack, sizeDp = 44)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(playlist?.name ?: "Playlist", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${tracks.size} songs", color = TextSecondary, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.padding(horizontal = 20.dp)) {
            PrimaryPillButton(label = "Play all", onClick = { playback.playTracks(tracks, 0) }, icon = Icons.Rounded.PlayArrow)
            Spacer(Modifier.width(10.dp))
            PrimaryPillButton(label = "Shuffle", onClick = {
                playback.playTracks(tracks.shuffled(), 0)
            }, icon = Icons.Rounded.Shuffle)
        }
        Spacer(Modifier.height(6.dp))

        if (tracks.isEmpty()) {
            EmptyState(
                Icons.Rounded.PlayArrow,
                "Empty playlist",
                "Add songs from Search or the Now Playing queue menu."
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 170.dp)) {
                itemsIndexed(tracks, key = { _, t -> t.id }) { index, track ->
                    TrackRow(
                        track = track,
                        onClick = { playback.playTracks(tracks, index) },
                        trailing = {
                            Box {
                                IconButton(onClick = { menuTrack = track }, modifier = Modifier.size(30.dp)) {
                                    Icon(Icons.Rounded.MoreHoriz, contentDescription = "Actions", tint = TextSecondary)
                                }
                                DropdownMenu(expanded = menuTrack == track, onDismissRequest = { menuTrack = null }) {
                                    DropdownMenuItem(
                                        text = { Text("Move up", color = TextPrimary) },
                                        leadingIcon = { Icon(Icons.Rounded.ArrowUpward, contentDescription = null, tint = TextSecondary) },
                                        onClick = {
                                            menuTrack = null
                                            scope.launch { repo.moveTrack(playlistId, track.id, -1) }
                                        },
                                        enabled = index > 0
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Move down", color = TextPrimary) },
                                        leadingIcon = { Icon(Icons.Rounded.ArrowDownward, contentDescription = null, tint = TextSecondary) },
                                        onClick = {
                                            menuTrack = null
                                            scope.launch { repo.moveTrack(playlistId, track.id, +1) }
                                        },
                                        enabled = index < tracks.size - 1
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Remove from playlist", color = Danger) },
                                        leadingIcon = { Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = null, tint = Danger) },
                                        onClick = {
                                            menuTrack = null
                                            scope.launch { repo.removeTrackFromPlaylist(playlistId, track.id) }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete playlist", color = Danger) },
                                        leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = Danger) },
                                        onClick = { menuTrack = null; confirmDelete = true }
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete playlist?", color = TextPrimary) },
            text = { Text("The playlist and its ordering will be removed. Songs remain in your library.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch {
                        repo.deletePlaylist(playlistId)
                        onBack()
                    }
                }) { Text("Delete", color = Danger) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel", color = TextSecondary) }
            },
            containerColor = AppSurfaceHi
        )
    }
}
