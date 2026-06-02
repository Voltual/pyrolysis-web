//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版

package me.voltual.pyrolysis

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.*
import kotlinx.browser.document
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.preloadFont
import me.voltual.pyrolysis.di.commonModule
import me.voltual.pyrolysis.di.platformModule
import me.voltual.pyrolysis.Res
import me.voltual.pyrolysis.unifont
import org.koin.core.context.startKoin

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    startKoin {
        modules(commonModule, platformModule)
    }

    // 正确获取 index.html 中定义的专用容器 div
    val composeRoot = document.getElementById("ComposeApp")!!

    // 将专用容器作为宿主传入，这样底层坐标计算才会百分之百精准
    ComposeViewport(composeRoot) {
        @OptIn(ExperimentalResourceApi::class)
        val unifont = preloadFont(Res.font.unifont).value
        val fontFamilyResolver = LocalFontFamilyResolver.current
        
        LaunchedEffect(fontFamilyResolver, unifont) {
            if (unifont != null) {
                fontFamilyResolver.preload(FontFamily(listOf(unifont)))
            }
        }

        if (unifont != null) {
            PyrolysisApp(
                platformEntryProvider = { _, _ -> null }
            )
//            WasmDebugWidget()
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // 这里加个本地文本样式，避免字体没加载出来时无法显示
                Text("正在加载系统核心字体...")
            }
        }
    }
}