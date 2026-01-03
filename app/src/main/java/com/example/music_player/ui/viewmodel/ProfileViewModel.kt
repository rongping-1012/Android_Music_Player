package com.example.music_player.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.music_player.data.DataStoreManager
import com.example.music_player.data.local.entity.FavoriteMusic
import com.example.music_player.data.repository.MusicRepository
import com.example.music_player.data.local.entity.PlayHistory
import com.example.music_player.data.local.entity.User
import com.example.music_player.data.UserService
import com.example.music_player.service.MusicServiceConnection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(val user: User) : ProfileUiState()
    object LoggedOut : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

sealed class UpdateResultState {
    object Idle : UpdateResultState()
    object Loading : UpdateResultState()
    data class Success(val message: String) : UpdateResultState()
    data class Error(val message: String) : UpdateResultState()
}

class ProfileViewModel(
    private val userService: UserService,
    private val musicServiceConnection: MusicServiceConnection,
    private val musicRepository: MusicRepository,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _avatarUri = MutableStateFlow<String?>(null)
    val avatarUri: StateFlow<String?> = _avatarUri.asStateFlow()

    private val _profileState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val profileState: StateFlow<ProfileUiState> = _profileState.asStateFlow()

    private val _updateNicknameState = MutableStateFlow<UpdateResultState>(UpdateResultState.Idle)
    val updateNicknameState: StateFlow<UpdateResultState> = _updateNicknameState.asStateFlow()

    private val _updatePasswordState = MutableStateFlow<UpdateResultState>(UpdateResultState.Idle)
    val updatePasswordState: StateFlow<UpdateResultState> = _updatePasswordState.asStateFlow()
    
    private val _playHistory = MutableStateFlow<List<PlayHistory>>(emptyList())
    val playHistory: StateFlow<List<PlayHistory>> = _playHistory.asStateFlow()
    
    private val _favorites = MutableStateFlow<List<FavoriteMusic>>(emptyList())
    val favorites: StateFlow<List<FavoriteMusic>> = _favorites.asStateFlow()

    init {
        loadCurrentUser()
        loadAvatarUri()
    }

    private fun loadAvatarUri() {
        viewModelScope.launch {
            val user = userService.getCurrentUser()
            if (user != null) {
                // 从文件系统加载头像
                val avatarPath = dataStoreManager.getAvatarPath(user.username)
                _avatarUri.value = avatarPath
            }
        }
    }

    fun loadCurrentUser() {
        viewModelScope.launch {
            _profileState.value = ProfileUiState.Loading
            val user = userService.getCurrentUser()
            if (user != null) {
                _profileState.value = ProfileUiState.Success(user)
            } else {
                _profileState.value = ProfileUiState.Error("无法加载用户信息")
            }
        }
    }

    fun updateNickname(newNickname: String) {
        viewModelScope.launch {
            _updateNicknameState.value = UpdateResultState.Loading
            val result = userService.updateNickname(newNickname)
            result.onSuccess {
                _updateNicknameState.value = UpdateResultState.Success("昵称更新成功")
                loadCurrentUser() // Refresh user data
            }.onFailure {
                _updateNicknameState.value = UpdateResultState.Error(it.message ?: "更新失败")
            }
        }
    }
    
    fun updateGender(gender: com.example.music_player.data.model.Gender) {
        viewModelScope.launch {
            _updateNicknameState.value = UpdateResultState.Loading
            val result = userService.updateGender(gender)
            result.onSuccess {
                _updateNicknameState.value = UpdateResultState.Success("性别更新成功")
                loadCurrentUser() // Refresh user data
            }.onFailure {
                _updateNicknameState.value = UpdateResultState.Error(it.message ?: "更新失败")
            }
        }
    }

    fun changePassword(oldPassword: String, newPassword: String) {
        viewModelScope.launch {
            _updatePasswordState.value = UpdateResultState.Loading
            val result = userService.changePassword(oldPassword, newPassword)
            result.onSuccess {
                _updatePasswordState.value = UpdateResultState.Success("密码修改成功")
            }.onFailure {
                _updatePasswordState.value = UpdateResultState.Error(it.message ?: "修改失败")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            // 退出登录前停止音乐播放
            musicServiceConnection.stopMusic()
            userService.logout()
            _profileState.value = ProfileUiState.LoggedOut
        }
    }

    fun resetUpdateStates() {
        _updateNicknameState.value = UpdateResultState.Idle
        _updatePasswordState.value = UpdateResultState.Idle
    }

    fun loadMusicFromFolder(uri: Uri) {
        viewModelScope.launch {
            musicRepository.setSelectedFolderUri(uri.toString())
        }
    }

    fun setAvatarUri(uri: Uri) {
        viewModelScope.launch {
            val user = userService.getCurrentUser()
            if (user != null) {
                // 保存头像到文件系统
                val savedPath = dataStoreManager.saveAvatar(user.username, uri)
                if (savedPath != null) {
                    // 重新加载头像路径
                    _avatarUri.value = dataStoreManager.getAvatarPath(user.username)
                }
            }
        }
    }
    
    fun loadPlayHistory() {
        viewModelScope.launch {
            val user = userService.getCurrentUser()
            if (user != null) {
                try {
                    val history = musicRepository.getUserPlayHistory(user.username)
                    _playHistory.value = history
                } catch (e: Exception) {
                    _playHistory.value = emptyList()
                }
            }
        }
    }
    
    fun loadFavorites() {
        viewModelScope.launch {
            val user = userService.getCurrentUser()
            if (user != null) {
                try {
                    val favorites = musicRepository.getUserFavorites(user.username)
                    _favorites.value = favorites
                } catch (e: Exception) {
                    _favorites.value = emptyList()
                }
            }
        }
    }
    
    fun removeFavorite(favorite: FavoriteMusic) {
        viewModelScope.launch {
            val user = userService.getCurrentUser()
            if (user != null) {
                try {
                    musicRepository.removeFavorite(user.username, favorite.songPath)
                    // 重新加载列表
                    loadFavorites()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
