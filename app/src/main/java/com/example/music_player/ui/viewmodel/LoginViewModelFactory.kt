package com.example.music_player.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.music_player.data.UserService

class LoginViewModelFactory(private val userService: UserService) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(userService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

