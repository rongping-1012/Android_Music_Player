package com.example.music_player.ui.screen

import android.content.Intent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Brightness2
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.music_player.R
import com.example.music_player.data.model.MusicFile
import com.example.music_player.service.FloatingLyricService
import com.example.music_player.service.MusicService
import com.example.music_player.ui.component.LyricDisplay
import com.example.music_player.ui.component.ScrollingTitle
import com.example.music_player.ui.theme.ActiveColor
import com.example.music_player.ui.viewmodel.PlayerViewModel
import com.example.music_player.ui.viewmodel.UserViewModel
import com.example.music_player.utils.PermissionUtils
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlayerScreen(
    navController: NavController, 
    viewModel: PlayerViewModel, 
    userViewModel: UserViewModel,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val context = LocalContext.current
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()
    val playMode by viewModel.playMode.collectAsStateWithLifecycle()
    val playCount by viewModel.playCount.collectAsStateWithLifecycle()
    val lyricData by viewModel.lyricData.collectAsStateWithLifecycle()
    val favoriteMap by userViewModel.favoriteMap.collectAsStateWithLifecycle()
    
    var showPlaylist by remember { mutableStateOf(false) }
    var playlist by remember { mutableStateOf<List<MusicFile>>(emptyList()) }
    var currentIndex by remember { mutableIntStateOf(-1) }

    // 页面状态：0=播放控制模块，1=歌词展示模块
    val pagerState = rememberPagerState(pageCount = { 2 }, initialPage = 0)
    
    // 悬浮窗控制
    fun toggleFloatingLyric() {
        // 检查权限
        if (!PermissionUtils.canDrawOverlays(context)) {
            // 请求权限（跳转到系统设置页面）
            PermissionUtils.requestOverlayPermission(context)
            // 提示用户授权后需要重新点击按钮
            android.widget.Toast.makeText(
                context,
                "请在设置中授予悬浮窗权限，然后重新点击桌面歌词按钮",
                android.widget.Toast.LENGTH_LONG
            ).show()
            return
        }
        
        // 有权限，启动或切换悬浮窗服务
        val intent = Intent(context, FloatingLyricService::class.java).apply {
            action = FloatingLyricService.ACTION_TOGGLE
        }
        // API 26+ 使用 startForegroundService，否则使用 startService
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                @Suppress("DEPRECATION")
                context.startService(intent)
            }
        } catch (e: Exception) {
            android.widget.Toast.makeText(
                context,
                "启动桌面歌词失败: ${e.message}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    LaunchedEffect(Unit) {
        userViewModel.loadFavoriteStatus()
    }

    Scaffold(
        topBar = {
            Box(modifier = Modifier.fillMaxWidth()) {
            TopAppBar(
                title = {
                    Text(
                        "Now Playing",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = FontFamily.Cursive
                        ),
                        fontWeight = FontWeight.Bold,
                        color = ActiveColor
                    )
                },
                    actions = {
                        // 主题切换按钮
                        IconButton(onClick = onToggleTheme) {
                            Icon(
                                imageVector = if (darkTheme) Icons.Filled.WbSunny else Icons.Filled.Brightness2,
                                contentDescription = if (darkTheme) "切换到亮色主题" else "切换到暗色主题",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        // 桌面歌词按钮
                        IconButton(onClick = { toggleFloatingLyric() }) {
                            Icon(Icons.Filled.PictureInPicture, contentDescription = "桌面歌词")
                        }
                    },
                navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
                // 引导指示器居中显示在Now Playing和主题按钮之间
                PageIndicator(
                    currentPage = pagerState.currentPage,
                    pageCount = 2,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { page ->
            when (page) {
                0 -> PlayControlModule(
                    isPlaying = isPlaying,
                    currentSong = currentSong,
                    progress = progress,
                    duration = duration,
                    playMode = playMode,
                    playCount = playCount,
                    favoriteMap = favoriteMap,
                    viewModel = viewModel,
                    userViewModel = userViewModel,
                    onShowPlaylist = {
                        playlist = viewModel.getPlaylist()
                        currentIndex = viewModel.getCurrentIndex()
                        showPlaylist = true
                    }
                )
                1 -> LyricModule(
                    currentSong = currentSong,
                    lyricData = lyricData,
                    currentTime = progress.toLong(),
                    onSeekTo = { timeMs -> viewModel.seekToTime(timeMs) }
                )
            }
        }

        if (showPlaylist) {
            PlaylistOverlay(
                playlist = playlist,
                currentIndex = currentIndex,
                onSelect = { index ->
                    viewModel.playAt(index)
                    showPlaylist = false
                },
                onDismiss = { showPlaylist = false }
            )
        }
    }
}

/**
 * 播放控制模块
 */
@Composable
private fun PlayControlModule(
    isPlaying: Boolean,
    currentSong: MusicFile?,
    progress: Int,
    duration: Int,
    playMode: MusicService.PlayMode,
    playCount: Int,
    favoriteMap: Map<String, Boolean>,
    viewModel: PlayerViewModel,
    userViewModel: UserViewModel,
    onShowPlaylist: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 唱片旋转动画
            RotatingRecord(isPlaying = isPlaying)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            val song = currentSong

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (song != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ScrollingTitle(
                            text = song.name.substringBeforeLast("."),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            isPlaying = isPlaying,
                            modifier = Modifier.weight(1f)
                        )
                        // 爱心图标放在歌曲名后面
                        val isFav = favoriteMap[song.uri.toString()] == true
                        IconButton(
                            onClick = { userViewModel.toggleFavorite(song) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = if (isFav) R.drawable.ic_heart_filled else R.drawable.ic_heart_empty),
                                contentDescription = "Favorite",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(24.dp)
                        )
                        }
                    }
                    } else {
                        Text(
                            text = "No song playing",
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Start,
                            maxLines = 1
                        )
                    }
                    if (song != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Surface(
                                color = ActiveColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(999.dp),
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Headset,
                                        contentDescription = null,
                                        tint = ActiveColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "$playCount",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ActiveColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))

            Slider(
                value = progress.toFloat(),
                onValueChange = { newPosition -> viewModel.seekTo(newPosition.toInt()) },
                valueRange = 0f..(duration.toFloat().coerceAtLeast(0f)),
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatTime(progress))
                Text(formatTime(duration))
            }
            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
            // 左侧按钮组
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 播放模式按钮
                IconButton(onClick = { viewModel.cyclePlayMode() }) {
                    Icon(getPlayModeIcon(playMode), contentDescription = "Play Mode")
                }
            }
            
            // 上一首按钮
                IconButton(onClick = { viewModel.playPrevious() }) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(48.dp))
                }
            
            // 中间播放/暂停按钮
            IconButton(
                onClick = { viewModel.togglePlayPause() },
                modifier = Modifier.size(72.dp)
            ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.PauseCircle else Icons.Filled.PlayCircle,
                        contentDescription = "Play/Pause",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            
            // 下一首按钮
                IconButton(onClick = { viewModel.playNext() }) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Next", modifier = Modifier.size(48.dp))
                }

            // 左侧按钮组
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 播放列表按钮
                IconButton(onClick = onShowPlaylist) {
                    Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "播放列表")
                }
            }
        }
    }
}

