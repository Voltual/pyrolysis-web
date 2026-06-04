// FILE: shared/src/commonMain/kotlin/me/voltual/pyrolysis/util/UploadProvider.kt
package me.voltual.pyrolysis.util

import io.github.vinceglb.filekit.PlatformFile
import io.ktor.client.request.forms.ChannelProvider

/**
 * 创建一个支持流式传输的 ChannelProvider
 */
public expect fun createUploadProvider(file: PlatformFile): ChannelProvider