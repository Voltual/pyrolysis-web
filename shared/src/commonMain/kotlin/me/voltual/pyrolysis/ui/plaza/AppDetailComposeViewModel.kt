//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.pyrolysis.ui.plaza

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import me.voltual.pyrolysis.AppStore
import me.voltual.pyrolysis.feature.store.repository.IAppStoreRepository
import me.voltual.pyrolysis.data.unified.*
import kotlinx.coroutines.flow.MutableSharedFlow
import me.voltual.pyrolysis.KtorClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope


class AppDetailComposeViewModel(
    private val repositories: Map<AppStore, IAppStoreRepository>
) : ViewModel() {

    private val _appDetail = MutableStateFlow<UnifiedAppDetail?>(null)
    val appDetail: StateFlow<UnifiedAppDetail?> = _appDetail.asStateFlow()

    private val _comments = MutableStateFlow<List<UnifiedComment>>(emptyList())
    val comments: StateFlow<List<UnifiedComment>> = _comments.asStateFlow()

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()

    private val _showCommentDialog = MutableStateFlow(false)
    val showCommentDialog: StateFlow<Boolean> = _showCommentDialog.asStateFlow()

    private val _showReplyDialog = MutableStateFlow(false)
    val showReplyDialog: StateFlow<Boolean> = _showReplyDialog.asStateFlow()

    private val _currentReplyComment = MutableStateFlow<UnifiedComment?>(null)
    val currentReplyComment: StateFlow<UnifiedComment?> = _currentReplyComment.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var currentStore: AppStore = AppStore.XIAOQU_SPACE
    private var currentAppId: String = ""
    private var currentVersionId: Long = 0L

    private val _downloadSources = MutableStateFlow<List<UnifiedDownloadSource>>(emptyList())
    val downloadSources: StateFlow<List<UnifiedDownloadSource>> = _downloadSources.asStateFlow()

    private val _showDownloadDrawer = MutableStateFlow(false)
    val showDownloadDrawer: StateFlow<Boolean> = _showDownloadDrawer.asStateFlow()

    private val _snackbarEvent = MutableSharedFlow<String>()
    val snackbarEvent: SharedFlow<String> = _snackbarEvent.asSharedFlow()
    
/*    private val _navigateToDownloadEvent = MutableSharedFlow<Boolean>()
    val navigateToDownloadEvent: SharedFlow<Boolean> = _navigateToDownloadEvent.asSharedFlow()
*/
    private val _openUrlEvent = MutableSharedFlow<String>()
    val openUrlEvent: SharedFlow<String> = _openUrlEvent.asSharedFlow()

    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()
    
    private val _shareLink = MutableStateFlow<String?>(null)
    val shareLink: StateFlow<String?> = _shareLink.asStateFlow()

    private val repository: IAppStoreRepository
        get() = repositories[currentStore] ?: throw IllegalStateException("Repository not found")
        
    private val _refundEvent = MutableSharedFlow<RefundInfo>()
    val refundEvent: SharedFlow<RefundInfo> = _refundEvent.asSharedFlow()

    private val _updateEvent = MutableSharedFlow<String>()
    val updateEvent: SharedFlow<String> = _updateEvent.asSharedFlow()
    
    private val _navigateToPaymentEvent = MutableSharedFlow<PaymentInfo>()
    val navigateToPaymentEvent: SharedFlow<PaymentInfo> = _navigateToPaymentEvent.asSharedFlow()

    data class PaymentInfo(
        val appId: Long,
        val appName: String,
        val versionId: Long,
        val price: Int,
        val iconUrl: String,
        val previewContent: String
    )

    data class RefundInfo(
        val appId: String,
        val versionId: Long,
        val appName: String,
        val payMoney: Int
    )    

    fun initializeData(appId: String, versionId: Long, storeName: String) {
        val store = try {
            AppStore.valueOf(storeName)
        } catch (e: Exception) {
            AppStore.XIAOQU_SPACE
        }

        if (currentAppId == appId && currentVersionId == versionId && currentStore == store && _appDetail.value != null) {
            return
        }

        currentAppId = appId
        currentVersionId = versionId
        currentStore = store

        loadData()
    }
    
    fun requestRefund() {
        viewModelScope.launch {
            val detail = _appDetail.value ?: return@launch
            
            if (detail.store == AppStore.XIAOQU_SPACE) {
                val raw = detail.raw as? me.voltual.pyrolysis.KtorClient.AppDetail
                if (raw != null) {
                    _refundEvent.emit(
                        RefundInfo(
                            appId = currentAppId,
                            versionId = currentVersionId,
                            appName = detail.name,
                            payMoney = raw.pay_money
                        )
                    )
                }
            } else {
                _snackbarEvent.tryEmit("该商店不支持退币功能")
            }
        }
    }

    fun requestUpdate() {
        viewModelScope.launch {
            val detail = _appDetail.value ?: return@launch
            
            if (detail.store == AppStore.XIAOQU_SPACE) {
                val raw = detail.raw as? me.voltual.pyrolysis.KtorClient.AppDetail
                if (raw != null) {
                    val appDetailJson = KtorClient.JsonConverter.toJson(raw)
                    _updateEvent.emit(appDetailJson)
                }
            } else {
                _snackbarEvent.tryEmit("该商店不支持更新功能")
            }
        }
    }        

    fun refresh() {
        loadData()
    }

    fun checkPurchaseNeeded(): PaymentInfo? {
        val detail = _appDetail.value ?: return null
        
        if (detail.store == AppStore.XIAOQU_SPACE) {
            val raw = detail.raw as? me.voltual.pyrolysis.KtorClient.AppDetail
            if (raw != null && raw.is_pay == 1 && raw.pay_money > 0 && raw.is_user_pay != true) {
                return PaymentInfo(
                    appId = raw.id,
                    appName = raw.appname,
                    versionId = raw.apps_version_id,
                    price = raw.pay_money,
                    iconUrl = raw.app_icon,
                    previewContent = raw.app_introduce?.take(30) ?: ""
                )
            }
        }
        return null
    }
    
    fun toggleFavorite() {
        val currentDetail = _appDetail.value ?: return
        val targetState = currentDetail.isFavorite 

        viewModelScope.launch {
            val result = repository.toggleFavorite(currentAppId, currentDetail.isFavorite)

            if (result.isSuccess) {
                val finalFavoriteState = result.getOrNull() ?: targetState
                
                val newFavoriteCount = if (finalFavoriteState) {
                    (currentDetail.favoriteCount) + 1
                } else {
                    maxOf(0, (currentDetail.favoriteCount) - 1)
                }

                _appDetail.value = currentDetail.copy(
                    isFavorite = finalFavoriteState,
                    favoriteCount = newFavoriteCount
                )
                
                val message = if (finalFavoriteState) "已添加到收藏" else "已取消收藏"
                _snackbarEvent.emit(message)
            } else {
                val error = result.exceptionOrNull()?.message ?: "操作失败"
                _snackbarEvent.emit("收藏失败: $error")
            }
        }
    }

    fun handleDownloadClick() {
        viewModelScope.launch {
            val paymentInfo = checkPurchaseNeeded()
            if (paymentInfo != null) {
                _navigateToPaymentEvent.emit(paymentInfo)
            } else {
                _isLoading.value = true
                
                val result = repository.getAppDownloadSources(currentAppId, currentVersionId)
                _isLoading.value = false

                if (result.isSuccess) {
                    val sources = result.getOrThrow()
                    if (sources.isEmpty()) {
                        _errorMessage.value = "未找到下载源"
                    } else if (sources.size == 1) {
                        startDownload(sources.first().url)
                    } else {
                        _downloadSources.value = sources
                        _showDownloadDrawer.value = true
                    }
                } else {
                    _errorMessage.value = "获取下载链接失败: ${result.exceptionOrNull()?.message}"
                }
            }
        }
    }

    fun closeDownloadDrawer() {
        _showDownloadDrawer.value = false
    }

    private fun loadData() {
        _isLoading.value = true
        _errorMessage.value = ""

        viewModelScope.launch {
            try {
                coroutineScope {
                    val detailDeferred = async { repository.getAppDetail(currentAppId, currentVersionId) }
                    val favoriteDeferred = async { repository.getFavoriteState(currentAppId) }

                    val detailResult = detailDeferred.await()
                    val favoriteResult = favoriteDeferred.await()

                    if (detailResult.isSuccess) {
                        var detail: UnifiedAppDetail = detailResult.getOrThrow()
                        
                        favoriteResult.onSuccess { state ->
                            detail = detail.copy(isFavorite = state.isFavorite)
                        }

                        _appDetail.value = detail
                        loadComments()
                    } else {
                        _errorMessage.value = "加载详情失败: ${detailResult.exceptionOrNull()?.message}"
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "网络错误: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadComments() {
        viewModelScope.launch {
            val result = repository.getAppComments(currentAppId, currentVersionId, 1)
            if (result.isSuccess) {
                _comments.value = result.getOrThrow().first
            }
        }
    }

    fun openCommentDialog() {
        _showCommentDialog.value = true
        _currentReplyComment.value = null
    }

    fun closeCommentDialog() {
        _showCommentDialog.value = false
    }

    fun openReplyDialog(comment: UnifiedComment) {
        _currentReplyComment.value = comment
        _showReplyDialog.value = true
    }

    fun closeReplyDialog() {
        _showReplyDialog.value = false
        _currentReplyComment.value = null
    }

    fun submitComment(content: String) {
        viewModelScope.launch {
            val parentId = _currentReplyComment.value?.id
            val result = repository.postComment(currentAppId, currentVersionId, content, parentId, null)

            if (result.isSuccess) {
                loadComments()
                if (parentId == null) closeCommentDialog() else closeReplyDialog()
            } else {
                _errorMessage.value = "提交失败: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            val result = repository.deleteComment(commentId)
            
            if (result.isSuccess) {
                loadComments()
            } else {
                _errorMessage.value = "删除失败: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun deleteApp(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val result = repository.deleteApp(currentAppId, currentVersionId)
            if (result.isSuccess) {
                onSuccess()
            } else {
                val error = result.exceptionOrNull()?.message ?: "删除失败"
                _errorMessage.value = error
            }
        }
    }
        
    fun startDownload(downloadUrl: String) {
        viewModelScope.launch {
            try {
                _openUrlEvent.emit(downloadUrl)                
                _snackbarEvent.emit("正在尝试打开下载链接...")
            } catch (e: Exception) {
                _errorMessage.value = "启动下载失败: ${e.message}"
            }
        }
    }

}