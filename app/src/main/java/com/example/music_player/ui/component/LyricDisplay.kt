package com.example.music_player.ui.component

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.music_player.data.model.LyricData
import com.example.music_player.data.model.LyricLine
import com.example.music_player.ui.theme.ActiveColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 歌词展示组件
 * 支持自动滚动高亮、拖动调整播放进度
 * 
 * @param lyricData 歌词数据
 * @param currentTime 当前播放时间（毫秒）
 * @param onSeekTo 拖动歌词时调整播放进度的回调
 * @param modifier Modifier
 */
@Composable
fun LyricDisplay(
    lyricData: LyricData,
    currentTime: Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentLineIndex = lyricData.getCurrentLineIndex(currentTime)
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var lastManualSeekTime by remember { mutableStateOf(0L) }
    
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val lineHeightPx = with(density) { 60.dp.toPx() }
    val coroutineScope = rememberCoroutineScope()

    // 当用户手动点击歌词后，延迟恢复自动滚动
    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0) {
            // 用户点击了歌词，1秒后恢复自动滚动
            kotlinx.coroutines.delay(1000)
            selectedIndex = -1
        }
    }

    // 自动滚动到当前歌词行，将当前行放在第二行位置
    LaunchedEffect(currentLineIndex, currentTime) {
        // 只有在没有手动选择时才自动滚动
        if (selectedIndex < 0 && currentLineIndex >= 0 && lyricData.lines.isNotEmpty()) {
            // 计算目标滚动位置：当前行应该显示在第二行（第一行高度60dp）
            // 所以需要向上滚动 (currentLineIndex - 1) * lineHeightPx
            val targetScroll = if (currentLineIndex > 0) {
                ((currentLineIndex - 1) * lineHeightPx).toInt()
            } else {
                0
            }
            scrollState.animateScrollTo(targetScroll)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    // 根据点击位置计算目标歌词行
                    // 注意：第一行是占位Spacer，所以需要减去一行的高度
                    val scrollOffset = scrollState.value
                    val y = tapOffset.y + scrollOffset - lineHeightPx.toInt() // 减去占位行高度
                    val index = (y / lineHeightPx).toInt().coerceIn(0, lyricData.lines.size - 1)
                    if (index < lyricData.lines.size && index >= 0) {
                        selectedIndex = index
                        val targetTime = lyricData.lines[index].time
                        lastManualSeekTime = targetTime
                        onSeekTo(targetTime)
                        // 立即滚动到选中的行，使其显示在第二行（在协程作用域中调用挂起函数）
                        val targetScroll = if (index > 0) {
                            ((index - 1) * lineHeightPx).toInt()
                        } else {
                            0
                        }
                        coroutineScope.launch {
                            scrollState.animateScrollTo(targetScroll)
                        }
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 32.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (lyricData.isEmpty()) {
                Text(
                    text = "暂无歌词",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 32.dp)
                )
            } else {
                // 添加一个占位行，使当前播放行显示在第二行
                Spacer(modifier = Modifier.height(60.dp))
                
                lyricData.lines.forEachIndexed { index, line ->
                    val isCurrentLine = index == currentLineIndex && selectedIndex < 0
                    val isSelectedLine = index == selectedIndex
                    
                    LyricLineItem(
                        line = line,
                        isHighlighted = isCurrentLine || isSelectedLine,
                        modifier = Modifier.height(60.dp)
                    )
                }
            }
        }
    }
}

/**
 * 单行歌词组件
 */
@Composable
private fun LyricLineItem(
    line: LyricLine,
    isHighlighted: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = line.text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = if (isHighlighted) 20.sp else 16.sp,
                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (isHighlighted) ActiveColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}


