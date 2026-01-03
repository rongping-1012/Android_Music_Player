package com.example.music_player.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit 客户端配置
 * 单例模式，提供统一的网络请求客户端
 */
object RetrofitClient {
    
    // 配置 API 基础 URL
    // 注意：请根据实际后端 API 地址修改此 URL
    // 示例：如果后端 API 地址是 https://your-api.com/api/v1/，则设置为 "https://your-api.com/api/v1/"
    private const val BASE_URL = "https://api.example.com/"
    
    // 如果暂时没有后端 API，可以使用 Mock 服务或本地测试服务器
    // 例如：使用 JSONPlaceholder: "https://jsonplaceholder.typicode.com/"
    // 或者本地测试: "http://10.0.2.2:8080/" (Android 模拟器访问本地服务器)
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        // 在开发环境启用日志，生产环境关闭
        level = HttpLoggingInterceptor.Level.BODY
        // TODO: 使用 BuildConfig.DEBUG 时，需要确保 BuildConfig 已生成
        // level = if (com.example.music_player.BuildConfig.DEBUG) {
        //     HttpLoggingInterceptor.Level.BODY
        // } else {
        //     HttpLoggingInterceptor.Level.NONE
        // }
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val apiService: ApiService = retrofit.create(ApiService::class.java)
}

