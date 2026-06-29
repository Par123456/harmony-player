package com.example.service

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.audiofx.Equalizer
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.model.RepeatMode
import com.example.model.Song
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException

class AudioPlayerManager private constructor(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var equalizer: Equalizer? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null

    // Player states
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _progress = MutableStateFlow(0L)
    val progress: StateFlow<Long> = _progress.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    // Queue states
    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    // Original unshuffled queue to restore when shuffle is turned off
    private var originalQueue = listOf<Song>()

    // Sleep Timer States
    private val _sleepTimerActive = MutableStateFlow(false)
    val sleepTimerActive: StateFlow<Boolean> = _sleepTimerActive.asStateFlow()

    private val _sleepTimerRemaining = MutableStateFlow(0L) // in seconds
    val sleepTimerRemaining: StateFlow<Long> = _sleepTimerRemaining.asStateFlow()

    private val _sleepTimerTotal = MutableStateFlow(0L) // in seconds
    val sleepTimerTotal: StateFlow<Long> = _sleepTimerTotal.asStateFlow()

    // Equalizer States
    private val _eqEnabled = MutableStateFlow(true)
    val eqEnabled: StateFlow<Boolean> = _eqEnabled.asStateFlow()

    private val _eqPreset = MutableStateFlow("Flat")
    val eqPreset: StateFlow<String> = _eqPreset.asStateFlow()

    // We support 5 frequency bands: 60Hz, 230Hz, 910Hz, 4kHz, 14kHz
    private val _eqBands = MutableStateFlow(listOf(0, 0, 0, 0, 0)) // Range -12 to +12 dB
    val eqBands: StateFlow<List<Int>> = _eqBands.asStateFlow()

    init {
        initializeMediaPlayer()
    }

    private fun initializeMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setOnCompletionListener {
                    handleSongCompletion()
                }
                setOnPreparedListener {
                    _isLoading.value = false
                    startPlayback()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("AudioPlayerManager", "MediaPlayer error: what=$what, extra=$extra")
                    _isLoading.value = false
                    _isPlaying.value = false
                    false
                }
            }
        }
    }

    private fun handleSongCompletion() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                // Repeat current song
                _currentSong.value?.let { playSong(it) }
            }
            RepeatMode.ALL -> {
                playNext()
            }
            RepeatMode.OFF -> {
                if (hasNext()) {
                    playNext()
                } else {
                    _isPlaying.value = false
                    _progress.value = 0L
                    stopProgressTracker()
                }
            }
        }
    }

    fun playSong(song: Song) {
        _isLoading.value = true
        _currentSong.value = song
        _progress.value = 0L

        // Add song to queue if it isn't already there
        if (!_queue.value.any { it.id == song.id }) {
            addToQueue(song)
        }

        initializeMediaPlayer()

        try {
            mediaPlayer?.apply {
                reset()
                setDataSource(song.path)
                prepareAsync()
            }
        } catch (e: IOException) {
            Log.e("AudioPlayerManager", "Failed to play song: ${song.title}", e)
            _isLoading.value = false
            _isPlaying.value = false
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer ?: return
        if (player.isPlaying) {
            pausePlayback()
        } else {
            if (_currentSong.value != null) {
                startPlayback()
            } else if (_queue.value.isNotEmpty()) {
                playSong(_queue.value.first())
            }
        }
    }

    private fun startPlayback() {
        mediaPlayer?.let { player ->
            try {
                player.start()
                // Apply playback speed
                applySpeed(_playbackSpeed.value)
                // Bind real hardware Equalizer if supported
                initEqualizer()
                applyEqualizerSettings()
                
                _isPlaying.value = true
                startProgressTracker()
            } catch (e: Exception) {
                Log.e("AudioPlayerManager", "Error starting playback", e)
            }
        }
    }

    private fun pausePlayback() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            }
            _isPlaying.value = false
            stopProgressTracker()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let { player ->
            player.seekTo(positionMs.toInt())
            _progress.value = positionMs
        }
    }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
        applySpeed(speed)
    }

    private fun applySpeed(speed: Float) {
        mediaPlayer?.let { player ->
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                try {
                    if (player.isPlaying || _isPlaying.value) {
                        player.playbackParams = player.playbackParams.setSpeed(speed)
                    }
                } catch (e: Exception) {
                    Log.e("AudioPlayerManager", "Could not set playback speed", e)
                }
            }
        }
    }

    // Queue Controls
    fun setQueue(songs: List<Song>) {
        originalQueue = songs
        if (_shuffleEnabled.value) {
            _queue.value = songs.shuffled()
        } else {
            _queue.value = songs
        }
    }

    fun addToQueue(song: Song) {
        if (!_queue.value.any { it.id == song.id }) {
            _queue.value = _queue.value + song
            originalQueue = originalQueue + song
        }
    }

    fun clearQueue() {
        pausePlayback()
        _queue.value = emptyList()
        originalQueue = emptyList()
        _currentSong.value = null
        _progress.value = 0L
    }

    fun removeFromQueue(index: Int) {
        val currentQueue = _queue.value.toMutableList()
        if (index in currentQueue.indices) {
            val removedSong = currentQueue.removeAt(index)
            _queue.value = currentQueue

            // Adjust original queue
            val origList = originalQueue.toMutableList()
            origList.remove(removedSong)
            originalQueue = origList

            // If the currently playing song is removed, play next
            if (_currentSong.value?.id == removedSong.id) {
                if (currentQueue.isNotEmpty()) {
                    val nextIndex = index.coerceAtMost(currentQueue.size - 1)
                    playSong(currentQueue[nextIndex])
                } else {
                    clearQueue()
                }
            }
        }
    }

    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        val currentQueue = _queue.value.toMutableList()
        if (fromIndex in currentQueue.indices && toIndex in currentQueue.indices) {
            val item = currentQueue.removeAt(fromIndex)
            currentQueue.add(toIndex, item)
            _queue.value = currentQueue
        }
    }

    fun toggleShuffle() {
        val enabled = !_shuffleEnabled.value
        _shuffleEnabled.value = enabled
        val current = _currentSong.value
        
        if (enabled) {
            // Keep current song first, shuffle the rest
            val currentList = _queue.value.toMutableList()
            current?.let { currentList.remove(it) }
            val shuffled = currentList.shuffled()
            _queue.value = if (current != null) listOf(current) + shuffled else shuffled
        } else {
            // Restore original order
            _queue.value = originalQueue
        }
    }

    fun toggleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    fun hasNext(): Boolean {
        val current = _currentSong.value ?: return false
        val idx = _queue.value.indexOfFirst { it.id == current.id }
        return idx != -1 && idx < _queue.value.size - 1
    }

    fun hasPrevious(): Boolean {
        val current = _currentSong.value ?: return false
        val idx = _queue.value.indexOfFirst { it.id == current.id }
        return idx > 0
    }

    fun playNext() {
        val current = _currentSong.value
        if (current == null) {
            if (_queue.value.isNotEmpty()) playSong(_queue.value.first())
            return
        }

        val idx = _queue.value.indexOfFirst { it.id == current.id }
        if (idx != -1 && idx < _queue.value.size - 1) {
            playSong(_queue.value[idx + 1])
        } else if (_repeatMode.value == RepeatMode.ALL && _queue.value.isNotEmpty()) {
            playSong(_queue.value.first())
        }
    }

    fun playPrevious() {
        val current = _currentSong.value ?: return
        val idx = _queue.value.indexOfFirst { it.id == current.id }
        if (idx > 0) {
            playSong(_queue.value[idx - 1])
        } else if (_repeatMode.value == RepeatMode.ALL && _queue.value.isNotEmpty()) {
            playSong(_queue.value.last())
        }
    }

    // Sleep Timer Controls
    fun startSleepTimer(minutes: Long) {
        stopSleepTimer()
        val seconds = minutes * 60
        _sleepTimerTotal.value = seconds
        _sleepTimerRemaining.value = seconds
        _sleepTimerActive.value = true

        sleepTimerJob = scope.launch(Dispatchers.Main) {
            while (_sleepTimerRemaining.value > 0) {
                delay(1000)
                _sleepTimerRemaining.value -= 1
            }
            // Timer finished, stop music
            pausePlayback()
            _sleepTimerActive.value = false
        }
    }

    fun stopSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimerActive.value = false
        _sleepTimerRemaining.value = 0L
    }

    // Equalizer Controls
    private fun initEqualizer() {
        try {
            val sessionId = mediaPlayer?.audioSessionId ?: 0
            if (sessionId != 0 && equalizer == null) {
                equalizer = Equalizer(0, sessionId).apply {
                    enabled = _eqEnabled.value
                }
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Failed to initialize Equalizer effect", e)
        }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        _eqEnabled.value = enabled
        try {
            equalizer?.enabled = enabled
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setEqPreset(presetName: String) {
        _eqPreset.value = presetName
        
        // 8 standard presets mapping: -12 to +12 dB range
        val bands = when (presetName) {
            "Flat" -> listOf(0, 0, 0, 0, 0)
            "Bass Boost" -> listOf(8, 5, 1, 0, 0)
            "Treble Boost" -> listOf(0, 0, 1, 5, 8)
            "Vocal" -> listOf(-3, 1, 6, 4, -1)
            "Rock" -> listOf(5, 3, -1, 4, 6)
            "Pop" -> listOf(-2, 2, 5, 1, -1)
            "Jazz" -> listOf(4, 2, 1, 3, 2)
            "Classical" -> listOf(5, 3, 0, 2, 4)
            else -> listOf(0, 0, 0, 0, 0)
        }
        _eqBands.value = bands
        applyEqualizerSettings()
    }

    fun setBandGain(bandIndex: Int, value: Int) {
        val list = _eqBands.value.toMutableList()
        if (bandIndex in list.indices) {
            list[bandIndex] = value.coerceIn(-12, 12)
            _eqBands.value = list
            _eqPreset.value = "Custom"
            applyEqualizerSettings()
        }
    }

    private fun applyEqualizerSettings() {
        if (!_eqEnabled.value) return
        val eq = equalizer ?: return
        
        try {
            // Native EQ has a default level range in milliBel (mB). Usually -1500mB to +1500mB.
            // We scale our -12dB to +12dB range to milliBels (1dB = 100 milliBels).
            val numBands = eq.numberOfBands.toInt()
            val gains = _eqBands.value
            
            for (i in 0 until numBands) {
                if (i < gains.size) {
                    val millibels = (gains[i] * 100).toShort()
                    eq.setBandLevel(i.toShort(), millibels)
                }
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Failed to apply Equalizer band settings", e)
        }
    }

    // Progress Tracker loop
    private fun startProgressTracker() {
        stopProgressTracker()
        progressJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        _progress.value = player.currentPosition.toLong()
                    }
                }
                delay(250)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
    }

    fun release() {
        stopProgressTracker()
        stopSleepTimer()
        mediaPlayer?.release()
        mediaPlayer = null
        equalizer?.release()
        equalizer = null
        scope.cancel()
    }

    companion object {
        @Volatile
        private var INSTANCE: AudioPlayerManager? = null

        fun getInstance(context: Context): AudioPlayerManager {
            return INSTANCE ?: synchronized(this) {
                val instance = AudioPlayerManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
