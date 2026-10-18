package com.lumina.music.ui.library

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lumina.music.ServiceLocator
import com.lumina.music.core.model.Playlist
import com.lumina.music.core.model.Track
import com.lumina.music.data.LibraryRepository
import com.lumina.music.playback.PlaybackConnection
import com.lumina.music.ui.components.Artwork
import com.lumina.music.ui.components.CircleIconButton
import com.lumina.music.ui.components.EmptyState
import com.lumina.music.ui.components.GlassCard
import com.lumina.music.ui.components.PrimaryPillButton
import com.lumina.music.ui.components.PillChip
import com.lumina.music.ui.components.SectionHeader
import com.lumina.music.ui.components.TrackActionsSheet
import com.lumina.music.ui.components.TrackRow
import com.lumina.music.ui.theme.AppPrimary
import com.lumina.music.ui.theme.GlassFill
import com.lumina.music.ui.theme.TextPrimary
import com.lumina.music.ui.theme.TextSecondary
import com.lumina.music.ui.theme.TextTertiary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LibraryViewModel(private val repo: LibraryRepository) : ViewModel() {

    data class LibraryUiState(
        val tab: Int = 0,
        val localLoading: Boolean = false,
        val showCreateDialog: Boolean = false
    )

    private val _state = MutableStateFlow(LibraryUiState())
    val state = _state.asStateFlow()

    val liked = repo.likedTracks().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val history = repo.history().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val local = repo.localAndImported().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val playlists = combine(repo.playlists(), repo.playlistTrackCounts()) { list, counts ->
        list.map { it.copy(trackCount = counts[it.id] ?: 0) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectTab(index: Int) = _state.update { it.copy(tab = index) }
    fun showCreate(show: Boolean) = _state.update { it.copy(showCreateDialog = show) }

    fun scanLocal(onDone: (Int) -> Unit = {}) {
        _state.update { it.copy(localLoading = true) }
        viewModelScope.launch {
            val count = repo.scanLocalMusic()
            _state.update { it.copy(localLoading = false) }
            onDone(count)
        }
    }

    fun importUris(uris: List<android.net.Uri>, onDone: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val count = repo.importUris(uris)
            onDone(count)
        }
    }

    fun createPlaylist(name: String, onCreated: (String) -> Unit = {}) {
        viewModelScope.launch {
            val id = repo.createPlaylist(name)
            onCreated(id)
        }
    }

    fun deletePlaylist(id: String) {
        viewModelScope.launch { repo.deletePlaylist(id) }
    }

    fun renamePlaylist(id: String, name: String) {
        viewModelScope.launch { repo.renamePlaylist(id, name) }
    }
}

private val LIBRARY_TABS = listOf("Liked", "Playlists", "History", "On device")

@Composable
fun LibraryScreen(playback: PlaybackConnection, onOpenPlaylist: (String) -> Unit) {
    val context = LocalContext.current
    val vm: LibraryViewModel = viewModel { LibraryViewModel(ServiceLocator.libraryRepository(context)) }
    val state by vm.state.collectAsStateWithLifecycle()
    val liked by vm.liked.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val local by vm.local.collectAsStateWithLifecycle()
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    val statusTop = androidx.compose.foundation.layout.WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    var actionsTrack by remember { mutableStateOf<Track?>(null) }
    var renameTarget by remember { mutableStateOf<Playlist?>(null) }
    var renameName by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<Playlist?>(null) }
    var newPlaylistName by remember { mutableStateOf("") }

    val audioPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.scanLocal()
    }
    val importFiles = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) vm.importUris(uris)
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(top = statusTop)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Your library", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text(
                    "${liked.size} liked · ${playlists.size} playlists · ${local.size} device files",
                    color = TextSecondary,
                    fontSize = 12.5.sp
                )
            }
            CircleIconButton(icon = Icons.Rounded.Add, contentDescription = "New playlist", onClick = {
                vm.showCreate(true)
            }, sizeDp = 44)
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LIBRARY_TABS.forEachIndexed { index, label ->
                PillChip(label = label, selected = state.tab == index, onClick = { vm.selectTab(index) })
            }
        }
        Spacer(Modifier.height(6.dp))

        when (state.tab) {
            0 -> {
                if (liked.isEmpty()) {
                    EmptyState(Icons.Rounded.Favorite, "No liked songs yet", "Tap the heart on any track to keep it here.")
                } else {
                    LazyColumn(contentPadding = PaddingValues(bottom = 170.dp)) {
                        item {
                            Row(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                                PrimaryPillButton(label = "Play all", onClick = { playback.playTracks(liked, 0) }, icon = Icons.Rounded.PlayArrow)
                                Spacer(Modifier.width(10.dp))
                                PrimaryPillButton(label = "Shuffle", onClick = {
                                    playback.playTracks(liked.shuffled(), 0)
                                }, icon = Icons.Rounded.Shuffle)
                            }
                        }
                        itemsIndexed(liked, key = { _, t -> t.id }) { _, track ->
                            TrackRow(track, onClick = {
                                playback.playTracks(liked, liked.indexOf(track).coerceAtLeast(0))
                            }, trailing = {
                                IconButton(onClick = { actionsTrack = track }, modifier = Modifier.size(30.dp)) {
                                    Icon(Icons.Rounded.QueueMusic, contentDescription = "More", tint = TextSecondary, modifier = Modifier.size(20.dp))
                                }
                            })
                        }
                    }
                }
            }
            1 -> {
                if (playlists.isEmpty()) {
                    EmptyState(Icons.Rounded.LibraryMusic, "No playlists yet", "Create one with the + button and fill it with discoveries.")
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        itemsIndexed(playlists, key = { _, p -> p.id }) { _, playlist ->
                            Column(
                                modifier = Modifier
                                    .width(150.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { onOpenPlaylist(playlist.id) }
                                    .padding(4.dp)
                            ) {
                                GlassCard(shape = RoundedCornerShape(18.dp)) {
                                    Column(Modifier.padding(10.dp)) {
                                        Artwork(
                                            url = null,
                                            contentDescription = playlist.name,
                                            modifier = Modifier
                                                .size(110.dp)
                                                .clip(RoundedCornerShape(14.dp)),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(playlist.name, color = TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${playlist.trackCount} songs", color = TextSecondary, fontSize = 11.5.sp)
                                Row {
                                    IconButton(onClick = { renameTarget = playlist; renameName = playlist.name }, modifier = Modifier.size(30.dp)) {
                                        Icon(Icons.Rounded.Edit, contentDescription = "Rename", tint = TextTertiary, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(onClick = { deleteTarget = playlist }, modifier = Modifier.size(30.dp)) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = TextTertiary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                if (history.isEmpty()) {
                    EmptyState(Icons.Rounded.History, "Nothing played yet", "Your listening history will appear here.")
                } else {
                    LazyColumn(contentPadding = PaddingValues(bottom = 170.dp)) {
                        itemsIndexed(history, key = { _, t -> t.id + t.lastPlayedAt.hashCode() }) { _, track ->
                            TrackRow(track, onClick = {
                                if (track.isPlayable) playback.playTrack(track) else playback.openOfficial(track)
                            })
                        }
                    }
                }
            }
            else -> {
                LazyColumn(contentPadding = PaddingValues(bottom = 170.dp)) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                        ) {
                            PrimaryPillButton(label = "Scan device music", onClick = {
                                if (Build.VERSION.SDK_INT >= 33) {
                                    audioPermission.launch(Manifest.permission.READ_MEDIA_AUDIO)
                                } else {
                                    audioPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                                }
                            }, icon = Icons.Rounded.MusicNote)
                            Spacer(Modifier.width(10.dp))
                            PrimaryPillButton(label = "Import files", onClick = {
                                importFiles.launch(arrayOf("audio/*", "application/ogg"))
                            }, icon = Icons.Rounded.FolderOpen)
                        }
                    }
                    if (local.isEmpty()) {
                        item {
                            EmptyState(
                                Icons.Rounded.FolderOpen,
                                "No local music found",
                                "Scan your device or import audio files with Android's file picker."
                            )
                        }
                    } else {
                        itemsIndexed(local, key = { _, t -> t.id }) { _, track ->
                            TrackRow(track, onClick = {
                                playback.playTracks(local, local.indexOf(track).coerceAtLeast(0))
                            })
                        }
                    }
                }
            }
        }
    }

    // Create playlist dialog
    if (state.showCreateDialog) {
        AlertDialog(
            onDismissRequest = { vm.showCreate(false); newPlaylistName = "" },
            title = { Text("New playlist", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    placeholder = { Text("Playlist name", color = TextTertiary) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newPlaylistName.isNotBlank()) {
                        vm.createPlaylist(newPlaylistName.trim())
                    }
                    vm.showCreate(false); newPlaylistName = ""
                }) { Text("Create", color = AppPrimary) }
            },
            dismissButton = {
                TextButton(onClick = { vm.showCreate(false) }) { Text("Cancel", color = TextSecondary) }
            },
            containerColor = com.lumina.music.ui.theme.AppSurfaceHi
        )
    }

    // Rename dialog
    renameTarget?.let { playlist ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename playlist", color = TextPrimary) },
            text = {
                OutlinedTextField(value = renameName, onValueChange = { renameName = it }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    if (renameName.isNotBlank()) vm.renamePlaylist(playlist.id, renameName.trim())
                    renameTarget = null
                }) { Text("Save", color = AppPrimary) }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Cancel", color = TextSecondary) }
            },
            containerColor = com.lumina.music.ui.theme.AppSurfaceHi
        )
    }

    // Delete confirm
    deleteTarget?.let { playlist ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete “${playlist.name}”?", color = TextPrimary) },
            text = { Text("This removes the playlist. Songs stay in your library.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { vm.deletePlaylist(playlist.id); deleteTarget = null }) {
                    Text("Delete", color = com.lumina.music.ui.theme.Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel", color = TextSecondary) }
            },
            containerColor = com.lumina.music.ui.theme.AppSurfaceHi
        )
    }

    actionsTrack?.let { track ->
        TrackActionsSheet(track = track, onDismiss = { actionsTrack = null })
    }
}
