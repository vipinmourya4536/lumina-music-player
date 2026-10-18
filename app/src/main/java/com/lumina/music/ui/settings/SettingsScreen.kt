package com.lumina.music.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumina.music.ServiceLocator
import com.lumina.music.ui.components.GlassCard
import com.lumina.music.ui.theme.AppPrimary
import com.lumina.music.ui.theme.AppSurface
import com.lumina.music.ui.theme.GlassFill
import com.lumina.music.ui.theme.TextPrimary
import com.lumina.music.ui.theme.TextSecondary
import com.lumina.music.ui.theme.TextTertiary
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefs = remember { ServiceLocator.prefs(context) }
    val repo = remember { ServiceLocator.libraryRepository(context) }
    val providers = remember { ServiceLocator.providerManager(context) }
    val scope = rememberCoroutineScope()
    val statusTop = androidx.compose.foundation.layout.WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    var ytKey by remember { mutableStateOf("") }
    var ytKeySaved by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        ytKey = prefs.ytApiKey() ?: ""
        ytKeySaved = ytKey.isNotBlank()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = statusTop + 8.dp, bottom = 170.dp)
    ) {
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text("Settings", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("Providers, storage and about", color = TextSecondary, fontSize = 12.5.sp)
            }
        }

        item {
            GlassCard(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = AppPrimary, modifier = Modifier.width(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Music providers", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))
                    ProviderRow("Audius", "Full-length streams · no key needed", active = true)
                    ProviderRow("Apple Music", "Official metadata, artwork & 30s previews", active = true)
                    ProviderRow(
                        "YouTube Music",
                        "Official-link playback via your own YouTube Data API key",
                        active = ytKeySaved
                    )
                    ProviderRow("On device", "MediaStore music + imported files", active = true)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = ytKey,
                        onValueChange = { ytKey = it; ytKeySaved = false },
                        placeholder = { Text("YouTube Data API v3 key", color = TextTertiary, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Rounded.Key, contentDescription = null, tint = TextTertiary, modifier = Modifier.width(18.dp)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = GlassFill,
                            unfocusedContainerColor = GlassFill,
                            cursorColor = AppPrimary,
                            focusedBorderColor = AppPrimary.copy(alpha = 0.4f),
                            unfocusedBorderColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = {
                            scope.launch {
                                prefs.setYtApiKey(ytKey)
                                ytKeySaved = ytKey.isNotBlank()
                            }
                        }) {
                            Icon(Icons.Rounded.Save, contentDescription = null, tint = AppPrimary, modifier = Modifier.width(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Save key", color = AppPrimary, fontSize = 13.sp)
                        }
                        if (ytKeySaved) {
                            Text("Key active — YouTube results will appear in search", color = TextTertiary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        item {
            GlassCard(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("Storage", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    SettingsAction(
                        icon = Icons.Rounded.DeleteSweep,
                        title = "Clear cached metadata",
                        subtitle = "Removes cached online results. Likes, playlists and history stay."
                    ) {
                        scope.launch { repo.purgeCachedMetadata() }
                    }
                    SettingsAction(
                        icon = Icons.Rounded.DeleteSweep,
                        title = "Clear search history",
                        subtitle = "Removes recent search queries."
                    ) {
                        scope.launch { repo.clearSearchHistory() }
                    }
                }
            }
        }

        item {
            GlassCard(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Policy, contentDescription = null, tint = AppPrimary, modifier = Modifier.width(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Playback policy", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Lumina only plays sources it is allowed to play. Audius streams are legal, key-free and " +
                            "artist-approved. Apple Music results play the official 30-second previews that Apple " +
                            "publishes for exactly this purpose. YouTube Music results open in the official player — " +
                            "no stream extraction ever happens inside this app.",
                        color = TextSecondary,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        item {
            GlassCard(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("About Lumina", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Version 1.0.0 · Native Android · Kotlin + Jetpack Compose + Media3\n\n" +
                            "Lumina's provider architecture is inspired by the plugin-first design of the open-source " +
                            "BloomeeTunes project (GPL-2.0). Lumina is an independent, original implementation: no " +
                            "source code was copied, and all providers use official or explicitly licensed interfaces.",
                        color = TextSecondary,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderRow(name: String, description: String, active: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(description, color = TextSecondary, fontSize = 12.sp)
        }
        Text(
            if (active) "ACTIVE" else "NEEDS KEY",
            color = if (active) com.lumina.music.ui.theme.AppAccent else TextTertiary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(
                    if (active) com.lumina.music.ui.theme.AppAccent.copy(alpha = 0.15f) else Color.Transparent
                )
                .padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun SettingsAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.width(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = TextSecondary, fontSize = 11.5.sp)
        }
    }
}
