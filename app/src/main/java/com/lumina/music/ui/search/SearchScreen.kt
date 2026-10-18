package com.lumina.music.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lumina.music.ServiceLocator
import com.lumina.music.core.model.SearchOutcome
import com.lumina.music.core.model.Track
import com.lumina.music.data.LibraryRepository
import com.lumina.music.playback.PlaybackConnection
import com.lumina.music.providers.ProviderManager
import com.lumina.music.ui.components.EmptyState
import com.lumina.music.ui.components.GlassCard
import com.lumina.music.ui.components.LoadingBlock
import com.lumina.music.ui.components.PillChip
import com.lumina.music.ui.components.TrackActionsSheet
import com.lumina.music.ui.components.TrackRow
import com.lumina.music.ui.theme.AppPrimary
import com.lumina.music.ui.theme.GlassBorder
import com.lumina.music.ui.theme.GlassFill
import com.lumina.music.ui.theme.TextPrimary
import com.lumina.music.ui.theme.TextSecondary
import com.lumina.music.ui.theme.TextTertiary
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SearchViewModel(
    private val providers: ProviderManager,
    private val repo: LibraryRepository
) : ViewModel() {

    data class SearchUiState(
        val loading: Boolean = false,
        val tracks: List<Track> = emptyList(),
        val history: List<String> = emptyList(),
        val status: Map<String, Boolean> = emptyMap(),
        val filter: String? = null
    )

    val query = MutableStateFlow("")
    private val _state = MutableStateFlow(SearchUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.recentQueries().collect { list -> _state.update { it.copy(history = list) } }
        }
        @OptIn(FlowPreview::class)
        viewModelScope.launch {
            query.debounce(350).collectLatest { q ->
                if (q.isBlank()) {
                    _state.update { it.copy(tracks = emptyList(), loading = false, status = emptyMap(), filter = null) }
                } else {
                    performSearch(q)
                }
            }
        }
    }

    fun setQuery(value: String) = query.update { value.trimStart() }

    fun submit() {
        val q = query.value.trim()
        if (q.isBlank()) return
        viewModelScope.launch { repo.addQuery(q) }
    }

    fun setFilter(providerId: String?) = _state.update { it.copy(filter = providerId) }

    fun clearHistory() {
        viewModelScope.launch { repo.clearSearchHistory() }
    }

    private suspend fun performSearch(q: String) {
        _state.update { it.copy(loading = true) }
        val outcome: SearchOutcome = providers.searchAll(q)
        _state.update {
            it.copy(
                loading = false,
                tracks = outcome.tracks,
                status = outcome.providerStatus,
                filter = null
            )
        }
    }
}

@OptIn(FlowPreview::class)
@Composable
fun SearchScreen(playback: PlaybackConnection) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val vm: SearchViewModel = viewModel {
        SearchViewModel(ServiceLocator.providerManager(context), ServiceLocator.libraryRepository(context))
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    var actionsTrack by remember { mutableStateOf<Track?>(null) }
    val statusTop = androidx.compose.foundation.layout.WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        Modifier
            .fillMaxSize()
            .padding(top = statusTop)
    ) {
        // Large focused search field
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = query,
                onValueChange = vm::setQuery,
                placeholder = { Text("Songs, artists, albums…", color = TextTertiary, fontSize = 16.sp) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = TextTertiary) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { vm.setQuery("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = TextSecondary)
                        }
                    }
                },
                singleLine = true,
                textStyle = TextStyle(color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = GlassFill,
                    unfocusedContainerColor = GlassFill,
                    cursorColor = AppPrimary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        }

        // Provider status chips
        if (state.status.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val providersInStatus = state.status.keys.toList()
                PillChip(
                    label = "All sources",
                    selected = state.filter == null,
                    onClick = { vm.setFilter(null) }
                )
                providersInStatus.forEach { id ->
                    val ok = state.status[id] == true
                    PillChip(
                        label = if (ok) labelFor(id) else "${labelFor(id)} · offline",
                        selected = state.filter == id,
                        onClick = { vm.setFilter(if (state.filter == id) null else id) }
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))

        if (query.isBlank()) {
            // History + hints
            LazyColumn(contentPadding = PaddingValues(bottom = 170.dp)) {
                if (state.history.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Recent searches", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text(
                                "Clear",
                                color = AppPrimary,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { vm.clearHistory() }
                                    .padding(6.dp)
                            )
                        }
                    }
                    items(state.history) { q ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { vm.setQuery(q); vm.submit() }
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.History, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(q, color = TextSecondary, fontSize = 14.sp)
                        }
                    }
                }
                item {
                    GlassCard(
                        modifier = Modifier
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Column(Modifier.padding(18.dp)) {
                            Text("How playback works here", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Audius tracks play in full. Apple Music results offer official 30-second previews. " +
                                    "YouTube Music is official-link only — Lumina never extracts streams it is not allowed to. " +
                                    "Every result always tells you what it can do.",
                                color = TextSecondary,
                                fontSize = 12.5.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        } else {
            val visible = if (state.filter == null) state.tracks else state.tracks.filter { it.provider == state.filter }
            if (state.loading && visible.isEmpty()) {
                LoadingBlock()
            } else if (visible.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.Search,
                    title = "No results",
                    subtitle = "Try a different query or another source — some providers may be offline."
                )
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 170.dp)) {
                    item {
                        Text(
                            "${visible.size} results",
                            color = TextTertiary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                        )
                    }
                    items(visible, key = { it.id + it.provider }) { track ->
                        TrackRow(
                            track = track,
                            onClick = {
                                if (track.isPlayable) {
                                    val playable = visible.filter { it.isPlayable }
                                    playback.playTracks(playable, playable.indexOf(track).coerceAtLeast(0))
                                    vm.submit()
                                } else {
                                    playback.openOfficial(track)
                                }
                            },
                            trailing = {
                                IconButton(onClick = { actionsTrack = track }, modifier = Modifier.size(30.dp)) {
                                    Icon(
                                        Icons.Rounded.MoreHoriz,
                                        contentDescription = "More actions",
                                        tint = TextSecondary
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    actionsTrack?.let { track ->
        TrackActionsSheet(
            track = track,
            onDismiss = { actionsTrack = null }
        )
    }
}

private fun labelFor(id: String): String = when (id) {
    "audius" -> "Audius"
    "itunes" -> "Apple Music"
    "youtube_music" -> "YouTube Music"
    "local" -> "On device"
    else -> id
}
