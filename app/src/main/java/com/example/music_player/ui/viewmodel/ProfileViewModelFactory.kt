package com.example.music_player.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.music_player.data.DataStoreManager
import com.example.music_player.data.MusicRepository
import com.example.music_player.data.UserService
import com.example.music_player.service.MusicServiceConnection

class ProfileViewModelFactory(
    private val userService: UserService,
    private val musicServiceConnection: MusicServiceConnection,
    private val musicRepository: MusicRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(
                userService,
                musicServiceConnection,
                musicRepository,
                DataStoreManager(context)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
