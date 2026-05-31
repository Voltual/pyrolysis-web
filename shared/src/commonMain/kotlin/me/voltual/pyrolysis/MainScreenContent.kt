//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.pyrolysis

// Androidx & Jetpack Compose 核心基础与布局
import androidx.compose.foundation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

// Jetpack Material 3 设计组件与图标
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search

// Jetpack Compose 状态管理
import androidx.compose.runtime.*

// Jetpack Lifecycle & ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

// Jetpack Navigation 3 (包含 ViewModel 装饰器)
import androidx.navigation3.runtime.*
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator

// Kotlin 协程与流
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Koin 依赖注入 (Android, Compose & ViewModel)
import org.koin.compose.koinInject
import org.koin.androidx.compose.koinViewModel

// ----------------------------------------------------
// 项目核心基础库、数据层与网络 (Core & Data)
// ----------------------------------------------------
import me.voltual.pyrolysis.AppStore
import me.voltual.pyrolysis.KtorClient
import me.voltual.pyrolysis.data.UpdateInfo
import me.voltual.pyrolysis.data.UpdateSettingsDataStore
import me.voltual.pyrolysis.data.UserAgreementDataStore
import me.voltual.pyrolysis.core.database.LogEntry
import me.voltual.pyrolysis.core.database.LogDao
import me.voltual.pyrolysis.core.utils.UpdateCheckResult
import me.voltual.pyrolysis.core.utils.UpdateChecker

// 项目通用 UI 组件、主题与动画 (Core UI)
import me.voltual.pyrolysis.core.ui.theme.*
import me.voltual.pyrolysis.core.ui.theme.ThemeCustomizeScreen
import me.voltual.pyrolysis.core.ui.components.UserAgreementDialog
import me.voltual.pyrolysis.core.ui.components.UpdateDialog
import me.voltual.pyrolysis.core.ui.animation.*

// ----------------------------------------------------
// 项目业务 UI 界面 (Feature Screens)
// ----------------------------------------------------
import me.voltual.pyrolysis.ui.*

// 认证 (Auth)
import me.voltual.pyrolysis.ui.auth.LoginScreen
import me.voltual.pyrolysis.ui.auth.LoginViewModel

// 社区 (Community)
import me.voltual.pyrolysis.ui.community.*
import me.voltual.pyrolysis.ui.community.compose.PostDetailScreen

// 主页与排行 (Home & Rank)
import me.voltual.pyrolysis.ui.home.*
import me.voltual.pyrolysis.ui.rank.RankingListScreen

// 搜索 (Search)
import me.voltual.pyrolysis.ui.search.SearchScreen
import me.voltual.pyrolysis.ui.search.SearchViewModel

// 广场、应用详情与发现 (Plaza & App)
import me.voltual.pyrolysis.ui.plaza.ResourcePlazaScreen
import me.voltual.pyrolysis.ui.plaza.AppPage
import me.voltual.pyrolysis.ui.plaza.SearchPage
import me.voltual.pyrolysis.ui.plaza.ExplorePage
import me.voltual.pyrolysis.ui.plaza.SortFilterSheet
import me.voltual.pyrolysis.ui.plaza.AppDetailScreen
import me.voltual.pyrolysis.ui.plaza.AppReleaseScreen
import me.voltual.pyrolysis.ui.plaza.AppReleaseViewModel

// 播放器 (Player)
import me.voltual.pyrolysis.ui.player.PlayerScreen
import me.voltual.pyrolysis.ui.player.PlayerViewModel

// 用户中心 (User)
import me.voltual.pyrolysis.ui.user.*
import me.voltual.pyrolysis.ui.user.compose.UserListScreen

// 消息与通知 (Message)
import me.voltual.pyrolysis.ui.message.MessageCenterScreen
import me.voltual.pyrolysis.ui.message.MessageViewModel

// 支付与账单 (Payment & Billing)
import me.voltual.pyrolysis.ui.payment.PaymentCenterScreen
import me.voltual.pyrolysis.ui.payment.PaymentType
import me.voltual.pyrolysis.ui.payment.PaymentViewModel
import me.voltual.pyrolysis.ui.billing.BillingScreen
import me.voltual.pyrolysis.ui.billing.BillingViewModel

