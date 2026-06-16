// Copyright (C) 2025 Voltual
package me.voltual.pyrolysis.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import me.voltual.pyrolysis.AppStore

@Serializable
sealed interface AppDestination : NavKey

// --- 核心导航 ---
@Serializable
data object Home : AppDestination

@Serializable
data object Login : AppDestination

@Serializable
data object About : AppDestination

@Serializable
data object LogViewer : AppDestination

@Serializable
data object ThemeCustomize : AppDestination

@Serializable
data object StoreManager : AppDestination

@Serializable
data object UpdateSettings : AppDestination

@Serializable
data object ProxySettings : AppDestination

// --- 社区与帖子 ---
@Serializable
data object Community : AppDestination

@Serializable
data object MyLikes : AppDestination

@Serializable
data object HotPosts : AppDestination

@Serializable
data object FollowingPosts : AppDestination

@Serializable
data object BrowseHistory : AppDestination

@Serializable
data class PostDetail(val postId: Long) : AppDestination

@Serializable
data object CreatePost : AppDestination

@Serializable
data class CreateRefundPost(
    val appId: Long,
    val versionId: Long,
    val appName: String,
    val payMoney: Int
) : AppDestination

@Serializable
data class ImagePreview(val imageUrl: String) : AppDestination

// --- 用户相关 ---
@Serializable
data class UserDetail(
    val userId: Long,
    val store: AppStore = AppStore.XIAOQU_SPACE
) : AppDestination

@Serializable
data class MyPosts(
    val userId: Long,
    val nickname: String? = null
) : AppDestination

@Serializable
data class Search(
    val userId: String? = null,
    val nickname: String? = null
) : AppDestination

@Serializable
data object MyComments : AppDestination

@Serializable
data object MyReviews : AppDestination

@Serializable
data object FollowList : AppDestination

@Serializable
data object FanList : AppDestination

@Serializable
data class AccountProfile(val store: AppStore = AppStore.XIAOQU_SPACE) : AppDestination

@Serializable
data object SignInSettings : AppDestination

// --- 资源广场与应用 ---
@Serializable
data class ResourcePlaza(
    val isMyResource: Boolean,
    val userId: Long = -1L,
    val mode: String = "public",
    val storeName: String = AppStore.XIAOQU_SPACE.name
) : AppDestination

@Serializable
data object Explore : AppDestination

@Serializable
data object SortFilterSheet : AppDestination

@Serializable
data class AppDetail(
    val appId: String,
    val versionId: Long,
    val storeName: String
) : AppDestination

@Serializable
data class AppPage(
    val packageName :String
) : AppDestination

@Serializable
data object SearchPage : AppDestination

@Serializable
data object PrefsReposPage : AppDestination

@Serializable
data object CreateAppRelease : AppDestination

@Serializable
data class UpdateAppRelease(val appDetailJson: String) : AppDestination

// --- 消息、账单、支付 ---
@Serializable
data object MessageCenter : AppDestination

@Serializable
data object Billing : AppDestination

@Serializable
data object PaymentCenterAdvanced : AppDestination

@Serializable
data class PaymentForApp(
    val appId: Long,
    val appName: String,
    val versionId: Long,
    val price: Int,
    val iconUrl: String,
    val previewContent: String
) : AppDestination

@Serializable
data class PaymentForPost(
    val postId: Long,
    val postTitle: String,
    val previewContent: String,
    val authorName: String,
    val authorAvatar: String,
    val postTime: String
) : AppDestination

// --- 其他 ---
@Serializable
data object RankingList : AppDestination

@Serializable
data class Player(val bvid: String) : AppDestination