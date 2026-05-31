// Copyright (C) 2025 Voltual
package me.voltual.pyrolysis.ui

import androidx.compose.ui.focus.FocusManager
import androidx.navigation3.runtime.NavKey

/** Handles navigation events (forward and back) by updating the navigation state. */
class Navigator(
  val state: NavigationState,
  private val focusManager: FocusManager? = null,
  private val topAppBarController: TopAppBarController? = null,
) {
  private fun forceCleanup() {
    focusManager?.clearFocus(force = true) 
    topAppBarController?.clear()
  }

  fun logoutAndReset() {
    state.resetToStart()
  }

  fun navigate(route: AppDestination) {
    forceCleanup()

    if (route in state.backStacks.keys) {
      state.topLevelRoute = route
    } else {
      // 修复：去掉了错误的 .state. 修正为直接调用 state.topLevelRoute
      state.backStacks[state.topLevelRoute]?.add(route)
    }
  }

  // 兼容性重载：如果有些地方仍在使用顶层的 NavKey，进行安全分发
  fun navigate(route: NavKey) {
     if (route is AppDestination) {
         navigate(route)
     } else {
         error("Route $route must implement AppDestination")
     }
  }

  fun goBack() {
    forceCleanup()

    val currentStack =
      state.backStacks[state.topLevelRoute] ?: error("Stack for ${state.topLevelRoute} not found")

    if (currentStack.lastOrNull() == state.topLevelRoute) {
      state.topLevelRoute = state.startRoute
    } else {
      currentStack.removeLastOrNull()
    }
  }
}