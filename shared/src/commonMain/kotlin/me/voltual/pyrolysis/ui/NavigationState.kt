// Copyright (C) 2025 Voltual
package me.voltual.pyrolysis.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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

// 内存级平台暂存器，防 Web 端组件重绘/配置变更导致状态丢失
private var kmpNavigationCache: String? = null

/**
 * 创建一个支持跨平台（包含 Web / Wasm）的状态持久化导航状态。
 * 彻底移除 rememberSaveable，改用纯 remember + LaunchedEffect，根除多平台签名冲突。
 */
@Composable
fun rememberNavigationState(
    startRoute: AppDestination,
    topLevelRoutes: Set<AppDestination>
): NavigationState {
    
    val polymorphicSerializer = remember(startRoute) { AppDestination.serializer() }

    // 修复：彻底切换为普通的 remember，不走 rememberSaveable 任何多平台歧义重载
    val topLevelRoute = remember(startRoute) {
        val initialRoute = try {
            kmpNavigationCache?.let { 
                NavigationJson.decodeFromString(polymorphicSerializer, it) 
            } ?: startRoute
        } catch (e: Exception) {
            startRoute
        }
        mutableStateOf(initialRoute)
    }

    // 当路由改变时，自动将状态落盘/缓存
    androidx.compose.runtime.LaunchedEffect(topLevelRoute.value) {
        try {
            kmpNavigationCache = NavigationJson.encodeToString(polymorphicSerializer, topLevelRoute.value)
        } catch (_: Exception) {}
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
 * 将 NavigationState 转换为 NavEntries，保持与官方 Recipe 一致的响应式更新逻辑
 */
@Composable
fun NavigationState.toEntries(
    entryProvider: (AppDestination) -> NavEntry<AppDestination>
): SnapshotStateList<NavEntry<AppDestination>> {

    val decoratedEntries = backStacks.mapValues { (_, stack) ->
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
            .toMutableStateList() // 修复：补全了漏掉的调用小括号 ()
    }
}