/**
 * 歌词展示模块
 */
@Composable
private fun LyricModule(
    currentSong: MusicFile?,
    lyricData: com.example.music_player.data.model.LyricData,
    currentTime: Long,
    onSeekTo: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 歌曲名称（去掉文件扩展名）
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = currentSong?.name?.substringBeforeLast(".") ?: "No song playing",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        // 歌词滚动区域
        LyricDisplay(
            lyricData = lyricData,
            currentTime = currentTime,
            onSeekTo = onSeekTo,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * 页面指示器（顶部引导锚点）
 * active状态显示为长椭圆，非active状态显示为小圆点
 */
@Composable
private fun PageIndicator(
    currentPage: Int,
    pageCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { page ->
            val isSelected = page == currentPage
            Box(
                modifier = Modifier
                    .height(4.dp)
                    .width(if (isSelected) 24.dp else 4.dp)
                    .background(
                        color = if (isSelected) ActiveColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

@Composable
private fun PlaylistOverlay(
    playlist: List<MusicFile>,
    currentIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        androidx.compose.material3.Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .padding(24.dp)
                .clickable(enabled = false) {}, // absorb clicks
            shape = androidx.compose.material3.MaterialTheme.shapes.large,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "播放列表",
                    style = MaterialTheme.typography.titleMedium,
                    color = ActiveColor
                )
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(playlist) { index, item ->
                        val isCurrent = index == currentIndex
                        androidx.compose.material3.Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(index) },
                            shape = MaterialTheme.shapes.medium,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.surface,
                            tonalElevation = if (isCurrent) 2.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = (index + 1).toString(),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isCurrent) ActiveColor else MaterialTheme.colorScheme.onSurface
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name.substringBeforeLast("."),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (isCurrent) {
                                    Text(
                                        text = "正在播放",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ActiveColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(timeMs: Int): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(timeMs.toLong())
    val seconds = TimeUnit.MILLISECONDS.toSeconds(timeMs.toLong()) - TimeUnit.MINUTES.toSeconds(minutes)
    return String.format("%02d:%02d", minutes, seconds)
}

private fun getPlayModeIcon(mode: MusicService.PlayMode): ImageVector {
    return when (mode) {
        MusicService.PlayMode.SEQUENTIAL -> Icons.Filled.Repeat
        MusicService.PlayMode.SHUFFLE -> Icons.Filled.Shuffle
        MusicService.PlayMode.REPEAT_ONE -> Icons.Filled.RepeatOne
    }
}

@Composable
fun RotatingRecord(isPlaying: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "record_rotation")
    
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 3000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Box(
        modifier = Modifier.size(280.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = com.example.music_player.R.drawable.record),
            contentDescription = "唱片",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = if (isPlaying) rotationAngle else 0f
                },
            contentScale = ContentScale.Fit
        )
    }
}
