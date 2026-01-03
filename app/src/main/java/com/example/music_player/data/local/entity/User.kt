package com.example.music_player.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.music_player.data.model.Gender
import java.util.Date

@Entity(tableName = "user_table")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var nickname: String,
    val username: String,
    val password: String,
    var darkMode: Boolean = false, // 用户主题偏好，默认为亮色模式
    var gender: Gender = Gender.MALE, // 性别，默认为男生
    var lastLoginTime: Date? = null // 最后登录时间
)

