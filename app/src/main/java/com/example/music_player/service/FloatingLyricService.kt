package com.example.music_player.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.music_player.MainActivity
import com.example.music_player.data.model.LyricData
import com.example.music_player.data.parser.LyricParser
import com.example.music_player.utils.PermissionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * 桌面歌词悬浮窗服务
 * 使用原生TextView实现，避免Compose在Service中的生命周期问题
 * 支持显示当前歌词、拖动位置、基础播放操作
 */
class FloatingLyricService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var musicServiceConnection: MusicServiceConnection? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private var currentLineTextView: TextView? = null // 当前行（深色高亮）
    private var nextLineTextView: TextView? = null // 下一行（浅色）
    private var controlBar: LinearLayout? = null
    private var lockButton: ImageButton? = null
    private var lyricData: LyricData? = null
    private var currentLyricText: String = "暂无歌词"
    private var nextLyricText: String = ""

    // 控制状态
    private var isLocked = false
    private var fontSize = 16f // 默认字体大小
    private val minFontSize = 12f
    private val maxFontSize = 32f
    private val fontSizeStep = 2f

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "floating_lyric_channel"
    private val UI_HIDE_DELAY = 5000L // 5秒后隐藏UI

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateLyricDisplay()
            handler.postDelayed(this, 100) // 每100ms更新一次
        }
    }
    private val hideUIRunnable = Runnable {
        hideUI()
    }

    inner class LyricBinder : Binder() {
        fun getService(): FloatingLyricService = this@FloatingLyricService
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 必须在5秒内调用startForeground
        // Android 14+ (API 34+) 需要指定前台服务类型
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                startForeground(
                    NOTIFICATION_ID,
                    createNotification(),
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } catch (e: Exception) {
                // 如果失败，尝试不使用类型
                startForeground(NOTIFICATION_ID, createNotification())
            }
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }

        // 先检查权限，如果没有权限则停止服务
        if (!PermissionUtils.canDrawOverlays(this)) {
            Toast.makeText(this, "需要悬浮窗权限才能显示桌面歌词", Toast.LENGTH_SHORT).show()
            stopSelf()
            return START_NOT_STICKY
        }

        when (intent?.action) {
            ACTION_SHOW -> showFloatingWindow()
            ACTION_HIDE -> hideFloatingWindow()
            ACTION_TOGGLE -> {
                if (floatingView != null) {
                    hideFloatingWindow()
                } else {
                    showFloatingWindow()
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = LyricBinder()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "桌面歌词",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示桌面歌词悬浮窗"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("桌面歌词")
            .setContentText("正在显示桌面歌词")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun showFloatingWindow() {
        if (floatingView != null) return

        // 检查悬浮窗权限
        if (!PermissionUtils.canDrawOverlays(this)) {
            Toast.makeText(this, "需要悬浮窗权限才能显示桌面歌词", Toast.LENGTH_SHORT).show()
            stopSelf()
            return
        }

        // 创建MusicServiceConnection（延迟创建，避免泄漏）
        if (musicServiceConnection == null) {
            musicServiceConnection = MusicServiceConnection(this)
            observeMusicService()
        }

        // 获取屏幕宽度和高度
        val screenWidth: Int
        val screenHeight: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30+ 使用新的 API
            val windowMetrics = windowManager?.currentWindowMetrics
            val bounds = windowMetrics?.bounds
            screenWidth = bounds?.width() ?: 1080
            screenHeight = bounds?.height() ?: 1920
        } else {
            // API 30 以下使用旧 API（但添加 @Suppress 抑制警告）
            @Suppress("DEPRECATION")
            val display = windowManager?.defaultDisplay
            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            display?.getMetrics(displayMetrics)
            screenWidth = displayMetrics.widthPixels
            screenHeight = displayMetrics.heightPixels
        }
        val lyricWidth = (screenWidth * 0.75f).toInt() // 屏幕宽度的四分之三
        
        layoutParams = WindowManager.LayoutParams().apply {
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            format = PixelFormat.TRANSLUCENT
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            width = lyricWidth // 固定宽度为屏幕的四分之三
            height = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = Gravity.TOP or Gravity.START
            // 初始位置：水平居中，垂直位置在屏幕上方1/3处
            x = (screenWidth - lyricWidth) / 2 // 水平居中
            y = screenHeight / 3 // 垂直位置在屏幕上方1/3处
        }

        // 创建原生View布局（透明背景，固定宽度）
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.TRANSPARENT) // 透明背景
            setPadding(16, 8, 16, 8)
            // 设置固定宽度和居中布局
            layoutParams = LinearLayout.LayoutParams(
                lyricWidth, // 固定宽度
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // 控制按钮栏（使用固定高度，避免隐藏时布局偏移）
        controlBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 8)
            visibility = View.VISIBLE
            // 设置最小高度，确保隐藏时也占用空间
            minimumHeight = 48 // dp转px，约48dp
        }

        // 锁定按钮（使用图标）
        lockButton = ImageButton(this).apply {
            setImageResource(if (isLocked) com.example.music_player.R.drawable.ic_lock else com.example.music_player.R.drawable.ic_unlock)
            setBackgroundColor(Color.parseColor("#80000000")) // 半透明黑色背景
            setPadding(12, 8, 12, 8)
            setOnClickListener {
                isLocked = !isLocked
                setImageResource(if (isLocked) com.example.music_player.R.drawable.ic_lock else com.example.music_player.R.drawable.ic_unlock)
                // 锁定后，确保窗口位置固定（歌词在窗口中心，窗口位置就是拖动到的位置）
                if (isLocked) {
                    val currentParams = this@FloatingLyricService.layoutParams
                    val currentView = floatingView
                    val currentWindowManager = windowManager
                    // 确保参数类型正确
                    if (currentParams != null && currentView != null && currentWindowManager != null) {
                        try {
                            currentWindowManager.updateViewLayout(currentView, currentParams)
                        } catch (e: Exception) {
                            android.util.Log.e("FloatingLyricService", "Error updating view layout: ${e.message}", e)
                        }
                    }
                }
                resetHideUITimer()
            }
        }

        // 放大字体按钮
        val fontSizeIncreaseButton = ImageButton(this).apply {
            setImageResource(com.example.music_player.R.drawable.ic_font_increase)
            setBackgroundColor(Color.parseColor("#80000000")) // 半透明黑色背景
            setPadding(12, 8, 12, 8)
            setOnClickListener {
                fontSize = (fontSize + fontSizeStep).coerceAtMost(maxFontSize)
                currentLineTextView?.textSize = fontSize
                nextLineTextView?.textSize = fontSize * 0.85f
                resetHideUITimer()
            }
        }

        // 缩小字体按钮
        val fontSizeDecreaseButton = ImageButton(this).apply {
            setImageResource(com.example.music_player.R.drawable.ic_font_decrease)
            setBackgroundColor(Color.parseColor("#80000000")) // 半透明黑色背景
            setPadding(12, 8, 12, 8)
            setOnClickListener {
                fontSize = (fontSize - fontSizeStep).coerceAtLeast(minFontSize)
                currentLineTextView?.textSize = fontSize
                nextLineTextView?.textSize = fontSize * 0.85f
                resetHideUITimer()
            }
        }

        controlBar?.addView(lockButton)
        controlBar?.addView(fontSizeIncreaseButton)
        controlBar?.addView(fontSizeDecreaseButton)

        // 当前行TextView（深色高亮）
        currentLineTextView = TextView(this).apply {
            text = "暂无歌词"
            textSize = fontSize
            setTextColor(Color.parseColor("#FF6B9D")) // 使用ActiveColor类似的颜色，深色高亮
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 4)
            // 设置固定宽度，避免文本长度变化导致布局偏移
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            // 设置单行显示，超出部分用省略号
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }

        // 下一行TextView（浅色）
        nextLineTextView = TextView(this).apply {
            text = ""
            textSize = fontSize * 0.85f // 稍微小一点
            val baseColor = Color.parseColor("#FF6B9D")
            setTextColor(Color.argb(128, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor))) // 浅色，透明度50%
            gravity = Gravity.CENTER
            setPadding(0, 4, 0, 8)
            // 设置固定宽度，避免文本长度变化导致布局偏移
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            // 设置单行显示，超出部分用省略号
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }

        container.addView(controlBar)
        container.addView(currentLineTextView)
        container.addView(nextLineTextView)

        // 添加拖动功能（仅在未锁定时可用）
        // 给歌词TextView设置触摸监听，处理拖动
        currentLineTextView?.setOnTouchListener { _, event ->
            // 触摸歌词时显示UI
            showUI()
            
            // 如果锁定，不处理拖动
            if (isLocked) {
                false
            } else {
                handleDragEvent(event)
            }
        }

        nextLineTextView?.setOnTouchListener { _, event ->
            // 触摸歌词时显示UI
            showUI()
            
            // 如果锁定，不处理拖动
            if (isLocked) {
                false
            } else {
                handleDragEvent(event)
            }
        }
        
        // 给容器也设置触摸监听，处理拖动（避免控制按钮区域）
        container.setOnTouchListener { _, event ->
            // 如果点击的是控制按钮区域，不处理拖动
            if (event.y < (controlBar?.height ?: 0)) {
                false
            } else {
                // 触摸时显示UI
                showUI()
                // 如果锁定，不处理拖动
                if (isLocked) {
                    false
                } else {
                    handleDragEvent(event)
                }
            }
        }

        floatingView = container
        try {
            layoutParams?.let { params ->
                windowManager?.addView(floatingView, params)
            }
            // 开始更新歌词显示
            handler.post(updateRunnable)
            // 5秒后自动隐藏UI
            resetHideUITimer()
        } catch (e: Exception) {
            Toast.makeText(this, "无法显示悬浮窗，请检查权限设置", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun observeMusicService() {
        val connection = musicServiceConnection ?: return

        // 监听歌曲变化，加载歌词
        serviceScope.launch {
            connection.currentSong.collect { song ->
                if (song != null) {
                    // 切换歌曲时，保持当前窗口位置不变
                    val savedParams = this@FloatingLyricService.layoutParams
                    loadLyric(song.uri, song.name)
                    // 确保切换歌曲后窗口位置不变
                    val currentView = floatingView
                    val currentWindowManager = windowManager
                    if (savedParams != null && currentView != null && currentWindowManager != null) {
                        try {
                            currentWindowManager.updateViewLayout(currentView, savedParams)
                        } catch (e: Exception) {
                            android.util.Log.e("FloatingLyricService", "Error updating view layout on song change: ${e.message}", e)
                        }
                    }
                } else {
                    lyricData = null
                    currentLyricText = "暂无歌词"
                }
            }
        }

        // 监听播放进度变化（通过定时器更新，这里只监听歌曲变化）
    }

    private fun loadLyric(musicUri: Uri, musicName: String) {
        serviceScope.launch(Dispatchers.IO) {
            try {
                val lyricUri = LyricParser.findLyricFile(this@FloatingLyricService, musicUri, musicName)
                if (lyricUri != null) {
                    val lyric = LyricParser.parseFromUri(this@FloatingLyricService, lyricUri)
                    lyricData = lyric
                } else {
                    lyricData = null
                    currentLyricText = "暂无歌词"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                lyricData = null
                currentLyricText = "暂无歌词"
            }
        }
    }

    private fun updateLyricDisplay() {
        val connection = musicServiceConnection ?: return
        val currentTime = connection.progress.value.toLong()
        val lyric = lyricData

        if (lyric != null && !lyric.isEmpty()) {
            val currentLineIndex = lyric.getCurrentLineIndex(currentTime)
            if (currentLineIndex >= 0 && currentLineIndex < lyric.lines.size) {
                // 当前行
                val currentLine = lyric.lines[currentLineIndex]
                currentLyricText = currentLine.text

                // 下一行（下下句）：如果当前行播放完，显示下下句；否则显示下一句
                val nextLineIndex = if (currentLineIndex + 1 < lyric.lines.size) {
                    currentLineIndex + 1
                } else {
                    -1
                }
                nextLyricText = if (nextLineIndex >= 0) {
                    lyric.lines[nextLineIndex].text
                } else {
                    ""
                }
            } else {
                currentLyricText = "暂无歌词"
                nextLyricText = ""
            }
        } else {
            currentLyricText = "暂无歌词"
            nextLyricText = ""
        }

        // 更新当前行TextView
        currentLineTextView?.let { textView ->
            val savedLayoutParams = textView.layoutParams
            textView.text = currentLyricText
            if (savedLayoutParams != null) {
                textView.layoutParams = savedLayoutParams
            }
        }

        // 更新下一行TextView
        nextLineTextView?.let { textView ->
            val savedLayoutParams = textView.layoutParams
            textView.text = nextLyricText
            if (savedLayoutParams != null) {
                textView.layoutParams = savedLayoutParams
            }
        }
    }

    /**
     * 处理拖动事件
     */
    private fun handleDragEvent(event: MotionEvent): Boolean {
        val currentParams = layoutParams
        val currentWindowManager = windowManager
        val currentFloatingView = floatingView

        if (currentParams == null || currentWindowManager == null || currentFloatingView == null) {
            return false
        }

        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = currentParams.x
                initialY = currentParams.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                true
            }
            MotionEvent.ACTION_MOVE -> {
                val newX = initialX + (event.rawX - initialTouchX).toInt()
                val newY = initialY + (event.rawY - initialTouchY).toInt()
                currentParams.x = newX
                currentParams.y = newY
                currentWindowManager.updateViewLayout(currentFloatingView, currentParams)
                true
            }
            MotionEvent.ACTION_UP -> {
                // 拖动结束时，如果锁定，确保歌词在窗口中心位置
                if (isLocked) {
                    // 锁定后，歌词应该固定在当前位置的中心
                    // 窗口位置已经更新，歌词TextView会自动居中显示
                }
                true
            }
            else -> false
        }
    }

    /**
     * 显示UI（控制按钮栏）
     */
    private fun showUI() {
        controlBar?.visibility = View.VISIBLE
        // 确保所有子View都可见
        controlBar?.let { bar ->
            for (i in 0 until bar.childCount) {
                bar.getChildAt(i).visibility = View.VISIBLE
            }
        }
        resetHideUITimer()
    }
    
    /**
     * 隐藏UI（控制按钮栏）
     */
    private fun hideUI() {
        // 只隐藏子View，保持布局高度不变
        controlBar?.let { bar ->
            for (i in 0 until bar.childCount) {
                bar.getChildAt(i).visibility = View.GONE
            }
        }
    }

    /**
     * 重置隐藏UI的定时器
     */
    private fun resetHideUITimer() {
        handler.removeCallbacks(hideUIRunnable)
        handler.postDelayed(hideUIRunnable, UI_HIDE_DELAY)
    }

    private fun hideFloatingWindow() {
        handler.removeCallbacks(updateRunnable)
        handler.removeCallbacks(hideUIRunnable)
        floatingView?.let {
            windowManager?.removeView(it)
            floatingView = null
            currentLineTextView = null
            nextLineTextView = null
            controlBar = null
            lockButton = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateRunnable)
        hideFloatingWindow()
        // 解绑ServiceConnection，避免泄漏
        musicServiceConnection?.unbind()
        musicServiceConnection = null
        layoutParams = null
        serviceScope.cancel()
    }

    companion object {
        const val ACTION_SHOW = "com.example.music_player.FloatingLyricService.SHOW"
        const val ACTION_HIDE = "com.example.music_player.FloatingLyricService.HIDE"
        const val ACTION_TOGGLE = "com.example.music_player.FloatingLyricService.TOGGLE"
    }
}
