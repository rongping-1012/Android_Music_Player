package com.example.music_player.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.music_player.data.DataStoreManager
import com.example.music_player.data.model.MusicFile
import com.example.music_player.data.repository.MusicRepository
import com.example.music_player.data.UserService
import com.example.music_player.data.remote.BannerData
import com.example.music_player.service.MusicServiceConnection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// 定义UI状态
sealed class MusicListUiState {
    object Idle : MusicListUiState() // New state for before permission check
    object Loading : MusicListUiState()
    data class Success(val musicFiles: List<MusicFile>) : MusicListUiState()
    data class Error(val message: String) : MusicListUiState()
}

class UserViewModel(
    private val musicRepository: MusicRepository,
    private val musicServiceConnection: MusicServiceConnection,
    private val context: Context
) : ViewModel() {

    val isPlaying = musicServiceConnection.isPlaying
    val currentSong = musicServiceConnection.currentSong
    val errorMessage = musicServiceConnection.errorMessage

    private var musicFiles: List<MusicFile> = emptyList()
    private var filteredMusicFiles: List<MusicFile> = emptyList()
    private val dataStoreManager = DataStoreManager(context)
    private val userService = UserService(context)

    private val _uiState = MutableStateFlow<MusicListUiState>(MusicListUiState.Idle)
    val uiState: StateFlow<MusicListUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _favoriteMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val favoriteMap: StateFlow<Map<String, Boolean>> = _favoriteMap.asStateFlow()
    
    private val _banners = MutableStateFlow<List<BannerData>>(emptyList())
    val banners: StateFlow<List<BannerData>> = _banners.asStateFlow()
    
    private val _bannersLoading = MutableStateFlow(false)
    val bannersLoading: StateFlow<Boolean> = _bannersLoading.asStateFlow()

    fun loadMusic() {
        viewModelScope.launch {
            _uiState.value = MusicListUiState.Loading
            try {
                val selectedFolderUri = musicRepository.getSelectedFolderUri()
                val files = if (selectedFolderUri != null) {
                    musicRepository.getMusicFilesFromFolder(Uri.parse(selectedFolderUri))
                } else {
                    musicRepository.getMusicFiles()
                }
                musicFiles = files
                filteredMusicFiles = files
                _uiState.value = MusicListUiState.Success(files)
            } catch (e: Exception) {
                _uiState.value = MusicListUiState.Error(e.message ?: "加载音乐文件失败")
            }
        }
    }

    fun loadMusicFromFolder(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = MusicListUiState.Loading
            try {
                musicRepository.setSelectedFolderUri(uri.toString())
                val files = musicRepository.getMusicFilesFromFolder(uri)
                musicFiles = files // Store the list for playback
                filteredMusicFiles = files
                _uiState.value = MusicListUiState.Success(files)
            } catch (e: Exception) {
                _uiState.value = MusicListUiState.Error(e.message ?: "加载音乐文件失败")
            }
        }
    }

    fun playMusic(position: Int) {
        // 直接使用过滤后的列表和位置，确保播放正确的歌曲
        if (position in filteredMusicFiles.indices) {
            // 直接使用过滤后的列表播放，确保点击的歌曲就是播放的歌曲
            musicServiceConnection.playMusic(filteredMusicFiles, position)
        }
    }

    fun togglePlayPause() {
        musicServiceConnection.togglePlayPause()
    }

    fun searchMusic(query: String) {
        _searchQuery.value = query
        filteredMusicFiles = if (query.isBlank()) {
            musicFiles
        } else {
            musicFiles.filter { it.name.contains(query, ignoreCase = true) }
        }
        _uiState.value = MusicListUiState.Success(filteredMusicFiles)
    }
    
    fun loadFavoriteStatus() {
        viewModelScope.launch {
            try {
                val username = userService.currentUsername.first()
                if (username.isNotEmpty()) {
                    val favorites = musicRepository.getUserFavorites(username)
                    val favoriteMap = favorites.associate { it.songPath to true }
                    _favoriteMap.value = favoriteMap
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun clearError() {
        musicServiceConnection.clearError()
    }
    
    fun toggleFavorite(musicFile: MusicFile) {
        viewModelScope.launch {
            try {
                val username = userService.currentUsername.first()
                if (username.isEmpty()) return@launch
                
                val songPath = musicFile.uri.toString()
                val isFavorite = _favoriteMap.value[songPath] == true
                
                if (isFavorite) {
                    musicRepository.removeFavorite(username, songPath)
                } else {
                    musicRepository.addFavorite(username, songPath, musicFile.name)
                }
                
                // 更新本地状态
                val newMap = _favoriteMap.value.toMutableMap()
                if (isFavorite) {
                    newMap.remove(songPath)
                } else {
                    newMap[songPath] = true
                }
                _favoriteMap.value = newMap
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 加载轮播图数据（网络请求）
     */
    fun loadBanners() {
        viewModelScope.launch {
            _bannersLoading.value = true
            try {
                val result = musicRepository.getBanners()
                result.onSuccess { bannerList ->
                    _banners.value = bannerList
                }.onFailure { exception ->
                    // 网络请求失败时，使用空列表或默认数据
                    _banners.value = emptyList()
                    android.util.Log.e("UserViewModel", "加载轮播图失败", exception)
                }
            } catch (e: Exception) {
                _banners.value = emptyList()
                android.util.Log.e("UserViewModel", "加载轮播图异常", e)
            } finally {
                _bannersLoading.value = false
            }
        }
    }
}
