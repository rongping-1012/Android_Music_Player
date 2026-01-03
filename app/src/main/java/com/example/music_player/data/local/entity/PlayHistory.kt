package com.example.music_player.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "play_history")
data class PlayHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val username: String,
    val songPath: String,
    val songName: String,
    val playTime: Date
)

