# Lumina Music Player

A native Android music player built with Kotlin, Jetpack Compose, and Media3. Multi-provider music discovery, real background playback with a full MediaSession, and home-screen widgets.

## Features

- **Multi-provider search** — Audius (full tracks), iTunes (30s previews), YouTube Music (official links), and on-device local music are queried in parallel and merged into one normalized result list
- **Real playback engine** — Media3 / ExoPlayer behind a `MediaSessionService`; playback survives leaving the app, with a genuine media notification, lock-screen controls, Bluetooth / headset button support, and audio-focus handling
- **Home-screen widgets** — three responsive sizes (small / medium / large) built with Jetpack Glance, driven by the same MediaSession as everything else
- **Library** — liked songs, playlists (create / rename / delete / reorder), recently played, search history, and imported local music, all stored locally in Room (no account needed)
- **Premium dark UI** — pure-black surfaces, ambient artwork glow, glass accents, large rounded corners, Compose Material 3

## Architecture

```
app/src/main/java/com/lumina/music/
├── core/        # Track model, provider capability flags, HTTP helper
├── providers/   # MusicProvider interface + Audius / iTunes / YouTube / Local implementations
├── playback/    # MediaSessionService (ExoPlayer), MediaController bridge, UI state mirror
├── data/        # Room database (tracks, playlists, search history), DataStore prefs, repositories
├── ui/          # Compose screens: Home, Search, Library, Now Playing, Mini-player, Settings
└── widget/      # Glance widget (responsive small / medium / large layouts)
```

Every music source implements the same `MusicProvider` interface and reports its
capabilities (`DIRECT_PLAYBACK_AVAILABLE`, `PREVIEW_ONLY`, `OFFICIAL_PLAYER_ONLY`,
`LOCAL_ONLY`), so the UI only ever offers actions a source can actually perform.
Providers are isolated: if one fails or is slow, the others still return results.

## Building

Requirements: JDK 17, Android SDK with platform 35.

```bash
./gradlew :app:assembleDebug     # debug build
./gradlew :app:assembleRelease   # release build (unsigned unless signing is configured)
```

### Release signing (optional)

Place `lumina-release.jks` in the project `keystore/` directory, or point to it
with `LUMINA_KEYSTORE`, plus `LUMINA_KEYSTORE_PASSWORD`, `LUMINA_KEY_ALIAS`,
`LUMINA_KEY_PASSWORD` — either as environment variables or Gradle properties in
`~/.gradle/gradle.properties`. Without a keystore the release build simply
produces an unsigned APK, so the project builds out of the box for everyone.

## Notes on sources

- **Audius** streams via its official public API.
- **iTunes** plays the official 30-second preview clips exposed by Apple's search API.
- **YouTube Music** never rips or proxies audio; when a track is only playable on
  YouTube the app says so and offers to open the official player.

## License

All rights reserved by the author.
