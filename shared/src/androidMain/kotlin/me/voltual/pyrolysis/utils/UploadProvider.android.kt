package me.voltual.pyrolysis.utils

import io.github.vinceglb.filekit.PlatformFile
import io.ktor.client.request.forms.ChannelProvider
import io.ktor.utils.io.writeFully
import io.ktor.utils.io.writer
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

/**
 * JVM 平台流式上传实现
 * 使用 kotlinx.io 配合 Ktor ByteWriteChannel
 */
@OptIn(DelicateCoroutinesApi::class)
public actual fun createUploadProvider(file: PlatformFile): ChannelProvider {
    return ChannelProvider {
        // 在 JVM 上，我们使用专门的 IO 调度器
        GlobalScope.writer(Dispatchers.IO) {
            try {
                // 使用 kotlinx.io 的 SystemFileSystem 打开文件
                val path = Path(file.path)
                SystemFileSystem.source(path).buffered().use { source ->
                    val buffer = ByteArray(8192) // 8KB 缓冲区
                    while (!source.exhausted()) {
                        // 从文件读取数据到缓冲区
                        val bytesRead = source.readAtMostTo(buffer)
                        if (bytesRead > 0) {
                            // 将缓冲区数据写入 Ktor 通道
                            channel.writeFully(buffer, 0, bytesRead)
                        }
                    }
                }
            } catch (e: Exception) {
                // 捕获读取异常并关闭通道
                channel.close(e)
            }
        }.channel
    }
}