//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版

package me.voltual.pyrolysis

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.*
import kotlinx.browser.document
import me.voltual.pyrolysis.di.commonModule
import me.voltual.pyrolysis.di.platformModule
import me.voltual.pyrolysis.core.ui.theme.sharedThemeFontFamily
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
        // 1. 直接在 Composable 上下文中安全地加载字体资源
        val font = org.jetbrains.compose.resources.Font(
            resource = Res.font.unifont,
            weight = FontWeight.Normal
        )
        
        // 2. 记住并构建 FontFamily
        val fontFamily = remember(font) { FontFamily(font) }
        val fontsLoaded = remember { mutableStateOf(false) }

        // 3. 在 LaunchedEffect 中仅执行纯 Kotlin 状态赋值，完美避开编译器限制
        LaunchedEffect(fontFamily) {
            sharedThemeFontFamily = fontFamily
            fontsLoaded.value = true
        }

        if (fontsLoaded.value) {
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