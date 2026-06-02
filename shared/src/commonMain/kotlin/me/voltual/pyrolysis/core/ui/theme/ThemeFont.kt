package me.voltual.pyrolysis.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily

/**
 * 跨平台获取全局字体的统一接口。
 */
@Composable
expect fun getThemeFontFamily(): FontFamily