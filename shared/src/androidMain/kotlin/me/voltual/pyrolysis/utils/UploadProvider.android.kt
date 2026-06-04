// FILE: shared/src/jvmMain/kotlin/me/voltual/pyrolysis/util/UploadProvider.jvm.kt
package me.voltual.pyrolysis.utils

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import io.ktor.client.request.forms.ChannelProvider
import io.ktor.utils.io.ByteReadChannel

public actual fun createUploadProvider(file: PlatformFile): ChannelProvider {
    return ChannelProvider {
        // JVM 上简单处理，直接读取字节。
        // 如需优化，可从文件路径创建 ByteReadChannel
        ByteReadChannel(file.readBytes())
    }
}