package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.Song

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val id: String, // songId
    val title: String,
    val artist: String,
    val album: String,
    val path: String,
    val duration: Long,
    val isLocal: Boolean,
    val albumArtUri: String?,
    val playedAt: Long = System.currentTimeMillis()
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
        fun fromSong(song: Song): HistoryEntity = HistoryEntity(
            id = song.id,
            title = song.title,
            artist = song.artist,
            album = song.album,
            path = song.path,
            duration = song.duration,
            isLocal = song.isLocal,
            albumArtUri = song.albumArtUri,
            playedAt = System.currentTimeMillis()
        )
    }
}
