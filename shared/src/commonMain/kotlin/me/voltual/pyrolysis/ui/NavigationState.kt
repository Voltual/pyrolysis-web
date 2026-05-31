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
    
    // 修复：明确使用带 init 参数的 rememberSaveable 签名，排除多平台库的参数歧义
    val topLevelRoute = rememberSaveable(
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
        },
        inputs = arrayOf(startRoute, topLevelRoutes), // 显式通过 inputs 数组传递依赖
        init = {
            mutableStateOf(startRoute)
        }
    )

    // 针对 Navigation 3 内部返回的 NavBackStack<NavKey> 进行安全向下转型
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
        // 修复：官方自带的这个 Decorator 返回的是针对特定泛型的对象
        // 我们将其强转为兼容基类 NavKey 的 Decorator 列表，以满足 rememberDecoratedNavEntries 的强类型契约
        @Suppress("UNCHECKED_CAST")
        val decorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator<AppDestination>()
        ) as List<androidx.navigation3.runtime.NavEntryDecorator<NavKey>>
        
        @Suppress("UNCHECKED_CAST")
        rememberDecoratedNavEntries(
            backStack = stack as NavBackStack<NavKey>, 
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