// FILE: me.voltual.pyrolysis.feature.store.repository/IAppStoreRepository.kt
//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.pyrolysis.feature.store.repository

import me.voltual.pyrolysis.data.unified.*
import kotlinx.io.files.Path

/**
 * 统一的应用商店仓库接口定义。
 * 已移除 java.io 依赖，全面适配 Okio。
 * 新增 ByteArray 重载以支持 Wasm 等无文件系统环境。
 */
interface IAppStoreRepository {

    // 给默认实现返回"不支持"

    suspend fun getCategories(): Result<List<UnifiedCategory>> =
        Result.failure(UnsupportedOperationException("当前商店不支持获取分类"))

    suspend fun getApps(categoryId: String?, page: Int, userId: String?): Result<Pair<List<UnifiedAppItem>, Int>> =
        Result.failure(UnsupportedOperationException("当前商店不支持获取应用列表"))

    suspend fun searchApps(query: String, page: Int, userId: String?): Result<Pair<List<UnifiedAppItem>, Int>> =
        Result.failure(UnsupportedOperationException("当前商店不支持搜索应用"))

    suspend fun getAppDetail(appId: String, versionId: Long): Result<UnifiedAppDetail> =
        Result.failure(UnsupportedOperationException("当前商店不支持获取应用详情"))

    suspend fun getAppComments(appId: String, versionId: Long, page: Int): Result<Pair<List<UnifiedComment>, Int>> =
        Result.failure(UnsupportedOperationException("当前商店不支持查看评论"))

    suspend fun postComment(appId: String, versionId: Long, content: String, parentCommentId: String?, mentionUserId: String?): Result<Unit> =
        Result.failure(UnsupportedOperationException("当前商店不支持发布评论"))

    suspend fun deleteComment(commentId: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("当前商店不支持删除评论"))

    suspend fun deleteComment(appId: String, commentId: String): Result<Unit> =
        deleteComment(commentId) // 默认转发给单参数版本，减少子类重写负担

    suspend fun toggleFavorite(appId: String, isCurrentlyFavorite: Boolean): Result<Boolean> =
        Result.failure(UnsupportedOperationException("当前商店不支持收藏功能"))

    suspend fun deleteApp(appId: String, versionId: Long): Result<Unit> =
        Result.failure(UnsupportedOperationException("当前商店不支持卸载/删除应用"))

    suspend fun getAppDownloadSources(appId: String, versionId: Long): Result<List<UnifiedDownloadSource>> =
        Result.failure(UnsupportedOperationException("当前商店不支持获取下载源"))

    suspend fun releaseApp(params: UnifiedAppReleaseParams): Result<Unit> =
        Result.failure(UnsupportedOperationException("当前商店不支持发布应用"))

    suspend fun getMyReviews(page: Int): Result<Pair<List<UnifiedComment>, Int>> =
        Result.failure(UnsupportedOperationException("当前商店不支持获取我的评价"))

    suspend fun getMyComments(page: Int): Result<Pair<List<UnifiedComment>, Int>> =
        Result.failure(UnsupportedOperationException("当前商店不支持获取我的评论"))

    suspend fun deleteReview(reviewId: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("当前商店不支持删除评价"))

    // ==========================================================
    // 文件与用户相关：默认“不支持”
    // ==========================================================

    /**
     * 上传图片，使用 okio.Path 代替 java.io.File (适用于 JVM/Native)
     */
    suspend fun uploadImage(path: Path, type: String): Result<String> =
        Result.failure(UnsupportedOperationException("当前商店不支持基于路径的图片上传"))

    /**
     * 上传图片，使用 ByteArray (适用于所有平台, 特别是 Wasm)
     */
    suspend fun uploadImage(bytes: ByteArray, filename: String, type: String): Result<String> =
        Result.failure(UnsupportedOperationException("当前商店不支持基于字节的图片上传"))


    /**
     * 上传 APK，使用 okio.Path 代替 java.io.File (适用于 JVM/Native)
     */
    suspend fun uploadApk(path: Path, serviceType: String): Result<String> =
        Result.failure(UnsupportedOperationException("当前商店不支持基于路径的 APK 上传"))

    /**
     * 上传 APK，使用 ByteArray (适用于所有平台, 特别是 Wasm)
     */
    suspend fun uploadApk(bytes: ByteArray, filename: String, serviceType: String): Result<String> =
        Result.failure(UnsupportedOperationException("当前商店不支持基于字节的 APK 上传"))

    suspend fun getCurrentUserDetail(): Result<UnifiedUserDetail> =
        Result.failure(UnsupportedOperationException("当前商店不支持获取用户信息"))

    suspend fun updateUserProfile(params: UpdateUserProfileParams): Result<Unit> =
        Result.failure(UnsupportedOperationException("当前商店不支持修改个人资料"))

    suspend fun uploadAvatar(imageBytes: ByteArray, filename: String): Result<String> =
        Result.failure(UnsupportedOperationException("当前商店不支持上传头像"))

    suspend fun getAppVersionsByPackageName(packageName: String): Result<List<UnifiedAppItem>> {
        return Result.failure(UnsupportedOperationException("当前商店不支持获取版本列表"))
    }

    suspend fun getAppVersionsByPackageName(packageName: String, page: Int): Result<Pair<List<UnifiedAppItem>, Int>> {
        return Result.failure(UnsupportedOperationException("当前商店不支持获取分页的版本列表"))
    }

    /**
     * 获取应用相对于当前登录用户的收藏/点赞状态
     */
    suspend fun getFavoriteState(appId: String): Result<UnifiedFavoriteState> =
        Result.success(UnifiedFavoriteState(isFavorite = false))
}