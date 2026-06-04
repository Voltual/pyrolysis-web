// FILE: shared/src/wasmJsMain/kotlin/me/voltual/pyrolysis/utils/UploadProvider.wasmJs.kt
package me.voltual.pyrolysis.utils

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.WebFile
import io.github.vinceglb.filekit.BrowserFile
import io.ktor.client.request.forms.ChannelProvider
import io.ktor.utils.io.writeFully
import io.ktor.utils.io.writer
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.await
import org.khronos.webgl.get

/**
 * Wasm 平台流式上传实现
 */
@OptIn(DelicateCoroutinesApi::class)
public actual fun createUploadProvider(file: PlatformFile): ChannelProvider {
    // 强制转换为 WebFile.FileWrapper 以获取底层的 JS BrowserFile 对象
    val browserFile = (file.webFile as? WebFile.FileWrapper)?.file 
        ?: throw Exception("PlatformFile 不包含有效的浏览器文件对象")

    return ChannelProvider {
        // 必须在 CoroutineScope 上调用 writer
        // 在此处使用 GlobalScope 是安全的，因为 Ktor 会在请求结束时关闭通道
        GlobalScope.writer(Dispatchers.Default) {
            val reader = getReader(browserFile)
            try {
                while (true) {
                    // await() 现在处于协程作用域内，可以正常调用
                    val result = readChunk(reader).await<JsAny>()
                    if (isDone(result)) break
                    
                    val uint8Array = getValue(result)
                    if (uint8Array != null) {
                        val length = uint8Array.length
                        val byteArray = ByteArray(length)
                        for (i in 0 until length) {
                            byteArray[i] = uint8Array[i]
                        }
                        // channel 是 ByteWriteScope 的成员，writeFully 是挂起函数
                        channel.writeFully(byteArray)
                    }
                }
            } catch (e: Exception) {
                channel.cancel(e)
            } finally {
                releaseReaderLock(reader)
            }
        }.channel
    }
}