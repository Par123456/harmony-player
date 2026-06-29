package com.example.data

import android.content.Context
import android.provider.MediaStore
import com.example.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SongRepository(
    private val favoriteDao: FavoriteDao,
    private val playlistDao: PlaylistDao,
    private val historyDao: HistoryDao
) {

    val allFavorites: Flow<List<Song>> = favoriteDao.getAllFavorites().map { list ->
        list.map { it.toSong() }
    }

    // Playlists Operations
    val allPlaylists: Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()

    fun getSongsForPlaylist(playlistId: String): Flow<List<Song>> {
        return playlistDao.getSongsForPlaylist(playlistId).map { list ->
            list.map { it.toSong() }
        }
    }

    suspend fun createPlaylist(name: String) {
        playlistDao.insertPlaylist(PlaylistEntity(name = name))
    }

    suspend fun deletePlaylist(playlistId: String) {
        playlistDao.deletePlaylistWithSongs(playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: String, song: Song) {
        playlistDao.insertPlaylistSong(PlaylistSongEntity.fromSong(playlistId, song))
    }

    suspend fun removeSongFromPlaylist(playlistId: String, songId: String) {
        playlistDao.deletePlaylistSong(playlistId, songId)
    }

    // History Operations
    val history: Flow<List<Song>> = historyDao.getHistory().map { list ->
        list.map { it.toSong() }
    }

    suspend fun addSongToHistory(song: Song) {
        historyDao.insertHistory(HistoryEntity.fromSong(song))
    }

    suspend fun clearHistory() {
        historyDao.clearHistory()
    }

    suspend fun addFavorite(song: Song) {
        favoriteDao.insert(FavoriteEntity.fromSong(song))
    }

    suspend fun removeFavorite(songId: String) {
        favoriteDao.deleteById(songId)
    }

    suspend fun isFavorite(songId: String): Boolean {
        return favoriteDao.isFavorite(songId)
    }

    suspend fun clearFavorites() {
        favoriteDao.clearAll()
    }

    fun getDemoSongs(): List<Song> {
        return listOf(
            Song(
                id = "demo_1",
                title = "Lofi Study Session",
                artist = "Lofi Breeze",
                album = "Acoustic Horizon",
                path = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                duration = 372000, // 6 mins 12 secs
                isLocal = false,
                albumArtUri = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=400&q=80"
            ),
            Song(
                id = "demo_2",
                title = "Chill Ambient Waves",
                artist = "Ethereal Soundscapes",
                album = "Dream Cycle",
                path = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                duration = 423000, // 7 mins 3 secs
                isLocal = false,
                albumArtUri = "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?w=400&q=80"
            ),
            Song(
                id = "demo_3",
                title = "Synthwave Neon Drive",
                artist = "Retro Future",
                album = "Laser Grid",
                path = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                duration = 302000, // 5 mins 2 secs
                isLocal = false,
                albumArtUri = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=400&q=80"
            ),
            Song(
                id = "demo_4",
                title = "Acoustic Sunset Chill",
                artist = "Golden Hour Duo",
                album = "Campfire Memories",
                path = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
                duration = 318000, // 5 mins 18 secs
                isLocal = false,
                albumArtUri = "https://images.unsplash.com/photo-1510915228340-29c85a43dcfe?w=400&q=80"
            )
        )
    }

    fun scanLocalSongs(context: Context): List<Song> {
        val songList = mutableListOf<Song>()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        
        try {
            val cursor = context.contentResolver.query(
                uri,
                projection,
                selection,
                null,
                "${MediaStore.Audio.Media.TITLE} ASC"
            )
            
            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val pathCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                
                while (c.moveToNext()) {
                    val id = c.getLong(idCol).toString()
                    val title = c.getString(titleCol) ?: "Unknown Track"
                    val artist = c.getString(artistCol) ?: "Unknown Artist"
                    val album = c.getString(albumCol) ?: "Unknown Album"
                    val duration = c.getLong(durationCol)
                    val path = c.getString(pathCol) ?: ""
                    val albumId = c.getLong(albumIdCol)
                    
                    val artUri = "content://media/external/audio/albums/$albumId"
                    
                    if (path.isNotEmpty() && duration > 0) {
                        songList.add(
                            Song(
                                id = id,
                                title = title,
                                artist = artist,
                                album = album,
                                path = path,
                                duration = duration,
                                isLocal = true,
                                albumArtUri = artUri
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return songList
    }
}
