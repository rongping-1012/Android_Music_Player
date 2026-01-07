package com.example.music_player

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.music_player.data.UserService
import com.example.music_player.navigation.AppNavigation
import com.example.music_player.ui.theme.Music_PlayerTheme
import com.example.music_player.utils.AssetMusicManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val userService = remember { UserService(applicationContext) }
            val currentUsername by userService.currentUsername.collectAsStateWithLifecycle(initialValue = "")
            val scope = rememberCoroutineScope()
            
            // 根据当前用户加载主题偏好
            var darkTheme by remember { mutableStateOf(false) }
            
            val context = LocalContext.current
            
            // 检查存储权限（Android 10 及以下需要 WRITE_EXTERNAL_STORAGE）
            val writePermission = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            } else {
                null // Android 11+ 不需要 WRITE_EXTERNAL_STORAGE 权限来写入公共 Music 目录
            }
            
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted || writePermission == null) {
                    scope.launch {
                        AssetMusicManager.copyMusicFiles(context)
                    }
                }
            }
            
            // 首次启动时复制音乐文件
            LaunchedEffect(Unit) {
                if (writePermission == null || ContextCompat.checkSelfPermission(context, writePermission) == PackageManager.PERMISSION_GRANTED) {
                    // 已有权限或不需要权限，直接复制
                    scope.launch {
                        AssetMusicManager.copyMusicFiles(context)
                    }
                } else {
                    // 请求权限
                    permissionLauncher.launch(writePermission)
                }
            }
            
            LaunchedEffect(currentUsername) {
                // 管理员账号强制使用亮色主题
                darkTheme = if (currentUsername == "admin") {
                    false
                } else {
                    userService.getDarkMode()
                }
            }

            Music_PlayerTheme(darkTheme = darkTheme) {
                AppNavigation(
                    darkTheme = darkTheme,
                    toggleTheme = {
                        // 只有在登录状态下才允许切换主题（管理员页面不能切换）
                        if (currentUsername.isNotBlank() && currentUsername != "admin") {
                        scope.launch {
                                val newDarkMode = !darkTheme
                                userService.setDarkMode(newDarkMode)
                                darkTheme = newDarkMode
                            }
                        }
                    }
                )
    }
}
    }
}