// 日志 (Log)
import me.voltual.pyrolysis.ui.log.LogScreen
import me.voltual.pyrolysis.ui.log.LogViewModel

// 设置中心 (Settings)
import me.voltual.pyrolysis.ui.settings.repos.PrefsReposPage
import me.voltual.pyrolysis.ui.settings.storage.StoreManagerScreen
import me.voltual.pyrolysis.ui.settings.signin.SignInSettingsScreen
import me.voltual.pyrolysis.ui.settings.update.UpdateSettingsScreen
import me.voltual.pyrolysis.ui.settings.update.UpdateSettingsViewModel

val topLevelRoutes: Set<NavKey> = setOf(Home)

@Composable
fun PyrolysisApp(
    agreementDataStore: UserAgreementDataStore = koinInject(), 
    modifier: Modifier = Modifier
) {
    // 1. 初始化全局导航相关状态
    val navigationState = rememberNavigationState(
        startRoute = Home,
        topLevelRoutes = topLevelRoutes
    )
    val focusManager = LocalFocusManager.current 
    val topAppBarController = remember { TopAppBarController() }
    val navigator = remember(focusManager, topAppBarController, navigationState) {
        Navigator(navigationState, focusManager, topAppBarController)
    }

    // 2. 提供全局的 CompositionLocal 注入
    CompositionLocalProvider(
        LocalNavigator provides navigator,
        LocalNavigationState provides navigationState,
        LocalTopAppBarController provides topAppBarController,
    ) {
        val snackbarHostState = remember { SnackbarHostState() }

        // 3. 处理用户协议与隐私政策状态
        val userAccepted by agreementDataStore.isUserAgreementAccepted.collectAsState(initial = true)
        val xiaoquAccepted by agreementDataStore.isXiaoquAccepted.collectAsState(initial = true)

        var isAgreementDataLoaded by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            delay(150)
            isAgreementDataLoaded = true
        }

        val showAgreementDialog = isAgreementDataLoaded && !(userAccepted && xiaoquAccepted)

        // 4. 应用全局主题
        BBQTheme(appDarkTheme = ThemeManager.isAppDarkTheme) {
            MainScreenContent(
                navigationState = navigationState,
                navigator = navigator,
                snackbarHostState = snackbarHostState,
                showAgreementDialog = showAgreementDialog
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(
    navigationState: NavigationState,
    navigator: Navigator,
    snackbarHostState: SnackbarHostState,
    showAgreementDialog: Boolean
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val authRepository: AuthRepository = koinInject() 
    val themeStore: ThemeColorDataStore = koinInject()

    val currentRoute = navigationState.currentRoute
    val currentTopLevelRoute = navigationState.topLevelRoute

    val showBackButton = remember(currentRoute) {
        currentRoute != Home && currentRoute != Login
    }
    
    val topAppBarController = LocalTopAppBarController.current

    val isPlayerScreen = remember(currentRoute) { currentRoute is Player }

    val useDarkTheme = ThemeManager.isAppDarkTheme
    val lightBgUri by themeStore.drawerHeaderLightBackgroundUriFlow.collectAsState(initial = null)
    val darkBgUri by themeStore.drawerHeaderDarkBackgroundUriFlow.collectAsState(initial = null)
    val drawerHeaderBackgroundUri = if (useDarkTheme) darkBgUri else lightBgUri

    val isLoggedIn = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val credentials = authRepository.credentials.first()
        isLoggedIn.value = credentials.userId != 0L
        if (isLoggedIn.value) {
            tryAutoLogin(
                username = credentials.username, 
                password = credentials.password, 
                authRepository = authRepository, 
                navigator = navigator, 
                snackbarHostState = snackbarHostState
            )
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            Box(modifier = Modifier.width(360.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .roundScreenPadding()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    DrawerHeader(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        backgroundUri = drawerHeaderBackgroundUri
                    )
                    NavigationDrawerItems(
                        navigator = navigator,
                        currentTopLevelRoute = currentTopLevelRoute,
                        drawerState = drawerState,
                        scope = scope
                    )
                }
            }
        },
        gesturesEnabled = true,
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
            topBar = {
                if (!isPlayerScreen) {
                    TopAppBar(
                        title = {
                            val customContent = topAppBarController.titleContent
                            if (customContent != null) {
                                customContent()
                            } else {
                                Text(
                                    text = topAppBarController.customTitle ?: getTitleForDestination(currentRoute),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                            }
                        },
                        navigationIcon = {
                            if (showBackButton) {
                                IconButton(onClick = { navigator.goBack() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.back),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            } else {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = stringResource(R.string.open_drawer),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        },
                        actions = {
                            if (currentRoute != Login) {
                                IconButton(onClick = {
                                    navigator.navigate(Search(userId = null, nickname = null))
                                }) {
                                    Icon(Icons.Default.Search, "搜索", tint = MaterialTheme.colorScheme.onSurface)
                                }
                                IconButton(onClick = { navigator.navigate(CreatePost) }) {
                                    Icon(Icons.Default.Add, "发帖", tint = MaterialTheme.colorScheme.onSurface)
                                }
                                IconButton(onClick = { navigator.navigate(BrowseHistory) }) {
                                    Icon(Icons.Default.History, "浏览历史", tint = MaterialTheme.colorScheme.onSurface)
                                }

                                topAppBarController.actions.forEach { action ->
                                    val iconTint = action.tint?.invoke() ?: MaterialTheme.colorScheme.onSurface
                                    IconButton(onClick = action.onClick) {
                                        action.icon(iconTint)
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            },
            snackbarHost = { BBQSnackbarHost(hostState = snackbarHostState) },
            content = { innerPadding ->
                val contentPadding = when {
                    isPlayerScreen -> PaddingValues(0.dp)
                    else -> innerPadding
                }

                val currentBackStack = navigationState.backStacks[currentTopLevelRoute] 
                    ?: navigationState.backStacks[navigationState.startRoute]!! 

                Box(modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding) 
                    .roundScreenPadding()
                ) {
                    BBQNavDisplay(
                        backStack = currentBackStack,
                        onBack = { navigator.goBack() },
                        snackbarHostState = snackbarHostState,
                        modifier = Modifier.fillMaxSize(),
                        platformEntryProvider = { key ->
                            when (key) {
                                is PrefsReposPage -> {
                                    { PrefsReposPage() }
                                }
                                is StoreManager -> {
                                    { StoreManagerScreen() }
                                }
                                is AppPage -> {
                                    { AppPage(packageName = key.packageName, onDismiss = { navigator.goBack() }) }
                                }
                                is SearchPage -> {
                                    { SearchPage(onDismiss = { navigator.goBack() }) }
                                }
                                is Explore -> {
                                    { ExplorePage() }
                                }
                                is SortFilterSheet -> {
                                    { SortFilterSheet(onDismiss = { navigator.goBack() }) }
                                }
                                else -> null
                            }
                        }
                    )

                    if (showAgreementDialog) {
                        UserAgreementDialog(
                            onAgreed = { /* ... */ },
                        )
                    }

                    CheckForUpdates(snackbarHostState)
                }
            }
        )
    }
}

@Composable
fun getTitleForDestination(route: NavKey?): String {
    return when (route) {
        Home -> "首页"
        Login -> "登录"
        is ResourcePlaza -> {
            when {
                route.isMyResource -> "我的资源"
                route.userId != -1L -> "Ta的资源"
                route.mode == "my_upload" -> "我的上传"
                route.mode == "my_favourite" -> "我的收藏"
                route.mode == "my_history" -> "历史足迹"
                else -> "资源广场"
            }
        }
        RankingList -> "天梯竞赛"
        MessageCenter -> "消息中心"
        BrowseHistory -> "浏览历史"
        Billing -> "账单"
        ThemeCustomize -> "主题设置"
        is Search -> "搜索"
        is PostDetail -> "帖子详情"
        is UserDetail -> "用户详情"
        is MyPosts -> "我的帖子"
        CreatePost -> "创建新帖"
        is CreateRefundPost -> "申请退币"
        CreateAppRelease -> "发布应用"
        is UpdateAppRelease -> "更新应用"
        LogViewer -> "日志"
        is AccountProfile -> "账号资料"
        FollowList -> "我的关注"
        FanList -> "我的粉丝"
        MyLikes -> "我喜欢的"
        HotPosts -> "热点"
        FollowingPosts -> "关注的人"
        Community -> "社区"
        is PaymentForApp -> "买应用"
        is PaymentForPost -> "给帖子投币"
        PaymentCenterAdvanced -> "投币"
        is Player -> "视频播放"
        About -> "关于"
        is ImagePreview -> "图片预览"
        StoreManager -> "存储管理"
        is AppDetail -> "应用详情"
        is AppPage -> "应用页"
        UpdateSettings -> "更新设置"
        MyComments -> "我的评论"
        MyReviews -> "我的评价"
        SignInSettings -> "签到设置"
        PrefsReposPage -> "仓库管理"
        Explore -> "仓库探索"
        SearchPage -> "搜索页"
        SortFilterSheet -> "排序和过滤"
        else -> "在~ $route ~里~哦"
    }
}

@Composable
fun CheckForUpdates(snackbarHostState: SnackbarHostState) {
    val coroutineScope = rememberCoroutineScope()
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    val updateSettingsDataStore: UpdateSettingsDataStore = koinInject()
    
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val autoCheckUpdates = updateSettingsDataStore.autoCheckUpdates.first()
        if (autoCheckUpdates) {
            UpdateChecker.checkForUpdates() { result ->
                when (result) {
                    is UpdateCheckResult.Success -> {
                        updateInfo = result.updateInfo
                        showDialog = true
                    }
                    is UpdateCheckResult.NoUpdate -> {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("当前已是最新版本")
                        }
                    }
                    is UpdateCheckResult.Error -> {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(result.message)
                        }
                    }
                }
            }
        }
    }

    updateInfo?.let { info ->
        if (showDialog) {
            UpdateDialog(updateInfo = info) {
                showDialog = false
                updateInfo = null
            }
        }
    }
}


private fun tryAutoLogin(
    username: String,
    password: String,
    authRepository: AuthRepository, 
    navigator: Navigator,
    snackbarHostState: SnackbarHostState
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val deviceId = authRepository.deviceId.first()
            val result = KtorClient.ApiServiceImpl.login(
                username = username,
                password = password,
                device = deviceId
            )

            withContext(Dispatchers.Main) {
                when {
                    result.isSuccess -> {
                        val loginResponse = result.getOrNull()
                        if (loginResponse != null && loginResponse.code == 1) {
                            val loginData = loginResponse.data
                            if (loginData != null) {
                                authRepository.saveCredentials(
                                    username,
                                    password,
                                    loginData.usertoken,
                                    loginData.id
                                )
                            } else {
                                authRepository.clearCredentials()
                                snackbarHostState.showSnackbar("登录数据为空")
                                navigator.navigate(Login)
                            }
                        } else {
                            authRepository.clearCredentials()
                            val errorMsg = loginResponse?.msg ?: "登录失败"
                            snackbarHostState.showSnackbar(errorMsg)
                            navigator.navigate(Login)
                        }
                    }
                    else -> {
                        authRepository.clearCredentials()
                        val exception = result.exceptionOrNull()
                        val errorMsg = when (exception) {
                            is PyrolysisNetworkException -> {
                                when {
                                    exception.message?.contains("429") == true -> "请求太频繁"
                                    exception.message?.contains("500") == true -> "服务器错误"
                                    else -> "网络错误: ${exception.message}"
                                }
                            }
                            else -> "登录异常: ${exception?.message ?: "未知错误"}"
                        }
                        snackbarHostState.showSnackbar(errorMsg)
                        navigator.navigate(Login)
                    }
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                authRepository.clearCredentials()
                snackbarHostState.showSnackbar("登录异常: ${e.message}")
                navigator.navigate(Login)
            }
        }
    }
}