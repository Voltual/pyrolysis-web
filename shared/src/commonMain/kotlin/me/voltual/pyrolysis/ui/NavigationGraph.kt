//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.pyrolysis.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.navigation3.runtime.NavBackStack
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.*
import androidx.navigation3.scene.DialogSceneStrategy
import kotlinx.coroutines.launch
import me.voltual.pyrolysis.AppStore
import me.voltual.pyrolysis.KtorClient
import me.voltual.pyrolysis.ui.auth.LoginScreen
import me.voltual.pyrolysis.ui.auth.LoginViewModel
import me.voltual.pyrolysis.ui.billing.BillingScreen
import me.voltual.pyrolysis.ui.billing.BillingViewModel
import me.voltual.pyrolysis.ui.community.*
import me.voltual.pyrolysis.ui.community.compose.PostDetailScreen
import me.voltual.pyrolysis.ui.home.*
import me.voltual.pyrolysis.ui.log.LogScreen
import me.voltual.pyrolysis.ui.log.LogViewModel
import me.voltual.pyrolysis.ui.message.MessageCenterScreen
import me.voltual.pyrolysis.ui.message.MessageViewModel
import me.voltual.pyrolysis.ui.payment.PaymentCenterScreen
import me.voltual.pyrolysis.ui.payment.PaymentType
import me.voltual.pyrolysis.ui.payment.PaymentViewModel
import me.voltual.pyrolysis.ui.player.PlayerScreen
import me.voltual.pyrolysis.ui.player.PlayerViewModel
import me.voltual.pyrolysis.ui.plaza.ResourcePlazaScreen
import me.voltual.pyrolysis.ui.plaza.AppDetailScreen
import me.voltual.pyrolysis.ui.plaza.AppReleaseScreen
import me.voltual.pyrolysis.ui.plaza.AppReleaseViewModel
import me.voltual.pyrolysis.ui.rank.RankingListScreen
import me.voltual.pyrolysis.ui.rank.RankingListViewModel
import me.voltual.pyrolysis.ui.search.SearchScreen
import me.voltual.pyrolysis.ui.search.SearchViewModel
import me.voltual.pyrolysis.ui.settings.signin.SignInSettingsScreen
import me.voltual.pyrolysis.ui.settings.update.UpdateSettingsScreen
import me.voltual.pyrolysis.ui.settings.update.UpdateSettingsViewModel
import me.voltual.pyrolysis.ui.settings.proxy.ProxySettingsScreen
import me.voltual.pyrolysis.core.ui.theme.ThemeCustomizeScreen
import me.voltual.pyrolysis.ui.user.*
import me.voltual.pyrolysis.ui.user.compose.UserListScreen
import androidx.compose.foundation.*
import androidx.navigation3.ui.NavDisplay
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.compose.material3.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.*
import me.voltual.pyrolysis.core.ui.animation.*

