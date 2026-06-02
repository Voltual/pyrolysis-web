package me.voltual.pyrolysis.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily

@Composable
actual fun getThemeFontFamily(): FontFamily {
    // 修复：使用命名参数 name = "Unifont" 显式调用顶级工厂函数，规避 Wasm 编译器符号解析冲突
    return FontFamily(name = "Unifont")
}