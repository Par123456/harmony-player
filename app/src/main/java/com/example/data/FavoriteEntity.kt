package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.Song

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val path: String,
    val duration: Long,
    val isLocal: Boolean,
    val albumArtUri: String?,
    val addedAt: Long = System.currentTimeMillis()
) {
    fun toSong(): Song = Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        path = path,
        duration = duration,
        isLocal = isLocal,
        albumArtUri = albumArtUri
    )

    companion object {
        fun fromSong(song: Song): FavoriteEntity = FavoriteEntity(
            id = song.id,
            title = song.title,
            artist = song.artist,
            album = song.album,
            path = song.path,
            duration = song.duration,
            isLocal = song.isLocal,
            albumArtUri = song.albumArtUri
        )
    }
}
