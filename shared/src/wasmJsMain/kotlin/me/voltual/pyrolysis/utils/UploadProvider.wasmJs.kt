// FILE: shared/src/wasmJsMain/kotlin/me/voltual/pyrolysis/util/UploadProvider.wasmJs.kt
package me.voltual.pyrolysis.util

import io.github.vinceglb.filekit.*
import io.ktor.client.request.forms.ChannelProvider
import io.ktor.utils.io.writeFully
import io.ktor.utils.io.writer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.await
import me.voltual.pyrolysis.BrowserFile

public actual fun createUploadProvider(file: PlatformFile): ChannelProvider {
    val browserFile = (file.webFile as? WebFile.FileWrapper)?.file 
        ?: throw Exception("Not a browser file")

    return ChannelProvider {
        // 使用 Ktor 的 writer 开启一个协程通道
        writer(Dispatchers.Default) {
            val reader = getReader(browserFile)
            try {
                while (true) {
                    val result = readChunk(reader).await<JsAny>()
                    if (isDone(result)) break
                    
                    val uint8Array = getValue(result)
                    if (uint8Array != null) {
                        val length = uint8Array.length
                        val byteArray = ByteArray(length)
                        for (i in 0 until length) {
                            byteArray[i] = uint8Array[i]
                        }
                        channel.writeFully(byteArray)
                    }
                }
            } finally {
                releaseReaderLock(reader)
            }
        }.channel
    }
}