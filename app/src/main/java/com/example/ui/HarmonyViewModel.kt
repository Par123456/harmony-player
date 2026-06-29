package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.SongRepository
import com.example.model.HarmonyTab
import com.example.model.RepeatMode
import com.example.model.Song
import com.example.service.AudioPlayerManager
import com.example.service.UpdateManager
import com.example.service.UpdateState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HarmonyViewModel(
    private val repository: SongRepository,
    private val playerManager: AudioPlayerManager,
    private val updateManager: UpdateManager
) : ViewModel() {

    // UI navigation and UI states
    private val _activeTab = MutableStateFlow(HarmonyTab.LIBRARY)
    val activeTab: StateFlow<HarmonyTab> = _activeTab.asStateFlow()

    private val _nowPlayingExpanded = MutableStateFlow(false)
    val nowPlayingExpanded: StateFlow<Boolean> = _nowPlayingExpanded.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Loaded tracks list (Demo + Scanned)
    private val _allSongs = MutableStateFlow<List<Song>>(emptyList())
    val allSongs: StateFlow<List<Song>> = _allSongs.asStateFlow()

    // Filtered list based on Search
    val filteredSongs: StateFlow<List<Song>> = combine(_allSongs, _searchQuery) { songs, query ->
        if (query.isBlank()) {
            songs
        } else {
            songs.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true) ||
                it.album.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Player state delegations
    val currentSong: StateFlow<Song?> = playerManager.currentSong
    val isPlaying: StateFlow<Boolean> = playerManager.isPlaying
    val isLoading: StateFlow<Boolean> = playerManager.isLoading
    val progress: StateFlow<Long> = playerManager.progress
    val playbackSpeed: StateFlow<Float> = playerManager.playbackSpeed
    val queue: StateFlow<List<Song>> = playerManager.queue
    val shuffleEnabled: StateFlow<Boolean> = playerManager.shuffleEnabled
    val repeatMode: StateFlow<RepeatMode> = playerManager.repeatMode

    // Sleep Timer delegations
    val sleepTimerActive: StateFlow<Boolean> = playerManager.sleepTimerActive
    val sleepTimerRemaining: StateFlow<Long> = playerManager.sleepTimerRemaining
    val sleepTimerTotal: StateFlow<Long> = playerManager.sleepTimerTotal

    // Equalizer delegations
    val eqEnabled: StateFlow<Boolean> = playerManager.eqEnabled
    val eqPreset: StateFlow<String> = playerManager.eqPreset
    val eqBands: StateFlow<List<Int>> = playerManager.eqBands

    // Favorites reactive flow
    val favorites: StateFlow<List<Song>> = repository.allFavorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Playlists reactive flow
    val playlists: StateFlow<List<com.example.data.PlaylistEntity>> = repository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // History reactive flow
    val history: StateFlow<List<Song>> = repository.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Update state delegations
    val updateState: StateFlow<UpdateState> = updateManager.updateState

    init {
        // Load default demo tracks
        _allSongs.value = repository.getDemoSongs()
        // Synchronize default player queue with these tracks so that clicking next/previous is functional
        playerManager.setQueue(repository.getDemoSongs())

        // Auto-add playing songs to Listening History
        viewModelScope.launch {
            currentSong.collect { song ->
                if (song != null) {
                    repository.addSongToHistory(song)
                }
            }
        }
    }

    // Playlist CRUD Methods
    fun createPlaylist(name: String) {
        viewModelScope.launch {
            repository.createPlaylist(name)
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
        }
    }

    fun addSongToPlaylist(playlistId: String, song: Song) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, song)
        }
    }

    fun removeSongFromPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(playlistId, songId)
        }
    }

    fun getSongsForPlaylist(playlistId: String): Flow<List<Song>> {
        return repository.getSongsForPlaylist(playlistId)
    }

    // History Methods
    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun setTab(tab: HarmonyTab) {
        _activeTab.value = tab
    }

    fun setNowPlayingExpanded(expanded: Boolean) {
        _nowPlayingExpanded.value = expanded
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Media Actions
    fun selectSong(song: Song) {
        // When user plays a song from Library, make sure player has current filtered list as queue
        val currentList = _allSongs.value
        playerManager.setQueue(currentList)
        playerManager.playSong(song)
    }

    fun togglePlayPause() {
        playerManager.togglePlayPause()
    }

    fun seekTo(positionMs: Long) {
        playerManager.seekTo(positionMs)
    }

    fun setPlaybackSpeed(speed: Float) {
        playerManager.setSpeed(speed)
    }

    fun playNext() {
        playerManager.playNext()
    }

    fun playPrevious() {
        playerManager.playPrevious()
    }

    // Queue Actions
    fun addToQueue(song: Song) {
        playerManager.addToQueue(song)
    }

    fun removeFromQueue(index: Int) {
        playerManager.removeFromQueue(index)
    }

    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        playerManager.reorderQueue(fromIndex, toIndex)
    }

    fun toggleShuffle() {
        playerManager.toggleShuffle()
    }

    fun toggleRepeatMode() {
        playerManager.toggleRepeatMode()
    }

    fun clearQueue() {
        playerManager.clearQueue()
    }

    // Favorites Actions
    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            val isFav = repository.isFavorite(song.id)
            if (isFav) {
                repository.removeFavorite(song.id)
            } else {
                repository.addFavorite(song)
            }
        }
    }

    fun isSongFavorite(songId: String): Flow<Boolean> {
        return repository.allFavorites.map { list -> list.any { it.id == songId } }
    }

    fun clearAllFavorites() {
        viewModelScope.launch {
            repository.clearFavorites()
        }
    }

    // Sleep Timer Actions
    fun startSleepTimer(minutes: Long) {
        playerManager.startSleepTimer(minutes)
    }

    fun stopSleepTimer() {
        playerManager.stopSleepTimer()
    }

    // Equalizer Actions
    fun setEqualizerEnabled(enabled: Boolean) {
        playerManager.setEqualizerEnabled(enabled)
    }

    fun setEqPreset(presetName: String) {
        playerManager.setEqPreset(presetName)
    }

    fun setBandGain(bandIndex: Int, value: Int) {
        playerManager.setBandGain(bandIndex, value)
    }

    // Auto-update Actions
    fun checkForUpdates() {
        updateManager.checkForUpdates()
    }

    fun startUpdateDownload(url: String) {
        updateManager.startDownload(url)
    }

    fun installUpdate(file: java.io.File) {
        updateManager.installApk(file)
    }

    fun resetUpdateState() {
        updateManager.resetState()
    }

    // Scan Device for Songs
    fun scanDeviceMusic(context: Context) {
        viewModelScope.launch {
            val scanned = repository.scanLocalSongs(context)
            if (scanned.isNotEmpty()) {
                // Merge demo songs with scanned local songs
                val combined = repository.getDemoSongs() + scanned
                _allSongs.value = combined
                playerManager.setQueue(combined)
            }
        }
    }

    // Provider Factory for standard Compose instantiation
    class Factory(
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = com.example.data.HarmonyDatabase.getDatabase(context)
            val repo = SongRepository(db.favoriteDao(), db.playlistDao(), db.historyDao())
            val player = AudioPlayerManager.getInstance(context)
            val updater = UpdateManager.getInstance(context)
            return HarmonyViewModel(repo, player, updater) as T
        }
    }
}
