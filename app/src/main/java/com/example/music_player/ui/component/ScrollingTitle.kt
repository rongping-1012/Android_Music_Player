package com.example.music_player.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset

@Composable
fun ScrollingTitle(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
    fontWeight: FontWeight? = null,
    color: Color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
    isPlaying: Boolean = false
) {
    var textWidth by remember { mutableStateOf(0f) }
    var containerWidth by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    
    // 正在播放时总是滚动
    val shouldScroll = isPlaying && containerWidth > 0f && textWidth > 0f
    
    // 计算是否需要滚动（文本超出容器）
    val needsScrolling = remember(textWidth, containerWidth) {
        textWidth > containerWidth && containerWidth > 0f
    }
    
    // 计算滚动距离：容器宽度 + 文本宽度（从右侧完全移出到左侧完全移出）
    val scrollDistance = remember(textWidth, containerWidth) {
        if (containerWidth > 0f && textWidth > 0f) {
            containerWidth + textWidth + 50f // 从右侧到左侧完全移出
        } else {
            0f
        }
    }
    
    // 跟踪是否是第一次播放
    var isFirstPlay by remember { mutableStateOf(true) }
    
    // 当播放状态改变时，重置第一次播放标志
    LaunchedEffect(isPlaying) {
        if (!isPlaying) {
            isFirstPlay = true
        }
    }
    
    // 创建无限循环滚动动画
    val infiniteTransition = rememberInfiniteTransition(label = "marquee")
    
    // 计算动画持续时间（滚动速度约50像素/秒）
    val animationDuration = remember(scrollDistance, shouldScroll) {
        if (shouldScroll && scrollDistance > 0f) {
            // 根据滚动距离计算持续时间，最小4000ms，最大20000ms
            (scrollDistance / 50f * 1000f).toInt().coerceIn(4000, 20000)
        } else {
            5000 // 默认5秒
        }
    }
    
    // 循环滚动动画
    val scrollOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (shouldScroll && scrollDistance > 0f) scrollDistance else 0f,
        animationSpec = if (shouldScroll && scrollDistance > 0f) {
            infiniteRepeatable(
                animation = tween(
                    durationMillis = animationDuration,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart // 循环播放
            )
        } else {
            // 不滚动时，使用一个极短的动画
            infiniteRepeatable(
                animation = tween(
                    durationMillis = 1,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            )
        },
        label = "scrollOffset"
    )
    
    // 监听动画完成，标记第一次播放结束
    LaunchedEffect(scrollOffset, scrollDistance) {
        if (shouldScroll && scrollDistance > 0f && scrollOffset >= scrollDistance - 1f && isFirstPlay) {
            isFirstPlay = false
        }
    }
    
    Box(
        modifier = modifier
            .clipToBounds()
            .onGloballyPositioned { coordinates ->
                val newWidth = coordinates.size.width.toFloat()
                if (newWidth > 0f) {
                    containerWidth = newWidth
                }
            }
    ) {
        // 使用不可见的文本来测量实际文本宽度（不限制宽度，确保完整测量）
        Text(
            text = text,
            style = style,
            fontWeight = fontWeight,
            color = androidx.compose.ui.graphics.Color.Transparent,
            maxLines = 1,
            overflow = TextOverflow.Visible,
            softWrap = false, // 不换行，确保完整测量
            modifier = Modifier
                .wrapContentWidth() // 不限制宽度
                .onGloballyPositioned { coordinates ->
                    val newWidth = coordinates.size.width.toFloat()
                    if (newWidth > 0f) {
                        textWidth = newWidth
                    }
                }
        )
        
        // 只有一个文本实例
        // 第一次播放：从左侧开始（x=0）
        // 之后循环：从右侧开始（x=containerWidth）
        val textOffset = if (shouldScroll && scrollDistance > 0f && containerWidth > 0f) {
            if (isFirstPlay) {
                // 第一次播放：从左侧开始，向左移动
                -scrollOffset
            } else {
                // 之后循环：从右侧开始，向左移动
                // 使用模运算确保循环时位置正确
                val cycleOffset = scrollOffset % scrollDistance
                containerWidth - cycleOffset
            }
        } else {
            0f
        }
        
        Text(
            text = text,
            style = style,
            fontWeight = fontWeight,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Visible,
            softWrap = false, // 不换行，确保完整显示
            modifier = Modifier
                .wrapContentWidth() // 不限制宽度，确保完整显示
                .offset { 
                    IntOffset(textOffset.toInt(), 0)
                }
        )
    }
}

