package com.example.music_player.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import com.example.music_player.ui.theme.ActiveColor
import com.example.music_player.ui.theme.ActiveColorLight
import com.example.music_player.ui.theme.ActiveMusicGradientDark
import com.example.music_player.ui.theme.ActiveMusicGradientLight
import com.example.music_player.ui.theme.DarkBackground
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.music_player.data.model.MusicFile
import com.example.music_player.navigation.Screen
import com.example.music_player.ui.component.BannerCarousel
import com.example.music_player.ui.component.BottomPlayerBar
import com.example.music_player.ui.component.ScrollingTitle
import com.example.music_player.ui.viewmodel.MusicListUiState
import com.example.music_player.ui.viewmodel.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserHomeScreen(
    @Suppress("UNUSED_PARAMETER") navController: NavController, 
    viewModel: UserViewModel
) {
    // navController 保留用于未来导航功能，当前未使用
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // 显示错误提示
    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    var searchActive by remember { mutableStateOf(false) }

    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        if (permissionsMap.values.all { it }) {
            viewModel.loadMusic()
        }
    }

    LaunchedEffect(Unit) {
        if (permissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {
            viewModel.loadMusic()
        } else {
            launcher.launch(permissions)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (!searchActive) {
                        Text(
                            "Music Player",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = FontFamily.Cursive
                            ),
                            fontWeight = FontWeight.Bold,
                            color = ActiveColor
                        )
                    } else {
                        SearchBar(
                            query = searchQuery,
                            onQueryChange = { viewModel.searchMusic(it) },
                            onSearchActiveChange = { searchActive = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                actions = {
                    if (!searchActive) {
                        IconButton(onClick = { searchActive = true }) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "搜索",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        IconButton(onClick = { 
                            searchActive = false
                            viewModel.searchMusic("")
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "关闭搜索",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is MusicListUiState.Idle -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "正在请求权限...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
                is MusicListUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                is MusicListUiState.Success -> {
                    if (state.musicFiles.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MusicNote,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    if (searchQuery.isNotBlank()) "未找到相关音乐" else "暂无音乐",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            }
                        }
                    } else {
                        LaunchedEffect(Unit) {
                            viewModel.loadFavoriteStatus()
                        }
                        MusicList(
                            musicFiles = state.musicFiles,
                            currentSong = currentSong,
                            viewModel = viewModel,
                            onItemClick = { index ->
                                viewModel.playMusic(index)
                            }
                        )
                    }
                }
                is MusicListUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Error,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                state.message,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    @Suppress("UNUSED_PARAMETER") onSearchActiveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { 
            Text(
                "搜索音乐...",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            ) 
        },
        leadingIcon = {
            Icon(
                Icons.Filled.Search, 
                contentDescription = "搜索",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        Icons.Filled.Clear, 
                        contentDescription = "清除",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedIndicatorColor = ActiveColor,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            cursorColor = ActiveColor
        )
    )
}

@Composable
fun MusicList(
    musicFiles: List<MusicFile>, 
    currentSong: MusicFile?, 
    viewModel: UserViewModel,
    onItemClick: (Int) -> Unit
) {
    val favoriteMap by viewModel.favoriteMap.collectAsStateWithLifecycle()
    
    // 轮播图数据（通过 Retrofit 获取，当前使用本地数据模拟）
    val bannersState by viewModel.banners.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        viewModel.loadBanners()
    }
    
    // 将 BannerData 转换为 BannerItem
    val banners = remember(bannersState) {
        bannersState.map { bannerData ->
            com.example.music_player.ui.component.BannerItem(
                id = bannerData.id,
                title = bannerData.title,
                imageUrl = bannerData.imageUrl,
                linkUrl = bannerData.linkUrl
            )
        }
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 轮播图
        item {
            BannerCarousel(
                banners = banners,
                modifier = Modifier.padding(bottom = 16.dp),
                onBannerClick = { banner ->
                    // 处理轮播图点击事件
                    android.util.Log.d("BannerCarousel", "Clicked banner: ${banner.title}")
                }
            )
        }
        
        itemsIndexed(musicFiles) { index, musicFile ->
            val isFavorite = favoriteMap[musicFile.uri.toString()] == true
            MusicItem(
                index = index + 1,
                musicFile = musicFile,
                isPlaying = currentSong != null && currentSong.name == musicFile.name && currentSong.uri == musicFile.uri,
                isFavorite = isFavorite,
                onFavoriteClick = { viewModel.toggleFavorite(musicFile) },
                onClick = { onItemClick(index) }
            )
        }
    }
}

@Composable
fun MusicItem(
    index: Int, 
    musicFile: MusicFile, 
    isPlaying: Boolean,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit
) {
    // 通过比较背景颜色的值来判断当前主题
    val isDarkTheme = MaterialTheme.colorScheme.background.value == DarkBackground.value
    val activeGradient = if (isDarkTheme) ActiveMusicGradientDark else ActiveMusicGradientLight
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(
                if (isPlaying) {
                    Modifier.background(
                        brush = activeGradient,
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(12.dp),
        color = if (isPlaying) 
            androidx.compose.ui.graphics.Color.Transparent
            else 
            MaterialTheme.colorScheme.surface,
        tonalElevation = if (isPlaying) 0.dp else 1.dp,
        shadowElevation = if (isPlaying) 0.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 序号（播放时显示三角符号，否则显示数字）
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isPlaying) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "正在播放",
                        tint = if (isDarkTheme) 
                            MaterialTheme.colorScheme.outline
                        else 
                            MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                Text(
                    text = index.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                )
                }
            }
            
            // 音乐信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                if (isPlaying) {
                    ScrollingTitle(
                        text = musicFile.name.substringBeforeLast("."),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) ActiveColorLight else ActiveColor,
                        isPlaying = true
                    )
                } else {
                Text(
                    text = musicFile.name.substringBeforeLast("."),
                    style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                }
                if (isPlaying) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "正在播放",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            }
            
            // 喜欢按钮
            IconButton(
                onClick = { onFavoriteClick() },
                modifier = Modifier.size(40.dp)
            ) {
                Image(
                    painter = if (isFavorite) {
                        painterResource(id = com.example.music_player.R.drawable.ic_heart_filled)
                    } else {
                        painterResource(id = com.example.music_player.R.drawable.ic_heart_empty)
                    },
                    contentDescription = if (isFavorite) "取消喜欢" else "喜欢",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

