package com.example.music_player.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryVariant,
    onPrimaryContainer = DarkPrimary,
    secondary = DarkSecondary,
    onSecondary = DarkOnPrimary,
    secondaryContainer = DarkTertiary,
    onSecondaryContainer = DarkPrimary,
    tertiary = DarkTertiary,
    onTertiary = DarkOnPrimary,
    tertiaryContainer = DarkSurfaceVariant,
    onTertiaryContainer = DarkPrimary,
    error = DarkError,
    onError = DarkOnPrimary,
    errorContainer = DarkSurfaceVariant,
    onErrorContainer = DarkError,
    background = DarkBackground, // 复杂渐变背景
    onBackground = DarkOnBackground,
    surface = DarkSurface, // 卡片/表单使用简洁的深色
    onSurface = DarkOnSurface, // 卡片上的文字使用浅色
    surfaceVariant = DarkSurfaceVariant, // 次要表面
    onSurfaceVariant = DarkOnBackground,
    outline = DarkOutline,
    outlineVariant = DarkOutline,
    scrim = DarkBackground,
    inverseSurface = DarkOnPrimary,
    inverseOnSurface = DarkBackground,
    inversePrimary = DarkPrimaryVariant
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryVariant,
    onPrimaryContainer = LightOnBackground,
    secondary = LightSecondary,
    onSecondary = LightOnPrimary,
    secondaryContainer = LightTertiary,
    onSecondaryContainer = LightOnBackground,
    tertiary = LightTertiary,
    onTertiary = LightOnPrimary,
    tertiaryContainer = LightSurfaceVariant,
    onTertiaryContainer = LightOnBackground,
    error = LightError,
    onError = LightOnPrimary,
    errorContainer = LightSurfaceVariant,
    onErrorContainer = LightError,
    background = LightBackground, // 复杂渐变背景
    onBackground = LightOnBackground,
    surface = LightSurface, // 卡片/表单使用简洁的白色
    onSurface = LightOnSurface, // 卡片上的文字使用深色
    surfaceVariant = LightSurfaceVariant, // 次要表面
    onSurfaceVariant = LightOnBackground,
    outline = LightOutline,
    outlineVariant = LightOutline,
    scrim = LightBackground,
    inverseSurface = LightOnBackground,
    inverseOnSurface = LightBackground,
    inversePrimary = LightPrimaryVariant
)

@Composable
fun Music_PlayerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is disabled to use custom color scheme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
