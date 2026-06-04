// FILE: shared/src/commonMain/kotlin/me/voltual/pyrolysis/util/UploadInputProvider.kt
package me.voltual.pyrolysis.util

import io.github.vinceglb.filekit.PlatformFile
import io.ktor.client.request.forms.InputProvider

/**
 * 为 PlatformFile 创建一个 Ktor InputProvider。
 * 在 Wasm 上，这将实现流式传输。在其他平台上，它会回退到内存读取。
 */
public expect fun createUploadInputProvider(file: PlatformFile): InputProvider