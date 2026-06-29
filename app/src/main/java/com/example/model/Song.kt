package com.example.model

import java.io.Serializable

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val path: String,
    val duration: Long, // in milliseconds
    val isLocal: Boolean,
    val albumArtUri: String? = null
) : Serializable {
    val durationText: String
        get() {
            val totalSecs = duration / 1000
            val mins = totalSecs / 60
            val secs = totalSecs % 60
            return String.format("%02d:%02d", mins, secs)
        }
}
