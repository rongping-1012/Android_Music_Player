package com.example.music_player.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository(
    private val userDao: UserDao,
    private val context: Context? = null
) {

    suspend fun getUser(username: String, password: String): User? {
        return withContext(Dispatchers.IO) {
            userDao.getUser(username, password)
        }
    }

    suspend fun getUserByUsername(username: String): User? {
        return withContext(Dispatchers.IO) {
            userDao.getUserByUsername(username)
        }
    }

    suspend fun insertUser(user: User) {
        withContext(Dispatchers.IO) {
            userDao.insertUser(user)
        }
    }

    suspend fun resetPassword(username: String, newPassword: String): Int {
        return withContext(Dispatchers.IO) {
            userDao.resetPassword(username, newPassword)
        }
    }

    suspend fun getAllUsers(): List<User> {
        return withContext(Dispatchers.IO) {
            userDao.getAllUsers()
        }
    }

    suspend fun deleteUser(user: User) {
        withContext(Dispatchers.IO) {
            // 使用 username（账号）来删除用户及其相关数据
            val username = user.username
            
            // 删除用户前，先删除该用户的所有相关数据
            if (context != null) {
                val database = AppDatabase.getDatabase(context)
                val dataStoreManager = DataStoreManager(context)

                // 删除用户的头像
                dataStoreManager.deleteAvatar(username)
                
                // 删除用户的喜欢音乐
                database.favoriteMusicDao().deleteAllUserFavorites(username)
                
                // 删除用户的播放历史
                database.playHistoryDao().clearUserHistory(username)
            }
            
            // 最后根据 username（账号）删除用户本身
            userDao.deleteUserByUsername(username)
        }
    }

    suspend fun updateNickname(username: String, newNickname: String) {
        withContext(Dispatchers.IO) {
            userDao.updateNickname(username, newNickname)
        }
    }

    suspend fun changePassword(userId: Int, oldPassword: String, newPassword: String): Int {
        return withContext(Dispatchers.IO) {
            userDao.updatePassword(userId, oldPassword, newPassword)
        }
    }
    
    suspend fun updateDarkMode(username: String, darkMode: Boolean) {
        withContext(Dispatchers.IO) {
            userDao.updateDarkMode(username, darkMode)
        }
    }
    
    suspend fun getUserDarkMode(username: String): Boolean {
        return withContext(Dispatchers.IO) {
            val user = userDao.getUserByUsername(username)
            user?.darkMode ?: false
        }
    }
    
    suspend fun updateGender(username: String, gender: Gender) {
        withContext(Dispatchers.IO) {
            userDao.updateGender(username, gender)
        }
    }
    
    suspend fun updateLastLoginTime(username: String, loginTime: java.util.Date) {
        withContext(Dispatchers.IO) {
            userDao.updateLastLoginTime(username, loginTime)
        }
    }
    
    suspend fun adminUpdatePassword(username: String, newPassword: String) {
        withContext(Dispatchers.IO) {
            userDao.adminUpdatePassword(username, newPassword)
        }
    }
}

