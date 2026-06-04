// FILE: shared/src/wasmJsMain/kotlin/me/voltual/pyrolysis/util/BrowserFileInput.kt
package me.voltual.pyrolysis.util

import io.ktor.utils.io.core.Input
import io.ktor.utils.io.core.internal.ChunkBuffer
import js.core.Promise
import js.typedarrays.Uint8Array
import kotlinx.coroutines.await
import org.w3c.files.File
import kotlin.math.min

// 1. 定义与 JS ReadableStream 相关的外部接口
@JsName("ReadableStream")
private external interface JsReadableStream {
    fun getReader(): JsReadableStreamDefaultReader
}

@JsName("ReadableStreamDefaultReader")
private external interface JsReadableStreamDefaultReader {
    fun read(): Promise<JsReadResult>
    fun releaseLock()
    val closed: Promise<Unit>
}

@JsName("Object")
private external interface JsReadResult {
    val value: Uint8Array?
    val done: Boolean
}

// 2. 扩展浏览器 File 类以安全地获取 stream
private val File.stream: JsReadableStream
    get() = asDynamic().stream().unsafeCast<JsReadableStream>()

/**
 * 一个 Ktor Input 实现，它从浏览器的 File.stream() API 流式读取数据。
 * 这避免了将整个文件读入内存。
 */
internal class BrowserFileInput(file: File) : Input() {
    private val reader: JsReadableStreamDefaultReader = file.stream.getReader()

    override fun fill(destination: ChunkBuffer, offset: Int, length: Int): Int {
        // Ktor 的流式上传不使用此同步方法
        throw UnsupportedOperationException("Synchronous reading is not supported for BrowserFile streams.")
    }

    override suspend fun readAvailable(destination: ByteArray, offset: Int, length: Int): Int {
        val result = reader.read().await()

        if (result.done) {
            return -1 // 表示流结束
        }

        val chunk = result.value ?: return 0 // 没有数据但流未结束
        val bytesToRead = min(chunk.length, length)

        // 将数据从 JS Uint8Array 复制到 Kotlin ByteArray
        for (i in 0 until bytesToRead) {
            destination[offset + i] = chunk[i]
        }

        return bytesToRead
    }

    override fun close() {
        // 释放读取器上的锁，允许其他读取器接管
        reader.releaseLock()
    }
}