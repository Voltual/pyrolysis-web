package me.voltual.pyrolysis.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily

/**
 * 全局共享的字体对象持有者。
 * 声明在 commonMain 中，确保所有平台、壳工程均可无障碍读写。
 */
var sharedThemeFontFamily: FontFamily = FontFamily.Default

@Composable
expect fun getThemeFontFamily(): FontFamily