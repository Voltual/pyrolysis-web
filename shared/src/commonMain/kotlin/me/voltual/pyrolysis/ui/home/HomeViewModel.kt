//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.pyrolysis.ui.home

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.voltual.pyrolysis.AuthRepository
import me.voltual.pyrolysis.KtorClient
import me.voltual.pyrolysis.core.ui.theme.ThemeManager
import me.voltual.pyrolysis.data.SignInSettingsDataStore
import kotlin.time.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

sealed class DataLoadState {
    data object NotLoaded : DataLoadState()
    data object Loading : DataLoadState()
    data object Loaded : DataLoadState()
    data object Error : DataLoadState()
}

data class HomeUiState(
    val showLoginPrompt: Boolean = true,
    val isLoading: Boolean = false,
    val avatarUrl: String? = null,
    val nickname: String = "You",
    val level: String = "LV0",
    val coins: String = "null",
    val userId: String = "null",
    val followersCount: String = "?",
    val fansCount: String = "?",
    val postsCount: String = "?",
    val signToday: Boolean = false,
    val likesCount: String = "?",
    val seriesDays: Int = 0,
    val signStatusMessage: String? = null,
    val createTime: String = "",
    val exp: Int = 0, 
    val lastSignTime: String = "",
    val displayDaysDiff: Int = 0,
    val dataLoadState: DataLoadState = DataLoadState.NotLoaded,
)

class HomeViewModel(
    private val authRepository: AuthRepository,
    private val signInSettingsDataStore: SignInSettingsDataStore
) : ViewModel() {
    
    var uiState = mutableStateOf(HomeUiState())
        private set
    var snackbarHostState = mutableStateOf<SnackbarHostState?>(null)
        private set

    fun setSnackbarHostState(hostState: SnackbarHostState) {
        snackbarHostState.value = hostState
    }

    private fun String.toLocalDateTime(): LocalDateTime? = runCatching {
        LocalDateTime.parse(this.replace(" ", "T"))
    }.getOrNull()

    fun loadUserData(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (!forceRefresh && uiState.value.dataLoadState == DataLoadState.Loaded) {
                return@launch
            }

            val userCredentials = authRepository.credentials.first()
            val token = userCredentials.token

            // 防御：如果空 token 理论上不该走到这，直接重置
            if (token.isEmpty()) {
                updateLoginState(false)
                return@launch
            }

            uiState.value = uiState.value.copy(
                isLoading = true,
                dataLoadState = DataLoadState.Loading
            )

            withContext(Dispatchers.Default) {
                KtorClient.ApiServiceImpl.getUserInfo(token = token)
            }.onSuccess { response ->
                val userData = response.data
                val daysDiff = calculateDaysDiff(userData.create_time, userData.signlasttime)
                
                val signStatusMessage = if (!userData.sign_today) {
                    "点这里签到领经验和硬币哦"
                } else {
                    null
                }

                uiState.value = uiState.value.copy(
                    showLoginPrompt = false,
                    avatarUrl = userData.usertx,
                    nickname = userData.nickname,
                    level = userData.hierarchy,
                    coins = userData.money.toString(),
                    userId = userData.username,
                    followersCount = userData.followerscount,
                    fansCount = userData.fanscount,
                    postsCount = userData.postcount,
                    likesCount = userData.likecount,
                    seriesDays = userData.series_days,
                    createTime = userData.create_time,
                    lastSignTime = userData.signlasttime,
                    displayDaysDiff = daysDiff,
                    isLoading = false,
                    exp = userData.exp,
                    signToday = userData.sign_today,
                    signStatusMessage = signStatusMessage,
                    dataLoadState = DataLoadState.Loaded
                )
                
                checkAndAutoSignIn()
            }.onFailure {
                uiState.value = uiState.value.copy(
                    isLoading = false,
                    dataLoadState = DataLoadState.Error
                )
            }
        }
    }        
    
    private fun checkAndAutoSignIn() {
        viewModelScope.launch {
            if (uiState.value.signToday) return@launch
            
            val autoSignInEnabled = signInSettingsDataStore.autoSignIn.first()
            if (autoSignInEnabled) {
                signIn(isAutoSignIn = true)
            }
        }
    }
    
    fun signIn(isAutoSignIn: Boolean = false) {
        viewModelScope.launch {
            val credentials = authRepository.credentials.first()
            val token = credentials.token

            uiState.value = uiState.value.copy(isLoading = true)

            withContext(Dispatchers.Default) {
                KtorClient.ApiServiceImpl.userSignIn(token = token)
            }.onSuccess { result ->
                if (result.code == 401) {
                    uiState.value = uiState.value.copy(
                        signStatusMessage = "登录已过期，请长按头像刷新",
                        showLoginPrompt = true,
                        signToday = true,
                        isLoading = false
                    )
                    resetLoadState()
                } else {
                    val message = if (isAutoSignIn) "自动签到成功: ${result.msg}" else result.msg
                    
                    uiState.value = uiState.value.copy(
                        signStatusMessage = message,
                        isLoading = false
                    )

                    launch {
                        delay(2000)
                        uiState.value = uiState.value.copy(signStatusMessage = null)
                    }

                    refreshUserData()
                }
            }.onFailure { e ->
                val message = if (isAutoSignIn) "自动签到失败: ${e.message}" else "签到失败: ${e.message}"
                uiState.value = uiState.value.copy(
                    signStatusMessage = message,
                    isLoading = false
                )
            }
        }
    }          

    fun refreshUserData() {
        loadUserData(forceRefresh = true)
    }

    fun resetLoadState() {
        uiState.value = uiState.value.copy(
            dataLoadState = DataLoadState.NotLoaded,
            showLoginPrompt = true
        )
    }

    @OptIn(kotlin.time.ExperimentalTime::class)
    fun calculateDaysDiff(startDate: String, endDate: String): Int {
        return try {
            val startDt = startDate.toLocalDateTime() ?: return 0
            val endDt = endDate.toLocalDateTime() ?: return 0
            
            val startInstant = startDt.toInstant(TimeZone.UTC)
            val endInstant = endDt.toInstant(TimeZone.UTC)
            
            (endInstant - startInstant).inWholeDays.toInt()
        } catch (e: Exception) {
            0
        }
    }

    @OptIn(kotlin.time.ExperimentalTime::class)
    fun recalculateDaysDiff() {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val currentTime = with(now) {
            "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')} " +
            "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}:${second.toString().padStart(2, '0')}"
        }
        val daysDiff = calculateDaysDiff(uiState.value.createTime, currentTime)
        uiState.value = uiState.value.copy(displayDaysDiff = daysDiff)
    }

    fun toggleDarkMode(systemIsDark: Boolean) {
        ThemeManager.toggleTheme(systemIsDark)
    }


    // 🌟 修复：这个方法现在被 UI 层的 LaunchedEffect 正常调用了
    fun updateLoginState(isLoggedIn: Boolean) {
        uiState.value = uiState.value.copy(showLoginPrompt = !isLoggedIn)
        if (!isLoggedIn) {
            resetLoadState()
        }
    }

    fun showSnackbar(message: String) {
        viewModelScope.launch {
            snackbarHostState.value?.showSnackbar(message)
        }
    }    
}