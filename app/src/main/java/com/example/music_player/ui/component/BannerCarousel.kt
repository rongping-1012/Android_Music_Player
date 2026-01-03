package com.example.music_player.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay

/**
 * 轮播图数据模型
 * 支持本地资源（drawableResId）或网络图片（imageUrl）
 */
data class BannerItem(
    val id: String,
    val title: String,
    val imageUrl: String? = null,  // 网络图片 URL
    val drawableResId: Int? = null, // 本地资源 ID
    val linkUrl: String? = null,
    val onClick: (() -> Unit)? = null
) {
    init {
        require(imageUrl != null || drawableResId != null) {
            "BannerItem must have either imageUrl or drawableResId"
        }
    }
}

/**
 * 轮播图组件
 * @param banners 轮播图数据列表
 * @param modifier 修饰符
 * @param autoPlayInterval 自动播放间隔（毫秒），默认 3000ms
 * @param onBannerClick 点击回调
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BannerCarousel(
    banners: List<BannerItem>,
    modifier: Modifier = Modifier,
    autoPlayInterval: Long = 3000,
    onBannerClick: ((BannerItem) -> Unit)? = null
) {
    if (banners.isEmpty()) {
        return
    }
    
    // 使用一个很大的页面数来实现无限循环
    // 使用 10000 作为中间点，可以向前和向后滑动足够多的次数
    val totalPages = 10000
    // 确保初始页面取模后等于0（第一张图）
    val basePage = totalPages / 2
    val initialPage = (basePage / banners.size) * banners.size
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { totalPages }
    )
    
    // 计算实际的 banner 索引（通过取模）
    val actualPage = pagerState.currentPage % banners.size
    
    var autoPlayEnabled by remember { mutableStateOf(true) }
    var lastAutoPlayTime by remember { mutableLongStateOf(0L) }
    
    // 监听用户滑动状态，滑动时暂停自动播放
    LaunchedEffect(pagerState.isScrollInProgress) {
        if (pagerState.isScrollInProgress) {
            autoPlayEnabled = false
            lastAutoPlayTime = System.currentTimeMillis()
        } else {
            // 用户停止滑动后，延迟恢复自动播放
            delay(2000)
            autoPlayEnabled = true
            lastAutoPlayTime = System.currentTimeMillis()
        }
    }
    
    // 自动轮播（仅在用户未滑动时）
    LaunchedEffect(Unit) {
        while (true) {
            delay(autoPlayInterval)
            val currentTime = System.currentTimeMillis()
            if (autoPlayEnabled && !pagerState.isScrollInProgress && 
                (currentTime - lastAutoPlayTime) >= autoPlayInterval) {
                // 直接切换到下一页，由于使用了很大的页面数，可以无限循环
                val nextPage = pagerState.currentPage + 1
                pagerState.animateScrollToPage(
                    page = nextPage,
                    animationSpec = tween(
                        durationMillis = 300, // 300ms 快速切换动画
                        easing = FastOutSlowInEasing
                    )
                )
                lastAutoPlayTime = currentTime
            }
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        // 使用HorizontalPager实现可滑动轮播
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp)),
            pageSize = androidx.compose.foundation.pager.PageSize.Fill, // 确保整页显示
            pageSpacing = 0.dp // 页面间距为0，确保无缝切换
        ) { page ->
            // 通过取模运算获取实际的 banner 索引，实现无限循环
            val bannerIndex = page % banners.size
            val banner = banners[bannerIndex]
            
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        onBannerClick?.invoke(banner)
                        banner.onClick?.invoke()
                    },
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // 支持本地资源或网络图片
                    if (banner.drawableResId != null) {
                        Image(
                            painter = painterResource(id = banner.drawableResId),
                            contentDescription = banner.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (banner.imageUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(banner.imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = banner.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    
                    // 标题渐变遮罩
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        androidx.compose.ui.graphics.Color.Transparent,
                                        androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f)
                                    ),
                                    startY = 0f,
                                    endY = Float.POSITIVE_INFINITY
                                )
                            )
                    )
                    
                    // 标题
                    Text(
                        text = banner.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    )
                }
            }
        }
        
        // 页面指示器
        if (banners.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                banners.forEachIndexed { index, _ ->
                    // 使用取模运算确保指示器正确显示
                    val isSelected = index == actualPage
                    val width by animateDpAsState(
                        targetValue = if (isSelected) 24.dp else 8.dp,
                        animationSpec = tween(
                            durationMillis = 300,
                            easing = FastOutSlowInEasing
                        ),
                        label = "indicator_width_$index"
                    )
                    
                    Box(
                        modifier = Modifier
                            .width(width)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                }
                            )
                    )
                }
            }
        }
    }
}

