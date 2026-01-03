package com.example.music_player.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.example.music_player.data.model.MusicFile
import com.example.music_player.service.MusicService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MusicServiceConnection(context: Context) : MusicService.OnPlaybackStateChangeListener {

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentSong = MutableStateFlow<MusicFile?>(null)
    val currentSong = _currentSong.asStateFlow()

    private val _progress = MutableStateFlow(0)
    val progress = _progress.asStateFlow()

    private val _duration = MutableStateFlow(0)
    val duration = _duration.asStateFlow()

    private val _playMode = MutableStateFlow(MusicService.PlayMode.SEQUENTIAL)
    val playMode = _playMode.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private var musicService: MusicService? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            musicService?.addPlaybackStateChangeListener(this@MusicServiceConnection)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicService?.removePlaybackStateChangeListener(this@MusicServiceConnection)
            musicService = null
        }
    }

    init {
        Intent(context, MusicService::class.java).also { intent ->
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            context.startService(intent)
        }
    }

    // --- Playback Controls ---
    fun playMusic(musicFiles: List<MusicFile>, position: Int) {
        // 确保先设置音乐列表，再播放指定位置
        musicService?.setMusicList(musicFiles)
        // 直接播放，setMusicList已经同步完成
        musicService?.playMusicAtPosition(position)
    }

    fun togglePlayPause() {
        if (musicService?.isPlaying() == true) {
            musicService?.pauseMusic()
        } else {
            musicService?.resumeMusic()
        }
    }

    fun playNext() {
        musicService?.playNext()
    }

    fun playPrevious() {
        musicService?.playPrevious()
    }

    fun seekTo(position: Int) {
        musicService?.seekTo(position)
    }

    fun cyclePlayMode() {
        val nextMode = when (musicService?.getPlayMode()) {
            MusicService.PlayMode.SEQUENTIAL -> MusicService.PlayMode.SHUFFLE
            MusicService.PlayMode.SHUFFLE -> MusicService.PlayMode.REPEAT_ONE
            MusicService.PlayMode.REPEAT_ONE -> MusicService.PlayMode.SEQUENTIAL
            null -> MusicService.PlayMode.SEQUENTIAL
        }
        musicService?.setPlayMode(nextMode)
    }

    fun stopMusic() {
        musicService?.pauseMusic()
    }
    
    fun setVolume(volume: Float) {
        musicService?.setVolume(volume)
    }
    
    fun getVolume(): Float {
        return musicService?.getVolume() ?: 1f
    }

    // --- Service Listener Callbacks ---
    override fun onSongChanged(song: MusicFile?) {
        _currentSong.value = song
    }

    override fun onPlayStateChanged(isPlaying: Boolean) {
        _isPlaying.value = isPlaying
    }

    override fun onProgressChanged(currentPosition: Int, duration: Int) {
        _progress.value = currentPosition
        _duration.value = duration
    }
    
    override fun onPlayModeChanged(mode: MusicService.PlayMode) {
        _playMode.value = mode
    }
    
    override fun onError(message: String) {
        _errorMessage.value = message
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
}