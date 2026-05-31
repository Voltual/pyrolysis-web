//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
// 使用了 2026 Android Open Source Project 官方架构设计
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
import kotlinx.serialization.modules.subclassesOfSealed

/**
 * 1. 对应官方的 config 定义
 * 通过 androidx.savedstate 的多平台开放序列化配置，让 Web/iOS 平台在编译期就能感知多态
 */
internal val config = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            // 比原版挨个注册子路由更高级：由于你的路由全由 sealed interface 统一管理
            // 这一行命令可以直接动态抓取 AppDestination 旗下的所有类，简洁且不易遗漏
            subclassesOfSealed<AppDestination>()
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

    // 严丝合缝对齐官方的 rememberSerializable 持久化方案
    // 自动利用对齐多平台的 PolymorphicSerializer 处理多态子路由状态
    val topLevelRoute = rememberSerializable(
        startRoute, topLevelRoutes,
        configuration = config,
        serializer = MutableStateSerializer(PolymorphicSerializer(NavKey::class))
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
    // 隐藏内部的底层 NavKey，对外暴露我们绝对强类型的 AppDestination
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

    // 完美契合官方的 Decorator 架构逻辑
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