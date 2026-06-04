// FILE: me.voltual.pyrolysis.feature.store.repository/XiaoQuRepository.kt
//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.pyrolysis.feature.store.repository

import io.ktor.client.call.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.utils.io.writeFully
import io.ktor.utils.io.writer
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import me.voltual.pyrolysis.AppStore
import me.voltual.pyrolysis.AuthRepository // 导入新 Repository
import me.voltual.pyrolysis.KtorClient
import me.voltual.pyrolysis.data.unified.*
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

/**
 * 小趣空间数据仓库实现。
 * 现已完全去 Context 化，使用 AuthRepository 注入。
 * 已适配 ByteArray 上传，支持 Wasm 平台。
 */
class XiaoQuRepository(
    private val apiClient: KtorClient.ApiService,
    private val authRepository: AuthRepository // 注入 AuthRepository
) : IAppStoreRepository {

    private val fileSystem = SystemFileSystem

    private suspend fun getToken(): String {
        // 直接从注入的 Repository 获取 token，不再需要 BBQApplication.instance
        return authRepository.credentials.first().token
    }

    override suspend fun getCurrentUserDetail(): Result<UnifiedUserDetail> {
        return try {
            val token = getToken()
            if (token.isEmpty()) return Result.failure(Exception("未登录"))

            apiClient.getUserInfo(token = token).map { response ->
                if (response.code == 1) {
                    UnifiedUserDetail(
                        id = response.data.id,
                        username = response.data.username,
                        displayName = response.data.nickname,
                        avatarUrl = response.data.usertx,
                        hierarchy = response.data.hierarchy,
                        money = response.data.money,
                        followersCount = response.data.followerscount,
                        fansCount = response.data.fanscount,
                        postCount = response.data.postcount,
                        likeCount = response.data.likecount,
                        store = AppStore.XIAOQU_SPACE,
                        raw = response.data
                    )
                } else {
                    throw Exception("获取用户信息失败: ${response.msg}")
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateUserProfile(params: UpdateUserProfileParams): Result<Unit> {
        return try {
            val token = getToken()
            if (token.isEmpty()) return Result.failure(Exception("未登录"))

            if (!params.nickname.isNullOrEmpty()) {
                apiClient.modifyUserInfo(token = token, nickname = params.nickname).getOrThrow().let {
                    if (it.code != 1) throw Exception("昵称修改失败")
                }
            }

            if (!params.qqNumber.isNullOrEmpty()) {
                apiClient.modifyUserInfo(token = token, qq = params.qqNumber).getOrThrow().let {
                    if (it.code != 1) throw Exception("QQ号修改失败")
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadAvatar(imageBytes: ByteArray, filename: String): Result<String> {
        return try {
            val token = getToken()
            if (token.isEmpty()) return Result.failure(Exception("未登录"))

            apiClient.uploadAvatar(appid = 1, token = token, file = imageBytes, filename = filename).map { response ->
                if (response.code == 1) "上传成功" else throw Exception("头像上传失败: ${response.msg}")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCategories(): Result<List<UnifiedCategory>> {
        val categories = listOf(
            UnifiedCategory("null_null", "最新分享"),
            UnifiedCategory("45_47", "影音阅读"),
            UnifiedCategory("45_55", "音乐听歌"),
            UnifiedCategory("45_61", "休闲娱乐"),
            UnifiedCategory("45_58", "文件管理"),
            UnifiedCategory("45_59", "图像摄影"),
            UnifiedCategory("45_53", "输入方式"),
            UnifiedCategory("45_54", "生活出行"),
            UnifiedCategory("45_50", "社交通讯"),
            UnifiedCategory("45_56", "上网浏览"),
            UnifiedCategory("45_60", "其他类型"),
            UnifiedCategory("45_62", "跑酷竞技")
        )
        return Result.success(categories)
    }

    private fun parseCategory(id: String?): Pair<Int?, Int?> {
        if (id == null || id == "null_null") return null to null
        val parts = id.split("_")
        return parts.getOrNull(0)?.toIntOrNull() to parts.getOrNull(1)?.toIntOrNull()
    }

    override suspend fun getApps(categoryId: String?, page: Int, userId: String?): Result<Pair<List<UnifiedAppItem>, Int>> {
        return try {
            val (catId, subCatId) = parseCategory(categoryId)
            val limit = if (userId != null) 12 else 9

            apiClient.getAppsList(
                limit = limit,
                page = page,
                sortOrder = "desc",
                categoryId = catId,
                subCategoryId = subCatId,
                userId = userId?.toLongOrNull()
            ).map { response ->
                if (response.code == 1) {
                    Pair(response.data.list.map { it.toUnifiedAppItem() }, response.data.pagecount.coerceAtLeast(1))
                } else {
                    throw Exception("API Error: ${response.msg}")
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchApps(query: String, page: Int, userId: String?): Result<Pair<List<UnifiedAppItem>, Int>> {
         return try {
            apiClient.getAppsList(
                limit = 20,
                page = page,
                appName = query,
                sortOrder = "desc",
                userId = userId?.toLongOrNull()
            ).map { response ->
                if (response.code == 1) {
                    Pair(response.data.list.map { it.toUnifiedAppItem() }, response.data.pagecount.coerceAtLeast(1))
                } else {
                    throw Exception("Search failed: ${response.msg}")
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAppDetail(appId: String, versionId: Long): Result<UnifiedAppDetail> {
        return try {
            val token = getToken()
            apiClient.getAppsInformation(token = token, appsId = appId.toLong(), appsVersionId = versionId).map { response ->
                if (response.code == 1) response.data.toUnifiedAppDetail() else throw Exception("API Error: ${response.msg}")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAppComments(appId: String, versionId: Long, page: Int): Result<Pair<List<UnifiedComment>, Int>> {
        return try {
            apiClient.getAppsCommentList(appsId = appId.toLong(), appsVersionId = versionId, limit = 20, page = page, sortOrder = "desc").map { response ->
                if (response.code == 1) {
                    Pair(response.data.list.map { it.toUnifiedComment() }, response.data.pagecount.coerceAtLeast(1))
                } else {
                    throw Exception("API Error: ${response.msg}")
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun postComment(appId: String, versionId: Long, content: String, parentCommentId: String?, mentionUserId: String?): Result<Unit> {
        return try {
            val token = getToken()
            val parentId = parentCommentId?.toLongOrNull() ?: 0L
            apiClient.postAppComment(token = token, content = content, appsId = appId.toLong(), appsVersionId = versionId, parentId = parentId).map { response ->
                if (response.code == 1) Unit else throw Exception("API Error: ${response.msg}")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteComment(commentId: String): Result<Unit> {
        return try {
            val token = getToken()
            apiClient.deleteAppComment(token = token, commentId = commentId.toLong()).map { response ->
                if (response.code == 1) Unit else throw Exception("API Error: ${response.msg}")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteApp(appId: String, versionId: Long): Result<Unit> {
        return try {
            val token = getToken()
            if (token.isEmpty()) throw Exception("未登录")
            apiClient.deleteApp(usertoken = token, apps_id = appId.toLong(), app_version_id = versionId).map { response ->
                if (response.code == 1) Unit else throw Exception(response.msg)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun releaseApp(params: UnifiedAppReleaseParams): Result<Unit> {
        return try {
            val token = getToken()
            if (token.isEmpty()) throw Exception("未登录")

            val introImagesString = params.introImages?.joinToString(",") ?: ""

            val result = if (params.isUpdate) {
                apiClient.updateApp(
                    usertoken = token,
                    apps_id = params.appId ?: 0L,
                    appname = params.appName,
                    icon = params.iconUrl,
                    app_size = params.sizeInMb.toString(),
                    app_introduce = params.introduce ?: "",
                    app_introduction_image = introImagesString,
                    file = params.apkUrl ?: "",
                    app_explain = params.explain,
                    app_version = params.versionName,
                    is_pay = params.isPay ?: 0,
                    pay_money = params.payMoney,
                    category_id = params.categoryId ?: 0,
                    sub_category_id = params.subCategoryId ?: 0
                )
            } else {
                apiClient.releaseApp(
                    usertoken = token,
                    appname = params.appName,
                    icon = params.iconUrl,
                    app_size = params.sizeInMb.toString(),
                    app_introduce = params.introduce ?: "",
                    app_introduction_image = introImagesString,
                    file = params.apkUrl ?: "",
                    app_explain = params.explain,
                    app_version = params.versionName,
                    is_pay = params.isPay ?: 0,
                    pay_money = params.payMoney,
                    category_id = params.categoryId ?: 0,
                    sub_category_id = params.subCategoryId ?: 0
                )
            }

            result.map { response ->
                if (response.code == 1) Unit else throw Exception(response.msg)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // 保持 Path 版本以兼容旧代码或非 Wasm 平台
    override suspend fun uploadImage(path: Path, type: String): Result<String> {
        return Result.failure(UnsupportedOperationException("Path-based upload is not supported in this context. Use ByteArray overload."))
    }

    override suspend fun uploadImage(bytes: ByteArray, filename: String, type: String): Result<String> {
        return try {
            val response = KtorClient.uploadHttpClient.submitForm(
                url = "api.php",
                formParameters = parameters {
                    // Ktor 能够直接处理 ByteArray
                    append("file", bytes, Headers.build {
                        append(HttpHeaders.ContentType, ContentType.Image.Any)
                        append(HttpHeaders.ContentDisposition, "filename=\"$filename\"")
                    })
                }
            )

            if (response.status.isSuccess()) {
                val responseBody: KtorClient.UploadResponse = response.body()
                if (responseBody.code == 0 && !responseBody.downurl.isNullOrBlank()) {
                    Result.success(responseBody.downurl)
                } else {
                    Result.failure(Exception(responseBody.msg))
                }
            } else {
                Result.failure(Exception("网络错误 ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // 保持 Path 版本以兼容旧代码或非 Wasm 平台
    override suspend fun uploadApk(path: Path, serviceType: String): Result<String> {
        return Result.failure(UnsupportedOperationException("Path-based upload is not supported in this context. Use ByteArray overload."))
    }

    override suspend fun uploadApk(bytes: ByteArray, filename: String, serviceType: String): Result<String> {
        return try {
            when (serviceType) {
                "KEYUN" -> uploadToKeyun(bytes, filename)
                "WANYUEYUN" -> uploadToWanyueyun(bytes, filename)
                else -> Result.failure(Exception("不支持的上传服务类型: $serviceType"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun uploadToKeyun(bytes: ByteArray, filename: String): Result<String> {
        return try {
            val response = KtorClient.uploadHttpClient.submitForm(
                url = "api.php",
                formParameters = parameters {
                    append("file", bytes, Headers.build {
                        append(HttpHeaders.ContentType, ContentType.Application.OctetStream)
                        append(HttpHeaders.ContentDisposition, "filename=\"$filename\"")
                    })
                }
            )

            if (response.status.isSuccess()) {
                val responseBody: KtorClient.UploadResponse = response.body()
                if (responseBody.code == 0 && !responseBody.downurl.isNullOrBlank()) {
                    Result.success(responseBody.downurl)
                } else {
                    Result.failure(Exception(responseBody.msg))
                }
            } else {
                Result.failure(Exception("网络错误 ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun uploadToWanyueyun(bytes: ByteArray, filename: String): Result<String> {
        return try {
            val response = KtorClient.wanyueyunUploadHttpClient.submitForm(
                url = "upload",
                formParameters = parameters {
                    append("Api", "小趣API")
                    append("file", bytes, Headers.build {
                        append(HttpHeaders.ContentType, ContentType.Application.Vnd.AndroidPackageArchive)
                        append(HttpHeaders.ContentDisposition, "filename=\"$filename\"")
                    })
                }
            )

            if (response.status.isSuccess()) {
                val responseBody: KtorClient.WanyueyunUploadResponse = response.body()
                if (responseBody.code == 200 && !responseBody.data.isNullOrBlank()) {
                    Result.success(responseBody.data)
                } else {
                    Result.failure(Exception(responseBody.msg))
                }
            } else {
                Result.failure(Exception("网络错误 ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteReview(reviewId: String): Result<Unit> = Result.failure(Exception("小趣空间暂不支持评价功能。"))

    override suspend fun getMyReviews(page: Int): Result<Pair<List<UnifiedComment>, Int>> = Result.failure(Exception("小趣空间暂不支持获取我的评价功能。"))

    override suspend fun deleteComment(appId: String, commentId: String): Result<Unit> = deleteComment(commentId)

    override suspend fun getMyComments(page: Int): Result<Pair<List<UnifiedComment>, Int>> = Result.failure(NotImplementedError("小趣空间不支持获取我的评论"))

    override suspend fun getAppDownloadSources(appId: String, versionId: Long): Result<List<UnifiedDownloadSource>> {
        return getAppDetail(appId, versionId).map { detail ->
            if (detail.downloadUrl != null) {
                listOf(UnifiedDownloadSource(name = "默认下载", url = detail.downloadUrl, isOfficial = true))
            } else {
                emptyList()
            }
        }
    }
}