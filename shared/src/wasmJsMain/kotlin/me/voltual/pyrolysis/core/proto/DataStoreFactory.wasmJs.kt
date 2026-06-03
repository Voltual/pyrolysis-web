package me.voltual.pyrolysis.core.proto

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
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
 */
class LocalStorageFileSystem : FileSystem() {
    
    // 辅助函数：把 Okio 强制传进来的绝对路径转回干净的 localStorage Key
    // 例如："/settings.preferences_pb" -> "settings.preferences_pb"
    private fun Path.toKey(): String = this.toString().removePrefix("/")

    override fun canonicalize(path: Path): Path = path

    override fun metadataOrNull(path: Path): FileMetadata? {
        val key = path.toKey() // 修改处
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
    
    override fun openReadWrite(file: Path, mustCreate: Boolean, mustExist: Boolean): FileHandle {
        throw UnsupportedOperationException("WasmJS 环境暂不支持文件句柄操作")
    }

    override fun createSymlink(source: Path, target: Path) {
        throw UnsupportedOperationException("WasmJS 环境暂不支持符号链接")
    }

    override fun source(file: Path): Source {
        val key = file.toKey() // 修改处
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
                val base64Data = kotlinx.browser.window.btoa(chars.concatToString())
                localStorage.setItem(file.toKey(), base64Data) // 修改处
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
        val sourceKey = source.toKey() // 修改处
        val targetKey = target.toKey() // 修改处
        val data = localStorage.getItem(sourceKey)
        if (data != null) {
            localStorage.setItem(targetKey, data)
            localStorage.removeItem(sourceKey)
        }
    }

    override fun delete(path: Path, mustCreate: Boolean) {
        localStorage.removeItem(path.toKey()) // 修改处
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
            // 核心修改：路径改用 "/" 开头，使其满足绝对路径校验
            producePath = { "/user_credentials_secure".toPath() }
        )
    )
}

actual fun createPreferenceDataStore(
    fileName: String,
    context: Any?
): DataStore<Preferences> {
    return PreferenceDataStoreFactory.create(
        storage = OkioStorage(
            fileSystem = LocalStorageFileSystem(),
            serializer = PreferencesSerializer,
            // 核心修改：确保 fileName 转换出来的路径是绝对路径
            producePath = { 
                val absolutePath = if (fileName.startsWith("/")) fileName else "/$fileName"
                absolutePath.toPath() 
            }
        )
    )
}