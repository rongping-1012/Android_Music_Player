package com.example.music_player.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.music_player.data.local.entity.PlayHistory
import java.util.Date

@Dao
interface PlayHistoryDao {
    @Insert
    suspend fun insertHistory(history: PlayHistory)

    @Query("SELECT * FROM play_history WHERE username = :username ORDER BY playTime DESC LIMIT 10")
    suspend fun getUserHistory(username: String): List<PlayHistory>

    @Query("DELETE FROM play_history WHERE username = :username")
    suspend fun clearUserHistory(username: String)
    
    @Query("SELECT * FROM play_history WHERE username = :username AND songPath = :songPath LIMIT 1")
    suspend fun getHistoryBySongPath(username: String, songPath: String): PlayHistory?
    
    @Query("UPDATE play_history SET playTime = :playTime WHERE username = :username AND songPath = :songPath")
    suspend fun updateHistoryTime(username: String, songPath: String, playTime: Date)
}

