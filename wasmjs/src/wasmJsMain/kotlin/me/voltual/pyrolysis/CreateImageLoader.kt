//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.pyrolysis

import coil3.ImageLoader
import coil3.PlatformContext
//import coil3.SingletonImageLoader
//一定要用Compose的setSingletonImageLoaderFactory啊啊啊啊啊！！！！
import coil3.compose.setSingletonImageLoaderFactory
import coil3.intercept.Interceptor
import coil3.request.ImageResult
import coil3.util.DebugLogger
import coil3.memory.MemoryCache
import coil3.request.crossfade
import kotlinx.browser.window 
import coil3.request.CachePolicy

internal val platformContext: PlatformContext = PlatformContext.INSTANCE

fun createImageLoader(context: PlatformContext): ImageLoader {
    return ImageLoader.Builder(context)
        .components {
            add(UniversalImageProxyInterceptor())
        }
        .diskCache { 
        // 除掉 Coil 内部隐式自动生成的默认磁盘缓存工厂！
            newDiskCache()
        }
        .crossfade(true)
        .build()
}
/**
 * 万能图片代理拦截器
 */
private class UniversalImageProxyInterceptor : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val originalRequest = chain.request
        val data = originalRequest.data

        val newChain = if (data is String && (data.startsWith("http://") || data.startsWith("https://"))) {
            val isAlreadyProxied = data.contains("/proxy-img/")
            
            if (!isAlreadyProxied) {
                // 1. 获取当前 Wasm 网页在浏览器里的真实 Origin (例如 https://your-worker.workers.dev)
                val currentOrigin = window.location.origin.removeSuffix("/")
                
                // 2. 拼接成绝对 URL，向 Coil 证明：“老兄，这绝对是个网络请求！”
                val newUri = "$currentOrigin/proxy-img/$data"
                
                val newRequest = originalRequest.newBuilder()
                    .data(newUri)
                    .crossfade(true)
                    .build()
                chain.withRequest(newRequest)
            } else {
                chain
            }
        } else {
            chain
        }

        return newChain.proceed()
    }
}