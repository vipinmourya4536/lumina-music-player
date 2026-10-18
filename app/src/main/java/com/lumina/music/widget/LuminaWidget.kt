package com.lumina.music.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import coil.imageLoader
import coil.request.ImageRequest
import com.lumina.music.R
import com.lumina.music.ServiceLocator
import com.lumina.music.core.util.Format
import com.lumina.music.data.WidgetSnapshot
import com.lumina.music.ui.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Compact, hardware-inspired glass music widget.
 * One resizable widget with three responsive layouts (small / medium / large),
 * all driving the real MediaSession through MediaController.
 */
class LuminaWidget : GlanceAppWidget() {

    companion object {
        val OPEN_PLAYER_KEY = androidx.glance.action.ActionParameters.Key<Boolean>("expand_now_playing")
    }

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(140.dp, 100.dp),
            DpSize(240.dp, 110.dp),
            DpSize(330.dp, 150.dp)
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = ServiceLocator.prefs(context).widgetSnapshot()
        val bitmap = WidgetArtLoader.load(context, snapshot.artworkUrl)
        provideContent {
            val size = LocalSize.current
            WidgetSurface(size, snapshot, bitmap)
        }
    }
}

object WidgetArtLoader {
    suspend fun load(context: Context, url: String?): Bitmap? {
        if (url.isNullOrBlank()) return null
        return withContext(Dispatchers.IO) {
            try {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .allowHardware(false)
                    .size(160, 160)
                    .build()
                val drawable = context.imageLoader.execute(request).drawable
                (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
            } catch (_: Throwable) {
                null
            }
        }
    }
}

private val SurfaceColor = ColorProvider(0xE912121D.toInt())
private val TextPrimary = ColorProvider(0xFFF4F2FF.toInt())
private val TextSecondary = ColorProvider(0xFF9C97B4.toInt())
private val Accent = ColorProvider(0xFFB49CFF.toInt())
private val ProgressTrack = ColorProvider(0x30FFFFFF.toInt())
private val ArtworkHolder = ColorProvider(0xFF232333.toInt())

@Composable
private fun WidgetSurface(size: DpSize, snapshot: WidgetSnapshot, artwork: Bitmap?) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(4.dp)
            .background(SurfaceColor)
            .cornerRadius(26.dp)
    ) {
        when {
            size.width < 240.dp -> SmallLayout(size, snapshot, artwork)
            size.width < 330.dp -> MediumLayout(size, snapshot, artwork)
            else -> LargeLayout(size, snapshot, artwork)
        }
    }
}

