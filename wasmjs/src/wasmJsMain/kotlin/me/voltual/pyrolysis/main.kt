//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版

package me.voltual.pyrolysis

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import kotlinx.browser.document
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.preloadFont
import me.voltual.pyrolysis.di.commonModule
import me.voltual.pyrolysis.di.platformModule
import me.voltual.pyrolysis.Res
import me.voltual.pyrolysis.unifont
import me.voltual.pyrolysis.core.ui.theme.BBQTheme
import me.voltual.pyrolysis.core.ui.icons.drawable.Fire
import org.koin.core.context.startKoin

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    startKoin {
        modules(commonModule, platformModule)
    }

    val composeRoot = document.getElementById("ComposeApp")!!

    ComposeViewport(composeRoot) {
        @OptIn(ExperimentalResourceApi::class)
        val unifontResource by preloadFont(Res.font.unifont).collectAsState()
        val fontFamilyResolver = LocalFontFamilyResolver.current
        
        // 这里的 BBQTheme 此时不使用自定义字体，仅用于获取颜色
        BBQTheme(useUnifont = false) {
            if (unifontResource != null) {
                // 字体加载完成后，更新解析器并进入主程序
                LaunchedEffect(unifontResource) {
                    fontFamilyResolver.preload(FontFamily(listOf(unifontResource!!)))
                }

                // 再次嵌套或切换状态以启用自定义字体
                BBQTheme(useUnifont = true) {
                    PyrolysisApp(
                        platformEntryProvider = { _, _ -> null }
                    )
                }
            } else {
                // 启动页：背景为 primaryContainer，中间是 Fire 图标
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Fire,
                            contentDescription = "Loading",
                            modifier = Modifier.size(64.dp),
                            tint = Color.Unspecified // 保持图标原始颜色
                        )
                    }
                }
            }
        }
    }
}