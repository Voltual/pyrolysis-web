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
import androidx.navigation3.runtime.SavedStateConfiguration
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.subclassesOfSealed

/**
 * 跨平台通用的全局 Json 配置
 */
private val NavigationJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/**
 * 官方标准：为你的密封接口创建 required 的跨平台开放多态序列化配置。
 * 这样底层才能无缝支持 Web、iOS 和 Android 应对状态恢复。
 */
private val KmpNavConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            // 一行命令，自动注册 AppDestination 下面所有的 @Serializable 子路由对象/类
            subclassesOfSealed<AppDestination>()
        }
    }
}

/**
 * 创建一个支持跨平台（包含 Android / Web / iOS）的标准导航状态。
 */
@Composable
fun rememberNavigationState(
    startRoute: AppDestination,
    topLevelRoutes: Set<AppDestination>
): NavigationState {
    
    // 使用标准的基于 kotlinx.serialization 的多态 Saver，彻底避开老旧 Android 特有方法
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

    // 核心修复：传入官方在多平台下强要求的 KmpNavConfig 配置对象！
    // 并且显式将返回的 NavBackStack<NavKey> 转型为我们收窄的强类型 AppDestination
    @Suppress("UNCHECKED_CAST")
    val backStacks = topLevelRoutes.associateWith { key -> 
        rememberNavBackStack(KmpNavConfig, key) as NavBackStack<AppDestination>
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
            .toMutableStateList()
    }
}