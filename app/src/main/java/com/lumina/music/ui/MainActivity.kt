package com.lumina.music.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lumina.music.ServiceLocator
import com.lumina.music.playback.PlaybackConnection
import com.lumina.music.ui.components.AmbientBackground
import com.lumina.music.ui.components.GlassCard
import com.lumina.music.ui.home.HomeScreen
import com.lumina.music.ui.library.LibraryScreen
import com.lumina.music.ui.library.PlaylistDetailScreen
import com.lumina.music.ui.miniplayer.MiniPlayer
import com.lumina.music.ui.nowplaying.NowPlayingScreen
import com.lumina.music.ui.search.SearchScreen
import com.lumina.music.ui.settings.SettingsScreen
import com.lumina.music.ui.theme.AppBg
import com.lumina.music.ui.theme.AppPrimary
import com.lumina.music.ui.theme.AppPrimaryDim
import com.lumina.music.ui.theme.LuminaTheme
import com.lumina.music.ui.theme.TextPrimary
import com.lumina.music.ui.theme.TextTertiary

class MainActivity : ComponentActivity() {

    private val connection: PlaybackConnection by lazy {
        ServiceLocator.playbackConnection(applicationContext)
    }

    private var startExpanded by mutableStateOf(false)

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Physical volume buttons control media volume while the app is in front.
        volumeControlStream = AudioManager.STREAM_MUSIC
        requestNotificationPermissionIfNeeded()
        startExpanded = connection.expandIntent(intent)
        setContent {
            LuminaTheme {
                LuminaRoot(
                    connection = connection,
                    startExpanded = startExpanded
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (connection.expandIntent(intent)) {
            startExpanded = true
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

private data class NavItem(val route: String, val label: String, val icon: ImageVector)

private val NAV_ITEMS = listOf(
    NavItem("home", "Home", Icons.Rounded.Home),
    NavItem("search", "Search", Icons.Rounded.Search),
    NavItem("library", "Library", Icons.Rounded.LibraryMusic),
    NavItem("settings", "Settings", Icons.Rounded.Settings)
)

@Composable
fun LuminaRoot(connection: PlaybackConnection, startExpanded: Boolean) {
    val navController: NavHostController = rememberNavController()
    var showNowPlaying by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(startExpanded) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val playbackState by connection.state.collectAsStateWithLifecycle()

    Box(
        Modifier
            .fillMaxSize()
            .background(AppBg)
    ) {
        AmbientBackground()

        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.fillMaxSize()
        ) {
            composable("home") {
                HomeScreen(
                    playback = connection,
                    onOpenSearch = { navController.navigate("search") { launchSingleTop = true } },
                    onOpenLiked = { navController.navigate("library") { launchSingleTop = true } }
                )
            }
            composable("search") { SearchScreen(playback = connection) }
            composable("library") {
                LibraryScreen(
                    playback = connection,
                    onOpenPlaylist = { id -> navController.navigate("playlist/$id") }
                )
            }
            composable("settings") { SettingsScreen() }
            composable("playlist/{id}") { entry ->
                PlaylistDetailScreen(
                    playlistId = entry.arguments?.getString("id") ?: "",
                    playback = connection,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        // Bottom chrome: mini player above floating navigation
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 10.dp)
        ) {
            AnimatedVisibility(
                visible = playbackState.hasTrack && !showNowPlaying,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                MiniPlayer(
                    state = playbackState,
                    playback = connection,
                    onExpand = { showNowPlaying = true }
                )
            }
            FloatingBottomNav(
                currentRoute = currentRoute,
                onSelect = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        // Now Playing overlay (expands over everything)
        AnimatedVisibility(
            visible = showNowPlaying,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(340)) + fadeIn(tween(220)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)) + fadeOut(tween(180)),
            modifier = Modifier.fillMaxSize()
        ) {
            NowPlayingScreen(
                playback = connection,
                onClose = { showNowPlaying = false }
            )
        }
    }
}

@Composable
private fun FloatingBottomNav(currentRoute: String?, onSelect: (String) -> Unit) {
    GlassCard(
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(30.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            NAV_ITEMS.forEach { item ->
                val selected = currentRoute == item.route
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(if (selected) AppPrimaryDim else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable { onSelect(item.route) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (selected) AppPrimary else TextTertiary
                    )
                    Text(
                        item.label,
                        color = if (selected) TextPrimary else TextTertiary,
                        fontSize = 10.5.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }
    }
}
