package com.example.music_player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.music_player.data.UserService
import com.example.music_player.navigation.AppNavigation
import com.example.music_player.ui.theme.Music_PlayerTheme
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