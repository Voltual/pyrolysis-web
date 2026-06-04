package me.voltual.pyrolysis.utils

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.WebFile
import io.ktor.utils.io.*
import kotlinx.io.Source
import kotlinx.io.Buffer
import kotlinx.io.asSource
import org.w3c.files.File
import kotlin.js.Promise

// 注意：这里需要根据你的 Ktor 版本调整，Ktor 3.0+ 在 Wasm 上对流的支持更原生
// 这是一个通用的 Wasm 流式读取实现思路
actual fun PlatformFile.asByteReadChannel(): ByteReadChannel {
    val webFile = this.webFile as? WebFile.FileWrapper 
        ?: throw Exception("Not a file")
    val jsFile = webFile.file // 这是 org.w3c.files.File

    // 在 Wasm/JS 中，Ktor 的 ByteReadChannel 可以从流或数组构建
    // 为了简单且兼容，如果文件不大，可以直接 readBytes。
    // 如果要严谨的流式，需要使用 JS 接口读取 ReadableStream 并写入 channel。
    // 这里先提供一个基于协程读取 chunk 的伪实现，防止 OOM
    val channel = ByteChannel(autoFlush = true)
    
    // 启动一个协程来填充 channel
    kotlinx.coroutines.GlobalScope.launch {
        try {
            val reader = org.w3c.files.FileReader()
            val chunkSize = 1024 * 64 // 64KB
            var offset = 0L
            val size = jsFile.size.toDouble().toLong()

            while (offset < size) {
                val end = minOf(offset + chunkSize, size)
                val blob = jsFile.slice(offset.toDouble(), end.toDouble())
                val chunk = readBlobAsByteArray(blob)
                channel.writeFully(chunk)
                offset = end
            }
        } finally {
            channel.close()
        }
    }
    return channel
}

// 辅助函数：将 Blob 读取为 ByteArray
private suspend fun readBlobAsByteArray(blob: org.w3c.files.Blob): ByteArray {
    return suspendCancellableCoroutine { cont ->
        val reader = org.w3c.files.FileReader()
        reader.onload = {
            val arrayBuffer = reader.result as org.khronos.webgl.ArrayBuffer
            val uint8Array = org.khronos.webgl.Uint8Array(arrayBuffer)
            val bytes = ByteArray(uint8Array.length)
            for (i in 0 until uint8Array.length) bytes[i] = uint8Array[i]
            cont.resume(bytes)
        }
        reader.onerror = { cont.resumeWithException(Exception("Read error")) }
        reader.readAsArrayBuffer(blob)
    }
}

actual fun PlatformFile.asSource(): Source {
    // 对于 ApkParser，由于 ZipInputStream 需要随机访问或连续流
    // 在 Wasm 浏览器端，目前最稳妥且不依赖 Node.js 的做法是 readBytes().asSource()
    // 即使这会占用内存。如果 APK 巨大，需要实现一个自定义的 kotlinx.io.RawSource 代理给 JS File.slice
    val bytes = kotlinx.coroutines.runBlocking { this@asSource.readBytes() }
    return Buffer().apply { write(bytes) }
}