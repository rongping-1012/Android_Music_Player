package com.example.music_player.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.music_player.data.UserService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// 定义UI状态，用于表示登录/注册/重置密码操作的结果
sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val isAdmin: Boolean = false) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class LoginViewModel(private val userService: UserService) : ViewModel() {

    private val _loginState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val loginState: StateFlow<AuthUiState> = _loginState

    private val _registerState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val registerState: StateFlow<AuthUiState> = _registerState

    private val _resetPasswordState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val resetPasswordState: StateFlow<AuthUiState> = _resetPasswordState

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _loginState.value = AuthUiState.Loading
            val result = userService.login(username, password)
            result.onSuccess {
                _loginState.value = AuthUiState.Success(isAdmin = it)
            }.onFailure {
                _loginState.value = AuthUiState.Error(it.message ?: "未知错误")
            }
        }
    }

    fun register(nickname: String, username: String, password: String, gender: com.example.music_player.data.model.Gender) {
        viewModelScope.launch {
            _registerState.value = AuthUiState.Loading
            if (nickname.isBlank() || username.isBlank() || password.isBlank()) {
                _registerState.value = AuthUiState.Error("请填写所有字段")
                return@launch
            }
            if (password.length < 6) {
                _registerState.value = AuthUiState.Error("密码至少需要6位")
                return@launch
            }
            val result = userService.register(nickname, username, password, gender)
            result.onSuccess {
                _registerState.value = AuthUiState.Success()
            }.onFailure {
                _registerState.value = AuthUiState.Error(it.message ?: "未知错误")
            }
        }
    }

    fun resetPassword(username: String, oldPassword: String, newPassword: String) {
        viewModelScope.launch {
            _resetPasswordState.value = AuthUiState.Loading
            if (username.isBlank() || oldPassword.isBlank() || newPassword.isBlank()) {
                _resetPasswordState.value = AuthUiState.Error("请填写所有字段")
                return@launch
            }
            if (newPassword.length < 6) {
                _resetPasswordState.value = AuthUiState.Error("新密码至少需要6位")
                return@launch
            }
            val result = userService.resetPassword(username, oldPassword, newPassword)
            result.onSuccess {
                _resetPasswordState.value = AuthUiState.Success()
            }.onFailure {
                _resetPasswordState.value = AuthUiState.Error(it.message ?: "未知错误")
            }
        }
    }

    // 重置状态，以便UI可以再次触发操作
    fun resetLoginState() {
        _loginState.value = AuthUiState.Idle
    }

    fun resetRegisterState() {
        _registerState.value = AuthUiState.Idle
    }

    fun resetResetPasswordState() {
        _resetPasswordState.value = AuthUiState.Idle
    }
}