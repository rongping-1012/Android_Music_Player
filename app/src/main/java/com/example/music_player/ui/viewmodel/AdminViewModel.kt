package com.example.music_player.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.music_player.data.DataStoreManager
import com.example.music_player.data.local.entity.User
import com.example.music_player.data.repository.UserRepository
import com.example.music_player.data.UserService
import com.example.music_player.service.MusicServiceConnection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class UserListUiState {
    object Loading : UserListUiState()
    data class Success(val users: List<User>) : UserListUiState()
    data class Error(val message: String) : UserListUiState()
}

sealed class AdminUiState {
    object LoggedOut : AdminUiState()
}

class AdminViewModel(
    private val userRepository: UserRepository,
    private val userService: UserService,
    private val musicServiceConnection: MusicServiceConnection,
    private val context: Context
) : ViewModel() {

    private val _adminState = MutableStateFlow<AdminUiState?>(null)
    val adminState: StateFlow<AdminUiState?> = _adminState.asStateFlow()

    private val _uiState = MutableStateFlow<UserListUiState>(UserListUiState.Loading)
    val uiState: StateFlow<UserListUiState> = _uiState.asStateFlow()

    init {
        loadUsers()
    }

    fun loadUsers() {
        viewModelScope.launch {
            _uiState.value = UserListUiState.Loading
            try {
                val users = userRepository.getAllUsers()
                _uiState.value = UserListUiState.Success(users)
            } catch (e: Exception) {
                _uiState.value = UserListUiState.Error(e.message ?: "加载用户列表失败")
            }
        }
    }

    fun deleteUser(user: User) {
        viewModelScope.launch {
            try {
                // 检查被删除的用户是否是当前登录用户
                val currentUsername = userService.currentUsername.first()
                val isCurrentUser = currentUsername == user.username
                
                // 删除用户（会自动删除该用户的喜欢和播放历史）
                userRepository.deleteUser(user)
                
                // 删除用户前，先将其主题偏好重置为亮色模式
                userRepository.updateDarkMode(user.username, false)
                
                // 如果被删除的是当前登录用户，清除音乐文件夹选择
                if (isCurrentUser) {
                    val dataStoreManager = DataStoreManager(context)
                    dataStoreManager.setSelectedFolderUri(null) // 清除音乐文件夹选择
                }

                loadUsers()
            } catch (e: Exception) {
                _uiState.value = UserListUiState.Error(e.message ?: "删除用户失败")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            // 退出登录前停止音乐播放
            musicServiceConnection.stopMusic()
            userService.logout()
            _adminState.value = AdminUiState.LoggedOut
        }
    }
    
    suspend fun adminUpdatePassword(username: String, newPassword: String): Result<Unit> {
        return userService.adminUpdatePassword(username, newPassword)
    }
}
