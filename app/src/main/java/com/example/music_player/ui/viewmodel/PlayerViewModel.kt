package com.example.music_player.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.music_player.service.MusicServiceConnection

class PlayerViewModel(private val musicServiceConnection: MusicServiceConnection) : ViewModel() {

    val isPlaying = musicServiceConnection.isPlaying
    val currentSong = musicServiceConnection.currentSong
    val progress = musicServiceConnection.progress
    val duration = musicServiceConnection.duration
    val playMode = musicServiceConnection.playMode

    fun togglePlayPause() {
        musicServiceConnection.togglePlayPause()
    }

    fun playNext() {
        musicServiceConnection.playNext()
    }

    fun playPrevious() {
        musicServiceConnection.playPrevious()
    }

    fun seekTo(position: Int) {
        musicServiceConnection.seekTo(position)
    }

    fun cyclePlayMode() {
        musicServiceConnection.cyclePlayMode()
    }
    
    fun setVolume(volume: Float) {
        musicServiceConnection.setVolume(volume)
    }
    
    fun getVolume(): Float {
        return musicServiceConnection.getVolume()
    }
}
