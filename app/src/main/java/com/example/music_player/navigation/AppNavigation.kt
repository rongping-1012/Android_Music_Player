package com.example.music_player.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.music_player.data.local.AppDatabase
import com.example.music_player.data.repository.MusicRepository
import com.example.music_player.data.repository.UserRepository
import com.example.music_player.data.UserService
import com.example.music_player.service.MusicServiceConnection
import com.example.music_player.ui.screen.*
import com.example.music_player.ui.theme.Music_PlayerTheme
import com.example.music_player.ui.viewmodel.*

@Composable
fun AppNavigation(darkTheme: Boolean, toggleTheme: () -> Unit) {
    val navController = rememberNavController()
    val context = LocalContext.current

    val userService = remember { UserService(context) }
    val userRepository = remember { UserRepository(AppDatabase.getDatabase(context).userDao(), context) }
    val musicServiceConnection = remember { MusicServiceConnection(context) }

    NavHost(navController = navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) {
            SplashScreen(navController = navController, userService = userService)
        }
        composable(Screen.Login.route) {
            val loginViewModel: LoginViewModel = viewModel(factory = LoginViewModelFactory(userService))
            // 登录页面强制使用亮色主题
            Music_PlayerTheme(darkTheme = false) {
            LoginScreen(navController = navController, viewModel = loginViewModel)
            }
        }
        composable(Screen.Register.route) {
            val loginViewModel: LoginViewModel = viewModel(factory = LoginViewModelFactory(userService))
            // 注册页面强制使用亮色主题
            Music_PlayerTheme(darkTheme = false) {
            RegisterScreen(navController = navController, viewModel = loginViewModel)
            }
        }
        composable(Screen.Main.route) {
            val musicRepository = MusicRepository(context)
            val userViewModel: UserViewModel = viewModel(factory = UserViewModelFactory(musicRepository, musicServiceConnection, context))
            val profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory(userService, musicServiceConnection, musicRepository, context))
            MainScreen(
                navController = navController,
                userViewModel = userViewModel,
                profileViewModel = profileViewModel,
                darkTheme = darkTheme,
                onToggleTheme = toggleTheme
            )
        }
        composable(Screen.UserHome.route) {
            val musicRepository = MusicRepository(context)
            val userViewModel: UserViewModel = viewModel(factory = UserViewModelFactory(musicRepository, musicServiceConnection, context))
            UserHomeScreen(navController = navController, viewModel = userViewModel)
        }
        composable(Screen.AdminHome.route) {
            val adminViewModel: AdminViewModel = viewModel(factory = AdminViewModelFactory(userRepository, userService, musicServiceConnection, context))
            // 管理员页面强制使用亮色主题
            Music_PlayerTheme(darkTheme = false) {
            AdminScreen(navController = navController, viewModel = adminViewModel)
            }
        }
        composable(Screen.Profile.route) {
            val musicRepository = MusicRepository(context)
            val profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory(userService, musicServiceConnection, musicRepository, context))
            ProfileScreen(navController = navController, viewModel = profileViewModel, darkTheme = darkTheme, onToggleTheme = toggleTheme)
        }
        composable(Screen.Player.route) {
            val musicRepository = MusicRepository(context)
            val userViewModel: UserViewModel = viewModel(factory = UserViewModelFactory(musicRepository, musicServiceConnection, context))
            val playerViewModel: PlayerViewModel = viewModel(factory = PlayerViewModelFactory(musicServiceConnection, context))
            PlayerScreen(
                navController = navController, 
                viewModel = playerViewModel, 
                userViewModel = userViewModel,
                darkTheme = darkTheme,
                onToggleTheme = toggleTheme
            )
        }
        composable(Screen.PlayHistory.route) {
            val musicRepository = MusicRepository(context)
            val profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory(userService, musicServiceConnection, musicRepository, context))
            PlayHistoryScreen(navController = navController, viewModel = profileViewModel, musicServiceConnection = musicServiceConnection)
        }
        composable(Screen.Favorite.route) {
            val musicRepository = MusicRepository(context)
            val profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory(userService, musicServiceConnection, musicRepository, context))
            FavoriteScreen(navController = navController, viewModel = profileViewModel, musicServiceConnection = musicServiceConnection)
        }
        composable(Screen.About.route) {
            AboutScreen(navController = navController)
        }
    }
}