@Composable
private fun ArtworkView(artwork: Bitmap?, size: Int, radius: Int) {
    Box(
        modifier = GlanceModifier
            .size(size.dp)
            .background(ArtworkHolder)
            .cornerRadius(radius.dp),
        contentAlignment = Alignment.Center
    ) {
        if (artwork != null) {
            Image(
                provider = ImageProvider(artwork),
                contentDescription = null,
                modifier = GlanceModifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Image(
                provider = ImageProvider(R.drawable.ic_music_note),
                contentDescription = null,
                modifier = GlanceModifier.size((size / 2).dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun ControlButton(iconRes: Int, size: Int, circular: Boolean, action: Action) {
    Box(
        modifier = GlanceModifier
            .size(size.dp)
            .background(if (circular) ColorProvider(0x2EB49CFF.toInt()) else ColorProvider(0x00000000))
            .cornerRadius((size / 2).dp)
            .clickable(action),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(iconRes),
            contentDescription = null,
            modifier = GlanceModifier.size((size * 0.52f).dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun TrackInfo(
    snapshot: WidgetSnapshot,
    titleSize: Int,
    modifier: GlanceModifier = GlanceModifier,
    showProvider: Boolean = false
) {
    Column(modifier = modifier) {
        Text(
            text = snapshot.title.ifBlank { if (snapshot.hasTrack) "Loading…" else "Nothing playing" },
            maxLines = 1,
            style = TextStyle(
                color = TextPrimary,
                fontSize = titleSize.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = GlanceModifier.height(2.dp))
        Text(
            text = snapshot.artist.ifBlank { "Tap play to resume" },
            maxLines = 1,
            style = TextStyle(color = TextSecondary, fontSize = (titleSize - 3).sp)
        )
        if (showProvider && snapshot.provider.isNotBlank()) {
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = providerLabel(snapshot.provider),
                maxLines = 1,
                style = TextStyle(color = Accent, fontSize = 9.sp)
            )
        }
    }
}

private fun providerLabel(id: String): String = when (id) {
    "audius" -> "Audius · full track"
    "itunes" -> "Apple Music · preview"
    "youtube_music" -> "YouTube Music"
    "local" -> "On device"
    else -> ""
}

@Composable
private fun OpenPlayer(artwork: Bitmap?, size: Int, radius: Int) {
    Box(
        modifier = GlanceModifier.clickable(
            actionStartActivity<MainActivity>(
                parameters = actionParametersOf(LuminaWidget.OPEN_PLAYER_KEY to true)
            )
        )
    ) {
        ArtworkView(artwork, size, radius)
    }
}

@Composable
private fun PlayPause(snapshot: WidgetSnapshot, buttonSize: Int) {
    ControlButton(
        iconRes = if (snapshot.isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
        size = buttonSize,
        circular = true,
        action = actionRunCallback<PlayPauseAction>()
    )
}

@Composable
private fun ProgressBar(snapshot: WidgetSnapshot, availableWidth: Dp) {
    val duration = snapshot.durationMs.coerceAtLeast(0)
    val position = snapshot.positionMs.coerceIn(0, if (duration > 0) duration else snapshot.positionMs)
    val fraction = if (duration > 0) position.toFloat() / duration.toFloat() else 0f
    val filled = (availableWidth * fraction)
        .coerceAtLeast(if (fraction > 0f) 3.dp else 0.dp)
        .coerceAtMost(availableWidth)
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(4.dp)
            .background(ProgressTrack)
            .cornerRadius(2.dp)
    ) {
        Box(
            modifier = GlanceModifier
                .width(filled)
                .height(4.dp)
                .background(Accent)
                .cornerRadius(2.dp)
        ) {}
    }
}

@Composable
private fun SmallLayout(size: DpSize, snapshot: WidgetSnapshot, artwork: Bitmap?) {
    Column(modifier = GlanceModifier.fillMaxSize().padding(10.dp)) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OpenPlayer(artwork, 54, 14)
            Spacer(modifier = GlanceModifier.width(10.dp))
            TrackInfo(snapshot, titleSize = 13, modifier = GlanceModifier.defaultWeight())
            Spacer(modifier = GlanceModifier.width(6.dp))
            PlayPause(snapshot, 40)
        }
        Spacer(modifier = GlanceModifier.height(8.dp))
        ProgressBar(snapshot, size.width - 20.dp)
    }
}

@Composable
private fun MediumLayout(size: DpSize, snapshot: WidgetSnapshot, artwork: Bitmap?) {
    Column(modifier = GlanceModifier.fillMaxSize().padding(12.dp)) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OpenPlayer(artwork, 56, 15)
            Spacer(modifier = GlanceModifier.width(10.dp))
            TrackInfo(snapshot, titleSize = 14, modifier = GlanceModifier.defaultWeight())
            Spacer(modifier = GlanceModifier.width(6.dp))
            ControlButton(
                iconRes = if (snapshot.liked) R.drawable.ic_heart_filled else R.drawable.ic_heart,
                size = 30,
                circular = false,
                action = actionRunCallback<LikeAction>()
            )
        }
        Spacer(modifier = GlanceModifier.height(10.dp))
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ControlButton(R.drawable.ic_prev, 30, false, actionRunCallback<PreviousAction>())
            Spacer(modifier = GlanceModifier.width(18.dp))
            PlayPause(snapshot, 42)
            Spacer(modifier = GlanceModifier.width(18.dp))
            ControlButton(R.drawable.ic_next, 30, false, actionRunCallback<NextAction>())
        }
        Spacer(modifier = GlanceModifier.height(8.dp))
        ProgressBar(snapshot, size.width - 24.dp)
    }
}

@Composable
private fun LargeLayout(size: DpSize, snapshot: WidgetSnapshot, artwork: Bitmap?) {
    Column(modifier = GlanceModifier.fillMaxSize().padding(14.dp)) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OpenPlayer(artwork, 80, 20)
            Spacer(modifier = GlanceModifier.width(14.dp))
            TrackInfo(snapshot, titleSize = 16, modifier = GlanceModifier.defaultWeight(), showProvider = true)
            Spacer(modifier = GlanceModifier.width(6.dp))
            ControlButton(
                iconRes = if (snapshot.liked) R.drawable.ic_heart_filled else R.drawable.ic_heart,
                size = 34,
                circular = false,
                action = actionRunCallback<LikeAction>()
            )
        }
        Spacer(modifier = GlanceModifier.height(14.dp))
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ControlButton(R.drawable.ic_shuffle, 32, false, actionRunCallback<ShuffleAction>())
            Spacer(modifier = GlanceModifier.width(26.dp))
            ControlButton(R.drawable.ic_prev, 34, false, actionRunCallback<PreviousAction>())
            Spacer(modifier = GlanceModifier.width(22.dp))
            PlayPause(snapshot, 54)
            Spacer(modifier = GlanceModifier.width(22.dp))
            ControlButton(R.drawable.ic_next, 34, false, actionRunCallback<NextAction>())
        }
        Spacer(modifier = GlanceModifier.height(12.dp))
        ProgressBar(snapshot, size.width - 28.dp)
        Spacer(modifier = GlanceModifier.height(4.dp))
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = "${Format.duration(snapshot.positionMs)} / ${Format.duration(snapshot.durationMs)}",
                style = TextStyle(color = TextSecondary, fontSize = 9.sp)
            )
        }
    }
}

class LuminaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LuminaWidget()
}
