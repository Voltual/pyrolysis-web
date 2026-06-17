//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.pyrolysis

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.browser.document
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.preloadFont
import coil3.compose.setSingletonImageLoaderFactory
import me.voltual.pyrolysis.di.commonModule
import me.voltual.pyrolysis.di.platformModule
import me.voltual.pyrolysis.Res
import me.voltual.pyrolysis.unifont
import me.voltual.pyrolysis.core.ui.theme.BBQTheme
import me.voltual.pyrolysis.core.ui.theme.ThemeManager
import me.voltual.pyrolysis.core.ui.theme.ThemeColorDataStore
import me.voltual.pyrolysis.core.ui.icons.drawable.Fire
import org.koin.core.context.startKoin
import org.koin.compose.koinInject

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    startKoin {
        modules(commonModule, platformModule)
    }    

    val composeRoot = document.getElementById("ComposeApp")!!

    ComposeViewport(composeRoot) {
        setSingletonImageLoaderFactory { context ->
            createImageLoader(context)
        }

        // 1. 注入 DataStore
        val themeStore: ThemeColorDataStore = koinInject()
        
        // 2. 仅收集自定义颜色
        val currentColors by themeStore.colorsFlow.collectAsState(initial = null)

        // 3. 当异步流返回颜色时，更新内存单例
        LaunchedEffect(currentColors) {
            currentColors?.let {
                ThemeManager.updateCustomColors(it)
            }
        }

        @OptIn(ExperimentalResourceApi::class)
        val unifontState = preloadFont(Res.font.unifont)
        val unifont = unifontState.value
        val fontFamilyResolver = LocalFontFamilyResolver.current
        
        // 4. 外层包裹 BBQTheme。
        // 注意：因为 themeMode 初始是 SYSTEM，BBQTheme 内部的 isSystemInDarkTheme() 
        // 在 WasmJS 启动时会一瞬间识别出浏览器的暗色/亮色模式
        BBQTheme {
            if (unifont != null) {
                LaunchedEffect(unifont) {
                    fontFamilyResolver.preload(FontFamily(listOf(unifont)))
                }

                PyrolysisApp(
                    platformEntryProvider = { _, _ -> null }
                )
            } else {
                // WasmJS 启动页：这里的背景色会完美跟随系统（暗色系统下就是暗色的 primaryContainer）
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Fire,
                            contentDescription = "Loading",
                            modifier = Modifier.size(100.dp),
                            tint = Color.Unspecified 
                        )
                    }
                }
            }
        }
    }
}