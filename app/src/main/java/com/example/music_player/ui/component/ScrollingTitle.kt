package com.example.music_player.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScrollingTitle(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
    fontWeight: FontWeight? = null,
    color: Color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
    isPlaying: Boolean = false
) {
    val finalStyle = style.copy(
        fontWeight = fontWeight ?: style.fontWeight,
        color = color
    )

    val marqueeModifier = if (isPlaying) {
        modifier
            .fillMaxWidth()
            .basicMarquee() // 仅在内容超出时自动滚动
    } else {
        modifier.fillMaxWidth()
    }

    Text(
        text = text,
        style = finalStyle,
        maxLines = 1,
        overflow = TextOverflow.Clip,
        softWrap = false,
        modifier = marqueeModifier
    )
}