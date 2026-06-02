//Copyright (C) 2025 Voltual
// 本程序 is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License...

package me.voltual.pyrolysis

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font // 显式导入顶级函数
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
import me.voltual.pyrolysis.core.ui.theme.wasmThemeFontFamily
import org.koin.core.context.startKoin

private const val FONT_URL = "./unifont.otf"

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    startKoin {
        modules(
            commonModule,
            platformModule
        )
    }

    ComposeViewport(document.body!!) {
        val fontFamilyResolver = LocalFontFamilyResolver.current
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
                val fontBytes = loadFontBytes(FONT_URL)
                // 修复：直接调用顶级函数，编译器将完美匹配 Font(String, ByteArray) 签名
                val fontFamily = FontFamily(
                    listOf(
                        Font("Unifont", fontBytes)
                    )
                )
                fontFamilyResolver.preload(fontFamily)
                
                wasmThemeFontFamily = fontFamily
                fontsLoaded.value = true
            } catch (e: Exception) {
                fontsLoaded.value = true
            }
        }
    }
}

private suspend fun loadFontBytes(url: String): ByteArray {
    val response = window.fetch(url).await<Response>()
    if (!response.ok) {
        throw Exception("无法获取字体文件: status = ${response.status}")
    }
    val arrayBuffer = response.arrayBuffer().await<ArrayBuffer>()
    return arrayBuffer.toByteArray()
}

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

@JsFun(
    """(src, size, dstAddr) => {
        const mem8 = new Int8Array(wasmExports.memory.buffer, dstAddr, size);
        mem8.set(src);
    }"""
)
private external fun jsExportInt8ArrayToWasm(src: Int8Array, size: Int, dstAddr: Int)