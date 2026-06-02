//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版

package me.voltual.pyrolysis

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.*
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.w3c.fetch.Response
import kotlin.wasm.unsafe.UnsafeWasmMemoryApi
import kotlin.wasm.unsafe.withScopedMemoryAllocator
import me.voltual.pyrolysis.di.commonModule
import me.voltual.pyrolysis.di.platformModule
import org.koin.core.context.startKoin

// 字体文件路径（需要放置在 wasmjs/src/wasmJsMain/resources/unifont.otf 目录中）
private const val FONT_URL = "./unifont.otf"

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // 1. 初始化 Koin 依赖注入
    startKoin {
        modules(
            commonModule,
            platformModule
        )
    }

    // 2. 挂载 ComposeViewport
    ComposeViewport(document.body!!) {
        val fontFamilyResolver = LocalFontFamilyResolver.current
        val fontsLoaded = remember { mutableStateOf(false) }

        if (fontsLoaded.value) {
            // 字体加载完成后，启动真正的应用程序
            PyrolysisApp(
                platformEntryProvider = { _, _ -> null }
            )
        } else {
            // 字体加载前，显示极简的优雅 Loading 界面
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("正在加载系统核心字体...")
            }
        }

        // 3. 异步拉取并预加载字体
        LaunchedEffect(Unit) {
            try {
                val fontBytes = loadFontBytes(FONT_URL)
                val fontFamily = FontFamily(listOf(Font(identity = "Unifont", data = fontBytes)))
                fontFamilyResolver.preload(fontFamily)
                fontsLoaded.value = true
            } catch (e: Exception) {
                // 容错处理：若加载失败则直接进入，防止白屏
                fontsLoaded.value = true
            }
        }
    }
}

/**
 * 异步 Fetch 网络字体文件并转换为 Kotlin ByteArray。
 */
private suspend fun loadFontBytes(url: String): ByteArray {
    val response = window.fetch(url).await<Response>()
    if (!response.ok) {
        throw okio.IOException("无法获取字体文件: status = ${response.status}")
    }
    val arrayBuffer = response.arrayBuffer().await<ArrayBuffer>()
    return arrayBuffer.toByteArray()
}

/**
 * 将 JavaScript 的 ArrayBuffer 高效转换为 Kotlin ByteArray。
 */
private fun ArrayBuffer.toByteArray(): ByteArray {
    val source = Int8Array(this, 0, byteLength)
    val size = source.length

    @OptIn(UnsafeWasmMemoryApi::class)
    return withScopedMemoryAllocator { allocator ->
        val memBuffer = allocator.allocate(size)
        val dstAddress = memBuffer.address.toInt()
        jsExportInt8ArrayToWasm(source, size, dstAddress)
        ByteArray(size) { i -> (memBuffer + i).loadByte() }
    }
}

/**
 * 利用 WebAssembly 内存段直接复制 JS 字节数据，避免高开销的循环遍历。
 */
@JsFun(
    """(src, size, dstAddr) => {
        const mem8 = new Int8Array(wasmExports.memory.buffer, dstAddr, size);
        mem8.set(src);
    }"""
)
private external fun jsExportInt8ArrayToWasm(src: Int8Array, size: Int, dstAddr: Int)