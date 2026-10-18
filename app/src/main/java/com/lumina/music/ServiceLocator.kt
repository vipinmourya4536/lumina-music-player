package com.lumina.music

import android.content.Context
import com.lumina.music.data.LibraryRepository
import com.lumina.music.data.Prefs
import com.lumina.music.data.db.LuminaDatabase
import com.lumina.music.playback.PlaybackConnection
import com.lumina.music.providers.ProviderManager

/** Tiny manual DI container — no reflection, no codegen, easy to follow. */
object ServiceLocator {

    @Volatile
    private var database: LuminaDatabase? = null

    @Volatile
    private var prefs: Prefs? = null

    @Volatile
    private var repository: LibraryRepository? = null

    @Volatile
    private var providerManager: ProviderManager? = null

    @Volatile
    private var playbackConnection: PlaybackConnection? = null

    fun database(context: Context): LuminaDatabase =
        database ?: synchronized(this) {
            database ?: LuminaDatabase.build(context.applicationContext).also { database = it }
        }

    fun prefs(context: Context): Prefs =
        prefs ?: synchronized(this) {
            prefs ?: Prefs(context.applicationContext).also { prefs = it }
        }

    fun libraryRepository(context: Context): LibraryRepository =
        repository ?: synchronized(this) {
            repository ?: LibraryRepository(
                context.applicationContext,
                database(context),
                prefs(context)
            ).also { repository = it }
        }

    fun providerManager(context: Context): ProviderManager =
        providerManager ?: synchronized(this) {
            providerManager ?: ProviderManager(context.applicationContext, prefs(context))
                .also { providerManager = it }
        }

    fun playbackConnection(context: Context): PlaybackConnection =
        playbackConnection ?: synchronized(this) {
            playbackConnection ?: PlaybackConnection(context.applicationContext).also {
                playbackConnection = it
            }
        }
}
