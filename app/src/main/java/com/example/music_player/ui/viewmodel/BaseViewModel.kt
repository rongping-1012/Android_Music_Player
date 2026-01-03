package com.example.music_player.ui.viewmodel

import androidx.lifecycle.ViewModel
import android.util.Log

/**
 * ViewModel 基类，提供统一的状态管理和错误处理
 */
abstract class BaseViewModel : ViewModel() {
    
    /**
     * 统一 UI 状态处理
     */
    sealed interface UiState<T> {
        data object Loading : UiState<Nothing>
        data class Success<T>(val data: T) : UiState<T>
        data class Error(val message: String) : UiState<Nothing>
    }
    
    /**
     * 统一错误处理
     */
    protected fun handleError(throwable: Throwable, tag: String = "BaseViewModel"): String {
        Log.e(tag, "Error occurred", throwable)
        return throwable.message ?: "未知错误"
    }
    
    /**
     * 记录信息日志
     */
    protected fun logInfo(message: String, tag: String = "BaseViewModel") {
        Log.i(tag, message)
    }
}

