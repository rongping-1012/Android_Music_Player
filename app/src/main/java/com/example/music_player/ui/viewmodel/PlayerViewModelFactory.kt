package com.example.music_player.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.music_player.service.MusicServiceConnection

class PlayerViewModelFactory(
    private val musicServiceConnection: MusicServiceConnection,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlayerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlayerViewModel(musicServiceConnection, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
