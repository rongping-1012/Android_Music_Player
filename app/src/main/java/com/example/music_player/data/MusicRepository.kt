package com.example.music_player.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.example.music_player.data.MusicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class MusicRepository(private val context: Context) {

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
    
    suspend fun getUserPlayHistory(username: String): List<com.example.music_player.data.PlayHistory> = withContext(Dispatchers.IO) {
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
    
    suspend fun getUserFavorites(username: String): List<com.example.music_player.data.FavoriteMusic> = withContext(Dispatchers.IO) {
        val favoriteDao = AppDatabase.getDatabase(context).favoriteMusicDao()
        favoriteDao.getUserFavorites(username)
    }
    
    suspend fun isFavorite(username: String, songPath: String): Boolean = withContext(Dispatchers.IO) {
        val favoriteDao = AppDatabase.getDatabase(context).favoriteMusicDao()
        favoriteDao.getFavoriteBySongPath(username, songPath) != null
    }
}
