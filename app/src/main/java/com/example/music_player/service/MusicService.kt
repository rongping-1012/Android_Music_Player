package com.example.music_player.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.example.music_player.data.local.AppDatabase
import com.example.music_player.data.model.MusicFile
import com.example.music_player.data.local.entity.PlayHistory
import com.example.music_player.data.DataStoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.util.*

class MusicService : Service() {
    private lateinit var mediaPlayer: MediaPlayer
    private var currentTrackIndex = 0
    private val musicFiles = mutableListOf<MusicFile>()
    private var isPlaying = false

    enum class PlayMode { SEQUENTIAL, SHUFFLE, REPEAT_ONE }
    private var currentPlayMode = PlayMode.SEQUENTIAL
    private val random = java.util.Random()
    private val playCountMap = mutableMapOf<String, Int>()
    
    // 洗牌播放列表：存储原始索引的随机排列
    private val shuffledPlaylist = mutableListOf<Int>()
    // 在洗牌列表中的当前位置
    private var shuffledIndex = 0
    
    // 当前正在准备的 URI，用于避免快速切换时的旧回调触发错误
    private var preparingUri: Uri? = null
    
    private val binder = MusicBinder()
    private val handler = Handler(Looper.getMainLooper())

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }
    
    // Expanded Listener Interface
    interface OnPlaybackStateChangeListener {
        fun onSongChanged(song: MusicFile?)
        fun onPlayStateChanged(isPlaying: Boolean)
        fun onProgressChanged(currentPosition: Int, duration: Int)
        fun onPlayModeChanged(mode: PlayMode)
        fun onError(message: String)
        fun onPlayCountChanged(count: Int)
    }

    private val listeners = mutableListOf<OnPlaybackStateChangeListener>()

    private val updateProgressAction = object : Runnable {
        override fun run() {
            if (isPlaying) {
                val currentPosition = mediaPlayer.currentPosition
                val duration = mediaPlayer.duration
                listeners.forEach { it.onProgressChanged(currentPosition, duration) }
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        mediaPlayer = MediaPlayer()
        currentPlayMode = loadPlayMode()
        setupMediaPlayer()
    }
    
    private fun setupMediaPlayer() {
        mediaPlayer.setOnCompletionListener {
            playNext()
        }
        
        // 注意：错误监听器在 playMusic 中动态设置，以便检查 preparingUri
        
        // 添加信息监听器
        mediaPlayer.setOnInfoListener { _, what, extra ->
            when (what) {
                android.media.MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING,
                android.media.MediaPlayer.MEDIA_INFO_NOT_SEEKABLE,
                android.media.MediaPlayer.MEDIA_INFO_UNSUPPORTED_SUBTITLE -> {
                    android.util.Log.w("MusicService", "MediaPlayer info: what=$what, extra=$extra")
                }
            }
            false
        }
    }

    fun playMusic(uri: Uri) {
        try {
            // 如果在随机播放模式下，找到该 URI 在洗牌列表中的位置
            if (currentPlayMode == PlayMode.SHUFFLE && shuffledPlaylist.isNotEmpty()) {
                val fileIndex = musicFiles.indexOfFirst { it.uri == uri }
                if (fileIndex != -1) {
                    val indexInShuffled = shuffledPlaylist.indexOf(fileIndex)
                    if (indexInShuffled != -1) {
                        shuffledIndex = indexInShuffled
                        currentTrackIndex = fileIndex
                    }
                }
            } else {
                // 非随机模式，直接找到索引
                val fileIndex = musicFiles.indexOfFirst { it.uri == uri }
                if (fileIndex != -1) {
                    currentTrackIndex = fileIndex
                }
            }
            
            // 设置当前正在准备的 URI，用于避免快速切换时的旧回调
            // 使用同步块确保线程安全
            synchronized(this) {
                preparingUri = uri
            }
            
            mediaPlayer.reset()
            
            // 保存当前 URI 到局部变量，用于后续检查
            val currentPreparingUri = uri
            
            // 尝试打开文件
            try {
                contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    // 检查 URI 是否仍然有效
                    synchronized(this) {
                        if (preparingUri != currentPreparingUri) {
                            android.util.Log.d("MusicService", "URI changed during file open, aborting")
                            return
                        }
                    }
                    mediaPlayer.setDataSource(pfd.fileDescriptor)
                    pfd.close()
                } ?: run {
                    // 如果 openFileDescriptor 失败，尝试直接使用 URI
                    synchronized(this) {
                        if (preparingUri != currentPreparingUri) {
                            android.util.Log.d("MusicService", "URI changed during fallback, aborting")
                            return
                        }
                    }
                    mediaPlayer.setDataSource(this, uri)
                }
            } catch (e: Exception) {
                android.util.Log.e("MusicService", "Error setting data source: ${e.message}")
                // 如果 URI 已经改变，不处理这个错误
                synchronized(this) {
                    if (preparingUri != currentPreparingUri) {
                        android.util.Log.d("MusicService", "URI changed during error handling, aborting")
                        return
                    }
                }
                // 尝试直接使用 URI
                try {
                    synchronized(this) {
                        if (preparingUri != currentPreparingUri) {
                            android.util.Log.d("MusicService", "URI changed during second attempt, aborting")
                            return
                        }
                    }
                mediaPlayer.setDataSource(this, uri)
                } catch (e2: Exception) {
                    android.util.Log.e("MusicService", "Error setting data source with URI: ${e2.message}")
                    // 如果 URI 已经改变，不处理这个错误
                    synchronized(this) {
                        if (preparingUri != currentPreparingUri) {
                            android.util.Log.d("MusicService", "URI changed during second error handling, aborting")
                            return
                        }
                        // 只有在 URI 仍然匹配时才报告错误
                        isPlaying = false
                        notifyPlayStateChanged()
                        // 注释掉误触发的警告
                        // notifyError("找不到音乐源或无法播放")
                    }
                    return
                }
            }
            
            // 再次检查 URI 是否仍然有效，然后再准备
            synchronized(this) {
                if (preparingUri != currentPreparingUri) {
                    android.util.Log.d("MusicService", "URI changed before prepareAsync, aborting")
                    return
                }
            }
            
            mediaPlayer.prepareAsync()
            
            mediaPlayer.setOnPreparedListener { mp ->
                // 检查这个回调是否对应当前正在准备的 URI
                synchronized(this) {
                    if (preparingUri != currentPreparingUri) {
                        android.util.Log.d("MusicService", "Ignoring prepared callback for old URI")
                        return@setOnPreparedListener
                    }
                }
                
                try {
                    mp.start()
                    incrementPlayCount(uri)
                    isPlaying = true
                    notifySongChanged()
                    notifyPlayCountChanged(uri)
                    notifyPlayStateChanged()
                    handler.post(updateProgressAction)
                    savePlayHistory(uri)
                } catch (e: Exception) {
                    android.util.Log.e("MusicService", "Error starting playback: ${e.message}")
                    // 如果 URI 已经改变，不处理这个错误
                    synchronized(this) {
                        if (preparingUri != currentPreparingUri) {
                            return@setOnPreparedListener
                        }
                    isPlaying = false
                    notifyPlayStateChanged()
                    notifyError("无法播放此音乐文件")
                }
                }
            }
            
            // 设置错误监听器，但只在 URI 匹配时报告错误
            mediaPlayer.setOnErrorListener { _, what, extra ->
                // 如果 URI 已经改变，不处理这个错误
                synchronized(this) {
                    if (preparingUri != currentPreparingUri) {
                        android.util.Log.d("MusicService", "Ignoring error callback for old URI: what=$what, extra=$extra")
                        return@setOnErrorListener true // 表示错误已处理，不触发默认错误处理
                    }
                }
                // 在同步块外处理错误，避免死锁
                android.util.Log.e("MusicService", "MediaPlayer error: what=$what, extra=$extra")
                isPlaying = false
                notifyPlayStateChanged()
                val errorMessage = when (what) {
                    android.media.MediaPlayer.MEDIA_ERROR_UNKNOWN -> "无法播放此音乐文件"
                    android.media.MediaPlayer.MEDIA_ERROR_SERVER_DIED -> "媒体服务器错误"
                    else -> "找不到音乐源或无法播放"
                }
                // 注释掉误触发的警告
                // notifyError(errorMessage)
                true // 表示错误已处理
            }
        } catch (e: Exception) {
            android.util.Log.e("MusicService", "Error in playMusic: ${e.message}", e)
            // 如果 URI 已经改变，不处理这个错误
            synchronized(this) {
                if (preparingUri != uri) {
                    android.util.Log.d("MusicService", "URI changed during outer catch, aborting")
                    return
                }
            isPlaying = false
            notifyPlayStateChanged()
            // 注释掉误触发的警告
            // notifyError("找不到音乐源或无法播放")
            }
        }
    }

    fun playMusicAtPosition(position: Int) {
        if (musicFiles.isEmpty()) return
        if (position in musicFiles.indices) {
            if (currentPlayMode == PlayMode.SHUFFLE) {
                // 在随机播放模式下，找到该位置在洗牌列表中的索引
                val indexInShuffled = shuffledPlaylist.indexOf(position)
                if (indexInShuffled != -1) {
                    shuffledIndex = indexInShuffled
                    currentTrackIndex = position
                    playMusic(musicFiles[position].uri)
                } else {
                    // 如果找不到（不应该发生），重新生成洗牌列表
                    generateShuffledPlaylist()
                    shuffledIndex = shuffledPlaylist.indexOf(position).coerceAtLeast(0)
                    currentTrackIndex = position
                    playMusic(musicFiles[position].uri)
                }
            } else {
            currentTrackIndex = position
            playMusic(musicFiles[position].uri)
            }
        } else {
            // 如果位置无效，重置为0
            currentTrackIndex = 0
            shuffledIndex = 0
            if (musicFiles.isNotEmpty()) {
                playMusic(musicFiles[0].uri)
            }
        }
    }

    fun playNext() {
        if (musicFiles.isEmpty()) return
        when (currentPlayMode) {
            PlayMode.REPEAT_ONE -> {
                // 单曲循环：无论是否来自完成事件，都重新播放当前歌曲
                playMusic(musicFiles[currentTrackIndex].uri)
            }
            PlayMode.SHUFFLE -> {
                // 使用洗牌列表进行播放
                if (shuffledPlaylist.isEmpty()) {
                    generateShuffledPlaylist()
                }
                // 移动到洗牌列表的下一个位置
                shuffledIndex = (shuffledIndex + 1) % shuffledPlaylist.size
                currentTrackIndex = shuffledPlaylist[shuffledIndex]
                playMusic(musicFiles[currentTrackIndex].uri)
            }
            PlayMode.SEQUENTIAL -> {
                currentTrackIndex = (currentTrackIndex + 1) % musicFiles.size
        playMusic(musicFiles[currentTrackIndex].uri)
            }
        }
    }

    fun playPrevious() {
        if (musicFiles.isEmpty()) return
        when (currentPlayMode) {
            PlayMode.REPEAT_ONE -> {
                // 单曲循环：重新播放当前歌曲
                playMusic(musicFiles[currentTrackIndex].uri)
            }
            PlayMode.SHUFFLE -> {
                // 使用洗牌列表进行播放
                if (shuffledPlaylist.isEmpty()) {
                    generateShuffledPlaylist()
                }
                // 移动到洗牌列表的上一个位置
                shuffledIndex = if (shuffledIndex > 0) shuffledIndex - 1 else shuffledPlaylist.size - 1
                currentTrackIndex = shuffledPlaylist[shuffledIndex]
                playMusic(musicFiles[currentTrackIndex].uri)
            }
            PlayMode.SEQUENTIAL -> {
                currentTrackIndex = if (currentTrackIndex > 0) currentTrackIndex - 1 else musicFiles.size - 1
        playMusic(musicFiles[currentTrackIndex].uri)
            }
        }
    }

    fun pauseMusic() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            isPlaying = false
            handler.removeCallbacks(updateProgressAction)
            notifyPlayStateChanged()
        }
    }
    
    fun resumeMusic() {
        if (!mediaPlayer.isPlaying) {
            mediaPlayer.start()
            isPlaying = true
            handler.post(updateProgressAction)
            notifyPlayStateChanged()
        }
    }
    
    fun seekTo(position: Int) = mediaPlayer.seekTo(position)
    
    fun setPlayMode(mode: PlayMode) {
        val previousMode = currentPlayMode
        currentPlayMode = mode
        
        // 如果切换到随机播放模式，生成洗牌列表
        if (mode == PlayMode.SHUFFLE && musicFiles.isNotEmpty()) {
            generateShuffledPlaylist()
            // 找到当前歌曲在洗牌列表中的位置
            val indexInShuffled = shuffledPlaylist.indexOf(currentTrackIndex)
            if (indexInShuffled != -1) {
                shuffledIndex = indexInShuffled
            } else {
                // 如果当前歌曲不在洗牌列表中（不应该发生），重置索引
                shuffledIndex = 0
            }
        } else if (previousMode == PlayMode.SHUFFLE && mode != PlayMode.SHUFFLE) {
            // 如果从随机播放模式切换出去，清空洗牌列表
            shuffledPlaylist.clear()
        }
        
        savePlayMode(mode)
        listeners.forEach { it.onPlayModeChanged(mode) }
    }
    
    // Getters
    fun getDuration(): Int = if (mediaPlayer.isPlaying) mediaPlayer.duration else 0
    fun getCurrentPosition(): Int = if (mediaPlayer.isPlaying) mediaPlayer.currentPosition else 0
    fun isPlaying(): Boolean = isPlaying
    fun getPlayMode(): PlayMode = currentPlayMode
    fun getCurrentSong(): MusicFile? = musicFiles.getOrNull(currentTrackIndex)
    fun getCurrentIndex(): Int = currentTrackIndex
    fun getMusicListSnapshot(): List<MusicFile> = musicFiles.toList()
    fun getPlayCount(uri: Uri?): Int = playCountMap[uri?.toString()] ?: 0

    private fun incrementPlayCount(uri: Uri) {
        val key = uri.toString()
        val newCount = (playCountMap[key] ?: 0) + 1
        playCountMap[key] = newCount
    }
    
    // Volume control
    fun setVolume(volume: Float) {
        // volume should be between 0.0f and 1.0f
        val clampedVolume = volume.coerceIn(0f, 1f)
        mediaPlayer.setVolume(clampedVolume, clampedVolume)
    }
    
    fun getVolume(): Float {
        // MediaPlayer doesn't have a getVolume method, so we'll use AudioManager
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        return if (maxVolume > 0) currentVolume.toFloat() / maxVolume.toFloat() else 1f
    }

    fun setMusicList(list: List<MusicFile>) {
        musicFiles.clear()
        musicFiles.addAll(list)
        // 重置当前播放索引，避免索引越界
        if (currentTrackIndex >= musicFiles.size) {
            currentTrackIndex = 0
        }
        // 如果在随机播放模式下，重新生成洗牌列表
        if (currentPlayMode == PlayMode.SHUFFLE && musicFiles.isNotEmpty()) {
            generateShuffledPlaylist()
            // 如果当前索引有效，找到它在洗牌列表中的位置
            if (currentTrackIndex in musicFiles.indices) {
                val indexInShuffled = shuffledPlaylist.indexOf(currentTrackIndex)
                if (indexInShuffled != -1) {
                    shuffledIndex = indexInShuffled
                } else {
                    shuffledIndex = 0
                }
            } else {
                shuffledIndex = 0
            }
        }
    }
    
    /**
     * 生成洗牌播放列表
     * 使用 Fisher-Yates 洗牌算法
     */
    private fun generateShuffledPlaylist() {
        shuffledPlaylist.clear()
        if (musicFiles.isEmpty()) return
        
        // 创建包含所有索引的列表
        val indices = musicFiles.indices.toMutableList()
        
        // Fisher-Yates 洗牌算法
        for (i in indices.size - 1 downTo 1) {
            val j = random.nextInt(i + 1)
            val temp = indices[i]
            indices[i] = indices[j]
            indices[j] = temp
        }
        
        shuffledPlaylist.addAll(indices)
        shuffledIndex = 0
    }

    // Listeners
    fun addPlaybackStateChangeListener(listener: OnPlaybackStateChangeListener) {
        listeners.add(listener)
        // Immediately notify new listener of current state
        listener.onPlayStateChanged(isPlaying)
        listener.onSongChanged(getCurrentSong())
        listener.onPlayModeChanged(currentPlayMode)
        listener.onPlayCountChanged(getPlayCount(getCurrentSong()?.uri))
    }

    fun removePlaybackStateChangeListener(listener: OnPlaybackStateChangeListener) {
        listeners.remove(listener)
    }

    private fun notifySongChanged() {
        listeners.forEach { it.onSongChanged(getCurrentSong()) }
    }
    
    private fun notifyPlayCountChanged(uri: Uri?) {
        val count = getPlayCount(uri)
        listeners.forEach { it.onPlayCountChanged(count) }
    }

    private fun notifyPlayStateChanged() {
        listeners.forEach { it.onPlayStateChanged(isPlaying) }
    }
    
    private fun notifyError(message: String) {
        listeners.forEach { it.onError(message) }
    }

    // Persistence
    private fun savePlayMode(mode: PlayMode) {
        getSharedPreferences("MusicPreferences", Context.MODE_PRIVATE).edit().putString("playMode", mode.name).apply()
    }

    private fun loadPlayMode(): PlayMode {
        val modeName = getSharedPreferences("MusicPreferences", Context.MODE_PRIVATE).getString("playMode", PlayMode.SEQUENTIAL.name)
        return PlayMode.valueOf(modeName ?: PlayMode.SEQUENTIAL.name)
    }

    private fun savePlayHistory(uri: Uri) {
        val currentMusic = getCurrentSong() ?: return
        val songPath = uri.toString()
        val playHistoryDao = AppDatabase.getDatabase(applicationContext).playHistoryDao()
        val dataStoreManager = DataStoreManager(applicationContext)
        CoroutineScope(Dispatchers.IO).launch {
            val username = dataStoreManager.currentUsername.first()
            if (username.isBlank()) {
                return@launch // 如果用户未登录，不保存播放历史
            }
            val existingHistory = playHistoryDao.getHistoryBySongPath(username, songPath)
            if (existingHistory != null) {
                // 如果已存在，更新播放时间
                playHistoryDao.updateHistoryTime(username, songPath, Date())
            } else {
                // 如果不存在，插入新记录
                val playHistory = PlayHistory(username = username, songPath = songPath, songName = currentMusic.name, playTime = Date())
                playHistoryDao.insertHistory(playHistory)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
        handler.removeCallbacks(updateProgressAction)
    }
} 