package me.voltual.pyrolysis.utils

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.source // 确保导入 FileKit 的 source 扩展函数
import io.ktor.client.request.forms.ChannelProvider
import io.ktor.utils.io.writeFully
import io.ktor.utils.io.writer
import io.ktor.utils.io.close // 显式导入 Ktor 的 close 扩展函数，修复 Unresolved reference 'close'
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.io.buffered

/**
 * Android 平台流式上传实现
 * 完美适配 FileKit 的 Source 机制，绝对不 OOM
 */
@OptIn(DelicateCoroutinesApi::class)
public actual fun createUploadProvider(file: PlatformFile): ChannelProvider {
    return ChannelProvider {
        // 使用专门的 IO 调度器
        GlobalScope.writer(Dispatchers.IO) {
            try {
                // 直接调用你在源码中实现的 file.source()
                // 并通过 .buffered() 包装以获得高效的缓冲区操作
                file.source().buffered().use { source ->
                    val buffer = ByteArray(8192) // 8KB 缓冲区
                    while (!source.exhausted()) {
                        // 从文件流中读取数据到缓冲区
                        val bytesRead = source.readAtMostTo(buffer)
                        if (bytesRead > 0) {
                            // 将缓冲区数据流式写入 Ktor 通道
                            channel.writeFully(buffer, 0, bytesRead)
                        }
                    }
                }
            } catch (e: Exception) {
                // 捕获读取异常并关闭通道
                // 如果依旧提示 close(e) 找不到，请确保上面导了 io.ktor.utils.io.close
                // 或者在 Ktor 3.x 中使用 channel.close(e)
                channel.close(e)
            }
        }.channel
    }
}