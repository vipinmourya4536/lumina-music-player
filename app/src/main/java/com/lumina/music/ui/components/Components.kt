package com.lumina.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lumina.music.core.model.PlaybackType
import com.lumina.music.core.model.Track
import com.lumina.music.core.model.providerCapabilityLabel
import com.lumina.music.core.model.providerDisplayName
import com.lumina.music.core.util.Format
import com.lumina.music.ui.theme.AppPrimary
import com.lumina.music.ui.theme.AppPrimaryDim
import com.lumina.music.ui.theme.AmbientMagenta
import com.lumina.music.ui.theme.AmbientTeal
import com.lumina.music.ui.theme.AmbientViolet
import com.lumina.music.ui.theme.GlassBorder
import com.lumina.music.ui.theme.GlassFill
import com.lumina.music.ui.theme.TextPrimary
import com.lumina.music.ui.theme.TextSecondary
import com.lumina.music.ui.theme.TextTertiary

/** Soft ambient light blobs used behind premium areas. Cheap: gradients only, no blur. */
@Composable
fun AmbientBackground(modifier: Modifier = Modifier, withTeal: Boolean = false) {
    Box(
        modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(AmbientViolet, Color.Transparent),
                    radius = 900f
                )
            )
    )
    Box(
        modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(AmbientMagenta, Color.Transparent),
                    radius = 1100f
                )
            )
    )
    if (withTeal) {
        Box(
            modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(AmbientTeal, Color.Transparent),
                        radius = 800f
                    )
                )
        )
    }
}

/** Translucent glass surface with a hairline border. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    content: @Composable () -> Unit = {}
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = GlassFill,
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
        content = content
    )
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        modifier = modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        color = TextPrimary,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun Artwork(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(14.dp)
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color(0xFF1C1C2A)),
        contentAlignment = Alignment.Center
    ) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
fun PillChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) AppPrimaryDim else GlassFill)
            .border(1.dp, if (selected) AppPrimary.copy(alpha = 0.5f) else GlassBorder, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = if (selected) TextPrimary else TextSecondary,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

@Composable
fun ProviderBadge(provider: String, modifier: Modifier = Modifier) {
    Text(
        text = "${providerDisplayName(provider)} · ${providerCapabilityLabel(provider)}",
        modifier = modifier,
        color = TextTertiary,
        fontSize = 11.sp
    )
}

@Composable
fun TrackRow(
    track: Track,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (highlight) AppPrimaryDim else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Artwork(url = track.artworkUrl, contentDescription = track.title, modifier = Modifier.size(52.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = track.artist,
                    color = TextSecondary,
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(8.dp))
                ProviderBadge(track.provider)
            }
        }
        Spacer(Modifier.width(8.dp))
        if (track.durationMs > 0) {
            Text(
                text = Format.duration(track.durationMs),
                color = TextTertiary,
                fontSize = 12.sp
            )
            Spacer(Modifier.width(8.dp))
        }
        when {
            trailing != null -> trailing()
            track.playbackType == PlaybackType.OFFICIAL_LINK -> {}
            else -> Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = "Play",
                tint = TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(44.dp))
        Spacer(Modifier.height(12.dp))
        Text(title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(
            subtitle,
            color = TextSecondary,
            fontSize = 13.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun LoadingBlock(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 28.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            color = AppPrimary,
            modifier = Modifier.size(26.dp),
            strokeWidth = 2.5.dp
        )
    }
}

@Composable
fun OfflineBanner(visible: Boolean, modifier: Modifier = Modifier) {
    if (!visible) return
    GlassCard(modifier = modifier.padding(horizontal = 20.dp, vertical = 6.dp), shape = RoundedCornerShape(18.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.CloudOff, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                "Online sources unreachable — your library and local music keep working.",
                color = TextSecondary,
                fontSize = 12.5.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color = GlassFill,
    tint: Color = TextPrimary,
    sizeDp: Int = 48
) {
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(container)
            .border(1.dp, GlassBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size((sizeDp * 0.5f).dp))
    }
}

@Composable
fun PrimaryPillButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(AppPrimary)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Color(0xFF16102E), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(label, color = Color(0xFF16102E), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
fun SubtleText(text: String, modifier: Modifier = Modifier, fontSize: Int = 12) {
    Text(
        text = text,
        modifier = modifier,
        color = TextTertiary,
        fontSize = fontSize.sp,
        style = MaterialTheme.typography.bodySmall
    )
}
