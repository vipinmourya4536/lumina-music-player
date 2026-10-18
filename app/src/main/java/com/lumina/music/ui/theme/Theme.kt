package com.lumina.music.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ---- Lumina visual language: deep black, ambient light, glass ----
val AppBg = Color(0xFF07070B)
val AppSurface = Color(0xFF10101A)
val AppSurfaceHi = Color(0xFF161622)
val GlassFill = Color(0x52121824)
val GlassFillStrong = Color(0x74141A26)
val GlassBorder = Color(0x1FFFFFFF)
val AppPrimary = Color(0xFFB49CFF)
val AppPrimaryDim = Color(0x2EB49CFF)
val AppSecondary = Color(0xFFFF7A9C)
val AppAccent = Color(0xFF5EE6A8)
val TextPrimary = Color(0xFFF4F2FF)
val TextSecondary = Color(0xFF9C97B4)
val TextTertiary = Color(0xFF6E6987)
val Danger = Color(0xFFFF6B6B)
val LikeColor = Color(0xFFFF7A9C)

val AmbientViolet = Color(0x2E8B5CF6)
val AmbientMagenta = Color(0x24D946EF)
val AmbientTeal = Color(0x1E5EE6A8)

private val LuminaColorScheme = darkColorScheme(
    primary = AppPrimary,
    onPrimary = Color(0xFF16102E),
    secondary = AppSecondary,
    onSecondary = Color(0xFF2B0F1A),
    tertiary = AppAccent,
    background = AppBg,
    onBackground = TextPrimary,
    surface = AppSurface,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFF181826),
    onSurfaceVariant = TextSecondary,
    outline = Color(0x33FFFFFF),
    outlineVariant = Color(0x22FFFFFF),
    error = Danger,
    onError = Color(0xFF2B0F0F)
)

private val LuminaShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

@Composable
fun LuminaTheme(content: @Composable () -> Unit) {
    // The app is intentionally dark-only (premium presentation).
    MaterialTheme(
        colorScheme = LuminaColorScheme,
        shapes = LuminaShapes,
        content = content
    )
}
