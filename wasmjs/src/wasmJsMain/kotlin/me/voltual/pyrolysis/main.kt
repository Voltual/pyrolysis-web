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
import me.voltual.pyrolysis.core.ui.theme.sharedThemeFontFamily // 导入 commonMain 的共享变量
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
        val fontsLoaded = remember { mutableStateOf(false) }

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

        LaunchedEffect(Unit) {
            try {
                // 异步加载字体
                val font = org.jetbrains.compose.resources.Font(
                    resource = Res.font.unifont,
                    weight = FontWeight.Normal
                )
                // 赋值给 commonMain 共享变量
                sharedThemeFontFamily = FontFamily(font)
                fontsLoaded.value = true
            } catch (e: Exception) {
                fontsLoaded.value = true
            }
        }
    }
}