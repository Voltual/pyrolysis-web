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
    
    private fun Path.toKey(): String = this.toString().removePrefix("/")

    override fun canonicalize(path: Path): Path = path

    override fun metadataOrNull(path: Path): FileMetadata? {
        val key = path.toKey()
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

    override fun openReadOnly(file: Path): FileHandle = throw UnsupportedOperationException("WasmJS 不支持句柄")
    
    override fun openReadWrite(file: Path, mustCreate: Boolean, mustExist: Boolean): FileHandle {
        throw UnsupportedOperationException("WasmJS 不支持句柄")
    }

    override fun createSymlink(source: Path, target: Path) {
        throw UnsupportedOperationException("WasmJS 不支持符号链接")
    }

    override fun source(file: Path): Source {
        val key = file.toKey()
        // 核心修复：如果找不到数据，返回空 Buffer 而不是抛出 IOException
        // 这会让 Serializer 读到 0 字节并返回默认值，从而正常初始化 DataStore
        val base64Data = localStorage.getItem(key) ?: return Buffer() 
        
        return try {
            val binaryString = kotlinx.browser.window.atob(base64Data)
            val bytes = ByteArray(binaryString.length) { i -> binaryString[i].code.toByte() }
            Buffer().write(bytes)
        } catch (e: Exception) {
            Buffer()
        }
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
                localStorage.setItem(file.toKey(), base64Data)
            }
            override fun timeout() = okio.Timeout.NONE
            override fun close() {
                flush()
            }
        }
    }

    override fun appendingSink(file: Path, mustCreate: Boolean): Sink = throw UnsupportedOperationException("不支持追加")

    override fun createDirectory(dir: Path, mustCreate: Boolean) {}

    override fun atomicMove(source: Path, target: Path) {
        val sourceKey = source.toKey()
        val targetKey = target.toKey()
        val data = localStorage.getItem(sourceKey)
        if (data != null) {
            localStorage.setItem(targetKey, data)
            localStorage.removeItem(sourceKey)
        }
    }

    override fun delete(path: Path, mustCreate: Boolean) {
        localStorage.removeItem(path.toKey())
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
            producePath = { 
                val absolutePath = if (fileName.startsWith("/")) fileName else "/$fileName"
                absolutePath.toPath() 
            }
        )
    )
}