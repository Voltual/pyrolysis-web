//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版

package me.voltual.pyrolysis

// Jetpack Compose 核心基础与布局
import androidx.compose.foundation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusManager
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// Jetpack Navigation 3
import androidx.navigation3.runtime.*
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.NavBackStack

// Kotlin 协程与流
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Koin 依赖注入
import org.koin.compose.koinInject

// 项目核心基础库、数据层与网络 (Core & Data)
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

// 项目业务 UI 界面 (Feature Screens)
import me.voltual.pyrolysis.ui.*
import me.voltual.pyrolysis.ui.auth.LoginScreen
import me.voltual.pyrolysis.ui.community.*
import me.voltual.pyrolysis.ui.community.compose.PostDetailScreen
import me.voltual.pyrolysis.ui.home.*
import me.voltual.pyrolysis.ui.rank.RankingListScreen
import me.voltual.pyrolysis.ui.search.SearchScreen
import me.voltual.pyrolysis.ui.plaza.ResourcePlazaScreen
import me.voltual.pyrolysis.ui.plaza.AppDetailScreen
import me.voltual.pyrolysis.ui.plaza.AppReleaseScreen
import me.voltual.pyrolysis.ui.player.PlayerScreen
import me.voltual.pyrolysis.ui.user.*
import me.voltual.pyrolysis.ui.user.compose.UserListScreen
import me.voltual.pyrolysis.ui.message.MessageCenterScreen
import me.voltual.pyrolysis.ui.payment.PaymentCenterScreen
import me.voltual.pyrolysis.ui.billing.BillingScreen
import me.voltual.pyrolysis.ui.log.LogScreen
import me.voltual.pyrolysis.ui.settings.update.UpdateSettingsScreen
import me.voltual.pyrolysis.ui.settings.signin.SignInSettingsScreen
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime


val topLevelRoutes: Set<NavKey> = setOf(Home)

@Composable
fun PyrolysisApp(
    agreementDataStore: UserAgreementDataStore = koinInject(), 
    modifier: Modifier = Modifier,
    platformEntryProvider: @Composable (NavKey, Navigator) -> (@Composable () -> Unit)? = { _, _ -> null }
) {
    val navigationState = rememberNavigationState(
        startRoute = Home,
        topLevelRoutes = topLevelRoutes
    )
    val focusManager = LocalFocusManager.current 
    val topAppBarController = remember { TopAppBarController() }
    val navigator = remember(focusManager, topAppBarController, navigationState) {
        Navigator(navigationState, focusManager, topAppBarController)
    }

    CompositionLocalProvider(
        LocalNavigator provides navigator,
        LocalNavigationState provides navigationState,
        LocalTopAppBarController provides topAppBarController,
    ) {
        val snackbarHostState = remember { SnackbarHostState() }

/*        val userAccepted by agreementDataStore.isUserAgreementAccepted.collectAsState(initial = true)
        val xiaoquAccepted by agreementDataStore.isXiaoquAccepted.collectAsState(initial = true)

        var isAgreementDataLoaded by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            delay(150)
            isAgreementDataLoaded = true
        }

        val showAgreementDialog = isAgreementDataLoaded && !(userAccepted && xiaoquAccepted)*/
   // 强行改成 false，直接不渲染弹窗
val showAgreementDialog = false // isAgreementDataLoaded && !(userAccepted && xiaoquAccepted)

        BBQTheme(appDarkTheme = ThemeManager.isAppDarkTheme) {
            MainScreenContent(
                navigationState = navigationState,
                navigator = navigator,
                snackbarHostState = snackbarHostState,
                showAgreementDialog = showAgreementDialog,
                platformEntryProvider = platformEntryProvider
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
    showAgreementDialog: Boolean,
    platformEntryProvider: @Composable (NavKey, Navigator) -> (@Composable () -> Unit)?
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

/*    val isLoggedIn = remember { mutableStateOf(false) }
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
    }*/

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
                                        contentDescription = "返回", // 彻底移除 Android R 依赖
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            } else {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "打开菜单", // 彻底移除 Android R 依赖
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
                            // 优先调用外部平台注入的页面提供器
                            platformEntryProvider(key, navigator)
                        }
                    )

                    if (showAgreementDialog) {
                        UserAgreementDialog(
                            onAgreed = { /* ... */ },
                        )
                    }

                    CheckForUpdates(snackbarHostState)
                    WasmDebugClock(
                        modifier = Modifier
                            .align(Alignment.BottomEnd) // 居于右下角悬浮
                            .padding(16.dp)
                            .zIndex(999f) // 确保在最顶层，不被页面组件遮挡
                    )
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
    // 适配 KMP 协程，在 Default 调度器启动，并在需要时切换
    CoroutineScope(Dispatchers.Default).launch {
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

@Composable
fun WasmDebugClock(modifier: Modifier = Modifier) {
    // 自动刷新时间戳
    var currentTimeString by remember { mutableStateOf("00:00:00.000") }
    // 交互状态：点击时改变颜色
    var clickCount by remember { mutableStateOf(0) }
    val colors = listOf(Color.Red, Color.Green, Color.Blue, Color.Magenta, Color.Cyan)
    val currentBgColor = colors[clickCount % colors.size]

    // 每帧或每隔一定时间获取系统时间，测试渲染引擎是否在工作
    LaunchedEffect(Unit) {
        while (true) {
            try {
                // 使用 kotlinx.datetime 获取本地时间
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                val milli = (Clock.System.now().toEpochMilliseconds() % 1000).toString().padStart(3, '0')
                currentTimeString = "${now.hour.toString().padStart(2, '0')}:${now.minute.toString().padStart(2, '0')}:${now.second.toString().padStart(2, '0')}.$milli"
            } catch (e: Exception) {
                // 后备方案：如果 kotlinx.datetime 在 Wasm 环境下因环境问题崩溃，改用时间戳递增
                currentTimeString = "Render Active: ${LocalTime.now() /* 或任意递增值 */}"
            }
            delay(33) // 约 30 FPS 刷新率，足够观察是否卡顿
        }
    }

    // 悬浮窗 UI
    Box(
        modifier = modifier
            .background(currentBgColor.copy(alpha = 0.8f), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .clickable { 
                // 如果能走进这个回调，说明触控/鼠标交互事件分发正常
                clickCount++ 
            }
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
            Text(
                text = "Wasm Debug Clock",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.bind))
            Text(
                text = currentTimeString,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.bind))
            Text(
                text = "点击次数: $clickCount",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Yellow
            )
        }
    }
}