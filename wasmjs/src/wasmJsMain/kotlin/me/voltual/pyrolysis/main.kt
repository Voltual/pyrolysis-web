//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版

package me.voltual.pyrolysis

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Text
import androidx.compose.material3.Button       // 引入 Material 3 按钮
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column    // 引入垂直布局
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
    // 恢复 Koin 初始化
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

        // 判断字体是否加载完成
        if (fontsLoaded.value) {
            // --- 将原 PyrolysisApp 替换为 Material 3 测试组件 ---
            
            // 用于测试 Material 3 组件的状态
            var count by remember { mutableStateOf(0) }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 动态改变文字的 Text 组件（此时已应用加载好的字体环境）
                    Text(
                        text = "当前按钮点击次数: $count",

                    )
                    
                    // Material 3 按钮
                    Button(onClick = { count++ }) {
                        Text("点我改变文字")
                    }
                }
            }
            
            // --------------------------------------------------
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