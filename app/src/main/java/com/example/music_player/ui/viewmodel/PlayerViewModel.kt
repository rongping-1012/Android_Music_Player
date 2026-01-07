package com.example.music_player.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.music_player.data.model.LyricData
import com.example.music_player.data.parser.LyricParser
import com.example.music_player.service.MusicServiceConnection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val musicServiceConnection: MusicServiceConnection,
    private val context: Context
) : ViewModel() {

    val isPlaying = musicServiceConnection.isPlaying
    val currentSong = musicServiceConnection.currentSong
    val progress = musicServiceConnection.progress
    val duration = musicServiceConnection.duration
    val playMode = musicServiceConnection.playMode
    val playCount = musicServiceConnection.playCount

    private val _lyricData = MutableStateFlow<LyricData>(LyricData(emptyList()))
    val lyricData = _lyricData.asStateFlow()

    init {
        // 监听歌曲变化，自动加载歌词
        viewModelScope.launch {
            currentSong.collect { song ->
                if (song != null) {
                    loadLyric(song.uri, song.name)
                } else {
                    _lyricData.value = LyricData(emptyList())
                }
            }
        }
    }

    /**
     * 加载歌词文件
     */
    fun loadLyric(musicUri: Uri, musicName: String) {
        viewModelScope.launch {
            // 尝试查找歌词文件
            val lyricUri = LyricParser.findLyricFile(context, musicUri, musicName)
            if (lyricUri != null) {
                val lyric = LyricParser.parseFromUri(context, lyricUri)
                _lyricData.value = lyric
            } else {
                // 如果找不到歌词文件，尝试从同目录查找
                // 这里可以根据实际需求扩展查找逻辑
                _lyricData.value = LyricData(emptyList())
            }
        }
    }

    /**
     * 手动设置歌词数据（用于测试或外部加载）
     */
    fun setLyricData(lyricData: LyricData) {
        _lyricData.value = lyricData
    }

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

    /**
     * 根据时间跳转（用于歌词拖动）
     * 无论播放状态还是暂停状态，点击歌词都会跳转并继续播放
     */
    fun seekToTime(timeMs: Long) {
        // 跳转到指定位置
        musicServiceConnection.seekTo(timeMs.toInt())
        // 如果当前是暂停状态，自动开始播放
        if (!musicServiceConnection.isPlaying.value) {
            musicServiceConnection.togglePlayPause()
        }
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

    fun getPlaylist(): List<com.example.music_player.data.model.MusicFile> {
        return musicServiceConnection.getPlaylist()
    }

    fun getCurrentIndex(): Int {
        return musicServiceConnection.getCurrentIndex()
    }

    fun playAt(position: Int) {
        musicServiceConnection.playAt(position)
    }
}
