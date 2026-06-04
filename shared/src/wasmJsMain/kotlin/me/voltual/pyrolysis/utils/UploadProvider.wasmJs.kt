package me.voltual.pyrolysis.util

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.WebFile
import io.github.vinceglb.filekit.BrowserFile
import io.ktor.client.request.forms.ChannelProvider
import io.ktor.utils.io.writeFully
import io.ktor.utils.io.writer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.await
import org.khronos.webgl.get // 极其重要：确保 [] 操作符指向 Uint8Array 而非正则匹配

public actual fun createUploadProvider(file: PlatformFile): ChannelProvider {
    // 获取底层的浏览器 File 对象 (JsAny)
    val browserFile = (file.webFile as? WebFile.FileWrapper)?.file 
        ?: throw Exception("PlatformFile 不包含有效的浏览器文件对象")

    return ChannelProvider {
        // 使用 Ktor 的 writer 开启异步流式写入
        writer(Dispatchers.Default) {
            val reader = getReader(browserFile)
            try {
                while (true) {
                    // 等待 JS Promise 返回数据块
                    val result = readChunk(reader).await<JsAny>()
                    if (isDone(result)) break
                    
                    val uint8Array = getValue(result)
                    if (uint8Array != null) {
                        val length = uint8Array.length
                        val byteArray = ByteArray(length)
                        // 将 JS 内存数据复制到 Kotlin 内存
                        for (i in 0 until length) {
                            byteArray[i] = uint8Array[i]
                        }
                        // 写入 Ktor 发送通道
                        channel.writeFully(byteArray)
                    }
                }
            } finally {
                releaseReaderLock(reader)
            }
        }.channel
    }
}