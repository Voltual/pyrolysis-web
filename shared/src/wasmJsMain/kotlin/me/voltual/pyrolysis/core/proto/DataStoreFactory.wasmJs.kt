package me.voltual.pyrolysis.core.proto

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioStorage
import okio.Buffer
import okio.FileHandle
import okio.FileMetadata
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.Sink
import okio.Source
import kotlinx.browser.localStorage

/**
 * 专为浏览器 localStorage 设计的极简 Okio FileSystem。
 * 让 DataStore 官方的 OkioStorage 能够无缝运行在 Web 浏览器环境中。
 */
class LocalStorageFileSystem : FileSystem() {
    override fun canonicalize(path: Path): Path = path

    override fun metadataOrNull(path: Path): FileMetadata? {
        val key = path.toString()
        val data = localStorage.getItem(key) ?: return null
        val length = try {
            val binaryString = kotlinx.browser.window.atob(data)
            binaryString.length.toLong()
        } catch (e: Exception) {
            0L
        }
        return FileMetadata(
            isRegularFile = true,
            size = length
        )
    }

    override fun list(dir: Path): List<Path> = emptyList()
    override fun listOrNull(dir: Path): List<Path>? = null

    override fun openReadOnly(file: Path): FileHandle = throw UnsupportedOperationException("WasmJS 环境暂不支持文件句柄操作")
    override fun openReadWrite(file: Path): FileHandle = throw UnsupportedOperationException("WasmJS 环境暂不支持文件句柄操作")

    override fun source(file: Path): Source {
        val key = file.toString()
        val base64Data = localStorage.getItem(key) ?: throw okio.IOException("未找到指定的存储键值: $file")
        val binaryString = kotlinx.browser.window.atob(base64Data)
        val bytes = ByteArray(binaryString.length) { i -> binaryString[i].code.toByte() }
        val buffer = Buffer().write(bytes)
        return buffer
    }

    override fun sink(file: Path, mustCreate: Boolean): Sink {
        val buffer = Buffer()
        return object : Sink {
            override fun write(source: Buffer, byteCount: Long) {
                buffer.write(source, byteCount)
            }
            override fun flush() {
                val bytes = buffer.readByteArray()
                val chars = CharArray(bytes.size) { i -> bytes[i].toInt().and(0xff).toChar() }
                // 修复弃用 API 警告，使用标准的 concatToString()
                val base64Data = kotlinx.browser.window.btoa(chars.concatToString())
                localStorage.setItem(file.toString(), base64Data)
            }
            override fun timeout() = okio.Timeout.NONE
            override fun close() {
                flush()
            }
        }
    }

    override fun appendingSink(file: Path, mustCreate: Boolean): Sink = throw UnsupportedOperationException("WasmJS 环境暂不支持追加写入")

    override fun createDirectory(dir: Path, mustCreate: Boolean) {}

    override fun atomicMove(source: Path, target: Path) {
        val sourceKey = source.toString()
        val targetKey = target.toString()
        val data = localStorage.getItem(sourceKey)
        if (data != null) {
            localStorage.setItem(targetKey, data)
            localStorage.removeItem(sourceKey)
        }
    }

    override fun delete(path: Path, mustCreate: Boolean) {
        localStorage.removeItem(path.toString())
    }
}

actual fun createDataStore(
    serializer: UserCredentialsSerializer,
    context: Any?
): DataStore<UserCredentials> {
    return DataStoreFactory.create(
        storage = OkioStorage(
            fileSystem = LocalStorageFileSystem(),
            serializer = serializer,
            producePath = { "user_credentials_secure".toPath() }
        )
    )
}