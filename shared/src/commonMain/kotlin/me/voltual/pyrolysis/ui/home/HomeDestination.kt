//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版

package me.voltual.pyrolysis.ui.home

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.voltual.pyrolysis.AppStore
import me.voltual.pyrolysis.AuthRepository 
import me.voltual.pyrolysis.ui.*
import me.voltual.pyrolysis.core.ui.theme.BBQTheme
import me.voltual.pyrolysis.core.ui.theme.ThemeManager
import me.voltual.pyrolysis.core.ui.theme.ThemeMode
import org.koin.compose.viewmodel.koinViewModel 
import org.koin.compose.koinInject            

@Composable
fun HomeDestination(
    snackbarHostState: SnackbarHostState
) {
    val viewModel: HomeViewModel = koinViewModel()
    val authRepository: AuthRepository = koinInject()
    
    val uiState by viewModel.uiState
    val navigator = LocalNavigator.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val userCredentials = authRepository.credentials.first()
        val isLoggedIn = userCredentials.token.isNotEmpty()
        viewModel.updateLoginState(isLoggedIn)
        if (isLoggedIn && uiState.dataLoadState == DataLoadState.NotLoaded) {
            viewModel.loadUserData()
        }
    }

    // 获取系统暗色状态用于切换逻辑
    val systemIsDark = isSystemInDarkTheme()

    val onAvatarClick = remember(systemIsDark) {
        {
            if (!uiState.showLoginPrompt) {
                viewModel.toggleDarkMode(systemIsDark)
                val modeName = when (ThemeManager.themeMode) {
                    ThemeMode.SYSTEM -> "跟随系统"
                    ThemeMode.LIGHT -> "亮色"
                    ThemeMode.DARK -> "深色"
                }
                viewModel.showSnackbar("已切换至$modeName")
            } else {
                navigator.navigate(Login)
            }
        }
    }

    val onAvatarLongClick = remember {
        {
            if (!uiState.showLoginPrompt) {
                viewModel.refreshUserData() 
            }
        }
    }

    val onLoginClick = remember {
        { navigator.navigate(Login) }
    }

    // BBQTheme 内部已处理 ThemeManager.themeMode，无需传参
    BBQTheme {
        HomeScreen(
            state = HomeState(
                showLoginPrompt = uiState.showLoginPrompt,
                isLoading = uiState.isLoading,
                avatarUrl = uiState.avatarUrl,
                nickname = uiState.nickname,
                level = uiState.level,
                coins = uiState.coins,
                exp = uiState.exp,
                userId = uiState.userId,
                followersCount = uiState.followersCount,
                fansCount = uiState.fansCount,
                postsCount = uiState.postsCount,
                likesCount = uiState.likesCount,
                seriesDays = uiState.seriesDays,
                signStatusMessage = uiState.signStatusMessage,
                displayDaysDiff = uiState.displayDaysDiff
            ),
            onPaymentCenterClick = { navigator.navigate(PaymentCenterAdvanced) },
            onAvatarClick = onAvatarClick,
            onAvatarLongClick = onAvatarLongClick,
            onMessageCenterClick = { navigator.navigate(MessageCenter) },
            onBrowseHistoryClick = { navigator.navigate(BrowseHistory) },
            onMyLikesClick = { navigator.navigate(MyLikes) },
            onFollowersClick = { navigator.navigate(FollowList) },
            onFansClick = { navigator.navigate(FanList) },
            onPostsClick = {
                coroutineScope.launch {
                    val userId = authRepository.userId.first()
                    if (userId > 0) {
                        val nickname = uiState.nickname
                        navigator.navigate(MyPosts(userId, nickname))
                    } else {
                        viewModel.showSnackbar("未能获取用户id")
                    }
                }
            },
            onMyResourcesClick = {
                coroutineScope.launch {
                    val userId = authRepository.userId.first()
                    if (userId > 0) {
                        navigator.navigate(ResourcePlaza(isMyResource = true, userId = userId))
                    } else {
                        viewModel.showSnackbar("请先登录")
                        navigator.navigate(Login)
                    }
                }
            },
            onBillingClick = { navigator.navigate(Billing) },
            onLoginClick = onLoginClick,
            onSettingsClick = { navigator.navigate(ThemeCustomize) },
            onSignClick = { viewModel.signIn() }, 
            onAboutClick = { navigator.navigate(About) },
            onAccountProfileClick = { navigator.navigate(AccountProfile(AppStore.XIAOQU_SPACE)) },
            onRecalculateDays = { viewModel.recalculateDaysDiff() },
            onNavigateToMyReviews = { navigator.navigate(MyReviews) },
            onNavigateToMyComments = { navigator.navigate(MyComments) },
            onNavigateToCreateAppRelease = { navigator.navigate(CreateAppRelease) },
            modifier = Modifier.fillMaxSize(),
            viewModel = viewModel,
            snackbarHostState = snackbarHostState
        )
    }
