//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版

package me.voltual.pyrolysis.core.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

object ThemeManager {
    // 当前的主题模式，初始设定为跟随系统
    var themeMode by mutableStateOf(ThemeMode.SYSTEM)
    
    var customColorSet by mutableStateOf<CustomColorSet?>(null)
    
    /**
     * 计算当前是否应该处于深色模式
     * @param systemIsDark 系统当前的暗色状态
     */
    fun calculateIsDark(systemIsDark: Boolean): Boolean {
        return when (themeMode) {
            ThemeMode.SYSTEM -> systemIsDark
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
    }

    /**
     * 更新自定义调色板
     */
    fun updateCustomColors(colors: CustomColorSet) {
        customColorSet = colors
    }
    
    /**
     * 切换主题逻辑：
     * 如果当前跟随系统，则根据系统状态切换到对立的手动模式
     * 如果当前是手动模式，则在 浅色 -> 深色 -> 系统 之间循环
     */
    fun toggleTheme(systemIsDark: Boolean) {
        themeMode = when (themeMode) {
            ThemeMode.SYSTEM -> if (systemIsDark) ThemeMode.LIGHT else ThemeMode.DARK
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.SYSTEM
        }
    }

    // 为了兼容旧代码中可能存在的直接引用，提供一个只读代理
    // 注意：在 Composable 中应优先使用 calculateIsDark 以确保响应式
    val isAppDarkTheme: Boolean
        get() = themeMode == ThemeMode.DARK
}