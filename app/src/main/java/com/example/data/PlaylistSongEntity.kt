package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.Song

@Entity(tableName = "playlist_songs")
data class PlaylistSongEntity(
    @PrimaryKey val id: String, // format: "${playlistId}_${songId}"
    val playlistId: String,
    val songId: String,
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
        id = songId,
        title = title,
        artist = artist,
        album = album,
        path = path,
        duration = duration,
        isLocal = isLocal,
        albumArtUri = albumArtUri
    )

    companion object {
        fun fromSong(playlistId: String, song: Song): PlaylistSongEntity = PlaylistSongEntity(
            id = "${playlistId}_${song.id}",
            playlistId = playlistId,
            songId = song.id,
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
