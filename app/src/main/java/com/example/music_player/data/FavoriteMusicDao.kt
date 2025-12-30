package com.example.music_player.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete

@Dao
interface FavoriteMusicDao {
    @Insert
    suspend fun insertFavorite(favorite: FavoriteMusic)
    
    @Delete
    suspend fun deleteFavorite(favorite: FavoriteMusic)
    
    @Query("SELECT * FROM favorite_music WHERE username = :username ORDER BY id DESC")
    suspend fun getUserFavorites(username: String): List<FavoriteMusic>
    
    @Query("SELECT * FROM favorite_music WHERE username = :username AND songPath = :songPath LIMIT 1")
    suspend fun getFavoriteBySongPath(username: String, songPath: String): FavoriteMusic?
    
    @Query("DELETE FROM favorite_music WHERE username = :username AND songPath = :songPath")
    suspend fun deleteFavoriteBySongPath(username: String, songPath: String)
    
    @Query("DELETE FROM favorite_music WHERE username = :username")
    suspend fun deleteAllUserFavorites(username: String)
}

