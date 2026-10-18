package com.lumina.music.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [TrackEntity::class, PlaylistEntity::class, PlaylistTrackEntity::class, SearchQueryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LuminaDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun searchDao(): SearchDao

    companion object {
        fun build(context: Context): LuminaDatabase =
            Room.databaseBuilder(context.applicationContext, LuminaDatabase::class.java, "lumina.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
