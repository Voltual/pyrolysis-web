package me.voltual.pyrolysis.utils

import io.github.vinceglb.filekit.PlatformFile
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.io.Source
import kotlinx.io.asSource
import java.io.File

actual fun PlatformFile.asByteReadChannel(): ByteReadChannel {
    // 假设 PlatformFile 在 JVM 上有 path
    return File(this.path).inputStream().toByteReadChannel()
}

actual fun PlatformFile.asSource(): Source {
    return File(this.path).inputStream().asSource()
}