package com.example.music_player.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

// At the top level of your kotlin file:
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class DataStoreManager(private val context: Context) {

    companion object {
        private val USERNAME_KEY = stringPreferencesKey("current_username")
        private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode_enabled")
        private val SELECTED_FOLDER_URI_KEY = stringPreferencesKey("selected_folder_uri")
        private const val AVATAR_DIR = "avatars"
    }
    
    private val avatarDir: File
        get() {
            val dir = File(context.filesDir, AVATAR_DIR)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
    }

    val currentUsername: Flow<String> = context.dataStore.data
        .map {
            it[USERNAME_KEY] ?: ""
        }

    suspend fun setCurrentUsername(username: String) {
        context.dataStore.edit {
            it[USERNAME_KEY] = username
        }
    }

    suspend fun clearCurrentUsername() {
        context.dataStore.edit {
            it.remove(USERNAME_KEY)
        }
    }

    val darkModeEnabled: Flow<Boolean> = context.dataStore.data
        .map {
            it[DARK_MODE_KEY] ?: false // Default to light mode
        }

    suspend fun setDarkMode(isEnabled: Boolean) {
        context.dataStore.edit {
            it[DARK_MODE_KEY] = isEnabled
        }
    }

    val selectedFolderUri: Flow<String?> = context.dataStore.data
        .map {
            it[SELECTED_FOLDER_URI_KEY]
        }

    suspend fun setSelectedFolderUri(uri: String?) {
        context.dataStore.edit {
            if (uri != null) {
                it[SELECTED_FOLDER_URI_KEY] = uri
            } else {
                it.remove(SELECTED_FOLDER_URI_KEY)
            }
        }
    }

    /**
     * 获取用户头像文件
     */
    fun getAvatarFile(username: String): File {
        return File(avatarDir, "$username.jpg")
    }
    
    /**
     * 检查用户是否有头像
     */
    suspend fun hasAvatar(username: String): Boolean = withContext(Dispatchers.IO) {
        val file = getAvatarFile(username)
        file.exists() && file.length() > 0
    }
    
    /**
     * 保存头像
     * @param username 用户名
     * @param uri 头像URI
     * @return 保存的文件路径，失败返回null
     */
    suspend fun saveAvatar(username: String, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val file = getAvatarFile(username)
            
            // 如果文件已存在，先删除
            if (file.exists()) {
                file.delete()
            }
            
            // 从URI读取图片
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            if (bitmap != null) {
                // 压缩并保存图片
                val outputStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                outputStream.flush()
                outputStream.close()
                
                // 返回文件路径
                file.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 获取头像URI
     * @param username 用户名
     * @return 头像文件URI，如果不存在返回null
     */
    fun getAvatarUri(username: String): Uri? {
        val file = getAvatarFile(username)
        return if (file.exists() && file.length() > 0) {
            Uri.parse("file://${file.absolutePath}")
        } else {
            null
        }
    }
    
    /**
     * 获取头像文件路径
     * @param username 用户名
     * @return 头像文件路径，如果不存在返回null
     */
    fun getAvatarPath(username: String): String? {
        val file = getAvatarFile(username)
        return if (file.exists() && file.length() > 0) {
            file.absolutePath
        } else {
            null
        }
    }
    
    /**
     * 删除用户头像
     */
    suspend fun deleteAvatar(username: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = getAvatarFile(username)
            if (file.exists()) {
                file.delete()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}