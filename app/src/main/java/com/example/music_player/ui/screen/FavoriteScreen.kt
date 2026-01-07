package com.example.music_player.ui.screen

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.music_player.ui.theme.ActiveColor
import com.example.music_player.ui.theme.ActiveColorLight
import com.example.music_player.ui.theme.ActiveMusicGradientDark
import com.example.music_player.ui.theme.ActiveMusicGradientLight
import com.example.music_player.ui.theme.DarkBackground
import androidx.navigation.NavController
import com.example.music_player.data.model.MusicFile
import com.example.music_player.data.local.entity.FavoriteMusic
import com.example.music_player.navigation.Screen
import com.example.music_player.service.MusicServiceConnection
import com.example.music_player.ui.component.BottomPlayerBar
import com.example.music_player.ui.component.ScrollingTitle
import com.example.music_player.ui.viewmodel.ProfileViewModel
import androidx.compose.ui.text.font.FontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteScreen(
    navController: NavController,
    viewModel: ProfileViewModel,
    musicServiceConnection: MusicServiceConnection
) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val currentSong by musicServiceConnection.currentSong.collectAsStateWithLifecycle()
    val isPlaying by musicServiceConnection.isPlaying.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        viewModel.loadFavorites()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Favorite Music",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = FontFamily.Cursive
                        ),
                        fontWeight = FontWeight.Bold,
                        color = ActiveColor
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (favorites.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "暂无喜欢的音乐",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(favorites) { favorite ->
                            val isCurrentlyPlaying = currentSong != null && 
                                currentSong?.name == favorite.songName && 
                                currentSong?.uri?.toString() == favorite.songPath &&
                                isPlaying
                            FavoriteItem(
                                favorite = favorite,
                                isPlaying = isCurrentlyPlaying,
                                onPlayClick = {
                                    // 播放歌曲：将整个收藏列表作为播放列表
                                    try {
                                        val musicFiles = favorites.map { fav ->
                                            MusicFile(
                                                name = fav.songName,
                                                uri = Uri.parse(fav.songPath)
                                            )
                                        }
                                        val position = favorites.indexOf(favorite)
                                        musicServiceConnection.playMusic(musicFiles, position)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                },
                                onRemoveClick = {
                                    viewModel.removeFavorite(favorite)
                                }
                            )
                        }
                    }
                    
                    // 底部播放栏
                    currentSong?.let { song ->
                        BottomPlayerBar(
                            song = song,
                            isPlaying = isPlaying,
                            onTogglePlayPause = { musicServiceConnection.togglePlayPause() },
                            onClick = { navController.navigate(Screen.Player.route) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FavoriteItem(
    favorite: FavoriteMusic,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    val isDarkTheme = MaterialTheme.colorScheme.background.value == DarkBackground.value
    val activeGradient = if (isDarkTheme) ActiveMusicGradientDark else ActiveMusicGradientLight
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlayClick)
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
            Color.Transparent
        else 
            MaterialTheme.colorScheme.surface,
        tonalElevation = if (isPlaying) 0.dp else 1.dp,
        shadowElevation = if (isPlaying) 0.dp else 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                if (isPlaying) {
                    ScrollingTitle(
                        text = favorite.songName.substringBeforeLast("."),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) ActiveColorLight else ActiveColor,
                        isPlaying = true
                    )
                } else {
                    Text(
                        text = favorite.songName.substringBeforeLast("."),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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
            
            // 取消喜欢按钮
            IconButton(
                onClick = onRemoveClick,
                modifier = Modifier.size(40.dp)
            ) {
                Image(
                    painter = painterResource(id = com.example.music_player.R.drawable.ic_heart_filled),
                    contentDescription = "取消喜欢",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

