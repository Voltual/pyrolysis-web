//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.pyrolysis.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
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
import kotlinx.serialization.json.Json

/**
 * 跨平台通用的全局 Json 配置，用于多态导航状态的序列化
 */
private val NavigationJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/**
 * 创建一个支持跨平台（Android / iOS / Desktop）的状态持久化导航状态。
 */
@Composable
fun rememberNavigationState(
    startRoute: AppDestination,
    topLevelRoutes: Set<AppDestination>
): NavigationState {
    
    // 修复：rememberSaveable 的第一个参数是 inputs，我们将 startRoute 和 topLevelRoutes 传入
    val topLevelRoute = rememberSaveable(
        startRoute, topLevelRoutes,
        saver = remember(startRoute) {
            val polymorphicSerializer = AppDestination.serializer()
            Saver<MutableState<AppDestination>, String>(
                save = { state -> 
                    NavigationJson.encodeToString(polymorphicSerializer, state.value) 
                },
                restore = { jsonString -> 
                    mutableStateOf(NavigationJson.decodeFromString(polymorphicSerializer, jsonString)) 
                }
            )
        }
    ) {
        mutableStateOf(startRoute)
    }

    // 修复：因为官方 rememberNavBackStack 返回的是 NavBackStack<NavKey>，我们在这里显式转换为我们需要的具体泛型
    @Suppress("UNCHECKED_CAST")
    val backStacks = topLevelRoutes.associateWith { key -> 
        rememberNavBackStack(key) as NavBackStack<AppDestination>
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
    val startRoute: AppDestination,
    topLevelRoute: MutableState<AppDestination>,
    val backStacks: Map<AppDestination, NavBackStack<AppDestination>>
) {
    var topLevelRoute: AppDestination by topLevelRoute
    
    val currentRoute: AppDestination?
        get() = backStacks[topLevelRoute]?.lastOrNull() 
            ?: backStacks[startRoute]?.lastOrNull()

    val stacksInUse: List<AppDestination>
        get() = if (topLevelRoute == startRoute) {
            listOf(startRoute)
        } else {
            listOf(startRoute, topLevelRoute)
        }

    fun resetToStart() {
        // 1. 先切换路由标识
        topLevelRoute = startRoute
        
        // 2. 遍历堆栈进行清理
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
 * 将 NavigationState 转换为 NavEntries，保持与官方 Recipe 一致的响应式更新逻辑
 */
@Composable
fun NavigationState.toEntries(
    entryProvider: (AppDestination) -> NavEntry<AppDestination>
): SnapshotStateList<NavEntry<AppDestination>> {

    val decoratedEntries = backStacks.mapValues { (_, stack) ->
        val decorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator<AppDestination>(),
        )
        @Suppress("UNCHECKED_CAST")
        rememberDecoratedNavEntries(
            backStack = stack as NavBackStack<NavKey>, // 适配官方的装饰器泛型契约
            entryDecorators = decorators,
            entryProvider = entryProvider as (NavKey) -> NavEntry<NavKey>
        ) as List<NavEntry<AppDestination>>
    }

    return remember(topLevelRoute, startRoute, decoratedEntries) {
        val routesInUse = if (topLevelRoute == startRoute) {
            listOf(startRoute)
        } else {
            listOf(startRoute, topLevelRoute)
        }

        routesInUse
            .flatMap { decoratedEntries[it] ?: emptyList() }
            .toMutableStateList()
    }
}