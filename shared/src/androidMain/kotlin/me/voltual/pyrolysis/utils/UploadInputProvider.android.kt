// FILE: shared/src/jvmMain/kotlin/me/voltual/pyrolysis/util/UploadInputProvider.kt
package me.voltual.pyrolysis.util

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import io.ktor.client.request.forms.InputProvider
import io.ktor.utils.io.core.inputOf

/**
 * JVM 实现：回退到内存读取。
 * 对于 JVM，更优化的方法是从文件路径创建 InputStream，但 readBytes 更简单且安全。
 */
public actual fun createUploadInputProvider(file: PlatformFile): InputProvider {
    return InputProvider { inputOf(file.readBytes()) }
}