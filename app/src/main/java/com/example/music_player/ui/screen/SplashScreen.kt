package com.example.music_player.ui.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import com.example.music_player.R
import com.example.music_player.data.UserService
import com.example.music_player.navigation.Screen
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(navController: NavController, userService: UserService) {
    val scale = remember { Animatable(0f) }
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(key1 = true) {
        coroutineScope {
            // 并行执行缩放和旋转
            launch {
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 2000)
                )
            }
            launch {
                rotation.animateTo(
                    targetValue = 720f, // 旋转两圈
                    animationSpec = tween(durationMillis = 2000)
                )
            }
        }
        // 动画结束后，根据登录状态决定跳转页面
        val username = userService.currentUsername.first()
        val targetRoute = when {
            username.isBlank() -> Screen.Login.route
            username == "admin" -> Screen.AdminHome.route
            else -> Screen.Main.route
        }
        navController.navigate(targetRoute) {
            popUpTo(Screen.Splash.route) { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF9B6ED3), // 0%
                        Color(0xFFC4B5FD), // 24%
                        Color(0xFFD8BFF0), // 48%
                        Color(0xFFFCC8E8), // 69%
                        Color(0xFFF4E5F9)  // 100%
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
            Image(
                painter = painterResource(id = R.drawable.ic_baseline_music_background_white),
                contentDescription = "App Logo",
                modifier = Modifier
                    .scale(scale.value)
                    .rotate(rotation.value)
            )
    }
}

