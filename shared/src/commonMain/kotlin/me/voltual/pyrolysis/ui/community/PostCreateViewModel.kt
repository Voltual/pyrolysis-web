//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.pyrolysis.ui.community

import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.name
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.voltual.pyrolysis.util.getAndroidDeviceInfo
import me.voltual.pyrolysis.AuthRepository
import me.voltual.pyrolysis.KtorClient
import me.voltual.pyrolysis.core.database.PostDraftRepository
import me.voltual.pyrolysis.data.PostDraftDataStore

class PostCreateViewModel(
    private val authRepository: AuthRepository,        // 注入 AuthRepository
    private val draftRepository: PostDraftRepository,   // 注入 Repository
    private val draftDataStore: PostDraftDataStore      // 注入 DataStore
) : ViewModel() { // 变为普通 ViewModel

    private val _uiState = MutableStateFlow(PostCreateUiState())
    val uiState: StateFlow<PostCreateUiState> = _uiState.asStateFlow()

    private val _preferencesState = MutableStateFlow(DraftPreferencesState())
    val preferencesState: StateFlow<DraftPreferencesState> = _preferencesState.asStateFlow()

    private val _postStatus = MutableStateFlow<PostStatus>(PostStatus.Idle)
    val postStatus: StateFlow<PostStatus> = _postStatus.asStateFlow()

    private val _showRestoreDialog = MutableStateFlow(false)
    val showRestoreDialog: StateFlow<Boolean> = _showRestoreDialog.asStateFlow()

    private var _snackbarHostState: SnackbarHostState? = null

    fun setSnackbarHostState(snackbarHostState: SnackbarHostState) {
        _snackbarHostState = snackbarHostState
    }

    init {
        viewModelScope.launch {
            draftDataStore.preferencesFlow.first().let { preferences ->
                _preferencesState.value = DraftPreferencesState(
                    autoRestoreDraft = preferences.autoRestoreDraft,
                    noStoreDraft = preferences.noStoreDraft
                )
            }

            draftRepository.draftFlow.first()?.let { draft ->
                if (draft.title.isNotBlank() || draft.content.isNotBlank() || draft.imageUrls.isNotBlank()) {
                    if (_preferencesState.value.autoRestoreDraft) {
                        restoreDraft(draft)
                    } else {
                        _showRestoreDialog.value = true
                    }
                }
            }
        }
        observeAndAutoSave()
    }

    private fun restoreDraft(draft: PostDraftRepository.DraftDto) {
        _uiState.update {
            it.copy(
                title = draft.title,
                content = draft.content,
                imageUrls = draft.imageUrls,
                selectedSubsectionId = draft.subsectionId
            )
        }
    }

    fun onRestoreDialogConfirm() {
        viewModelScope.launch {
            draftRepository.draftFlow.first()?.let { restoreDraft(it) }
            _showRestoreDialog.value = false
        }
    }

    fun onRestoreDialogDismiss() {
        _showRestoreDialog.value = false
    }

    fun setAutoRestoreDraft(enabled: Boolean) {
        viewModelScope.launch {
            draftDataStore.setAutoRestoreDraft(enabled)
            _preferencesState.value = _preferencesState.value.copy(autoRestoreDraft = enabled)
        }
    }

    fun setNoStoreDraft(enabled: Boolean) {
        viewModelScope.launch {
            draftDataStore.setNoStoreDraft(enabled)
            _preferencesState.value = _preferencesState.value.copy(noStoreDraft = enabled)
            if (enabled) draftRepository.clearDraft()
        }
    }

    fun onTitleChange(newTitle: String) {
        _uiState.update { it.copy(title = newTitle) }
    }

    fun onContentChange(newContent: String) {
        _uiState.update { it.copy(content = newContent) }
    }

    fun onSubsectionChange(newId: Int) {
        _uiState.update { it.copy(selectedSubsectionId = newId) }
    }

    fun uploadImage(file: PlatformFile) {
        if (_preferencesState.value.noStoreDraft) {
            showSnackbar("草稿存储已禁用")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(showProgressDialog = true, progressMessage = "正在上传图片...") }

            uploadImageKtor(file).onSuccess { imageUrl ->
                _uiState.update { currentState ->
                    val newUrlMap = currentState.imageUriToUrlMap + (file to imageUrl)
                    currentState.copy(
                        imageUrls = newUrlMap.values.joinToString(","),
                        imageUriToUrlMap = newUrlMap,
                        showProgressDialog = false
                    )
                }
                showSnackbar("图片上传成功")
            }.onFailure { e ->
                showSnackbar("上传失败: ${e.message}")
                _uiState.update { it.copy(showProgressDialog = false) }
            }
        }
    }

    private suspend fun uploadImageKtor(file: PlatformFile): Result<String> = withContext(Dispatchers.Default) {
        try {
            val fileBytes = file.readBytes()
            val response: HttpResponse = KtorClient.uploadHttpClient.post("api.php") {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("file", fileBytes, Headers.build {
                                append(HttpHeaders.ContentType, "image/jpeg")
                                append(HttpHeaders.ContentDisposition, "filename=\"${file.name}\"")
                            })
                        }
                    )
                )
            }

            val responseBody: KtorClient.UploadResponse = response.body()
            if (responseBody.code == 0 && !responseBody.downurl.isNullOrBlank()) {
                Result.success(responseBody.downurl)
            } else {
                Result.failure(Exception("上传失败: ${responseBody.msg}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun removeImage(file: PlatformFile) {
        _uiState.update { currentState ->
            val newUrlMap = currentState.imageUriToUrlMap - file
            currentState.copy(
                imageUrls = newUrlMap.values.joinToString(","),
                imageUriToUrlMap = newUrlMap
            )
        }
    }

    fun createPost(
        title: String,
        imageUrls: String,
        subsectionId: Int,
        bvNumber: String,
        tempDeviceName: String,
        mode: String,
        refundAppId: Long = 0L,
        refundVersionId: Long = 0L,
        refundPayMoney: Int = 0,
        selectedRefundReason: String = ""
    ) {
        viewModelScope.launch {
            _postStatus.value = PostStatus.Loading

            // 直接从 Repository 获取凭证，不再需要 getApplication()
            val credentials = authRepository.credentials.first()
            if (credentials.userId == 0L || credentials.token.isEmpty()) {
                _postStatus.value = PostStatus.Error("请先登录")
                showSnackbar("请先登录")
                return@launch
            }

            val videoPart = if (bvNumber.isNotBlank()) "【视频：$bvNumber】" else ""
            
            val androidDeviceInfo = getAndroidDeviceInfo()

val finalContent = if (mode == "refund") {
    """
    ${_uiState.value.content}

    问题类型:$selectedRefundReason$androidDeviceInfo
    退还金额:$refundPayMoney
    资源id:$refundAppId
    资源版本:$refundVersionId 機型：$tempDeviceName｜$videoPart
    """.trimIndent()
} else {
    "${_uiState.value.content} 機型：$tempDeviceName｜$videoPart"
}

            val finalSubsectionId = if (mode == "refund") 21 else subsectionId

            withContext(Dispatchers.Default) {
                KtorClient.ApiServiceImpl.createPost(
                    token = credentials.token,
                    title = title,
                    content = finalContent,
                    sectionId = finalSubsectionId,
                    imageUrls = imageUrls.ifBlank { null }
                )
            }.onSuccess {
                _postStatus.value = PostStatus.Success
                if (!_preferencesState.value.noStoreDraft) draftRepository.clearDraft()
                showSnackbar("发帖成功")
            }.onFailure { e ->
                _postStatus.value = PostStatus.Error(e.message ?: "发帖失败")
                showSnackbar("发帖失败: ${e.message}")
            }
        }
    }

    fun clearDraft() {
        viewModelScope.launch { draftRepository.clearDraft() }
    }

    fun resetPostStatus() {
        _postStatus.value = PostStatus.Idle
    }

    @OptIn(FlowPreview::class)
    private fun observeAndAutoSave() {
        _uiState
            .debounce(1000)
            .onEach { state ->
                if (_preferencesState.value.noStoreDraft) return@onEach

                if (state.title.isNotBlank() || state.content.isNotBlank() || state.imageUrls.isNotBlank()) {
                    draftRepository.saveDraft(
                        PostDraftRepository.DraftDto(
                            title = state.title,
                            content = state.content,
                            imageUris = emptyList(),
                            imageUrls = state.imageUrls,
                            subsectionId = state.selectedSubsectionId
                        )
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun showSnackbar(message: String) {
        viewModelScope.launch {
            _snackbarHostState?.showSnackbar(message)
        }
    }
}

data class PostCreateUiState(
    val title: String = "",
    val content: String = "",
    val selectedSubsectionId: Int = 11,
    val imageUrls: String = "",
    val imageUriToUrlMap: Map<PlatformFile, String> = emptyMap(),
    val showProgressDialog: Boolean = false,
    val progressMessage: String = ""
)

data class DraftPreferencesState(
    val autoRestoreDraft: Boolean = false,
    val noStoreDraft: Boolean = false
)

sealed class PostStatus {
    data object Idle : PostStatus()
    data object Loading : PostStatus()
    data object Success : PostStatus()
    data class Error(val message: String) : PostStatus()
}