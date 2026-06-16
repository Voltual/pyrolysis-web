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
import me.voltual.pyrolysis.core.ui.icons.drawable.Fire
import org.koin.core.context.startKoin

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
        @OptIn(ExperimentalResourceApi::class)
        val unifontState = preloadFont(Res.font.unifont)
        val unifont = unifontState.value
        val fontFamilyResolver = LocalFontFamilyResolver.current
        
        // 外部包裹 BBQTheme 以确保启动页能获取到主题中的 primaryContainer 颜色
        BBQTheme {
            if (unifont != null) {
                LaunchedEffect(unifont) {
                    fontFamilyResolver.preload(FontFamily(listOf(unifont)))
                }

                PyrolysisApp(
                    platformEntryProvider = { _, _ -> null }
                )
            } else {
                // WasmJS 启动页：背景为主题的 primaryContainer，中间是 Fire 图标
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
                            tint = Color.Unspecified // 保持图标原始颜色
                        )
                    }
                }
            }
        }
    }
}