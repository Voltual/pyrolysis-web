package me.voltual.pyrolysis.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily

/**
 * 全局 Wasm 字体对象持有者。
 * 由 main.kt 异步加载完成后进行赋值。
 */
var wasmThemeFontFamily: FontFamily = FontFamily.Default

@Composable
actual fun getThemeFontFamily(): FontFamily {
    // 直接返回预加载好的字体对象，100% 安全且无缝对接
    return wasmThemeFontFamily
}