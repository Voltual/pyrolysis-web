//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.

// 参考了https://github.com/terrakok/nav3-recipes/的实现
package me.voltual.pyrolysis.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
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
import kotlinx.serialization.modules.polymorphic

internal val config = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            // --- 核心导航 ---
            subclass(Home::class, Home.serializer())
            subclass(Login::class, Login.serializer())
            subclass(About::class, About.serializer())
            subclass(LogViewer::class, LogViewer.serializer())
            subclass(ThemeCustomize::class, ThemeCustomize.serializer())
            subclass(StoreManager::class, StoreManager.serializer())
            subclass(UpdateSettings::class, UpdateSettings.serializer())
            subclass(ProxySettings::class, ProxySettings.serializer())
            
            // --- 社区与帖子 ---
            subclass(Community::class, Community.serializer())
            subclass(MyLikes::class, MyLikes.serializer())
            subclass(HotPosts::class, HotPosts.serializer())
            subclass(FollowingPosts::class, FollowingPosts.serializer())
            subclass(BrowseHistory::class, BrowseHistory.serializer())
            subclass(PostDetail::class, PostDetail.serializer())
            subclass(CreatePost::class, CreatePost.serializer())
            subclass(CreateRefundPost::class, CreateRefundPost.serializer())
            subclass(ImagePreview::class, ImagePreview.serializer())
            
            // --- 用户相关 ---
            subclass(UserDetail::class, UserDetail.serializer())
            subclass(MyPosts::class, MyPosts.serializer())
            subclass(Search::class, Search.serializer())
            subclass(MyComments::class, MyComments.serializer())
            subclass(MyReviews::class, MyReviews.serializer())
            subclass(FollowList::class, FollowList.serializer())
            subclass(FanList::class, FanList.serializer())
            subclass(AccountProfile::class, AccountProfile.serializer())
            subclass(SignInSettings::class, SignInSettings.serializer())
            
            // --- 资源广场与应用 ---
            subclass(ResourcePlaza::class, ResourcePlaza.serializer())
            subclass(Explore::class, Explore.serializer())
            subclass(SortFilterSheet::class, SortFilterSheet.serializer())
            subclass(AppDetail::class, AppDetail.serializer())
            subclass(AppPage::class, AppPage.serializer())
            subclass(SearchPage::class, SearchPage.serializer())
            subclass(PrefsReposPage::class, PrefsReposPage.serializer())
            subclass(CreateAppRelease::class, CreateAppRelease.serializer())
            subclass(UpdateAppRelease::class, UpdateAppRelease.serializer())
            
            // --- 消息、账单、支付 ---
            subclass(MessageCenter::class, MessageCenter.serializer())
            subclass(Billing::class, Billing.serializer())
            subclass(PaymentCenterAdvanced::class, PaymentCenterAdvanced.serializer())
            subclass(PaymentForApp::class, PaymentForApp.serializer())
            subclass(PaymentForPost::class, PaymentForPost.serializer())
            
            // --- 其他 ---
            subclass(RankingList::class, RankingList.serializer())
            subclass(Player::class, Player.serializer())
        }
    }
}

@Composable
fun rememberNavigationState(
    startRoute: NavKey, 
    topLevelRoutes: Set<NavKey>
): NavigationState {

    val topLevelRoute = rememberSerializable(
        startRoute, topLevelRoutes,
        configuration = config,
        serializer = MutableStateSerializer(PolymorphicSerializer(NavKey::class))
    ) {
        mutableStateOf(startRoute)
    }

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

class NavigationState(
    val startRoute: NavKey,
    topLevelRoute: MutableState<NavKey>,
    val backStacks: Map<NavKey, NavBackStack<NavKey>>
) {
    var topLevelRoute: NavKey by topLevelRoute
    
    val currentRoute: NavKey?
        get() = backStacks[topLevelRoute]?.lastOrNull() 
            ?: backStacks[startRoute]?.lastOrNull()   
    
    val stacksInUse: List<NavKey>
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

@Composable
fun NavigationState.toEntries(
    entryProvider: (NavKey) -> NavEntry<NavKey>
): SnapshotStateList<NavEntry<NavKey>> {

    val decoratedEntries = backStacks.mapValues { (_, stack) ->
        val decorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
        )
        rememberDecoratedNavEntries(
            backStack = stack, 
            entryDecorators = decorators, 
            entryProvider = entryProvider
        )
    }

    return remember(topLevelRoute, startRoute, decoratedEntries) {
        stacksInUse
            .flatMap { decoratedEntries[it] ?: emptyList() }
            .toMutableStateList()
    }
}