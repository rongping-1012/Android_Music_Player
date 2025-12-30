package com.example.music_player.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.music_player.data.MusicRepository
import com.example.music_player.service.MusicServiceConnection

class UserViewModelFactory(
    private val musicRepository: MusicRepository,
    private val musicServiceConnection: MusicServiceConnection,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UserViewModel(musicRepository, musicServiceConnection, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
