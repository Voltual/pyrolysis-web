// Copyright (C) 2025 Voltual
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
    
    // 修复：使用最基础、最通用的 rememberSaveable(saver) { ... } 签名
    // 移除显式的 inputs 传递，完全依赖 remember(startRoute) 闭包捕获，规避多平台 KMP 的编译盲区
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
        }
    ) {
        mutableStateOf(startRoute)
    }

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
        // 将强转拆解开，完全抹平多平台编译器的泛型检测
        val baseDecorator = rememberSaveableStateHolderNavEntryDecorator<AppDestination>()
        @Suppress("UNCHECKED_CAST")
        val decorators = listOf(baseDecorator) as List<androidx.navigation3.runtime.NavEntryDecorator<NavKey>>
        
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