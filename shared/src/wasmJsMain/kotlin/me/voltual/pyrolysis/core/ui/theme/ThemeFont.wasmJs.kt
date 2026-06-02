package me.voltual.pyrolysis.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily

@Composable
actual fun getThemeFontFamily(): FontFamily {
    // WasmJS 端直接引用预加载好的 "Unifont" 命名空间字体
    return FontFamily("Unifont")
}