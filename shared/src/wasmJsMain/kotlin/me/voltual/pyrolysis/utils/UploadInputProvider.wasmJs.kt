// FILE: shared/src/wasmJsMain/kotlin/me/voltual/pyrolysis/util/UploadInputProvider.kt
package me.voltual.pyrolysis.util

import io.github.vinceglb.filekit.*
import io.ktor.client.request.forms.InputProvider
import io.ktor.utils.io.core.inputOf

/**
 * Wasm 实现：优先使用流式 BrowserFileInput，否则回退到内存读取。
 */
public actual fun createUploadInputProvider(file: PlatformFile): InputProvider {
    // 尝试从 PlatformFile 中获取底层的浏览器 File 对象
    val browserFile = (file.webFile as? WebFile.FileWrapper)?.file

    return if (browserFile != null) {
        // 如果成功，返回一个提供流式 Input 的 Provider
        InputProvider { BrowserFileInput(browserFile) }
    } else {
        // 如果失败（例如，它是一个目录），则回退到将文件读入内存
        InputProvider { inputOf(file.readBytes()) }
    }
}