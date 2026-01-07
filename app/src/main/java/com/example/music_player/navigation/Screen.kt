package com.example.music_player.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object Main : Screen("main")
    object UserHome : Screen("user_home")
    object AdminHome : Screen("admin_home")
    object Profile : Screen("profile")
    object Player : Screen("player")
    object PlayHistory : Screen("play_history")
    object Favorite : Screen("favorite")
    object About : Screen("about")
}

