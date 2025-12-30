package com.example.music_player.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class UserService(private val context: Context) {

    private val userRepository: UserRepository
    private val dataStoreManager: DataStoreManager

    init {
        val userDao = AppDatabase.getDatabase(context).userDao()
        userRepository = UserRepository(userDao, context)
        dataStoreManager = DataStoreManager(context)
    }

    val currentUsername: Flow<String> = dataStoreManager.currentUsername

    suspend fun login(username: String, password: String): Result<Boolean> {
        return try {
            if (username == "admin" && password == "admin123") {
                dataStoreManager.setCurrentUsername(username)
                Result.success(true) // isAdmin = true
            } else {
                val user = userRepository.getUser(username, password)
                if (user != null) {
                    // 更新最后登录时间
                    userRepository.updateLastLoginTime(username, java.util.Date())
                    dataStoreManager.setCurrentUsername(username)
                    Result.success(false) // isAdmin = false
                } else {
                    Result.failure(Exception("用户名或密码错误"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(nickname: String, username: String, password: String, gender: Gender): Result<Unit> {
        return try {
            val existingUser = userRepository.getUserByUsername(username)
            if (existingUser != null) {
                Result.failure(Exception("用户名已存在"))
            } else {
                userRepository.insertUser(User(nickname = nickname, username = username, password = password, gender = gender))
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetPassword(username: String, oldPassword: String, newPassword: String): Result<Unit> {
        return try {
            val user = userRepository.getUserByUsername(username)
            if (user == null) {
                return Result.failure(Exception("用户名不存在"))
            }
            
            if (user.password != oldPassword) {
                return Result.failure(Exception("旧密码错误"))
            }
            
            val rowsUpdated = userRepository.resetPassword(username, newPassword)
            if (rowsUpdated > 0) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("密码重置失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() {
        dataStoreManager.clearCurrentUsername()
    }

    suspend fun getCurrentUser(): User? {
        val username = dataStoreManager.currentUsername.first()
        return if (username.isNotEmpty()) {
            userRepository.getUserByUsername(username)
        } else {
            null
        }
    }

    suspend fun updateNickname(newNickname: String): Result<Unit> {
        return try {
            val username = dataStoreManager.currentUsername.first()
            if (username.isEmpty()) return Result.failure(Exception("用户未登录"))
            userRepository.updateNickname(username, newNickname)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateGender(gender: Gender): Result<Unit> {
        return try {
            val username = dataStoreManager.currentUsername.first()
            if (username.isEmpty()) return Result.failure(Exception("用户未登录"))
            userRepository.updateGender(username, gender)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun changePassword(oldPassword: String, newPassword: String): Result<Unit> {
        return try {
            val username = dataStoreManager.currentUsername.first()
            if (username.isEmpty()) return Result.failure(Exception("用户未登录"))
            val user = userRepository.getUserByUsername(username) ?: return Result.failure(Exception("用户不存在"))
            
            if(user.password != oldPassword) return Result.failure(Exception("原密码错误"))

            val rowsUpdated = userRepository.changePassword(user.id, oldPassword, newPassword)
            if (rowsUpdated > 0) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("密码更新失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getDarkMode(): Boolean {
        val username = dataStoreManager.currentUsername.first()
        return if (username.isNotEmpty() && username != "admin") {
            // 管理员账号强制使用亮色模式
            userRepository.getUserDarkMode(username)
        } else {
            false // 未登录或管理员时默认亮色模式
        }
    }
    
    suspend fun setDarkMode(darkMode: Boolean) {
        val username = dataStoreManager.currentUsername.first()
        if (username.isNotEmpty()) {
            userRepository.updateDarkMode(username, darkMode)
        }
    }
    
    suspend fun adminUpdatePassword(username: String, newPassword: String): Result<Unit> {
        return try {
            if (newPassword.length < 6) {
                Result.failure(Exception("新密码至少需要6位"))
            } else {
                userRepository.adminUpdatePassword(username, newPassword)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
