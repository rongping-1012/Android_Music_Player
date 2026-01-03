package com.example.music_player.data.remote

import retrofit2.http.GET

/**
 * Retrofit API 接口定义
 * 用于网络数据请求
 * 
 * 注意：当前只保留轮播图接口，其他接口已移除
 * 如果将来需要添加新的网络功能，可以在此处添加
 */
interface ApiService {
    
    /**
     * 获取轮播图数据
     * @return 轮播图列表
     */
    @GET("banner/list")
    suspend fun getBanners(): ApiResponse<List<BannerData>>
}

/**
 * API 响应包装类
 */
data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T
)

/**
 * 轮播图数据模型
 */
data class BannerData(
    val id: String,
    val title: String,
    val imageUrl: String,
    val linkUrl: String? = null,
    val type: String? = null
)

