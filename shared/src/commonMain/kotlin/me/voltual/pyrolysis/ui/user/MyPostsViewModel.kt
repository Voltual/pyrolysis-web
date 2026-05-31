//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.pyrolysis.ui.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.voltual.pyrolysis.KtorClient
import me.voltual.pyrolysis.data.UserFilterDataStore
import org.koin.compose.viewmodel.koinViewModel


class MyPostsViewModel(private val userFilterDataStore: UserFilterDataStore) : ViewModel() {
    
    private val _posts = MutableStateFlow<List<KtorClient.Post>>(emptyList())
    val posts: StateFlow<List<KtorClient.Post>> = _posts.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private var _nickname: String? = null
    
    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()
    
    private var currentPage = 1
    private val _totalPages = MutableStateFlow(1)
    val totalPages: StateFlow<Int> = _totalPages.asStateFlow()
    
    private var _currentUserId: Long? = null
    private var _isInitialized = false
    
    fun setUserId(userId: Long) {
        setUserInfo(userId, "用户")
    }
    
    fun setUserInfo(userId: Long, nickname: String) {
        if (_currentUserId != userId || _nickname != nickname) {
            _currentUserId = userId
            _nickname = nickname
            _isInitialized = false
            resetState()
            
            viewModelScope.launch {
                userFilterDataStore.addOrUpdateUserFilter(userId, nickname)
                userFilterDataStore.setActiveUserFilter(userId)
            }
            
            loadDataIfNeeded()
        }
    }
    
    private fun resetState() {
        _posts.value = emptyList()
        currentPage = 1
        _totalPages.value = 1
        _errorMessage.value = ""
    }
    
    private fun loadDataIfNeeded() {
        if (!_isInitialized && _currentUserId != null && !_isLoading.value) {
            _isInitialized = true
            loadMyPosts()
        }
    }
    
    fun jumpToPage(page: Int) {
        if (page in 1.._totalPages.value) {
            _posts.value = emptyList()
            currentPage = page
            loadMyPosts()
        }
    }
    
    fun loadNextPage() {
        if (currentPage < _totalPages.value && !_isLoading.value) {
            currentPage++
            loadMyPosts()
        }
    }
    
    fun refresh() {
        currentPage = 1
        loadMyPosts()
    }
    
    /**
     * 加载帖子列表
     * 使用 Kotlin 风格的 Result 处理，彻底移除 java.io 依赖
     */
    private fun loadMyPosts() {
        val userId = _currentUserId ?: return
        if (_isLoading.value) return
        
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = ""
            
            KtorClient.ApiServiceImpl.getPostsList(
                limit = PAGE_SIZE,
                page = currentPage,
                userId = userId
            ).onSuccess { response ->
                if (response.code == 1) {
                    val data = response.data
                    _totalPages.value = data.pagecount
                    
                    val currentList = if (currentPage == 1) emptyList() else _posts.value
                    // 数据合并与去重
                    _posts.value = (currentList + data.list).distinctBy { it.postid }
                    _errorMessage.value = ""
                } else {
                    _errorMessage.value = "操作失败: ${response.msg.ifEmpty { "服务器错误" }}"
                }
            }.onFailure { exception ->
                // 此时 exception 可能是 PyrolysisNetworkException 或其他 Kotlin 异常
                _errorMessage.value = "加载失败: ${exception.message ?: "未知错误"}"
            }
            
            _isLoading.value = false
        }
    }
    
    private companion object {
        const val PAGE_SIZE = 10
    }
}