@Composable
fun BBQNavDisplay(
    backStack: List<NavKey>,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    platformEntryProvider: @Composable (NavKey) -> (@Composable () -> Unit)? = { null }
) {
    val mySceneStrategy = remember { DialogSceneStrategy<NavKey>() }
    val slideDistance = rememberSlideDistance()
    
    val decorators = listOf(
        rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
        rememberViewModelStoreNavEntryDecorator<NavKey>()
    )

    NavDisplay(
        backStack = backStack,
        onBack = onBack,
        entryDecorators = decorators,
        modifier = modifier.fillMaxSize(),
        sceneStrategy = mySceneStrategy,
        
        transitionSpec = {
            materialSharedAxisX(
                forward = true, 
                slideDistance = slideDistance
            )
        },

        popTransitionSpec = {
            materialSharedAxisX(
                forward = false, 
                slideDistance = slideDistance
            )
        },
        entryProvider = { key ->
            NavEntry(key) {
                val platformContent = platformEntryProvider(key)
                if (platformContent != null) {
                    platformContent()
                } else {
                    when (key) {
                        is Home -> {
                            HomeDestination(snackbarHostState = snackbarHostState)
                        }

                        is Login -> {
                            val navigator = LocalNavigator.current
                            val viewModel: LoginViewModel = koinViewModel()
                            LoginScreen(
                                viewModel = viewModel,
                                onLoginSuccess = { navigator.goBack() },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        is About -> {
                            AboutScreen(
                                modifier = Modifier.fillMaxSize(),
                                snackbarHostState = snackbarHostState
                            )
                        }

                        is Search -> {
                            val navigator = LocalNavigator.current
                            val viewModel: SearchViewModel = koinViewModel()
                            val userIdLong = key.userId?.toLongOrNull()
                            val nickname = key.nickname

                            LaunchedEffect(userIdLong, nickname) {
                                if (userIdLong != null && nickname != null) {
                                    viewModel.initFromNavArgs(userIdLong, nickname)
                                }
                            }
                            SearchScreen(
                                viewModel = viewModel,
                                onPostClick = { postId -> navigator.navigate(PostDetail(postId)) },
                                onLogClick = { navigator.navigate(LogViewer) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        is SignInSettings -> {
                            SignInSettingsScreen(snackbarHostState = snackbarHostState)
                        }

                        is ThemeCustomize -> {
                            ThemeCustomizeScreen(modifier = Modifier.fillMaxSize())
                        }

                        is PostDetail -> {
                            val navigator = LocalNavigator.current
                            val viewModel: PostDetailViewModel = koinViewModel()
                            PostDetailScreen(
                                postId = key.postId,
                                onPostDeleted = { navigator.goBack() },
                                viewModel = viewModel,   
                                snackbarHostState = snackbarHostState,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        is CreatePost -> {
                            val navigator = LocalNavigator.current
                            val viewModel: PostCreateViewModel = koinViewModel()
                            PostCreateScreen(
                                viewModel = viewModel,
                                onBackClick = { navigator.goBack() },
                                mode = "create",
                                refundAppName = "",
                                refundAppId = 0L,
                                refundVersionId = 0L,
                                refundPayMoney = 0,
                                snackbarHostState = snackbarHostState,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        is CreateRefundPost -> {
                            val navigator = LocalNavigator.current
                            val viewModel: PostCreateViewModel = koinViewModel()
                            PostCreateScreen(
                                viewModel = viewModel,
                                onBackClick = { navigator.goBack() },
                                mode = "refund",
                                refundAppName = key.appName,
                                refundAppId = key.appId,
                                refundVersionId = key.versionId,
                                refundPayMoney = key.payMoney,
                                snackbarHostState = snackbarHostState,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        is BrowseHistory -> {
                            val navigator = LocalNavigator.current
                            val viewModel: BrowseHistoryViewModel = koinViewModel()
                            BrowseHistoryScreen(
                                onPostClick = { postId -> navigator.navigate(PostDetail(postId)) },
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize(),
                                snackbarHostState = snackbarHostState
                            )
                        }

                        is ImagePreview -> {
                            val navigator = LocalNavigator.current
                            ImagePreviewScreen(
                                imageUrl = key.imageUrl,
                                snackbarHostState = snackbarHostState,
                                onClose = { navigator.goBack() }
                            )
                        }                

                        is MyComments -> {
                            MyCommentsScreen(modifier = Modifier.fillMaxSize())
                        }

                        is MyReviews -> {
                            MyReviewsScreen(modifier = Modifier.fillMaxSize())
                        }

                        is UserDetail -> {
                            val navigator = LocalNavigator.current
                            val viewModel: UserDetailViewModel = koinViewModel()
                            LaunchedEffect(key.userId, key.store) {
                                viewModel.loadUserDetails(key.userId, key.store)
                            }
                            UserDetailScreen(
                                viewModel = viewModel,
                                onPostsClick = {
                                    val userData = viewModel.userData.value
                                    val nickname = userData?.displayName ?: "用户"
                                    navigator.navigate(MyPosts(key.userId, nickname))
                                },
                                onResourcesClick = { uid, targetStore ->
                                    navigator.navigate(
                                        ResourcePlaza(
                                            isMyResource = false,
                                            userId = uid,
                                            mode = "public",
                                            storeName = targetStore.name
                                        )
                                    )
                                },
                                onImagePreview = { imageUrl ->
                                    navigator.navigate(ImagePreview(imageUrl))
                                },
                                modifier = Modifier.fillMaxSize(),
                                snackbarHostState = snackbarHostState
                            )
                        }

                        is MyPosts -> {
                            val viewModel: MyPostsViewModel = koinViewModel()
                            MyPostsScreen(
                                viewModel = viewModel,
                                userId = key.userId,
                                nickname = key.nickname,
                                snackbarHostState = snackbarHostState,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        is FollowList -> {
                            val navigator = LocalNavigator.current
                            val viewModel: UserListViewModel = koinViewModel()
                            LaunchedEffect(Unit) {
                                viewModel.setListType(UserListType.FOLLOWERS)
                                viewModel.loadInitialData()
                            }
                            val state by viewModel.uiState.collectAsStateWithLifecycle()
                            UserListScreen(
                                users = state.users,
                                isLoading = state.isLoading,
                                errorMessage = state.errorMessage,
                                onLoadMore = { viewModel.loadNextPage() },
                                onUserClick = { userId -> navigator.navigate(UserDetail(userId)) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        is FanList -> {
                            val navigator = LocalNavigator.current
                            val viewModel: UserListViewModel = koinViewModel()
                            LaunchedEffect(Unit) {
                                viewModel.setListType(UserListType.FANS)
                                viewModel.loadInitialData()
                            }
                            val state by viewModel.uiState.collectAsStateWithLifecycle()
                            UserListScreen(
                                users = state.users,
                                isLoading = state.isLoading,
                                errorMessage = state.errorMessage,
                                onLoadMore = { viewModel.loadNextPage() },
                                onUserClick = { userId -> navigator.navigate(UserDetail(userId)) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        is AccountProfile -> {
                            val viewModel: UserProfileViewModel = koinViewModel()
                            AccountProfileScreen(
                                modifier = Modifier.fillMaxSize(),
                                snackbarHostState = snackbarHostState,
                                viewModel = viewModel,
                                store = key.store
                            )
                        }

                        is ResourcePlaza -> {
                            val navigator = LocalNavigator.current
                            ResourcePlazaScreen(
                                isMyResourceMode = key.isMyResource,
                                mode = key.mode,
                                storeName = key.storeName,
                                navigateToAppDetail = { appId, versionId, store ->
                                    navigator.navigate(AppDetail(appId, versionId, store))
                                },
                                userId = if (key.userId != -1L) key.userId.toString() else null,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        is AppDetail -> {
                            AppDetailScreen(
                                appId = key.appId,
                                versionId = key.versionId,
                                storeName = key.storeName,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        is CreateAppRelease -> {
                            val viewModel: AppReleaseViewModel = koinViewModel()
                            AppReleaseScreen(
                                viewModel = viewModel,
                                snackbarHostState = snackbarHostState,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        is UpdateAppRelease -> {
                            val viewModel: AppReleaseViewModel = koinViewModel()
                            if (key.appDetailJson.isNotBlank()) {
                                val appDetail = KtorClient.JsonConverter.fromJson(key.appDetailJson)
                                if (appDetail != null) {
                                    viewModel.populateFromAppDetail(appDetail)
                                }
                            }
                            AppReleaseScreen(
                                viewModel = viewModel,
                                snackbarHostState = snackbarHostState,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        is LogViewer -> {
                            val viewModel: LogViewModel = koinViewModel()
                            LogScreen(
                                viewModel = viewModel,
                                snackbarHostState = snackbarHostState,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        is MessageCenter -> {
                            val navigator = LocalNavigator.current
                            val viewModel: MessageViewModel = koinViewModel()
                            MessageCenterScreen(
                                viewModel = viewModel,
                                onMessageClick = { postId -> navigator.navigate(PostDetail(postId)) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        is Billing -> {
                            val viewModel: BillingViewModel = koinViewModel()
                            LaunchedEffect(Unit) { viewModel.loadBilling() }
                            BillingScreen(viewModel = viewModel)
                        }

                        is PaymentCenterAdvanced -> {
                            val viewModel: PaymentViewModel = koinViewModel()
                            viewModel.setPaymentInfo(type = PaymentType.POST_REWARD, locked = false)
                            PaymentCenterScreen(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        is PaymentForApp -> {
                            val viewModel: PaymentViewModel = koinViewModel()
                            viewModel.setPaymentInfo(
                                type = PaymentType.APP_PURCHASE,
                                appId = key.appId,
                                appName = key.appName,
                                versionId = key.versionId,
                                price = key.price,
                                iconUrl = key.iconUrl,
                                previewContent = key.previewContent,
                                locked = true
                            )
                            PaymentCenterScreen(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        is PaymentForPost -> {
                            val viewModel: PaymentViewModel = koinViewModel()
                            viewModel.setPaymentInfo(
                                type = PaymentType.POST_REWARD,
                                postId = key.postId,
                                postTitle = key.postTitle,
                                previewContent = key.previewContent,
                                authorName = key.authorName,
                                authorAvatar = key.authorAvatar,
                                postTime = key.postTime,
                                locked = false
                            )
                            PaymentCenterScreen(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        is UpdateSettings -> {
                            val viewModel: UpdateSettingsViewModel = koinViewModel()
                            UpdateSettingsScreen(
                                viewModel = viewModel,
                                snackbarHostState = snackbarHostState
                            )
                        }

                        is ProxySettings -> {
                            ProxySettingsScreen(
                                snackbarHostState = snackbarHostState,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        is Community -> {
                            CommunityScreen(snackbarHostState = snackbarHostState)
                        }

                        is MyLikes -> {
                            MyLikesScreen(snackbarHostState = snackbarHostState)
                        }

                        is HotPosts -> {
                            HotPostsScreen(snackbarHostState = snackbarHostState)
                        }

                        is FollowingPosts -> {
                            FollowingPostsScreen(snackbarHostState = snackbarHostState)
                        }

                        is RankingList -> {
                            val viewModel: RankingListViewModel = koinViewModel()
                            RankingListScreen(viewModel=viewModel)
                        }

                        is Player -> {
                            val navigator = LocalNavigator.current
                            val viewModel: PlayerViewModel = koinViewModel()
                            LaunchedEffect(key.bvid) { viewModel.loadVideoData(key.bvid) }
                            PlayerScreen(
                                viewModel = viewModel,
                                onBack = { navigator.goBack() }
                            )
                        }                

                        is PrefsReposPage -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("当前平台暂不支持仓库设置", color = Color.Gray)
                            }
                        }

                        is StoreManager -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("当前平台暂不支持存储空间管理", color = Color.Gray)
                            }
                        }

                        is AppPage -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("当前平台暂不支持应用详情页", color = Color.Gray)
                            }
                        }

                        is SearchPage -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("当前平台暂不支持搜索页", color = Color.Gray)
                            }
                        }

                        is Explore -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("当前平台暂不支持探索页", color = Color.Gray)
                            }
                        }

                        is SortFilterSheet -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("当前平台暂不支持筛选", color = Color.Gray)
                            }
                        }

                        else -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Unknown Key: ${key::class.simpleName}", color = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    )
}