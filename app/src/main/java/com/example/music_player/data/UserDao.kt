package com.example.music_player.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import java.util.Date

@Dao
interface UserDao {
    @Insert
    suspend fun insertUser(user: User)

    @Query("SELECT * FROM user_table WHERE username = :username AND password = :password")
    suspend fun getUser(username: String, password: String): User?

    @Query("SELECT * FROM user_table")
    suspend fun getAllUsers(): List<User>

    @Delete
    suspend fun deleteUser(user: User)

    @Query("DELETE FROM user_table WHERE username = :username")
    suspend fun deleteUserByUsername(username: String)

    @Query("SELECT * FROM user_table WHERE username = :username")
    suspend fun getUserByUsername(username: String): User?

    @Update
    suspend fun updateUser(user: User)

    @Query("UPDATE user_table SET username = :newUsername WHERE username = :oldUsername")
    suspend fun updateUsername(oldUsername: String, newUsername: String)

    @Query("UPDATE user_table SET nickname = :newNickname WHERE username = :username")
    suspend fun updateNickname(username: String, newNickname: String)

    @Query("UPDATE user_table SET password = :newPassword WHERE id = :userId AND password = :oldPassword")
    suspend fun updatePassword(userId: Int, oldPassword: String, newPassword: String): Int

    @Query("UPDATE user_table SET password = :newPassword WHERE username = :username")
    suspend fun resetPassword(username: String, newPassword: String): Int

    @Query("UPDATE user_table SET password = :newPassword WHERE username = :username")
    suspend fun updatePassword(username: String, newPassword: String)
    
    @Query("UPDATE user_table SET darkMode = :darkMode WHERE username = :username")
    suspend fun updateDarkMode(username: String, darkMode: Boolean)
    
    @Query("UPDATE user_table SET gender = :gender WHERE username = :username")
    suspend fun updateGender(username: String, gender: Gender)
    
    @Query("UPDATE user_table SET lastLoginTime = :loginTime WHERE username = :username")
    suspend fun updateLastLoginTime(username: String, loginTime: Date)
    
    @Query("UPDATE user_table SET password = :newPassword WHERE username = :username")
    suspend fun adminUpdatePassword(username: String, newPassword: String)

}
