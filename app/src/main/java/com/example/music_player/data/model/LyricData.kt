package com.example.music_player.data.model

/**
 * 歌词数据模型
 * @param lines 歌词行列表，按时间排序
 * @param offset 时间偏移量（毫秒），用于调整歌词时间
 */
data class LyricData(
    val lines: List<LyricLine>,
    val offset: Long = 0L
) {
    /**
     * 根据当前播放时间获取当前应该显示的歌词行索引
     * @param currentTime 当前播放时间（毫秒）
     * @return 歌词行索引，如果没有找到返回-1
     */
    fun getCurrentLineIndex(currentTime: Long): Int {
        val adjustedTime = currentTime + offset
        for (i in lines.indices.reversed()) {
            if (adjustedTime >= lines[i].time) {
                return i
            }
        }
        return -1
    }

    /**
     * 根据当前播放时间获取当前应该显示的歌词行
     * @param currentTime 当前播放时间（毫秒）
     * @return 歌词行，如果没有找到返回null
     */
    fun getCurrentLine(currentTime: Long): LyricLine? {
        val index = getCurrentLineIndex(currentTime)
        return if (index >= 0) lines[index] else null
    }

    /**
     * 检查是否有歌词数据
     */
    fun isEmpty(): Boolean = lines.isEmpty()
}

