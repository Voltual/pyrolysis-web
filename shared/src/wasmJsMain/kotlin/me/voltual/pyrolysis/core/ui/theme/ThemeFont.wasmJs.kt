package me.voltual.pyrolysis.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily

@Composable
actual fun getThemeFontFamily(): FontFamily {
    return sharedThemeFontFamily
}