package com.lumina.music.ui.home

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
import androidx.compose.foundation.layout.calculateEndPadding
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lumina.music.ServiceLocator
import com.lumina.music.core.model.DiscoverSection
import com.lumina.music.core.model.Track
import com.lumina.music.data.LibraryRepository
import com.lumina.music.playback.PlaybackConnection
import com.lumina.music.providers.ProviderManager
import com.lumina.music.ui.components.Artwork
import com.lumina.music.ui.components.CircleIconButton
import com.lumina.music.ui.components.GlassCard
import com.lumina.music.ui.components.LoadingBlock
import com.lumina.music.ui.components.OfflineBanner
import com.lumina.music.ui.components.PillChip
import com.lumina.music.ui.components.PrimaryPillButton
import com.lumina.music.ui.components.SectionHeader
import com.lumina.music.ui.theme.AppPrimary
import com.lumina.music.ui.theme.GlassBorder
import com.lumina.music.ui.theme.GlassFill
import com.lumina.music.ui.theme.TextPrimary
import com.lumina.music.ui.theme.TextSecondary
import com.lumina.music.ui.theme.TextTertiary
import com.lumina.music.core.util.Format
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val providers: ProviderManager,
    private val repo: LibraryRepository
) : ViewModel() {

    data class HomeUiState(
        val loading: Boolean = true,
        val featured: Track? = null,
        val trending: List<Track> = emptyList(),
        val topCharts: List<Track> = emptyList(),
        val underground: List<Track> = emptyList(),
        val recent: List<Track> = emptyList(),
        val offline: Boolean = false,
        val chipIndex: Int = 0
    )

    private val _state = MutableStateFlow(HomeUiState())
    val state = _state.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            repo.history().collect { list ->
                _state.update { it.copy(recent = list.take(14)) }
            }
        }
    }

    fun selectChip(index: Int) = _state.update { it.copy(chipIndex = index) }

    fun refresh() = load()

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, offline = false) }
            val (trending, underground, top) = coroutineScope {
                val t = async { providers.discover(DiscoverSection.TRENDING) }
                val u = async { providers.discover(DiscoverSection.NEW_RELEASES) }
                val c = async { providers.discover(DiscoverSection.TOP_CHARTS) }
                Triple(t.await(), u.await(), c.await())
            }
            val offline = trending.isEmpty() && underground.isEmpty() && top.isEmpty()
            _state.update {
                it.copy(
                    loading = false,
                    trending = trending,
                    underground = underground,
                    topCharts = top,
                    featured = trending.firstOrNull(),
                    offline = offline
                )
            }
        }
    }
}

private val HOME_CHIPS = listOf("All", "Trending", "New & underground", "Top charts")

@Composable
fun HomeScreen(
    playback: PlaybackConnection,
    onOpenSearch: () -> Unit,
    onOpenLiked: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val vm: HomeViewModel = viewModel {
        HomeViewModel(ServiceLocator.providerManager(context), ServiceLocator.libraryRepository(context))
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val statusTop = androidx.compose.foundation.layout.WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = statusTop + 10.dp, bottom = 170.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        com.lumina.music.core.util.Format.greeting(),
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Text(
                        "Find your next favorite song",
                        color = TextPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(10.dp))
                CircleIconButton(icon = Icons.Rounded.Favorite, contentDescription = "Liked music", onClick = onOpenLiked, sizeDp = 46)
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(GlassFill)
                        .border(1.dp, GlassBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Person, contentDescription = "Profile", tint = AppPrimary, modifier = Modifier.size(22.dp))
                }
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(GlassFill)
                        .border(1.dp, GlassBorder, RoundedCornerShape(50))
                        .clickable(onClick = onOpenSearch)
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Search songs, artists, albums…", color = TextTertiary, fontSize = 14.sp)
                }
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HOME_CHIPS.forEachIndexed { index, label ->
                    PillChip(label = label, selected = state.chipIndex == index, onClick = { vm.selectChip(index) })
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
        item { OfflineBanner(visible = state.offline && state.recent.isNotEmpty()) }

        if (state.loading && state.trending.isEmpty()) {
            item { LoadingBlock() }
        }

        if (state.featured != null && (state.chipIndex == 0 || state.chipIndex == 1)) {
            item {
                FeaturedCard(
                    track = state.featured!!,
                    onPlay = { playback.playTracks(state.trending, 0) },
                    onLike = { playback.toggleLike(state.featured!!) },
                    onOpen = { playback.openOfficial(state.featured!!) }
                )
            }
        }

        if (state.trending.isNotEmpty() && (state.chipIndex == 0 || state.chipIndex == 1)) {
            item { SectionHeader("Trending now", Modifier.padding(top = 14.dp)) }
            item {
                TrackCarousel(state.trending) { index -> playback.playTracks(state.trending, index) }
            }
        }

        if (state.underground.isNotEmpty() && (state.chipIndex == 0 || state.chipIndex == 2)) {
            item { SectionHeader("New & underground", Modifier.padding(top = 14.dp)) }
            item {
                TrackCarousel(state.underground) { index -> playback.playTracks(state.underground, index) }
            }
        }

        if (state.topCharts.isNotEmpty() && (state.chipIndex == 0 || state.chipIndex == 3)) {
            item { SectionHeader("Top charts", Modifier.padding(top = 14.dp)) }
            item {
                TrackCarousel(state.topCharts) { index -> playback.playTracks(state.topCharts, index) }
            }
        }

        if (state.recent.isNotEmpty()) {
            item { SectionHeader("Recently played", Modifier.padding(top = 14.dp)) }
            item {
                TrackCarousel(state.recent) { index -> playback.playTracks(state.recent, index) }
            }
        }
    }
}

@Composable
private fun FeaturedCard(
    track: Track,
    onPlay: () -> Unit,
    onLike: () -> Unit,
    onOpen: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(28.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(210.dp)
        ) {
            Artwork(
                url = track.artworkUrl,
                contentDescription = track.title,
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(28.dp)
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.55f to Color(0x6607070B),
                            1f to Color(0xE607070B)
                        )
                    )
            )
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(18.dp)
            ) {
                Text("FEATURED DISCOVERY", color = AppPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    track.title,
                    color = TextPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    track.artist,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PrimaryPillButton(label = "Play", icon = Icons.Rounded.PlayArrow, onClick = onPlay)
                    Spacer(Modifier.width(10.dp))
                    CircleIconButton(
                        icon = Icons.Rounded.Favorite,
                        contentDescription = "Like",
                        onClick = onLike,
                        sizeDp = 40,
                        tint = com.lumina.music.ui.theme.LikeColor
                    )
                    Spacer(Modifier.width(10.dp))
                    CircleIconButton(
                        icon = Icons.Rounded.OpenInNew,
                        contentDescription = "Open source",
                        onClick = onOpen,
                        sizeDp = 40,
                        tint = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackCarousel(tracks: List<Track>, onPlayAt: (Int) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        itemsIndexed(tracks, key = { i, t -> "${t.id}_$i" }) { index, track ->
            Column(
                modifier = Modifier
                    .width(156.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onPlayAt(index) }
                    .padding(4.dp)
            ) {
                Artwork(
                    url = track.artworkUrl,
                    contentDescription = track.title,
                    modifier = Modifier
                        .size(148.dp)
                        .clip(RoundedCornerShape(18.dp))
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    track.title,
                    color = TextPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    track.artist,
                    color = TextSecondary,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (track.durationMs > 0) {
                    Text(Format.duration(track.durationMs), color = TextTertiary, fontSize = 10.sp)
                }
            }
        }
    }
}
