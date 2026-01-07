package com.example.music_player.data.model

/**
 * 歌词行数据模型
 * @param time 时间戳（毫秒）
 * @param text 歌词文本
 */
data class LyricLine(
    val time: Long,
    val text: String
)


