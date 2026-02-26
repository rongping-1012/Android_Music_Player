package com.example.music_player.utils

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * 资源音乐文件管理器
 * 负责将 assets 目录中的音乐文件复制到公共 Music 目录
 */
object AssetMusicManager {
    
    private const val TAG = "AssetMusicManager"
    private const val ASSETS_MUSIC_FOLDER = "music" // assets 目录下的音乐文件夹名
    private const val TARGET_FOLDER_NAME = "test" // 复制到的目标文件夹名（/Music/test）
    
    /**
     * 获取公共 Music 目录下的目标文件夹
     * @param context Context
     * @return 目标文件夹 File 对象，如果无法访问返回 null
     */
    private fun getTargetDirectory(context: Context): File? {
        return try {
            // 获取公共 Music 目录
            val musicDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ 使用 MediaStore，但我们可以尝试访问公共目录
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            } else {
                // Android 9 及以下
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            }
            
            // 创建 test 子目录
            val targetDir = File(musicDir, TARGET_FOLDER_NAME)
            
            // 检查 Music 目录是否可访问
            if (!musicDir.exists() && !musicDir.mkdirs()) {
                Log.e(TAG, "无法创建 Music 目录: ${musicDir.absolutePath}")
                return null
            }
            
            // 创建 test 子目录
            if (!targetDir.exists() && !targetDir.mkdirs()) {
                Log.e(TAG, "无法创建目标目录: ${targetDir.absolutePath}")
                return null
            }
            
            // 检查目录是否可写
            if (!targetDir.canWrite()) {
                Log.e(TAG, "目标目录不可写: ${targetDir.absolutePath}")
                return null
            }
            
            Log.d(TAG, "目标目录: ${targetDir.absolutePath}")
            targetDir
        } catch (e: Exception) {
            Log.e(TAG, "获取目标目录失败", e)
            null
        }
    }
    
    /**
     * 检查并复制音乐文件
     * @param context Context
     * @return 是否成功复制
     */
    suspend fun copyMusicFiles(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            // 获取目标目录（/Music/test）
            val targetDir = getTargetDirectory(context) ?: run {
                Log.e(TAG, "无法获取目标目录，复制失败")
                return@withContext false
            }
            // 检查是否已经复制过
            val flagFile = File(targetDir, ".copied")
            if (flagFile.exists()) {
                Log.d(TAG, "音乐文件已存在，跳过复制")
                return@withContext true
            }
            // 获取 assets 中的音乐文件列表
            val assetManager = context.assets
            val musicFiles = try {
                assetManager.list(ASSETS_MUSIC_FOLDER)?.toList() ?: emptyList()
            } catch (e: Exception) {
                Log.w(TAG, "assets/music 文件夹不存在或为空", e)
                return@withContext false
            }
            if (musicFiles.isEmpty()) {
                Log.w(TAG, "assets/music 文件夹中没有文件")
                return@withContext false
            }
            Log.d(TAG, "开始复制 ${musicFiles.size} 个音乐文件...")
            
            var successCount = 0
            // 复制每个音乐文件
            musicFiles.forEach { fileName ->
                try {
                    val inputStream = assetManager.open("$ASSETS_MUSIC_FOLDER/$fileName")
                    val outputFile = File(targetDir, fileName)
                    
                    // 如果文件已存在，跳过
                    if (outputFile.exists()) {
                        Log.d(TAG, "文件已存在，跳过: $fileName")
                        inputStream.close()
                        successCount++
                        return@forEach
                    }
                    
                    FileOutputStream(outputFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    inputStream.close()
                    
                    Log.d(TAG, "成功复制: $fileName")
                    successCount++
                } catch (e: Exception) {
                    Log.e(TAG, "复制文件失败: $fileName", e)
                }
            }
            
            // 创建标记文件，表示已复制完成
            if (successCount > 0) {
                flagFile.createNewFile()
                Log.d(TAG, "音乐文件复制完成，共 $successCount 个文件")
                return@withContext true
            } else {
                Log.w(TAG, "没有成功复制任何文件")
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(TAG, "复制音乐文件异常", e)
            return@withContext false
        }
    }
    
    /**
     * 获取目标音乐文件夹的路径
     * @param context Context
     * @return 文件夹路径，如果不存在返回 null
     */
    fun getMusicFolderPath(context: Context): String? {
        val targetDir = getTargetDirectory(context)
        return if (targetDir != null && targetDir.exists() && targetDir.isDirectory) {
            targetDir.absolutePath
        } else {
            null
        }
    }
    
    /**
     * 获取目标音乐文件夹的 URI（用于 DocumentFile）
     * @param context Context
     * @return 文件夹 URI，如果不存在返回 null
     */
    fun getMusicFolderUri(context: Context): android.net.Uri? {
        val targetDir = getTargetDirectory(context)
        return if (targetDir != null && targetDir.exists() && targetDir.isDirectory) {
            android.net.Uri.fromFile(targetDir)
        } else {
            null
        }
    }
    
    /**
     * 检查音乐文件是否已复制
     * @param context Context
     * @return 是否已复制
     */
    fun isMusicFilesCopied(context: Context): Boolean {
        val targetDir = getTargetDirectory(context) ?: return false
        val flagFile = File(targetDir, ".copied")
        return flagFile.exists()
    }
}

