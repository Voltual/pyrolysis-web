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
        modules(
            commonModule,
            platformModule
        )
    }

    ComposeViewport(document.body!!) {
        // 1. 使用官方标准 API 预加载 Unifont 字体
        @OptIn(ExperimentalResourceApi::class)
        val unifont = preloadFont(Res.font.unifont).value
        
        val fontFamilyResolver = LocalFontFamilyResolver.current
        
        // 2. 预加载完成后，将其注入到全局字体解析器中
        LaunchedEffect(fontFamilyResolver, unifont) {
            if (unifont != null) {
                fontFamilyResolver.preload(FontFamily(listOf(unifont)))
            }
        }

        // 3. 仅在字体加载完成后再初始化应用，完美规避豆腐块与白屏闪烁
        if (unifont != null) {
            PyrolysisApp(
                platformEntryProvider = { _, _ -> null }
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("正在加载系统核心字体...")
            }
        }
    }
}