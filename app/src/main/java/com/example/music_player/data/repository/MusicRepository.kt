package com.example.music_player.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.example.music_player.data.DataStoreManager
import com.example.music_player.data.local.AppDatabase
import com.example.music_player.data.local.entity.FavoriteMusic
import com.example.music_player.data.local.entity.PlayHistory
import com.example.music_player.data.model.MusicFile
import com.example.music_player.data.remote.BannerData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class MusicRepository(
    private val context: Context
) {

    private val dataStoreManager = DataStoreManager(context)

    suspend fun getSelectedFolderUri(): String? {
        return dataStoreManager.selectedFolderUri.first()
    }

    suspend fun setSelectedFolderUri(uri: String) {
        dataStoreManager.setSelectedFolderUri(uri)
    }

    suspend fun getMusicFiles(): List<MusicFile> = withContext(Dispatchers.IO) {
        val musicList = mutableListOf<MusicFile>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        // 不排序，保持原始顺序

        context.contentResolver.query(
            collection,
            projection,
            selection,
            null,
            null
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                musicList.add(MusicFile(name = name, uri = contentUri))
            }
        }
        musicList
    }

    suspend fun getMusicFilesFromFolder(uri: Uri): List<MusicFile> = withContext(Dispatchers.IO) {
        val fileList = mutableListOf<MusicFile>()
        val documentFile = DocumentFile.fromTreeUri(context, uri)

        documentFile?.listFiles()?.forEach { file: androidx.documentfile.provider.DocumentFile ->
            if (file.isFile && file.type?.startsWith("audio/") == true) {
                fileList.add(MusicFile(name = file.name ?: "未知文件", uri = file.uri))
            }
        }
        fileList
    }
    
    suspend fun getUserPlayHistory(username: String): List<PlayHistory> = withContext(Dispatchers.IO) {
        val playHistoryDao = AppDatabase.getDatabase(context).playHistoryDao()
        playHistoryDao.getUserHistory(username)
    }
    
    suspend fun addFavorite(username: String, songPath: String, songName: String) = withContext(Dispatchers.IO) {
        val favoriteDao = AppDatabase.getDatabase(context).favoriteMusicDao()
        val existing = favoriteDao.getFavoriteBySongPath(username, songPath)
        if (existing == null) {
            favoriteDao.insertFavorite(FavoriteMusic(username = username, songPath = songPath, songName = songName))
        }
    }
    
    suspend fun removeFavorite(username: String, songPath: String) = withContext(Dispatchers.IO) {
        val favoriteDao = AppDatabase.getDatabase(context).favoriteMusicDao()
        favoriteDao.deleteFavoriteBySongPath(username, songPath)
    }
    
    suspend fun getUserFavorites(username: String): List<FavoriteMusic> = withContext(Dispatchers.IO) {
        val favoriteDao = AppDatabase.getDatabase(context).favoriteMusicDao()
        favoriteDao.getUserFavorites(username)
    }
    
    suspend fun isFavorite(username: String, songPath: String): Boolean = withContext(Dispatchers.IO) {
        val favoriteDao = AppDatabase.getDatabase(context).favoriteMusicDao()
        favoriteDao.getFavoriteBySongPath(username, songPath) != null
    }
    
    /**
     * 获取轮播图数据（通过 Retrofit 结构）
     * 
     * 当前实现：使用本地数据模拟网络请求，保留 Retrofit 结构以便将来切换
     * 
     * 切换到真实 API 的步骤：
     * 1. 在 MusicRepository 构造函数中添加：private val apiService: ApiService = RetrofitClient.apiService
     * 2. 在 RetrofitClient.kt 中配置实际的 BASE_URL
     * 3. 取消注释下面的真实网络请求代码，并注释掉本地数据部分
     */
    suspend fun getBanners(): Result<List<BannerData>> = withContext(Dispatchers.IO) {
        runCatching {
            // 模拟网络请求延迟
            kotlinx.coroutines.delay(300)
            
            // 当前方式：使用本地数据（无需后端 API）
            listOf(
                BannerData(
                    id = "1",
                    title = "音乐推荐",
                    imageUrl = "https://gd-hbimg.huaban.com/edf18fecf35a879a0c4b6a886c9e3edb2d97bd8f17f36-d1JkUj_fw658"
                ),
                BannerData(
                    id = "2",
                    title = "热门歌曲",
                    imageUrl = "https://i2.hdslb.com/bfs/archive/7a42bea9e2ca78335789ac689b9370ad8a0f4910.jpg"
                ),
                BannerData(
                    id = "3",
                    title = "精选音乐",
                    imageUrl = "https://img0.baidu.com/it/u=337707339,1636742769&fm=253&fmt=auto&app=138&f=JPEG?w=1734&h=800"
                )
            )
            
            // 切换到真实 API 时，取消注释下面的代码，并注释掉上面的本地数据
            /*
            val apiService: ApiService = RetrofitClient.apiService
            val response = apiService.getBanners()
            if (response.code == 200) {
                response.data
            } else {
                throw Exception(response.message)
            }
            */
        }
    }
}

