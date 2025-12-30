package com.example.music_player.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.music_player.data.UserRepository
import com.example.music_player.data.UserService
import com.example.music_player.service.MusicServiceConnection

class AdminViewModelFactory(
    private val userRepository: UserRepository,
    private val userService: UserService,
    private val musicServiceConnection: MusicServiceConnection,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AdminViewModel(userRepository, userService, musicServiceConnection, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
