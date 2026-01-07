package com.example.music_player.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.runtime.DisposableEffect
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.example.music_player.navigation.Screen
import com.example.music_player.ui.component.BottomPlayerBar
import com.example.music_player.ui.theme.ActiveColor
import com.example.music_player.ui.theme.ActiveColorLight
import com.example.music_player.ui.viewmodel.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController,
    userViewModel: UserViewModel,
    profileViewModel: com.example.music_player.ui.viewmodel.ProfileViewModel,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(0) } // 默认显示"音乐"Tab
    
    // 监听导航返回，如果从 Favorite 或 PlayHistory 返回，切换到"我的"Tab
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // 使用 DisposableEffect 监听导航变化
    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { controller, destination, _ ->
            if (destination.route == Screen.Main.route) {
                // 检查上一个路由是否是 Favorite 或 PlayHistory
                val previousEntry = controller.previousBackStackEntry
                if (previousEntry?.destination?.route == Screen.Favorite.route || 
                    previousEntry?.destination?.route == Screen.PlayHistory.route) {
                    selectedTab = 1 // 切换到"我的"Tab
                }
            }
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }
    val isPlaying by userViewModel.isPlaying.collectAsStateWithLifecycle()
    val currentSong by userViewModel.currentSong.collectAsStateWithLifecycle()
    
    // 两次返回退出程序功能
    val context = LocalContext.current
    var backPressTime by remember { mutableLongStateOf(0L) }
    val EXIT_INTERVAL = 2000L // 2秒
    
    // 检查是否在主页面（没有子页面打开）
    val isAtMainScreen = currentRoute == Screen.Main.route
    
    // 处理返回键事件
    BackHandler(enabled = isAtMainScreen) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - backPressTime > EXIT_INTERVAL) {
            // 第一次按返回键，显示提示
            backPressTime = currentTime
            Toast.makeText(context, "再按一次退出程序", Toast.LENGTH_SHORT).show()
        } else {
            // 2秒内再次按返回键，退出程序
            (context as? androidx.activity.ComponentActivity)?.finish()
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationBarItem(
                    icon = { 
                        Icon(
                            Icons.Filled.MusicNote, 
                            contentDescription = "音乐",
                            tint = if (selectedTab == 0) ActiveColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        ) 
                    },
                    label = { 
                        Text(
                            "音乐",
                            color = if (selectedTab == 0) ActiveColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        ) 
                    },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ActiveColor,
                        selectedTextColor = ActiveColor,
                        indicatorColor = ActiveColorLight.copy(alpha = 0.3f)
                    )
                )
                NavigationBarItem(
                    icon = { 
                        Icon(
                            Icons.Filled.Person, 
                            contentDescription = "我的",
                            tint = if (selectedTab == 1) ActiveColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        ) 
                    },
                    label = { 
                        Text(
                            "我的",
                            color = if (selectedTab == 1) ActiveColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        ) 
                    },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ActiveColor,
                        selectedTextColor = ActiveColor,
                        indicatorColor = ActiveColorLight.copy(alpha = 0.3f)
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (darkTheme) {
                        Brush.verticalGradient(
                            colors = listOf(
                                androidx.compose.ui.graphics.Color(0xFF1A1226),
                                androidx.compose.ui.graphics.Color(0xFF2A1F3A),
                                androidx.compose.ui.graphics.Color(0xFF302244)
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(
                                androidx.compose.ui.graphics.Color(0xFFF4E5F9),
                                androidx.compose.ui.graphics.Color(0xFFFCC8E8),
                                androidx.compose.ui.graphics.Color(0xFFD8BFF0),
                                androidx.compose.ui.graphics.Color(0xFFC4B5FD)
                            )
                        )
                    }
                )
        ) {
            Column(modifier = Modifier.padding(paddingValues)) {
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> MusicTabScreen(navController, userViewModel)
                        1 -> ProfileTabScreen(navController, profileViewModel, darkTheme, onToggleTheme)
                    }
                }
            
                // 底部播放栏
                currentSong?.let { song ->
                    BottomPlayerBar(
                        song = song,
                        isPlaying = isPlaying,
                        onTogglePlayPause = { userViewModel.togglePlayPause() },
                        onClick = { navController.navigate(Screen.Player.route) }
                    )
                }
            }
        }
    }
}

@Composable
fun MusicTabScreen(navController: NavController, viewModel: UserViewModel) {
    UserHomeScreen(navController, viewModel)
}

@Composable
fun ProfileTabScreen(
    navController: NavController,
    viewModel: com.example.music_player.ui.viewmodel.ProfileViewModel,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    ProfileScreen(navController, viewModel, darkTheme, onToggleTheme)
}
