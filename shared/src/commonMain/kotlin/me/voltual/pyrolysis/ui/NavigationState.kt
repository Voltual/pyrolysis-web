//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
// 参考了https://github.com/terrakok/nav3-recipes/的实现
define routes and register them with a `SavedStateConfiguration`, as shown below:
package me.voltual.pyrolysis.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.modules.SerializersModule

/**
 * 1. 显式多态映射表
 * 彻底废弃不稳定的 subclassesOfSealed，改为最稳固的明细注册。
 * 这能百分之百保证 kotlinx.serialization 在 Web 端 (Wasm/JS) 顺藤摸瓜找到对应的序列化器。
 */
internal val config = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            // 核心导航
            subclass(Home::class, Home.serializer())
            subclass(Login::class, Login.serializer())
            subclass(About::class, About.serializer())
            subclass(LogViewer::class, LogViewer.serializer())
            subclass(ThemeCustomize::class, ThemeCustomize.serializer())
            subclass(StoreManager::class, StoreManager.serializer())
            subclass(UpdateSettings::class, UpdateSettings.serializer())

            // 社区与帖子
            subclass(Community::class, Community.serializer())
            subclass(MyLikes::class, MyLikes.serializer())
            subclass(HotPosts::class, HotPosts.serializer())
            subclass(FollowingPosts::class, FollowingPosts.serializer())
            subclass(BrowseHistory::class, BrowseHistory.serializer())
            subclass(PostDetail::class, PostDetail.serializer())
            subclass(CreatePost::class, CreatePost.serializer())
            subclass(CreateRefundPost::class, CreateRefundPost.serializer())
            subclass(ImagePreview::class, ImagePreview.serializer())

            // 用户相关
            subclass(UserDetail::class, UserDetail.serializer())
            subclass(MyPosts::class, MyPosts.serializer())
            subclass(Search::class, Search.serializer())
            subclass(MyComments::class, MyComments.serializer())
            subclass(MyReviews::class, MyReviews.serializer())
            subclass(FollowList::class, FollowList.serializer())
            subclass(FanList::class, FanList.serializer())
            subclass(AccountProfile::class, AccountProfile.serializer())
            subclass(SignInSettings::class, SignInSettings.serializer())

            // 资源广场与应用
            subclass(ResourcePlaza::class, ResourcePlaza.serializer())
            subclass(Explore::class, Explore.serializer())
            subclass(SortFilterSheet::class, SortFilterSheet.serializer())
            subclass(AppDetail::class, AppDetail.serializer())
            subclass(AppPage::class, AppPage.serializer())
            subclass(SearchPage::class, SearchPage.serializer())
            subclass(PrefsReposPage::class, PrefsReposPage.serializer())
            subclass(CreateAppRelease::class, CreateAppRelease.serializer())
            subclass(UpdateAppRelease::class, UpdateAppRelease.serializer())

            // 消息、账单、支付
            subclass(MessageCenter::class, MessageCenter.serializer())
            subclass(Billing::class, Billing.serializer())
            subclass(PaymentCenterAdvanced::class, PaymentCenterAdvanced.serializer())
            subclass(PaymentForApp::class, PaymentForApp.serializer())
            subclass(PaymentForPost::class, PaymentForPost.serializer())

            // 其他
            subclass(RankingList::class, RankingList.serializer())
            subclass(Player::class, Player.serializer())
        }
    }
}

/**
 * 2. 强类型绑定的跨平台持久化导航状态
 */
@Composable
fun rememberNavigationState(
    startRoute: AppDestination, 
    topLevelRoutes: Set<AppDestination>
): NavigationState {

    // 修复：废弃位置不确定的 rememberSerializable，直接采用纯天然的 rememberSaveable。
    // 将官方提供的 MutableStateSerializer 转换为 androidx.compose.runtime.saveable.Saver。
    val stateSerializer = remember { MutableStateSerializer(PolymorphicSerializer(NavKey::class)) }
    val kmpSaver = remember(stateSerializer) {
        @Suppress("UNCHECKED_CAST")
        androidx.savedstate.compose.serialization.serializers.Saver(
            serializer = stateSerializer,
            configuration = config
        ) as androidx.compose.runtime.saveable.Saver<MutableState<NavKey>, Any>
    }

    val topLevelRoute = rememberSaveable(
        startRoute, topLevelRoutes,
        saver = kmpSaver
    ) {
        mutableStateOf(startRoute as NavKey)
    }

    // 将官方返回的通用 NavBackStack<NavKey> 无缝且安全地封装进我们的强类型 Map 中
    val backStacks = topLevelRoutes.associateWith { key ->
        rememberNavBackStack(config, key)
    }

    return remember(startRoute, topLevelRoutes) {
        NavigationState(
            startRoute = startRoute, 
            topLevelRoute = topLevelRoute, 
            backStacks = backStacks
        )
    }
}

/**
 * 3. 导航核心状态持有者（完全抹平泛型断层）
 */
class NavigationState(
    val startRoute: AppDestination,
    topLevelRoute: MutableState<NavKey>,
    val backStacks: Map<AppDestination, NavBackStack<NavKey>>
) {
    var topLevelRoute: AppDestination
        get() = _topLevelRoute as AppDestination
        set(value) { _topLevelRoute = value }
        
    private var _topLevelRoute: NavKey by topLevelRoute
    
    val currentRoute: AppDestination?
        get() = backStacks[topLevelRoute]?.lastOrNull() as? AppDestination
            ?: backStacks[startRoute]?.lastOrNull() as? AppDestination

    val stacksInUse: List<AppDestination>
        get() = if (topLevelRoute == startRoute) {
            listOf(startRoute)
        } else {
            listOf(startRoute, topLevelRoute)
        }

    fun resetToStart() {
        topLevelRoute = startRoute
        backStacks.forEach { (key, stack) ->
            if (key == startRoute) {
                while (stack.size > 1) {
                    stack.removeLastOrNull()
                }
            } else {
                if (stack.isNotEmpty()) {
                    while (stack.size > 1) {
                        stack.removeLastOrNull()
                    }
                }
            }
        }
    }
}

/**
 * 4. 转换 NavigationState 到 NavEntries (响应式观察)
 */
@Composable
fun NavigationState.toEntries(
    entryProvider: (AppDestination) -> NavEntry<AppDestination>
): SnapshotStateList<NavEntry<AppDestination>> {

    val decoratedEntries = backStacks.mapValues { (_, stack) ->
        val decorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
        )
        @Suppress("UNCHECKED_CAST")
        rememberDecoratedNavEntries(
            backStack = stack, 
            entryDecorators = decorators, 
            entryProvider = entryProvider as (NavKey) -> NavEntry<NavKey>
        ) as List<NavEntry<AppDestination>>
    }

    return remember(topLevelRoute, startRoute, decoratedEntries) {
        stacksInUse
            .flatMap { decoratedEntries[it] ?: emptyList() }
            .toMutableStateList()
    }
}