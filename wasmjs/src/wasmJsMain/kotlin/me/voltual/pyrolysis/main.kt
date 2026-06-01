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
import kotlinx.browser.document
import me.voltual.pyrolysis.di.commonModule
import me.voltual.pyrolysis.di.platformModule
import org.koin.core.context.startKoin

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // 1. 初始化 Web 端 Koin 依赖注入容器
    startKoin {
        modules(
            commonModule,
            platformModule
        )
    }

    // 2. 将 Compose 渲染视口挂载到浏览器的 document.body 上
    ComposeViewport(document.body!!) {
        PyrolysisApp(
            platformEntryProvider = { key, navigator ->
                // 如果 Web 端后续有专属的平台页面，可以在此处进行分支拦截与渲染
                // 目前全部采用 commonMain 共享页面，直接返回 null 即可
                null
            }
        )
    }
}