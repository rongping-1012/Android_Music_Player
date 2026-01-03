package com.example.music_player.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.music_player.service.MusicService
import com.example.music_player.ui.theme.ActiveColor
import com.example.music_player.ui.viewmodel.PlayerViewModel
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(navController: NavController, viewModel: PlayerViewModel) {
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()
    val playMode by viewModel.playMode.collectAsStateWithLifecycle()
    var showVolumeDialog by remember { mutableStateOf(false) }
    var currentVolume by remember { mutableFloatStateOf(viewModel.getVolume()) }

    Scaffold(
        topBar = {
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
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 唱片旋转动画
            RotatingRecord(isPlaying = isPlaying)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = currentSong?.name ?: "No song playing",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
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
                IconButton(onClick = { viewModel.cyclePlayMode() }) {
                    Icon(getPlayModeIcon(playMode), contentDescription = "Play Mode")
                }
                IconButton(onClick = { viewModel.playPrevious() }) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(48.dp))
                }
                IconButton(onClick = { viewModel.togglePlayPause() }, modifier = Modifier.size(72.dp)) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.PauseCircle else Icons.Filled.PlayCircle,
                        contentDescription = "Play/Pause",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                IconButton(onClick = { viewModel.playNext() }) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Next", modifier = Modifier.size(48.dp))
                }
                // 音量调节按钮（和播放方式按钮大小一致）
                IconButton(onClick = { showVolumeDialog = true }) {
                    Icon(Icons.Filled.VolumeUp, contentDescription = "音量")
                }
            }
        }
        
        // 音量调节对话框
        if (showVolumeDialog) {
            VolumeControlDialog(
                currentVolume = currentVolume,
                onVolumeChange = { newVolume ->
                    currentVolume = newVolume
                    viewModel.setVolume(newVolume)
                },
                onDismiss = { showVolumeDialog = false }
            )
        }
    }
}

@Composable
fun VolumeControlDialog(
    currentVolume: Float,
    onVolumeChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("音量调节") },
        text = {
            Column {
                Slider(
                    value = currentVolume,
                    onValueChange = onVolumeChange,
                    valueRange = 0f..1f,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0%")
                    Text("${(currentVolume * 100).toInt()}%")
                    Text("100%")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确定")
            }
        }
    )
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
