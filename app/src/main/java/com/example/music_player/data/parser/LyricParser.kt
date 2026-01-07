package com.example.music_player.data.parser

import android.content.Context
import android.net.Uri
import com.example.music_player.data.model.LyricData
import com.example.music_player.data.model.LyricLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.regex.Pattern

/**
 * LRC歌词解析器
 * 支持标准LRC格式和时间标签解析
 */
object LyricParser {

    // LRC时间标签正则表达式：[mm:ss.xx] 或 [mm:ss]
    private val TIME_TAG_PATTERN = Pattern.compile("\\[(\\d{2}):(\\d{2})(?:\\.(\\d{2}))?\\]")
    
    // 偏移量标签：[offset:+/-xxx]
    private val OFFSET_PATTERN = Pattern.compile("\\[offset:([+-]?\\d+)\\]")

    /**
     * 从URI解析LRC歌词文件
     * @param context Context
     * @param uri 歌词文件URI
     * @return LyricData，如果解析失败返回空的LyricData
     */
    suspend fun parseFromUri(context: Context, uri: Uri): LyricData = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, "UTF-8")).use { reader ->
                    parseFromReader(reader)
                }
            } ?: LyricData(emptyList())
        } catch (e: Exception) {
            e.printStackTrace()
            LyricData(emptyList())
        }
    }

    /**
     * 从文件路径解析LRC歌词文件
     * @param context Context
     * @param filePath 歌词文件路径
     * @return LyricData，如果解析失败返回空的LyricData
     */
    suspend fun parseFromPath(context: Context, filePath: String): LyricData = withContext(Dispatchers.IO) {
        try {
            java.io.File(filePath).bufferedReader(Charsets.UTF_8).use { reader ->
                parseFromReader(reader)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            LyricData(emptyList())
        }
    }

    /**
     * 从BufferedReader解析歌词
     */
    private fun parseFromReader(reader: BufferedReader): LyricData {
        val lines = mutableListOf<LyricLine>()
        var offset = 0L

        reader.forEachLine { line ->
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) return@forEachLine

            // 检查偏移量标签
            val offsetMatcher = OFFSET_PATTERN.matcher(trimmedLine)
            if (offsetMatcher.find()) {
                offset = offsetMatcher.group(1).toLongOrNull() ?: 0L
                return@forEachLine
            }

            // 解析时间标签和歌词文本
            val timeMatcher = TIME_TAG_PATTERN.matcher(trimmedLine)
            val timeTags = mutableListOf<Long>()
            
            while (timeMatcher.find()) {
                val minutes = timeMatcher.group(1).toInt()
                val seconds = timeMatcher.group(2).toInt()
                val milliseconds = timeMatcher.group(3)?.toIntOrNull() ?: 0
                
                val timeMs = (minutes * 60 + seconds) * 1000L + milliseconds * 10L
                timeTags.add(timeMs)
            }

            // 提取歌词文本（移除所有时间标签）
            // 使用 Java Pattern 的 matcher().replaceAll 来移除时间标签，避免与 Kotlin String.replace 重载冲突
            val text = TIME_TAG_PATTERN.matcher(trimmedLine).replaceAll("").trim()

            // 如果有时间标签和文本，添加到列表
            if (timeTags.isNotEmpty() && text.isNotEmpty()) {
                timeTags.forEach { time ->
                    lines.add(LyricLine(time, text))
                }
            }
        }

        // 按时间排序
        lines.sortBy { it.time }

        return LyricData(lines, offset)
    }

    /**
     * 根据音乐文件URI查找对应的LRC歌词文件
     * 查找规则：
     * 1. 如果是MediaStore URI，查找同目录下的.lrc文件
     * 2. 如果是DocumentFile URI，查找同目录下的.lrc文件
     * 3. 如果是file:// URI，直接查找同目录下的.lrc文件
     * 
     * @param context Context
     * @param musicUri 音乐文件URI
     * @param musicName 音乐文件名
     * @return 歌词文件URI，如果找不到返回null
     */
    suspend fun findLyricFile(context: Context, musicUri: Uri, musicName: String): Uri? = withContext(Dispatchers.IO) {
        try {
            val musicPath = musicUri.toString()
            val baseName = musicName.substringBeforeLast(".")
            
            // 支持多种歌词文件命名格式：
            // 1. 歌曲名.lrc (例如: again-YUI.lrc)
            // 2. 歌曲名-歌词.lrc (例如: again-YUI-歌词.lrc)
            // 3. 歌曲名-歌词（中文）.lrc
            val possibleLyricFileNames = listOf(
                "$baseName.lrc",
                "$baseName-歌词.lrc",
                "$baseName-歌词（中文）.lrc",
                "$baseName-歌词(中文).lrc"
            )
            
            android.util.Log.d("LyricParser", "查找歌词文件: musicUri=$musicPath, musicName=$musicName")
            android.util.Log.d("LyricParser", "可能的歌词文件名: $possibleLyricFileNames")
            
            // 方法1: 如果是MediaStore URI (content://media/external/audio/media/xxx)
            if (musicPath.startsWith("content://media/")) {
                try {
                    // 获取音乐文件的父目录路径
                    val projection = arrayOf(
                        android.provider.MediaStore.Audio.Media._ID,
                        android.provider.MediaStore.Audio.Media.DATA,
                        android.provider.MediaStore.Audio.Media.DISPLAY_NAME
                    )
                    val selection = "${android.provider.MediaStore.Audio.Media._ID} = ?"
                    val musicId = musicUri.lastPathSegment?.toLongOrNull()
                    
                    if (musicId != null) {
                        context.contentResolver.query(
                            android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            projection,
                            selection,
                            arrayOf(musicId.toString()),
                            null
                        )?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val dataColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DATA)
                                val dataPath = cursor.getString(dataColumn)
                                
                                // 如果获取到了文件路径，尝试在同目录查找.lrc文件
                                if (dataPath != null) {
                                    val musicFile = java.io.File(dataPath)
                                    val parentDir = musicFile.parentFile
                                    if (parentDir != null) {
                                        // 尝试所有可能的歌词文件名
                                        for (lyricFileName in possibleLyricFileNames) {
                                            val lyricFile = java.io.File(parentDir, lyricFileName)
                                            android.util.Log.d("LyricParser", "尝试查找: ${lyricFile.absolutePath}, exists=${lyricFile.exists()}")
                                            if (lyricFile.exists() && lyricFile.canRead()) {
                                                android.util.Log.d("LyricParser", "找到歌词文件(文件系统): ${lyricFile.absolutePath}")
                                                return@withContext Uri.fromFile(lyricFile)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // 方法1b: 通过MediaStore.Files查找所有可能的歌词文件名
                    val filesProjection = arrayOf(android.provider.MediaStore.Files.FileColumns._ID, android.provider.MediaStore.Files.FileColumns.DATA)
                    val filesUri = android.provider.MediaStore.Files.getContentUri("external")
                    
                    for (lyricFileName in possibleLyricFileNames) {
                        val filesSelection = "${android.provider.MediaStore.Files.FileColumns.DISPLAY_NAME} = ?"
                        val filesSelectionArgs = arrayOf(lyricFileName)
                        
                        context.contentResolver.query(
                            filesUri,
                            filesProjection,
                            filesSelection,
                            filesSelectionArgs,
                            null
                        )?.use { cursor ->
                            while (cursor.moveToNext()) {
                                val idColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Files.FileColumns._ID)
                                val dataColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Files.FileColumns.DATA)
                                val id = cursor.getLong(idColumn)
                                val dataPath = cursor.getString(dataColumn)
                                
                                // 验证文件路径是否与音乐文件在同一目录
                                if (dataPath != null) {
                                    val lyricFile = java.io.File(dataPath)
                                    if (lyricFile.exists() && lyricFile.canRead()) {
                                        android.util.Log.d("LyricParser", "找到歌词文件(MediaStore): $dataPath")
                                        val lyricUri = android.content.ContentUris.withAppendedId(filesUri, id)
                                        // 验证文件是否可读
                                        context.contentResolver.openInputStream(lyricUri)?.use {
                                            return@withContext lyricUri
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("LyricParser", "MediaStore查找失败", e)
                }
            }
            
            // 方法2: 如果是DocumentFile URI (content://com.android.externalstorage...)
            if (musicPath.startsWith("content://") && (musicPath.contains("document") || musicPath.contains("tree"))) {
                try {
                    val documentFile = androidx.documentfile.provider.DocumentFile.fromSingleUri(context, musicUri)
                    val parentFile = documentFile?.parentFile
                    android.util.Log.d("LyricParser", "DocumentFile查找: parentFile=${parentFile?.uri}")
                    parentFile?.listFiles()?.forEach { file ->
                        android.util.Log.d("LyricParser", "检查文件: ${file.name}, isFile=${file.isFile}")
                        // 检查所有可能的歌词文件名
                        if (file.isFile) {
                            val fileName = file.name ?: ""
                            for (lyricFileName in possibleLyricFileNames) {
                                if (fileName.equals(lyricFileName, ignoreCase = true)) {
                                    android.util.Log.d("LyricParser", "找到歌词文件(DocumentFile): ${file.uri}")
                                    return@withContext file.uri
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("LyricParser", "DocumentFile查找失败", e)
                }
            }
            
            // 方法3: 如果是file:// URI
            if (musicPath.startsWith("file://")) {
                try {
                    val file = java.io.File(musicUri.path ?: return@withContext null)
                    val parentDir = file.parentFile ?: return@withContext null
                    // 尝试所有可能的歌词文件名
                    for (lyricFileName in possibleLyricFileNames) {
                        val lyricFile = java.io.File(parentDir, lyricFileName)
                        android.util.Log.d("LyricParser", "尝试查找(file://): ${lyricFile.absolutePath}, exists=${lyricFile.exists()}")
                        if (lyricFile.exists() && lyricFile.canRead()) {
                            android.util.Log.d("LyricParser", "找到歌词文件(file://): ${lyricFile.absolutePath}")
                            return@withContext Uri.fromFile(lyricFile)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("LyricParser", "File URI查找失败", e)
                }
            }
            
            // 方法4: 尝试直接构造URI（适用于某些content:// URI）
            // 这个方法通常不太可靠，但作为最后的尝试
            try {
                val basePath = musicPath.substringBeforeLast(".")
                // 尝试构造 "歌曲名-歌词.lrc" 格式
                val lyricPath = "$basePath-歌词.lrc"
                val lyricUri = Uri.parse(lyricPath)
                android.util.Log.d("LyricParser", "尝试直接构造URI: $lyricPath")
                context.contentResolver.openInputStream(lyricUri)?.use {
                    android.util.Log.d("LyricParser", "找到歌词文件(直接构造): $lyricPath")
                    return@withContext lyricUri
                }
            } catch (e: Exception) {
                // 忽略错误
                android.util.Log.d("LyricParser", "直接构造URI失败", e)
            }
            
            android.util.Log.w("LyricParser", "未找到歌词文件，尝试了以下名称: $possibleLyricFileNames")
            null
        } catch (e: Exception) {
            android.util.Log.e("LyricParser", "查找歌词文件异常", e)
            null
        }
    }
}

