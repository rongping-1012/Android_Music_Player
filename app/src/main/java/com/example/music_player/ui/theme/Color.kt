package com.example.music_player.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// 亮色主题配色
val LightBackground = Color(0xFFF4E5F9) // 主色调-背景主色（复杂渐变背景）
val LightSurface = Color(0xFFFFFFFF) // 卡片/表单使用简洁的白色
val LightSurfaceVariant = Color(0xFFF9F5FC) // 次要表面（稍微带一点色调）
val LightPrimary = Color(0xFF9B6ED3) // 主色调-图标/重点文字
val LightPrimaryVariant = Color(0xFF8A4FFF) // 主色调-交互/高亮
val LightSecondary = Color(0xFFFCC8E8) // 辅助色-装饰/渐变
val LightTertiary = Color(0xFFC4B5FD) // 辅助色-卡片/渐变层次
val LightOnPrimary = Color(0xFFFFFFFF) // 辅助色-卡片/反白
val LightOnBackground = Color(0xFF6B489E) // 辅助色-深色文字/阴影
val LightOnSurface = Color(0xFF1C1B1F) // 卡片上的文字颜色（深色，确保可读性）
val LightError = Color(0xFFE64398) // 点缀色-交互标识
val LightOutline = Color(0xFFE0E0E0) // 点缀色-分割线/次要文字（更浅）

// 暗色主题配色
val DarkBackground = Color(0xFF1A1226) // 主色调-背景主色（复杂渐变背景）
val DarkSurface = Color(0xFF2A1F3A) // 卡片/表单使用简洁的深色（不要太花哨）
val DarkSurfaceVariant = Color(0xFF251B33) // 次要表面（稍微深一点）
val DarkPrimary = Color(0xFFD8BFF0) // 主色调-图标/重点文字
val DarkPrimaryVariant = Color(0xFF8A4FFF) // 主色调-交互/高亮
val DarkSecondary = Color(0xFFFCC8E8) // 辅助色-装饰/渐变（低透明度叠加）
val DarkTertiary = Color(0xFF302244) // 辅助色-卡片/渐变层次
val DarkOnPrimary = Color(0xFFF4E5F9) // 辅助色-卡片/反白
val DarkOnBackground = Color(0xFFD8BFF0) // 辅助色-深色文字/阴影
val DarkOnSurface = Color(0xFFE6E1E5) // 卡片上的文字颜色（浅色，确保可读性）
val DarkError = Color(0xFFE64398) // 点缀色-交互标识
val DarkOutline = Color(0xFF49454F) // 点缀色-分割线/次要文字（更浅）

val ActiveColor = Color(0xFFB4773B)
val ActiveColorLight = Color(0xFFFFA955)

// 横向渐变 - 用于音乐激活状态（亮色主题）
val ActiveMusicGradientLight = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFFF4E5F9),
        Color(0xFFFCC8E8),
        Color(0xFFD8BFF0),
        Color(0xFFC4B5FD)
    ),
    startX = 0f,
    endX = Float.POSITIVE_INFINITY
)

// 横向渐变 - 用于音乐激活状态（暗色主题）
val ActiveMusicGradientDark = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFF1A1226),  // DarkBackground
        Color(0xFF251B33),  // DarkSurfaceVariant
        Color(0xFF2A1F3A),  // DarkSurface
        Color(0xFF302244),  // DarkTertiary
        Color(0xFF3A2D4E)   // 稍亮的深色
    ),
    startX = 0f,
    endX = Float.POSITIVE_INFINITY
)
