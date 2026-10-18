package com.lumina.music

import android.app.Application
import com.lumina.music.playback.PlaybackConnection

class LuminaApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Warm up singletons: database, preferences, provider manager, playback session.
        ServiceLocator.database(this)
        ServiceLocator.prefs(this)
        ServiceLocator.providerManager(this)
        ServiceLocator.playbackConnection(this)
    }
}
