package me.voltual.pyrolysis.core.proto

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Storage
import androidx.datastore.core.StorageConnection
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.Buffer

/**
 * WasmJS 环境下基于浏览器 localStorage 的 DataStore 存储驱动。
 */
class LocalStorageStorage(
    private val key: String,
    private val serializer: UserCredentialsSerializer
) : Storage<UserCredentials> {
    override fun createConnection(): StorageConnection<UserCredentials> {
        return LocalStorageConnection(key, serializer)
    }
}

class LocalStorageConnection(
    private val key: String,
    private val serializer: UserCredentialsSerializer
) : StorageConnection<UserCredentials> {
    private val mutex = Mutex()

    override suspend fun readData(): UserCredentials {
        return mutex.withLock {
            val base64Data = kotlinx.browser.localStorage.getItem(key) ?: ""
            if (base64Data.isEmpty()) {
                return@withLock serializer.defaultValue
            }
            try {
                val bytes = base64ToByteArray(base64Data)
                // 仅将 ByteArray 包装进 Buffer 以满足 DataStore 接口
                val buffer = Buffer().apply { write(bytes) }
                serializer.readFrom(buffer)
            } catch (e: Exception) {
                serializer.defaultValue
            }
        }
    }

    override suspend fun writeData(value: UserCredentials) {
        mutex.withLock {
            val buffer = Buffer()
            serializer.writeTo(value, buffer)
            // 从 Buffer 中直接取出加密后的 ByteArray
            val bytes = buffer.readByteArray()
            val base64Data = byteArrayToBase64(bytes)
            kotlinx.browser.localStorage.setItem(key, base64Data)
        }
    }

    override fun close() {}

    private fun base64ToByteArray(base64: String): ByteArray {
        val binaryString = kotlinx.browser.window.atob(base64)
        val bytes = ByteArray(binaryString.length)
        for (i in binaryString.indices) {
            bytes[i] = binaryString[i].code.toByte()
        }
        return bytes
    }

    private fun byteArrayToBase64(bytes: ByteArray): String {
        val chars = CharArray(bytes.size) { i ->
            bytes[i].toInt().and(0xff).toChar()
        }
        val binaryString = String(chars)
        return kotlinx.browser.window.btoa(binaryString)
    }
}

actual fun createDataStore(
    serializer: UserCredentialsSerializer,
    context: Any?
): DataStore<UserCredentials> {
    return DataStoreFactory.create(
        storage = LocalStorageStorage("user_credentials_secure", serializer)
    )
}