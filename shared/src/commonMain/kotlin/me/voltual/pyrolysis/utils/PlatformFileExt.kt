package me.voltual.pyrolysis.utils

import io.github.vinceglb.filekit.PlatformFile
import io.ktor.utils.io.*

/**
 * 将 PlatformFile 转换为 ByteReadChannel 以实现流式上传
 */
expect fun PlatformFile.asByteReadChannel(): ByteReadChannel

/**
 * 将 PlatformFile 转换为 kotlinx.io.Source 以供 ApkParser 使用
 */
expect fun PlatformFile.asSource(): kotlinx.io.Source