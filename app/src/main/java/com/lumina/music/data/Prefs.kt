package com.lumina.music.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.luminaDataStore by preferencesDataStore(name = "lumina_prefs")

@Serializable
data class QueueSnapshot(
    val ids: List<String> = emptyList(),
    val index: Int = 0,
    val positionMs: Long = 0
)

@Serializable
data class WidgetSnapshot(
    val hasTrack: Boolean = false,
    val mediaId: String = "",
    val title: String = "",
    val artist: String = "",
    val artworkUrl: String? = null,
    val isPlaying: Boolean = false,
    val liked: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val provider: String = ""
)

/** Single source for persisted app preferences + queue/widget snapshots. */
class Prefs(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val store get() = context.luminaDataStore

    private val ytKey = stringPreferencesKey("yt_api_key")
    private val queueKey = stringPreferencesKey("queue_snapshot")
    private val widgetKey = stringPreferencesKey("widget_snapshot")

    // ---- YouTube Data API key (official-playback provider) ----
    val ytApiKeyFlow: Flow<String?> = store.data.map { it[ytKey] }

    suspend fun ytApiKey(): String? = store.data.first()[ytKey]

    suspend fun setYtApiKey(value: String) {
        store.edit { it[ytKey] = value.trim() }
    }

    // ---- Queue snapshot for playback resumption ----
    suspend fun saveQueue(snapshot: QueueSnapshot) {
        store.edit { it[queueKey] = json.encodeToString(snapshot) }
    }

    suspend fun loadQueue(): QueueSnapshot? {
        val raw = store.data.first()[queueKey] ?: return null
        return runCatching { json.decodeFromString<QueueSnapshot>(raw) }.getOrNull()
    }

    // ---- Home-screen widget snapshot ----
    suspend fun saveWidgetSnapshot(snapshot: WidgetSnapshot) {
        store.edit { it[widgetKey] = json.encodeToString(snapshot) }
    }

    suspend fun widgetSnapshot(): WidgetSnapshot {
        val raw = store.data.first()[widgetKey] ?: return WidgetSnapshot()
        return runCatching { json.decodeFromString<WidgetSnapshot>(raw) }.getOrDefault(WidgetSnapshot())
    }

    suspend fun updateWidgetSnapshot(transform: (WidgetSnapshot) -> WidgetSnapshot): WidgetSnapshot {
        val next = transform(widgetSnapshot())
        saveWidgetSnapshot(next)
        return next
    }
}
