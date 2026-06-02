package me.voltual.pyrolysis.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import org.jetbrains.compose.resources.Font
import me.voltual.pyrolysis.Res
import me.voltual.pyrolysis.unifont

@Composable
actual fun getThemeFontFamily(): FontFamily {
    // Android 端直接通过 Compose 资源系统同步加载
    return FontFamily(Font(resource = Res.font.unifont))
}