// shared/src/commonMain/kotlin/me/voltual/pyrolysis/ui/player/PlayerViewModel.kt
package me.voltual.pyrolysis.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import me.voltual.pyrolysis.api.BiliApiManager
import me.voltual.pyrolysis.data.PlayerDataStore
import me.voltual.pyrolysis.utils.decompressDanmaku // 导入抽象函数
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

enum class VideoScaleMode { FIT, FILL, ZOOM }

data class PlayerSettings(
    val scaleMode: VideoScaleMode = VideoScaleMode.FIT,
    val danmakuSize: Float = 1.2f
)

@KoinViewModel
class PlayerViewModel(
    private val playerDataStore: PlayerDataStore
) : ViewModel() {

    private val _playerUiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val playerUiState = _playerUiState.asStateFlow()

    private val _settings = MutableStateFlow(PlayerSettings())
    val settings = _settings.asStateFlow()

    init {
        viewModelScope.launch {
            playerDataStore.settingsFlow.firstOrNull()?.let {
                _settings.value = it
            }
        }
    }

    fun updateScaleMode(scaleMode: VideoScaleMode) {
        viewModelScope.launch {
            val newSettings = _settings.value.copy(scaleMode = scaleMode)
            _settings.value = newSettings
            playerDataStore.saveSettings(newSettings)
        }
    }

    fun updateDanmakuSize(newSize: Float) {
        viewModelScope.launch {
            val newSettings = _settings.value.copy(danmakuSize = newSize)
            _settings.value = newSettings
            playerDataStore.saveSettings(newSettings)
        }
    }    

    fun loadVideoData(bvid: String) {
        viewModelScope.launch {
            _playerUiState.value = PlayerUiState.Loading
            try {
                val infoResult = BiliApiManager.instance.getVideoInfo(bvid)
                
                if (infoResult.isFailure) {
                    _playerUiState.value = PlayerUiState.Error("获取视频信息失败: ${infoResult.exceptionOrNull()?.message}")
                    return@launch
                }

                val videoInfo = infoResult.getOrThrow()
                if (videoInfo.code != 0 || videoInfo.data == null) {
                    _playerUiState.value = PlayerUiState.Error("获取视频信息失败: ${videoInfo.message}")
                    return@launch
                }

                val videoData = videoInfo.data
                val page = videoData.pages.firstOrNull() ?: run {
                    _playerUiState.value = PlayerUiState.Error("未找到视频分页信息")
                    return@launch
                }

                val cid = page.cid
                var playUrl: String? = null
                var danmakuData: ByteArray? = null
                var errorMsg: String? = null

                // 这里的并发请求在 KMP commonMain 中完全支持
                val playUrlJob = launch {
                    try {
                        val playUrlResult = BiliApiManager.instance.getPlayUrl(bvid, cid)
                        if (playUrlResult.isSuccess) {
                            val playUrlResponse = playUrlResult.getOrThrow()
                            if (playUrlResponse.code == 0) {
                                playUrl = playUrlResponse.data?.durl?.firstOrNull()?.url
                            } else {
                                errorMsg = "获取播放地址失败: ${playUrlResponse.message}"
                            }
                        } else {
                            errorMsg = "网络错误: ${playUrlResult.exceptionOrNull()?.message}"
                        }
                    } catch (e: Exception) { 
                        errorMsg = "异常: ${e.message}" 
                    }
                }

                val danmakuJob = launch {
                    try {
                        val danmakuResult = BiliApiManager.instance.getDanmaku(cid)
                        if (danmakuResult.isSuccess) {
                            // 调用跨平台解压函数
                            danmakuData = decompressDanmaku(danmakuResult.getOrThrow())
                        }
                    } catch (e: Exception) { 
                        // 弹幕加载失败不影响主流程
                    }
                }

                playUrlJob.join()
                danmakuJob.join()

                if (playUrl != null) {
                    _playerUiState.value = PlayerUiState.Success(
                        title = videoData.title,
                        videoUrl = playUrl!!,
                        danmakuData = danmakuData
                    )
                } else {
                    _playerUiState.value = PlayerUiState.Error(errorMsg ?: "未知错误")
                }
            } catch (e: Exception) { 
                _playerUiState.value = PlayerUiState.Error("请求失败: ${e.message}") 
            }
        }
    }
}

// PlayerUiState 保持不变，它已经是纯 Kotlin 代码
sealed class PlayerUiState {
    object Loading : PlayerUiState()
    data class Success(
        val title: String,
        val videoUrl: String,
        val danmakuData: ByteArray?
    ) : PlayerUiState() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Success) return false
            if (title != other.title) return false
            if (videoUrl != other.videoUrl) return false
            return danmakuData.contentEquals(other.danmakuData)
        }

        override fun hashCode(): Int {
            var result = title.hashCode()
            result = 31 * result + videoUrl.hashCode()
            result = 31 * result + (danmakuData?.contentHashCode() ?: 0)
            return result
        }
    }
    data class Error(val message: String) : PlayerUiState()
}