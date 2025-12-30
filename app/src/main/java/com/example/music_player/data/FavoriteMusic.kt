package com.example.music_player.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_music")
data class FavoriteMusic(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val username: String,
    val songPath: String,
    val songName: String
)

